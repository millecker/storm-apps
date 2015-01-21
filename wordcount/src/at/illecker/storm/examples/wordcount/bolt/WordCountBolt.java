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
package at.illecker.storm.examples.wordcount.bolt;

import java.util.HashMap;
import java.util.Map;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class WordCountBolt extends BaseRichBolt {
  public static final String ID = "word-count-bolt";
  private static final long serialVersionUID = -1587421475240637474L;
  private String[] m_inputFields;
  private String[] m_outputFields;
  private OutputCollector m_collector;
  private HashMap<String, Long> m_counts = null;

  public WordCountBolt(String[] inputFields, String[] outputFields) {
    this.m_inputFields = inputFields;
    this.m_outputFields = outputFields;
  }

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declare(new Fields(m_outputFields)); // keys of output tuples
  }

  public void prepare(Map config, TopologyContext context,
      OutputCollector collector) {
    this.m_collector = collector;
    this.m_counts = new HashMap<String, Long>();
  }

  public void execute(Tuple tuple) {
    String word = tuple.getStringByField(m_inputFields[0]);
    Long count = this.m_counts.get(word);
    if (count == null) {
      count = 0L;
    }
    count++;
    this.m_counts.put(word, count);
    this.m_collector.ack(tuple);
    this.m_collector.emit(tuple, new Values(word, count));
  }
}
