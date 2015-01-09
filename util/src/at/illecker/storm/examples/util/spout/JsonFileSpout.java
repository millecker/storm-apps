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
package at.illecker.storm.examples.util.spout;

import java.io.File;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.io.JsonUtils;
import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;

public class JsonFileSpout extends BaseRichSpout {
  public static final String ID = "json-file-spout";
  public static final String CONF_JSON_FILE = "json.file";
  private static final long serialVersionUID = -566909258413711921L;
  private static final Logger LOG = LoggerFactory
      .getLogger(JsonFileSpout.class);

  private SpoutOutputCollector m_collector;
  private List<Map<String, Object>> m_elements;
  private int m_index = 0;

  public JsonFileSpout() {
  }

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declare(new Fields("jsonElement")); // key of output tuples
  }

  public void open(Map config, TopologyContext context,
      SpoutOutputCollector collector) {
    this.m_collector = collector;

    if (config.get(CONF_JSON_FILE) != null) {
      String jsonFilePath = config.get(CONF_JSON_FILE).toString();
      File jsonFile = new File(jsonFilePath);
      if (jsonFile.isFile()) {
        m_elements = JsonUtils.readJsonFile(jsonFile);
      } else {
        throw new RuntimeException("Error reading directory " + jsonFile);
      }
    } else {
      throw new RuntimeException(CONF_JSON_FILE + " property was not set!");
    }
  }

  public void nextTuple() {
    this.m_collector.emit(new Values(m_elements.get(m_index)));
    m_index++;
    if (m_index >= m_elements.size()) {
      m_index = 0;
    }
    try {
      // TODO minimize sleep time
      // default sleep 1 ms
      Thread.sleep(1000); // for development
    } catch (InterruptedException e) {
      LOG.error(e.getMessage());
    }
  }
}
