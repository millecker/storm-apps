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
package at.illecker.storm.examples.sentimentanalysis;

import java.io.File;
import java.util.Arrays;

import at.illecker.storm.examples.sentimentanalysis.bolt.POSTaggerBolt;
import at.illecker.storm.examples.sentimentanalysis.bolt.PolarityDetectionBolt;
import at.illecker.storm.examples.sentimentanalysis.bolt.SentenceSplitterBolt;
import at.illecker.storm.examples.sentimentanalysis.bolt.TweetFeatureExtractorBolt;
import at.illecker.storm.examples.util.spout.JsonFileSpout;
import at.illecker.storm.examples.util.spout.TwitterSpout;
import backtype.storm.Config;
import backtype.storm.StormSubmitter;
import backtype.storm.topology.IRichSpout;
import backtype.storm.topology.TopologyBuilder;

/*
 * Sentiment Analysis Topology
 * 
 * JSON Spout -> TweetFeatureExtractorBolt -> POSTaggerBolt 
 * -> PolarityDetectorBolt -> NGramBolt -> EmoticonsBolt
 * 
 */
public class SentimentAnalysisTopology {

  private static final String TWEET_SPOUT_ID = "tweet-spout";
  private static final String TWEET_FEATURE_EXTRACTOR_BOLT_ID = "tweet-feature-extractor-bolt";
  private static final String SENTENCE_SPLITTER_BOLT_ID = "sentence-splitter-bolt";
  private static final String POS_TAGGER_BOLT_ID = "pos-tagger-bolt";
  private static final String POLARITY_DETECTION_BOLT_ID = "polarity-detection-bolt";
  private static final String TOPOLOGY_NAME = "sentiment-analysis-topology";

  public static final String FILTER_LANG = "en";
  public static final String POS_TAGGER_MODEL = "resources/gate-EN-twitter-fast.model";
  public static final String SENTIMENT_WORD_LIST = "resources/AFINN-111.txt";

  public static void main(String[] args) throws Exception {
    String referenceFilePath = "";
    String consumerKey = "";
    String consumerSecret = "";
    String accessToken = "";
    String accessTokenSecret = "";
    String[] keyWords = null;

    if (args.length > 0) {
      if (args.length >= 1) {
        referenceFilePath = args[0];
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
      System.out.println("    Argument1=referenceFile");
      System.out.println("    Argument2=consumerKey");
      System.out.println("    Argument3=consumerSecret");
      System.out.println("    Argument4=accessToken");
      System.out.println("    Argument5=accessTokenSecret");
      System.out.println("    [Argument6=keyWords]");
    }

    // Check twitterDir and consumerKey
    File referenceFile = new File(referenceFilePath);
    if ((!referenceFile.isFile()) && (consumerKey.isEmpty())) {
      System.out
          .println("ReferenceFile does not exist and consumerKey is empty!");
      System.exit(1);
    }

    Config conf = new Config();
    conf.put(POSTaggerBolt.CONF_TAGGER_MODEL_FILE, POS_TAGGER_MODEL);
    conf.put(PolarityDetectionBolt.CONF_WORD_LIST_FILE, SENTIMENT_WORD_LIST);

    // Create Spout
    IRichSpout spout;
    if (referenceFile.isFile()) {
      conf.put(JsonFileSpout.CONF_JSON_FILE, referenceFile.getAbsolutePath());
      spout = new JsonFileSpout();
    } else {
      spout = new TwitterSpout(consumerKey, consumerSecret, accessToken,
          accessTokenSecret, keyWords, FILTER_LANG);
    }

    // Create Bolts
    TweetFeatureExtractorBolt tweetFeatureExtractorBolt = new TweetFeatureExtractorBolt();
    SentenceSplitterBolt sentenceSplitterBolt = new SentenceSplitterBolt();
    POSTaggerBolt posTaggerBolt = new POSTaggerBolt();
    PolarityDetectionBolt polarityDetectionBolt = new PolarityDetectionBolt();

    // Create Topology
    TopologyBuilder builder = new TopologyBuilder();

    // Set Spout
    builder.setSpout(TWEET_SPOUT_ID, spout);

    // Set Spout --> TweetFeatureExtractorBolt
    builder.setBolt(TWEET_FEATURE_EXTRACTOR_BOLT_ID, tweetFeatureExtractorBolt)
        .shuffleGrouping(TWEET_SPOUT_ID);

    // TweetFeatureExtractorBolt --> SentenceSplitterBolt
    builder.setBolt(SENTENCE_SPLITTER_BOLT_ID, sentenceSplitterBolt)
        .shuffleGrouping(TWEET_FEATURE_EXTRACTOR_BOLT_ID);

    // SentenceSplitterBolt --> POSTaggerBolt
    builder.setBolt(POS_TAGGER_BOLT_ID, posTaggerBolt).shuffleGrouping(
        SENTENCE_SPLITTER_BOLT_ID);

    // POSTaggerBolt --> PolarityDetectionBolt
    builder.setBolt(POLARITY_DETECTION_BOLT_ID, polarityDetectionBolt)
        .shuffleGrouping(POS_TAGGER_BOLT_ID);

    StormSubmitter
        .submitTopology(TOPOLOGY_NAME, conf, builder.createTopology());

    System.out.println("To kill the topology run:");
    System.out.println("storm kill " + TOPOLOGY_NAME);
  }
}
