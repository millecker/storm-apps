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
package at.illecker.storm.examples.svm.bolt;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import libsvm.svm_model;
import libsvm.svm_parameter;
import libsvm.svm_problem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.Configuration;
import at.illecker.storm.examples.util.io.JsonUtils;
import at.illecker.storm.examples.util.io.SerializationUtils;
import at.illecker.storm.examples.util.svm.SupportVectorMachine;
import at.illecker.storm.examples.util.svm.classifier.DynamicScoreClassifier;
import at.illecker.storm.examples.util.svm.feature.SimpleFeatureVectorGenerator;
import at.illecker.storm.examples.util.tagger.POSTagger;
import at.illecker.storm.examples.util.tokenizer.Tokenizer;
import at.illecker.storm.examples.util.tweet.SentimentTweet;
import at.illecker.storm.examples.util.tweet.Tweet;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import edu.stanford.nlp.ling.TaggedWord;

public class SupportVectorMachineBolt extends BaseRichBolt {
  public static final Configuration CONFIG = Configuration.getInstance();
  public static final String DATASET_PATH = CONFIG.getDataSetPath()
      + "dataset2" + File.separator;
  public static final String DATA = DATASET_PATH
      + "mislove_1000tweets_with_sentistrength_and_afinn.json";
  public static final String DATA_SER_FILE = DATASET_PATH
      + "mislove_1000tweets.ser";

  private static final long serialVersionUID = -3393202292186510304L;
  private static final Logger LOG = LoggerFactory
      .getLogger(SupportVectorMachineBolt.class);
  private static final boolean LOGGING = true;

  private OutputCollector m_collector;
  private int m_totalClasses;
  private DynamicScoreClassifier m_dsc;
  private svm_model m_model;

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    // no output
  }

  public void prepare(Map config, TopologyContext context,
      OutputCollector collector) {
    this.m_collector = collector;

    List<SentimentTweet> tweets = null;
    InputStream is = null;
    try {

      if (CONFIG.isRunningWithinJar()) {
        LOG.info("Load tweets within jar: " + DATA_SER_FILE);
        is = ClassLoader.getSystemResourceAsStream(DATA_SER_FILE);
      } else {
        LOG.info("Load tweets from file: " + DATA_SER_FILE);
        is = new FileInputStream(DATA_SER_FILE);
      }
      LOG.info("Load tweets...");
      tweets = SerializationUtils.deserialize(is);

    } catch (FileNotFoundException e) {
      LOG.error(e.getMessage());
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException ignore) {
        }
      }
    }

    m_totalClasses = 5;
    m_dsc = new DynamicScoreClassifier(m_totalClasses, 1, 9);

    svm_parameter svmParam = SupportVectorMachine.getDefaultParameter();
    svm_problem svmProb = SupportVectorMachine.generateProblem(tweets, m_dsc);

    // TODO load model instead of training...

    LOG.info("Train model...");
    m_model = SupportVectorMachine.train(svmProb, svmParam);
  }

  public void execute(Tuple tuple) {
    Tweet tweet = (Tweet) tuple.getValueByField("taggedTweet");

    double predictedClass = SupportVectorMachine.evaluate(tweet, m_model,
        m_totalClasses, m_dsc);
    LOG.info(tweet.toString() + " - predictedClass: " + predictedClass);

    this.m_collector.ack(tuple);
  }

  public static void main(String[] args) {
    SimpleFeatureVectorGenerator sfvg = null;
    POSTagger posTagger = null;
    boolean parameterSearch = false;
    try {
      // Prepare tweets
      LOG.info("Prepare tweets data...");
      List<SentimentTweet> tweets = SerializationUtils
          .deserialize(DATA_SER_FILE);
      if (tweets == null) {
        // Generate feature vectors
        LOG.info("Load SimpleFeatureVectorGenerator...");
        sfvg = SimpleFeatureVectorGenerator.getInstance();

        // Load POS Tagger
        posTagger = POSTagger.getInstance();

        LOG.info("Read tweets from " + DATA);
        tweets = new ArrayList<SentimentTweet>();
        List<Map<String, Object>> jsonTweets = JsonUtils
            .readJsonStream(new FileInputStream(DATA));
        for (Map<String, Object> jsonElement : jsonTweets) {
          tweets.add(SentimentTweet.fromJsonElement(jsonElement));
        }

        LOG.info("Process tweets...");
        for (Tweet tweet : tweets) {
          // Tokenizer
          List<String> tokens = Tokenizer.tokenize(tweet.getText());

          // POS Tagging
          List<TaggedWord> taggedSentence = posTagger.tagSentence(tokens);
          tweet.addTaggedSentence(taggedSentence);

          if (LOGGING) {
            LOG.info("Tweet: " + tweet);
          }

          // Generate Feature Vector
          tweet.genFeatureVector(sfvg);
          if (LOGGING) {
            LOG.info("FeatureVector: "
                + Arrays.toString(tweet.getFeatureVector()));
          }
        }

        SerializationUtils.serializeList(tweets, DATA_SER_FILE);
      }

      int trainSize = Math.round(tweets.size() * 0.7f); // 70% training
      List<SentimentTweet> trainTweets = tweets.subList(0, trainSize);
      List<SentimentTweet> testTweets = tweets
          .subList(trainSize, tweets.size());

      int totalClasses = 5;
      DynamicScoreClassifier dsc = new DynamicScoreClassifier(totalClasses, 1,
          9);
      // 0 = extreme-negative
      // 1 = negative
      // 2 = neutral
      // 3 = positive
      // 4 = extreme-positive

      svm_parameter svmParam = SupportVectorMachine.getDefaultParameter();
      svm_problem svmProb = SupportVectorMachine.generateProblem(trainTweets,
          dsc);

      // Optional parameter search of C and gamma
      if (parameterSearch) {
        // 1) coarse grained paramter search
        SupportVectorMachine.coarseGrainedParamterSearch(svmProb, svmParam);

        // 2) fine grained paramter search
        // C = 2^5, 2^6, ..., 2^13
        double[] c = new double[9];
        for (int i = 0; i < 9; i++) {
          c[i] = Math.pow(2, 5 + i);
        }
        // gamma = 2^−10, 2^−9, ..., 2^-3
        double[] gamma = new double[8];
        for (int j = 0; j < 8; j++) {
          gamma[j] = Math.pow(2, -10 + j);
        }

        // SupportVectorMaschine.paramterSearch(svmProb, svmParam, c, gamma);

      } else {

        // after parameter search
        svmParam.C = 0.5;
        svmParam.gamma = Math.pow(2, 3);

        // train model
        svm_model model = SupportVectorMachine.train(svmProb, svmParam);

        long countMatches = 0;
        for (Tweet tweet : testTweets) {
          double predictedClass = SupportVectorMachine.evaluate(tweet, model,
              totalClasses, dsc);
          if (predictedClass == dsc.classfyScore(tweet.getScore())) {
            countMatches++;
          }
        }

        LOG.info("Total test tweets: " + testTweets.size());
        LOG.info("Matches: " + countMatches);
        double accuracy = (double) countMatches / (double) testTweets.size();
        LOG.info("Accuracy: " + accuracy);
      }
    } catch (FileNotFoundException e) {
      LOG.error("FileNotFoundException: " + e.getMessage());
    } finally {
      if (sfvg != null) {
        sfvg.close();
      }
    }
  }
}
