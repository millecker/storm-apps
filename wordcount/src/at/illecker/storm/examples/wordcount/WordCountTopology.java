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

import java.util.Arrays;

import at.illecker.storm.examples.wordcount.bolt.ReportWordCountBolt;
import at.illecker.storm.examples.wordcount.bolt.SplitTweetBolt;
import at.illecker.storm.examples.wordcount.bolt.WordCountBolt;
import at.illecker.storm.examples.wordcount.spout.SampleTweetSpout;
import at.illecker.storm.examples.wordcount.spout.TwitterSpout;
import backtype.storm.Config;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.IRichSpout;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;

public class WordCountTopology {

  private static final String TWEET_SPOUT_ID = "tweet-spout";
  private static final String SPLIT_BOLT_ID = "split-bolt";
  private static final String COUNT_BOLT_ID = "count-bolt";
  private static final String REPORT_BOLT_ID = "report-bolt";
  private static final String TOPOLOGY_NAME = "word-count-topology";
  private static final int REPORT_PERIOD = 1000;

  public static void main(String[] args) throws Exception {
    String consumerKey = "";
    String consumerSecret = "";
    String accessToken = "";
    String accessTokenSecret = "";
    String[] keyWords = null;

    if (args.length > 0) {
      if (args.length >= 4) {
        consumerKey = args[0];
        System.out.println("TwitterSpout using ConsumerKey: " + consumerKey);
        consumerSecret = args[1];
        accessToken = args[2];
        accessTokenSecret = args[3];
        if (args.length == 5) {
          keyWords = args[4].split(" ");
          System.out.println("TwitterSpout using KeyWords: "
              + Arrays.toString(keyWords));
        }
      } else {
        System.out.println("Wrong argument size!");
        System.out.println("    Argument1=consumerKey");
        System.out.println("    Argument2=consumerSecret");
        System.out.println("    Argument3=accessToken");
        System.out.println("    Argument4=accessTokenSecret");
        System.out.println("    [Argument5=keyWords]");
      }
    }

    // Create Spout
    IRichSpout spout;
    if (consumerKey.isEmpty()) {
      spout = new SampleTweetSpout();
    } else {
      spout = new TwitterSpout(consumerKey, consumerSecret, accessToken,
          accessTokenSecret, keyWords);
    }

    // Create Bolts
    SplitTweetBolt splitBolt = new SplitTweetBolt();
    WordCountBolt countBolt = new WordCountBolt();
    ReportWordCountBolt reportBolt = new ReportWordCountBolt(REPORT_PERIOD);

    // Create Topology
    TopologyBuilder builder = new TopologyBuilder();
    // Set Spout
    builder.setSpout(TWEET_SPOUT_ID, spout);
    // Set Spout --> SplitTweetBolt
    builder.setBolt(SPLIT_BOLT_ID, splitBolt).shuffleGrouping(TWEET_SPOUT_ID);
    // Set SplitTweetBolt --> WordCountBolt
    builder.setBolt(COUNT_BOLT_ID, countBolt).fieldsGrouping(SPLIT_BOLT_ID,
        new Fields("word"));
    // Set WordCountBolt --> ReportWordCountBolt
    builder.setBolt(REPORT_BOLT_ID, reportBolt).globalGrouping(COUNT_BOLT_ID);

    Config conf = new Config();
    StormSubmitter
        .submitTopology(TOPOLOGY_NAME, conf, builder.createTopology());

    System.out.println("To kill the topology run:");
    System.out.println("storm kill " + TOPOLOGY_NAME);
  }
}
