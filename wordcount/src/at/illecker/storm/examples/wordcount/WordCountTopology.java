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

import at.illecker.storm.examples.util.spout.TwitterStreamSpout;
import at.illecker.storm.examples.wordcount.bolt.ReportWordCountBolt;
import at.illecker.storm.examples.wordcount.bolt.SplitTweetBolt;
import at.illecker.storm.examples.wordcount.bolt.WordCountBolt;
import at.illecker.storm.examples.wordcount.spout.SampleTweetSpout;
import backtype.storm.Config;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.IRichSpout;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.tuple.Fields;

public class WordCountTopology {
  private static final String TOPOLOGY_NAME = "word-count-topology";
  private static final int REPORT_PERIOD = 10000;
  private static final String FILTER_LANG = "en";

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
    String spoutID;
    if (consumerKey.isEmpty()) {
      spout = new SampleTweetSpout(new String[] { "tweet" });
      spoutID = SampleTweetSpout.ID;
    } else {
      spout = new TwitterStreamSpout(new String[] { "tweet" }, consumerKey,
          consumerSecret, accessToken, accessTokenSecret, keyWords, FILTER_LANG);
      spoutID = TwitterStreamSpout.ID;
    }

    // Create Bolts
    SplitTweetBolt splitTweetBolt = new SplitTweetBolt(
        new String[] { "tweet" }, new String[] { "word" });
    WordCountBolt wordCountBolt = new WordCountBolt(new String[] { "word" },
        new String[] { "word", "count" });
    ReportWordCountBolt reportWordCountBolt = new ReportWordCountBolt(
        new String[] { "word", "count" }, null, REPORT_PERIOD);

    // Create Topology
    TopologyBuilder builder = new TopologyBuilder();

    // Set Spout
    builder.setSpout(spoutID, spout);

    // Set Spout --> SplitTweetBolt
    builder.setBolt(SplitTweetBolt.ID, splitTweetBolt).shuffleGrouping(spoutID);

    // Set SplitTweetBolt --> WordCountBolt
    builder.setBolt(WordCountBolt.ID, wordCountBolt).fieldsGrouping(
        SplitTweetBolt.ID, new Fields("word"));

    // Set WordCountBolt --> ReportWordCountBolt
    builder.setBolt(ReportWordCountBolt.ID, reportWordCountBolt)
        .globalGrouping(WordCountBolt.ID);

    Config conf = new Config();
    StormSubmitter
        .submitTopology(TOPOLOGY_NAME, conf, builder.createTopology());

    System.out.println("To kill the topology run:");
    System.out.println("storm kill " + TOPOLOGY_NAME);
  }
}
