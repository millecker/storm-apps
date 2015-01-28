/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package at.illecker.storm.commons.spout;

import java.util.List;
import java.util.Map;

import at.illecker.storm.commons.util.TimeUtils;
import at.illecker.storm.commons.util.io.JsonUtils;
import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

public class JsonFileSpout extends BaseRichSpout {
  public static final String ID = "json-file-spout";
  public static final String CONF_JSON_FILE = ID + ".json.file";
  public static final String CONF_STARTUP_SLEEP_MS = ID + ".startup.sleep.ms";
  public static final String CONF_TUPLE_SLEEP_MS = ID + ".tuple.sleep.ms";
  private static final long serialVersionUID = -4355637229662565251L;
  private String[] m_outputFields;
  private SpoutOutputCollector m_collector;
  private List<Map<String, Object>> m_elements;
  private int m_index = 0;
  private long m_tupleSleepMs = 0;

  public JsonFileSpout(String[] outputFields) {
    this.m_outputFields = outputFields;
  }

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    // key of output tuples
    declarer.declare(new Fields(m_outputFields));
  }

  public void open(Map config, TopologyContext context,
      SpoutOutputCollector collector) {
    this.m_collector = collector;

    if (config.get(CONF_JSON_FILE) != null) {
      String jsonFilePath = config.get(CONF_JSON_FILE).toString();
      m_elements = JsonUtils.readJsonFile(jsonFilePath);
    } else {
      throw new RuntimeException(CONF_JSON_FILE + " property was not set!");
    }

    // Optional sleep between tuples emitting
    if (config.get(CONF_TUPLE_SLEEP_MS) != null) {
      m_tupleSleepMs = (Long) config.get(CONF_TUPLE_SLEEP_MS);
    } else {
      m_tupleSleepMs = 0;
    }

    // Optional startup sleep to finish bolt preparation
    // before spout starts emitting
    if (config.get(CONF_STARTUP_SLEEP_MS) != null) {
      long startupSleepMillis = (Long) config.get(CONF_STARTUP_SLEEP_MS);
      TimeUtils.sleepMillis(startupSleepMillis);
    }
  }

  public void nextTuple() {
    this.m_collector.emit(new Values(m_elements.get(m_index)));

    // index is used to endless loop within the collection
    m_index++;
    if (m_index >= m_elements.size()) {
      m_index = 0;
    }

    // Optional sleep between emitting tuples
    if (m_tupleSleepMs != 0) {
      TimeUtils.sleepMillis(m_tupleSleepMs);
    }
  }
}
