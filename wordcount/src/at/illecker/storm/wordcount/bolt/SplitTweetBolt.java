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
package at.illecker.storm.wordcount.bolt;

import java.util.Map;

import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class SplitTweetBolt extends BaseRichBolt {
  public static final String ID = "split-tweet-bolt";
  private static final long serialVersionUID = 883934440113385476L;
  private OutputCollector m_collector;

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declare(new Fields("word")); // keys of output tuples
  }

  public void prepare(Map config, TopologyContext context,
      OutputCollector collector) {
    this.m_collector = collector;
  }

  public void execute(Tuple tuple) {
    String text = tuple.getStringByField("text");
    String[] tokens = text.split(" ");
    for (String token : tokens) {
      this.m_collector.emit(tuple, new Values(token));
    }
    this.m_collector.ack(tuple);
  }

}
