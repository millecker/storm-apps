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
package at.illecker.storm.examples.wordcount;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;

public class ReportWordCountBolt extends BaseRichBolt {
  private OutputCollector m_collector;
  private HashMap<String, Long> m_counts = null;

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    // this bolt does not emit anything
  }

  public void prepare(Map config, TopologyContext context,
      OutputCollector collector) {
    this.m_collector = collector;
    this.m_counts = new HashMap<String, Long>();
  }

  public void execute(Tuple tuple) {
    String word = tuple.getStringByField("word");
    Long count = tuple.getLongByField("count");
    this.m_counts.put(word, count);
    this.m_collector.ack(tuple);
  }

  public void cleanup() {
    System.out.println("Final WordCounts");
    List<String> keys = new ArrayList<String>();
    keys.addAll(this.m_counts.keySet());
    Collections.sort(keys);
    for (String key : keys) {
      System.out.println(key + " : " + this.m_counts.get(key));
    }
  }
}
