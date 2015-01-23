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
package at.illecker.storm.examples.util.svm;

import java.io.File;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import libsvm.svm_model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.Configuration;
import at.illecker.storm.examples.util.Dataset;
import at.illecker.storm.examples.util.io.SerializationUtils;
import at.illecker.storm.examples.util.preprocessor.Preprocessor;
import at.illecker.storm.examples.util.svm.classifier.IdentityScoreClassifier;
import at.illecker.storm.examples.util.svm.feature.CombinedFeatureVectorGenerator;
import at.illecker.storm.examples.util.svm.feature.FeatureVectorGenerator;
import at.illecker.storm.examples.util.tagger.POSTagger;
import at.illecker.storm.examples.util.tfidf.TfIdfNormalization;
import at.illecker.storm.examples.util.tfidf.TfType;
import at.illecker.storm.examples.util.tfidf.TweetTfIdf;
import at.illecker.storm.examples.util.tokenizer.Tokenizer;
import at.illecker.storm.examples.util.tweet.FeaturedTweet;
import at.illecker.storm.examples.util.tweet.PreprocessedTweet;
import at.illecker.storm.examples.util.tweet.TaggedTweet;
import at.illecker.storm.examples.util.tweet.TokenizedTweet;
import at.illecker.storm.examples.util.tweet.Tweet;

public class SVMBenchmark {
  private static final Logger LOG = LoggerFactory.getLogger(SVMBenchmark.class);

  public static void main(String[] args) {
    final int numberOfThreads;
    if (args.length > 0) {
      numberOfThreads = Integer.parseInt(args[0]);
      LOG.info("Using " + numberOfThreads + " threads...");
    } else {
      numberOfThreads = 1;
    }

    Dataset dataset = Configuration.getDataSetSemEval2013();
    final int totalClasses = 3;
    // classes 0 = negative, 1 = neutral, 2 = positive

    final IdentityScoreClassifier isc = new IdentityScoreClassifier();

    // Load Preprocessor
    final Preprocessor preprocessor = Preprocessor.getInstance();

    // Load POS Tagger
    final POSTagger posTagger = POSTagger.getInstance();

    // Load TF-IDF
    List<TaggedTweet> taggedTrainTweets = SerializationUtils
        .deserialize(dataset.getTrainTaggedDataSerializationFile());
    TweetTfIdf tweetTfIdf = new TweetTfIdf(taggedTrainTweets, TfType.RAW,
        TfIdfNormalization.COS, true);

    // Load Feature Vector Generator
    LOG.info("Load CombinedFeatureVectorGenerator...");
    final FeatureVectorGenerator fvg = new CombinedFeatureVectorGenerator(
        tweetTfIdf);

    // Load SVM Model
    LOG.info("Loading SVM model...");
    final svm_model svmModel = SerializationUtils.deserialize(dataset
        .getDatasetPath() + File.separator + SVM.SVM_MODEL_FILE_SER);

    final CountDownLatch latch = new CountDownLatch(numberOfThreads);
    ExecutorService executorService = Executors
        .newFixedThreadPool(numberOfThreads);

    // Start Benchmark
    LOG.info("Start Benchmark...");
    long startTime = System.currentTimeMillis();

    // Load test tweets
    final List<Tweet> testTweets = dataset.getTestTweets();
    final int totalTweets = testTweets.size();
    final int tweetsPerThread = totalTweets / numberOfThreads;

    // Run threads
    for (int i = 0; i < numberOfThreads; i++) {
      int begin = i * tweetsPerThread;
      int end = (i == numberOfThreads - 1) ? totalTweets - 1
          : ((i + 1) * tweetsPerThread) - 1;
      // LOG.info("begin: " + begin + " end: " + end);

      final List<Tweet> subtestTweets = testTweets.subList(begin, end);

      executorService.submit(new Runnable() {
        public void run() {
          // Tokenize
          List<TokenizedTweet> tokenizedTweets = Tokenizer
              .tokenizeTweets(subtestTweets);

          // Preprocess
          List<PreprocessedTweet> preprocessedTweets = preprocessor
              .preprocessTweets(tokenizedTweets);

          // POS Tagging
          List<TaggedTweet> taggedTweets = posTagger
              .tagTweets(preprocessedTweets);

          // Feature Vector Generation
          List<FeaturedTweet> featuredTweets = fvg
              .generateFeatureVectors(taggedTweets);

          for (FeaturedTweet tweet : featuredTweets) {
            double predictedClass = SVM.evaluate(tweet, svmModel, totalClasses,
                isc);
          }

          latch.countDown();
        }
      });
    }

    try {
      latch.await();
    } catch (InterruptedException e) {
      System.err.println("InterruptedException: " + e.getMessage());
    }

    // End Benchmark
    long totalTime = System.currentTimeMillis() - startTime;
    LOG.info("Benchmark finished after " + totalTime + " ms");
    LOG.info("Total test tweets: " + testTweets.size());
    LOG.info("Tweets per second: "
        + (testTweets.size() / ((double) totalTime / 1000)));
  }
}
