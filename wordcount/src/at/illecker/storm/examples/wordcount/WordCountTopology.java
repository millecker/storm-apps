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

import backtype.storm.Config;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;

public class WordCountTopology {

  private static final String SENTENCE_SPOUT_ID = "tweet-spout";
  private static final String SPLIT_BOLT_ID = "split-bolt";
  private static final String COUNT_BOLT_ID = "count-bolt";
  private static final String REPORT_BOLT_ID = "report-bolt";
  private static final String TOPOLOGY_NAME = "word-count-topology";

  public static void main(String[] args) throws Exception {

    TweetSpout spout = new TweetSpout();
    SplitTweetBolt splitBolt = new SplitTweetBolt();
    WordCountBolt countBolt = new WordCountBolt();
    ReportWordCountBolt reportBolt = new ReportWordCountBolt();

    TopologyBuilder builder = new TopologyBuilder();

    // TweetSpout
    builder.setSpout(SENTENCE_SPOUT_ID, spout);
    // TweetSpout --> SplitTweetBolt
    builder.setBolt(SPLIT_BOLT_ID, splitBolt)
        .shuffleGrouping(SENTENCE_SPOUT_ID);
    // SplitTweetBolt --> WordCountBolt
    builder.setBolt(COUNT_BOLT_ID, countBolt).fieldsGrouping(SPLIT_BOLT_ID,
        new Fields("word"));
    // WordCountBolt --> ReportWordCountBolt
    builder.setBolt(REPORT_BOLT_ID, reportBolt).globalGrouping(COUNT_BOLT_ID);

    Config conf = new Config();
    StormSubmitter
        .submitTopology(TOPOLOGY_NAME, conf, builder.createTopology());

    System.out.println("To kill the topology run:");
    System.out.println("storm kill " + TOPOLOGY_NAME);
  }
}
