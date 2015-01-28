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
package at.illecker.storm.sentimentanalysis.svm;

import java.util.Arrays;

import at.illecker.storm.commons.Configuration;
import at.illecker.storm.commons.Dataset;
import at.illecker.storm.commons.bolt.FeatureGenerationBolt;
import at.illecker.storm.commons.bolt.POSTaggerBolt;
import at.illecker.storm.commons.bolt.PreprocessorBolt;
import at.illecker.storm.commons.bolt.SVMBolt;
import at.illecker.storm.commons.bolt.TokenizerBolt;
import at.illecker.storm.commons.spout.DatasetSpout;
import at.illecker.storm.commons.spout.TwitterStreamSpout;
import backtype.storm.Config;
import backtype.storm.StormSubmitter;
import backtype.storm.metric.LoggingMetricsConsumer;
import backtype.storm.topology.IRichSpout;
import backtype.storm.topology.TopologyBuilder;

public class SentimentAnalysisSVMTopology {
  public static final String TOPOLOGY_NAME = "sentiment-analysis-svm-topology";
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

    // Dataset SemEval2013
    Dataset dataset = Configuration.getDataSetSemEval2013();

    // Create Spout
    IRichSpout spout;
    String spoutID = "";
    if (consumerKey.isEmpty()) {
      spout = new DatasetSpout(new String[] { "tweet" }, dataset);
      spoutID = DatasetSpout.ID;
    } else {
      spout = new TwitterStreamSpout(new String[] { "tweet" }, consumerKey,
          consumerSecret, accessToken, accessTokenSecret, keyWords, FILTER_LANG);
      spoutID = TwitterStreamSpout.ID;
    }

    // Create Bolts
    TokenizerBolt tokenizerBolt = new TokenizerBolt(new String[] { "tweet" },
        new String[] { "splittedTweet" });
    PreprocessorBolt preprocessorBolt = new PreprocessorBolt(
        new String[] { "splittedTweet" }, new String[] { "preprocessedTweet" });
    POSTaggerBolt posTaggerBolt = new POSTaggerBolt(
        new String[] { "preprocessedTweet" }, new String[] { "taggedTweet" });
    FeatureGenerationBolt featureGenerationBolt = new FeatureGenerationBolt(
        new String[] { "taggedTweet" }, new String[] { "featuredTweet" },
        dataset);
    SVMBolt svmBolt = new SVMBolt(new String[] { "featuredTweet" }, null,
        dataset);

    // Create Topology
    TopologyBuilder builder = new TopologyBuilder();
    int numberOfWorkers = 2;
    // Parallelism assumptions:
    // One worker per node and 16 cores per node (12 Java threads per node)
    // 15 Executors per each worker
    // - Spout 1 thread
    // - TokenizerBolt 2 threads
    // - PreprocessorBolt 1 thread
    // - POSTaggerBolt 5 threads and 2 tasks per thread
    // - FeatureGenerationBolt 1 thread
    // - SVMBolt 5 threads

    // Set Spout
    builder.setSpout(spoutID, spout);

    // Set Spout --> TokenizerBolt
    builder.setBolt(TokenizerBolt.ID, tokenizerBolt, numberOfWorkers * 2)
        .shuffleGrouping(spoutID);

    // TokenizerBolt --> PreprocessorBolt
    builder.setBolt(PreprocessorBolt.ID, preprocessorBolt, numberOfWorkers)
        .shuffleGrouping(TokenizerBolt.ID);

    // PreprocessorBolt --> POSTaggerBolt
    builder.setBolt(POSTaggerBolt.ID, posTaggerBolt, numberOfWorkers * 5)
        .setNumTasks(numberOfWorkers * 5 * 2)
        .shuffleGrouping(PreprocessorBolt.ID);

    // POSTaggerBolt --> FeatureGenerationBolt
    builder.setBolt(FeatureGenerationBolt.ID, featureGenerationBolt,
        numberOfWorkers).shuffleGrouping(POSTaggerBolt.ID);

    // FeatureGenerationBolt --> SVMBolt
    builder.setBolt(SVMBolt.ID, svmBolt, numberOfWorkers * 5).shuffleGrouping(
        FeatureGenerationBolt.ID);

    Config conf = new Config();
    conf.setNumWorkers(numberOfWorkers);

    // Set max pending tuples
    conf.setMaxSpoutPending(5000);

    // conf.put(Config.TOPOLOGY_RECEIVER_BUFFER_SIZE, 8);
    // conf.put(Config.TOPOLOGY_TRANSFER_BUFFER_SIZE, 32);
    // conf.put(Config.TOPOLOGY_EXECUTOR_RECEIVE_BUFFER_SIZE, 16384);
    // conf.put(Config.TOPOLOGY_EXECUTOR_SEND_BUFFER_SIZE, 16384);

    conf.put(Config.WORKER_CHILDOPTS, "-Xmx16g");
    conf.put(Config.SUPERVISOR_CHILDOPTS, "-Xmx2g");

    // This will simply log all Metrics received into
    // $STORM_HOME/logs/metrics.log on one or more worker nodes.
    conf.registerMetricsConsumer(LoggingMetricsConsumer.class, numberOfWorkers);

    StormSubmitter
        .submitTopology(TOPOLOGY_NAME, conf, builder.createTopology());

    System.out.println("To kill the topology run:");
    System.out.println("storm kill " + TOPOLOGY_NAME);
  }
}