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
package at.illecker.storm.commons.svm;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import libsvm.svm;
import libsvm.svm_model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.commons.Configuration;
import at.illecker.storm.commons.Dataset;
import at.illecker.storm.commons.postagger.POSTagger;
import at.illecker.storm.commons.preprocessor.Preprocessor;
import at.illecker.storm.commons.svm.featurevector.CombinedFeatureVectorGenerator;
import at.illecker.storm.commons.svm.featurevector.FeatureVectorGenerator;
import at.illecker.storm.commons.svm.scoreclassifier.IdentityScoreClassifier;
import at.illecker.storm.commons.svm.scoreclassifier.ScoreClassifier;
import at.illecker.storm.commons.tfidf.TfIdfNormalization;
import at.illecker.storm.commons.tfidf.TfType;
import at.illecker.storm.commons.tfidf.TweetTfIdf;
import at.illecker.storm.commons.tokenizer.Tokenizer;
import at.illecker.storm.commons.tweet.FeaturedTweet;
import at.illecker.storm.commons.tweet.Tweet;
import at.illecker.storm.commons.util.io.SerializationUtils;

import com.google.caliper.Benchmark;
import com.google.caliper.Param;
import com.google.caliper.api.Macrobenchmark;
import com.google.caliper.runner.CaliperMain;

import edu.stanford.nlp.ling.TaggedWord;

public class SVMCaliperBenchmark extends Benchmark {
  private static final Logger LOG = LoggerFactory
      .getLogger(SVMCaliperBenchmark.class);

  @Param({ "1", "2" })
  private int n; // number of threads

  private int inputCount = 1;

  private final Preprocessor m_preprocessor = Preprocessor.getInstance();
  private final POSTagger m_posTagger = POSTagger.getInstance();

  private final Dataset m_dataset = Configuration.getDataSetSemEval2013();
  // classes 0 = positive, 1 = negative, 2 = neutral
  private final int m_totalClasses = 3;
  private final ScoreClassifier m_sc = new IdentityScoreClassifier();

  private final svm_model m_svmModel = SerializationUtils.deserialize(m_dataset
      .getDatasetPath() + File.separator + SVM.SVM_MODEL_FILE_SER);

  private final List<FeaturedTweet> m_featuredTrainTweets = SerializationUtils
      .deserialize(m_dataset.getTrainDataSerializationFile());
  private final TweetTfIdf m_tweetTfIdf = new TweetTfIdf(
      FeaturedTweet.getTaggedTweets(m_featuredTrainTweets), TfType.RAW,
      TfIdfNormalization.COS, true);
  private final FeatureVectorGenerator m_fvg = new CombinedFeatureVectorGenerator(
      m_tweetTfIdf);

  private ArrayList<Tweet> m_testTweets = null;
  private int m_totalTweets;
  private int m_tweetsPerThread;

  private ExecutorService m_executorService;
  private CountDownLatch m_latch;

  @Override
  protected void setUp() throws Exception {
    m_executorService = Executors.newFixedThreadPool(n);
    m_latch = new CountDownLatch(n); // number of threads

    // Load test tweets
    m_testTweets = (ArrayList<Tweet>) m_dataset.getTestTweets();
    for (int i = 0; i < inputCount - 1; i++) {
      m_testTweets.addAll((ArrayList<Tweet>) m_testTweets.clone());
    }

    m_totalTweets = m_testTweets.size();
    m_tweetsPerThread = m_totalTweets / n; // number of threads
  }

  @Override
  protected void tearDown() throws Exception {
    m_executorService.shutdown();
    try {
      svm.EXEC_SERV.shutdown();
    } catch (Exception e) {
      // ignore
    }
  }

  @Macrobenchmark
  public void timeCalculate() {
    try {
      long startTime = System.currentTimeMillis();

      for (int i = 0; i < n; i++) {
        final int begin = i * m_tweetsPerThread;
        final int end = (i == n - 1) ? m_totalTweets - 1
            : ((i + 1) * m_tweetsPerThread) - 1;
        // LOG.info("begin: " + begin + " end: " + end);

        m_executorService.submit(new Runnable() {
          public void run() {
            List<Tweet> subtestTweets = m_testTweets.subList(begin, end);

            // Tokenize
            List<List<String>> tokenizedTweets = Tokenizer
                .tokenizeTweets(subtestTweets);

            // Preprocess
            List<List<TaggedWord>> preprocessedTweets = m_preprocessor
                .preprocessTweets(tokenizedTweets);

            // POS Tagging
            List<List<TaggedWord>> taggedTweets = m_posTagger
                .tagTweets(preprocessedTweets);

            // Feature Vector Generation
            List<Map<Integer, Double>> featureVectors = m_fvg
                .generateFeatureVectors(taggedTweets);

            for (Map<Integer, Double> featureVector : featureVectors) {
              double predictedClass = SVM.evaluate(featureVector, m_svmModel,
                  m_totalClasses, m_sc);
            }

            m_latch.countDown();
          }
        });
      }

      try {
        m_latch.await();
      } catch (InterruptedException e) {
        LOG.error("InterruptedException: " + e.getMessage());
      }

      // End Benchmark
      long totalTime = System.currentTimeMillis() - startTime;
      LOG.info("Benchmark finished after " + totalTime + " ms");
      LOG.info("Total test tweets: " + m_totalTweets);
      double tweetsPerSecond = m_totalTweets / ((double) totalTime / 1000);
      LOG.info("Tweets per second: " + tweetsPerSecond);

      // return tweetsPerSecond;

    } catch (Exception e) {
      LOG.error("Exception: " + e.getMessage());
    }
  }

  public static void main(String[] args) {
    CaliperMain.main(SVMCaliperBenchmark.class, args);
  }
}
