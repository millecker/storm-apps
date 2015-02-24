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
import java.util.TreeMap;

import cmu.arktweetnlp.Tagger.TaggedToken;

import com.esotericsoftware.kryo.serializers.DefaultSerializers.TreeMapSerializer;

import at.illecker.storm.commons.Configuration;
import at.illecker.storm.commons.bolt.POSTaggerBolt;
import at.illecker.storm.commons.bolt.PreprocessorBolt;
import at.illecker.storm.commons.bolt.TokenizerBolt;
import at.illecker.storm.commons.kyro.TaggedTokenSerializer;
import at.illecker.storm.commons.spout.TwitterFilesSpout;
import at.illecker.storm.commons.spout.TwitterStreamSpout;
import backtype.storm.Config;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.IRichSpout;
import backtype.storm.topology.TopologyBuilder;

public class POSTaggerTopology {
  public static final String TOPOLOGY_NAME = "pos-tagger-topology";

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
      conf.put(TwitterFilesSpout.CONF_FILTER_LANGUAGE,
          Configuration.get("apps.postagger.spout.filter.language"));
      if (Configuration.get("apps.postagger.spout.startup.sleep.ms") != null) {
        conf.put(TwitterFilesSpout.CONF_STARTUP_SLEEP_MS,
            (Integer) Configuration
                .get("apps.postagger.spout.startup.sleep.ms"));
      }
      if (Configuration.get("apps.postagger.spout.tuple.sleep.ms") != null) {
        conf.put(TwitterFilesSpout.CONF_TUPLE_SLEEP_MS, (Integer) Configuration
            .get("apps.postagger.spout.startup.sleep.ms"));
      }
      spout = new TwitterFilesSpout();
      spoutID = TwitterFilesSpout.ID;

    } else {
      if (Configuration.get("apps.postagger.spout.startup.sleep.ms") != null) {
        conf.put(TwitterStreamSpout.CONF_STARTUP_SLEEP_MS,
            (Integer) Configuration
                .get("apps.postagger.spout.startup.sleep.ms"));
      }
      spout = new TwitterStreamSpout(twitterDirOrConsumerKey, consumerSecret,
          accessToken, accessTokenSecret, keyWords,
          (String) Configuration.get("apps.postagger.spout.filter.language"));
      spoutID = TwitterStreamSpout.ID;
    }

    // Create Bolts
    TokenizerBolt tokenizerBolt = new TokenizerBolt();
    PreprocessorBolt preprocessorBolt = new PreprocessorBolt();
    POSTaggerBolt posTaggerBolt = new POSTaggerBolt();

    // Create Topology
    TopologyBuilder builder = new TopologyBuilder();

    // Set Spout
    builder.setSpout(spoutID, spout,
        Configuration.get("apps.postagger.spout.parallelism", 1));

    // Set Spout --> TokenizerBolt
    builder.setBolt(TokenizerBolt.ID, tokenizerBolt,
        Configuration.get("apps.postagger.bolt.tokenizer.parallelism", 1))
        .shuffleGrouping(spoutID);

    // TokenizerBolt --> PreprocessorBolt
    builder.setBolt(PreprocessorBolt.ID, preprocessorBolt,
        Configuration.get("apps.postagger.bolt.preprocessor.parallelism", 1))
        .shuffleGrouping(TokenizerBolt.ID);

    // PreprocessorBolt --> POSTaggerBolt
    builder.setBolt(POSTaggerBolt.ID, posTaggerBolt,
        Configuration.get("apps.postagger.bolt.postagger.parallelism", 1))
        .shuffleGrouping(PreprocessorBolt.ID);

    // Set topology config
    conf.setNumWorkers(Configuration.get("apps.postagger.workers.num", 1));

    if (Configuration.get("apps.postagger.spout.max.pending") != null) {
      conf.setMaxSpoutPending((Integer) Configuration
          .get("apps.postagger.spout.max.pending"));
    }

    if (Configuration.get("apps.postagger.workers.childopts") != null) {
      conf.put(Config.WORKER_CHILDOPTS,
          Configuration.get("apps.postagger.workers.childopts"));
    }
    if (Configuration.get("apps.postagger.supervisor.childopts") != null) {
      conf.put(Config.SUPERVISOR_CHILDOPTS,
          Configuration.get("apps.postagger.supervisor.childopts"));
    }

    conf.put(TokenizerBolt.CONF_LOGGING,
        Configuration.get("apps.postagger.bolt.tokenizer.logging", false));
    conf.put(PreprocessorBolt.CONF_LOGGING,
        Configuration.get("apps.postagger.bolt.preprocessor.logging", false));
    conf.put(POSTaggerBolt.CONF_LOGGING,
        Configuration.get("apps.postagger.bolt.postagger.logging", false));
    conf.put(POSTaggerBolt.CONF_MODEL,
        Configuration.get("apps.postagger.bolt.postagger.model"));

    conf.put(Config.TOPOLOGY_FALL_BACK_ON_JAVA_SERIALIZATION, false);
    conf.registerSerialization(TaggedToken.class, TaggedTokenSerializer.class);
    conf.registerSerialization(TreeMap.class, TreeMapSerializer.class);

    StormSubmitter
        .submitTopology(TOPOLOGY_NAME, conf, builder.createTopology());

    System.out.println("To kill the topology run:");
    System.out.println("storm kill " + TOPOLOGY_NAME);
  }
}
