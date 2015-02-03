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
package at.illecker.storm.sentimentanalysis;

import java.util.Arrays;

import at.illecker.storm.commons.Configuration;
import at.illecker.storm.commons.bolt.POSTaggerBolt;
import at.illecker.storm.commons.bolt.PreprocessorBolt;
import at.illecker.storm.commons.bolt.SentimentDetectionBolt;
import at.illecker.storm.commons.bolt.TokenizerBolt;
import at.illecker.storm.commons.spout.DatasetSpout;
import at.illecker.storm.commons.spout.TwitterStreamSpout;
import backtype.storm.Config;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.IRichSpout;
import backtype.storm.topology.TopologyBuilder;

public class SentimentAnalysisTopology {
  public static final String TOPOLOGY_NAME = "sentiment-analysis-topology";

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

    Config conf = new Config();

    // Create Spout
    IRichSpout spout;
    String spoutID = "";
    if (consumerKey.isEmpty()) {
      if (Configuration.get("apps.sentiment.analysis.spout.startup.sleep.ms") != null) {
        conf.put(DatasetSpout.CONF_STARTUP_SLEEP_MS, (Integer) Configuration
            .get("apps.sentiment.analysis.spout.startup.sleep.ms"));
      }
      if (Configuration.get("apps.sentiment.analysis.spout.tuple.sleep.ms") != null) {
        conf.put(DatasetSpout.CONF_TUPLE_SLEEP_MS, (Integer) Configuration
            .get("apps.sentiment.analysis.spout.tuple.sleep.ms"));
      }
      if (Configuration.get("apps.sentiment.analysis.spout.tuple.sleep.ns") != null) {
        conf.put(DatasetSpout.CONF_TUPLE_SLEEP_NS, (Integer) Configuration
            .get("apps.sentiment.analysis.spout.tuple.sleep.ns"));
      }
      spout = new DatasetSpout(Configuration.getDataSetSemEval2013());
      spoutID = DatasetSpout.ID;
    } else {
      if (Configuration.get("apps.sentiment.analysis.spout.startup.sleep.ms") != null) {
        conf.put(TwitterStreamSpout.CONF_STARTUP_SLEEP_MS,
            (Integer) Configuration
                .get("apps.sentiment.analysis.spout.startup.sleep.ms"));
      }
      spout = new TwitterStreamSpout(consumerKey, consumerSecret, accessToken,
          accessTokenSecret, keyWords,
          (String) Configuration
              .get("apps.sentiment.analysis.spout.filter.language"));
      spoutID = TwitterStreamSpout.ID;
    }

    // Create Bolts
    TokenizerBolt tokenizerBolt = new TokenizerBolt();
    PreprocessorBolt preprocessorBolt = new PreprocessorBolt();
    POSTaggerBolt posTaggerBolt = new POSTaggerBolt();
    SentimentDetectionBolt sentimentDetectionBolt = new SentimentDetectionBolt();

    // Create Topology
    TopologyBuilder builder = new TopologyBuilder();

    // Set Spout
    builder.setSpout(spoutID, spout,
        Configuration.get("apps.sentiment.analysis.spout.parallelism", 1));

    // Set Spout --> TokenizerBolt
    builder.setBolt(
        TokenizerBolt.ID,
        tokenizerBolt,
        Configuration.get("apps.sentiment.analysis.bolt.tokenizer.parallelism",
            1)).shuffleGrouping(spoutID);

    // TokenizerBolt --> PreprocessorBolt
    builder.setBolt(
        PreprocessorBolt.ID,
        preprocessorBolt,
        Configuration.get(
            "apps.sentiment.analysis.bolt.preprocessor.parallelism", 1))
        .shuffleGrouping(TokenizerBolt.ID);

    // PreprocessorBolt --> POSTaggerBolt
    builder.setBolt(
        POSTaggerBolt.ID,
        posTaggerBolt,
        Configuration.get("apps.sentiment.analysis.bolt.postagger.parallelism",
            1)).shuffleGrouping(PreprocessorBolt.ID);

    // POSTaggerBolt --> SentimentDetectionBolt
    builder.setBolt(
        SentimentDetectionBolt.ID,
        sentimentDetectionBolt,
        Configuration.get(
            "apps.sentiment.analysis.bolt.sentimentdetection.parallelism", 1))
        .shuffleGrouping(POSTaggerBolt.ID);

    // Set topology config
    conf.setNumWorkers(Configuration.get("apps.sentiment.analysis.workers.num",
        1));

    if (Configuration.get("apps.sentiment.analysis.spout.max.pending") != null) {
      conf.setMaxSpoutPending((Integer) Configuration
          .get("apps.sentiment.analysis.spout.max.pending"));
    }

    if (Configuration.get("apps.sentiment.analysis.workers.childopts") != null) {
      conf.put(Config.WORKER_CHILDOPTS,
          Configuration.get("apps.sentiment.analysis.workers.childopts"));
    }
    if (Configuration.get("apps.sentiment.analysis.supervisor.childopts") != null) {
      conf.put(Config.SUPERVISOR_CHILDOPTS,
          Configuration.get("apps.sentiment.analysis.supervisor.childopts"));
    }

    conf.put(TokenizerBolt.CONF_LOGGING, Configuration.get(
        "apps.sentiment.analysis.bolt.tokenizer.logging", false));
    conf.put(PreprocessorBolt.CONF_LOGGING, Configuration.get(
        "apps.sentiment.analysis.bolt.preprocessor.logging", false));
    conf.put(POSTaggerBolt.CONF_LOGGING, Configuration.get(
        "apps.sentiment.analysis.bolt.postagger.logging", false));
    conf.put(POSTaggerBolt.CONF_MODEL,
        Configuration.get("apps.sentiment.analysis.bolt.postagger.model"));
    conf.put(SentimentDetectionBolt.CONF_LOGGING, Configuration.get(
        "apps.sentiment.analysis.bolt.sentimentdetection.logging", false));

    conf.put(Config.TOPOLOGY_FALL_BACK_ON_JAVA_SERIALIZATION, false);

    StormSubmitter
        .submitTopology(TOPOLOGY_NAME, conf, builder.createTopology());

    System.out.println("To kill the topology run:");
    System.out.println("storm kill " + TOPOLOGY_NAME);
  }

}
