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
package at.illecker.storm.examples.ngram;

import java.io.File;
import java.util.Arrays;

import at.illecker.storm.examples.ngram.bolt.BiGramBolt;
import at.illecker.storm.examples.util.spout.TwitterFilesSpout;
import at.illecker.storm.examples.util.spout.TwitterSpout;
import backtype.storm.Config;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.IRichSpout;
import backtype.storm.topology.TopologyBuilder;

public class NGramTopology {

  private static final String TWEET_SPOUT_ID = "tweet-spout";
  private static final String BI_GRAM_BOLT_ID = "bi-gram-bolt";
  private static final String TOPOLOGY_NAME = "n-gram-topology";
  public static final String FILTER_LANG = "en";

  public static void main(String[] args) throws Exception {
    String twitterDirPath = "";
    String consumerKey = "";
    String consumerSecret = "";
    String accessToken = "";
    String accessTokenSecret = "";
    String[] keyWords = null;

    if (args.length > 0) {
      if (args.length >= 1) {
        twitterDirPath = args[0];
        if (args.length >= 5) {
          consumerKey = args[1];
          System.out.println("TwitterSpout using ConsumerKey: " + consumerKey);
          consumerSecret = args[2];
          accessToken = args[3];
          accessTokenSecret = args[4];
          if (args.length == 6) {
            keyWords = args[5].split(" ");
            System.out.println("TwitterSpout using KeyWords: "
                + Arrays.toString(keyWords));
          }
        }
      }
    } else {
      System.out.println("Wrong argument size!");
      System.out.println("    Argument1=twitterDir");
      System.out.println("    Argument2=consumerKey");
      System.out.println("    Argument3=consumerSecret");
      System.out.println("    Argument4=accessToken");
      System.out.println("    Argument5=accessTokenSecret");
      System.out.println("    [Argument6=keyWords]");
    }

    // Check twitterDir and consumerKey
    File twitterDir = new File(twitterDirPath);
    if ((!twitterDir.isDirectory()) && (consumerKey.isEmpty())) {
      System.out
          .println("TwitterDirectory does not exist and consumerKey is empty!");
      System.exit(1);
    }

    Config conf = new Config();

    // Create Spout
    IRichSpout spout;
    if (twitterDir.isDirectory()) {
      conf.put(TwitterFilesSpout.CONF_TWITTER_DIR, twitterDir.getAbsolutePath());
      spout = new TwitterFilesSpout(FILTER_LANG);
    } else {
      spout = new TwitterSpout(consumerKey, consumerSecret, accessToken,
          accessTokenSecret, keyWords, FILTER_LANG);
    }

    // Create Bolts
    BiGramBolt biGramBolt = new BiGramBolt();

    // Create Topology
    TopologyBuilder builder = new TopologyBuilder();
    // Set Spout
    builder.setSpout(TWEET_SPOUT_ID, spout);
    // Set Spout --> BiGramBolt
    builder.setBolt(BI_GRAM_BOLT_ID, biGramBolt)
        .shuffleGrouping(TWEET_SPOUT_ID);

    StormSubmitter
        .submitTopology(TOPOLOGY_NAME, conf, builder.createTopology());

    System.out.println("To kill the topology run:");
    System.out.println("storm kill " + TOPOLOGY_NAME);
  }
}
