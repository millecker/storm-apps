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
package at.illecker.storm.postagger;

import java.io.File;
import java.util.Arrays;

import at.illecker.storm.commons.bolt.POSTaggerBolt;
import at.illecker.storm.commons.bolt.PreprocessorBolt;
import at.illecker.storm.commons.bolt.TokenizerBolt;
import at.illecker.storm.commons.spout.TwitterFilesSpout;
import at.illecker.storm.commons.spout.TwitterStreamSpout;
import backtype.storm.Config;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.IRichSpout;
import backtype.storm.topology.TopologyBuilder;

public class POSTaggerTopology {
  public static final String TOPOLOGY_NAME = "pos-tagger-topology";
  public static final String FILTER_LANG = "en";

  public static void main(String[] args) throws Exception {
    String twitterDirOrConsumerKey = "";
    String consumerSecret = "";
    String accessToken = "";
    String accessTokenSecret = "";
    String[] keyWords = null;

    if (args.length > 0) {
      if (args.length >= 1) {
        twitterDirOrConsumerKey = args[0];
        if (args.length >= 4) {
          consumerSecret = args[1];
          accessToken = args[2];
          accessTokenSecret = args[3];
          if (args.length == 5) {
            keyWords = args[4].split(" ");
            System.out.println("TwitterSpout using KeyWords: "
                + Arrays.toString(keyWords));
          }
        }
      }
    } else {
      System.out.println("Wrong argument size!");
      System.out.println("    Argument1=[twitterDir|consumerKey]");
      System.out.println("    Argument2=consumerSecret");
      System.out.println("    Argument3=accessToken");
      System.out.println("    Argument4=accessTokenSecret");
      System.out.println("    [Argument5=keyWords]");
    }

    // Check twitterDir and consumerKey
    File twitterDir = new File(twitterDirOrConsumerKey);
    if ((!twitterDir.isDirectory())
        && ((consumerSecret.isEmpty()) || (accessToken.isEmpty()) || (accessTokenSecret
            .isEmpty()))) {
      System.out
          .println("TwitterDirectory does not exist and consumerSecret, accessToken or accessTokenSecret is empty!");
      System.exit(1);
    }

    Config conf = new Config();

    // Create Spout
    IRichSpout spout;
    String spoutID = "";
    if (twitterDir.isDirectory()) {
      conf.put(TwitterFilesSpout.CONF_TWITTER_DIR, twitterDir.getAbsolutePath());
      // sleep 500 ms between emitting tuples
      conf.put(TwitterFilesSpout.CONF_TUPLE_SLEEP_MS, 500);
      spout = new TwitterFilesSpout(new String[] { "tweet" }, FILTER_LANG);
      spoutID = TwitterFilesSpout.ID;
    } else {
      spout = new TwitterStreamSpout(new String[] { "tweet" },
          twitterDirOrConsumerKey, consumerSecret, accessToken,
          accessTokenSecret, keyWords, FILTER_LANG);
      spoutID = TwitterStreamSpout.ID;
    }

    // Create Bolts
    TokenizerBolt tokenizerBolt = new TokenizerBolt(new String[] { "tweet" },
        new String[] { "splittedTweet" });
    PreprocessorBolt preprocessorBolt = new PreprocessorBolt(
        new String[] { "splittedTweet" }, new String[] { "preprocessedTweet" });
    POSTaggerBolt posTaggerBolt = new POSTaggerBolt(
        new String[] { "preprocessedTweet" }, new String[] { "taggedTweet" });

    // Create Topology
    TopologyBuilder builder = new TopologyBuilder();

    // Set Spout
    builder.setSpout(spoutID, spout);

    // Set Spout --> TokenizerBolt
    builder.setBolt(TokenizerBolt.ID, tokenizerBolt).shuffleGrouping(spoutID);

    // TokenizerBolt --> PreprocessorBolt
    builder.setBolt(PreprocessorBolt.ID, preprocessorBolt).shuffleGrouping(
        TokenizerBolt.ID);

    // PreprocessorBolt --> POSTaggerBolt
    builder.setBolt(POSTaggerBolt.ID, posTaggerBolt).shuffleGrouping(
        PreprocessorBolt.ID);

    conf.put(Config.WORKER_CHILDOPTS, "-Xmx16g");
    conf.put(Config.SUPERVISOR_CHILDOPTS, "-Xmx2g");

    // Enable logging in POSTaggerBolt
    conf.put(POSTaggerBolt.CONF_LOGGING, true);

    StormSubmitter
        .submitTopology(TOPOLOGY_NAME, conf, builder.createTopology());

    System.out.println("To kill the topology run:");
    System.out.println("storm kill " + TOPOLOGY_NAME);
  }
}
