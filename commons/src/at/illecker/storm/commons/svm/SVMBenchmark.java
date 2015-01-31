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
import at.illecker.storm.commons.Configuration;
import at.illecker.storm.commons.Dataset;
import at.illecker.storm.commons.postagger.POSTagger;
import at.illecker.storm.commons.preprocessor.Preprocessor;
import at.illecker.storm.commons.svm.featurevector.CombinedFeatureVectorGenerator;
import at.illecker.storm.commons.svm.featurevector.FeatureVectorGenerator;
import at.illecker.storm.commons.svm.scoreclassifier.IdentityScoreClassifier;
import at.illecker.storm.commons.tfidf.TfIdfNormalization;
import at.illecker.storm.commons.tfidf.TfType;
import at.illecker.storm.commons.tfidf.TweetTfIdf;
import at.illecker.storm.commons.tokenizer.Tokenizer;
import at.illecker.storm.commons.tweet.FeaturedTweet;
import at.illecker.storm.commons.tweet.Tweet;
import at.illecker.storm.commons.util.io.SerializationUtils;
import edu.stanford.nlp.ling.TaggedWord;

public class SVMBenchmark {

  public static void main(String[] args) {
    final int numberOfThreads;
    final int inputCount;
    System.out.println("\nStarting SVM Benchmark...");
    if (args.length > 0) {
      numberOfThreads = Integer.parseInt(args[0]);
      System.out.println("Using " + numberOfThreads + " threads...");
      if (args.length > 1) {
        inputCount = Integer.parseInt(args[1]);
        System.out
            .println("Using " + inputCount + " times the test dataset...");
      } else {
        inputCount = 1;
      }
    } else {
      numberOfThreads = 1;
      inputCount = 1;
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
    List<FeaturedTweet> featuredTrainTweets = SerializationUtils
        .deserialize(dataset.getTrainDataSerializationFile());
    TweetTfIdf tweetTfIdf = new TweetTfIdf(
        FeaturedTweet.getTaggedTweets(featuredTrainTweets), TfType.RAW,
        TfIdfNormalization.COS, true);

    // Load Feature Vector Generator
    System.out.println("Load CombinedFeatureVectorGenerator...");
    final FeatureVectorGenerator fvg = new CombinedFeatureVectorGenerator(
        tweetTfIdf);

    // Load SVM Model
    System.out.println("Loading SVM model...");
    final svm_model svmModel = SerializationUtils.deserialize(dataset
        .getDatasetPath() + File.separator + SVM.SVM_MODEL_FILE_SER);

    final CountDownLatch latch = new CountDownLatch(numberOfThreads);
    ExecutorService executorService = Executors
        .newFixedThreadPool(numberOfThreads);

    // Load test tweets
    final ArrayList<Tweet> testInputTweets = (ArrayList<Tweet>) dataset
        .getTestTweets();
    final ArrayList<Tweet> testTweets = new ArrayList<Tweet>(testInputTweets);
    for (int i = 0; i < inputCount - 1; i++) {
      testTweets.addAll((ArrayList<Tweet>) testInputTweets.clone());
    }

    final int totalTweets = testTweets.size();
    final int tweetsPerThread = totalTweets / numberOfThreads;

    // Start Benchmark
    System.out.println("Start Benchmark...");
    long startTime = System.currentTimeMillis();
    // Run threads
    for (int i = 0; i < numberOfThreads; i++) {
      final int begin = i * tweetsPerThread;
      final int end = (i == numberOfThreads - 1) ? totalTweets - 1
          : ((i + 1) * tweetsPerThread) - 1;
      // LOG.info("begin: " + begin + " end: " + end);

      executorService.submit(new Runnable() {
        public void run() {
          List<Tweet> subtestTweets = testTweets.subList(begin, end);

          // Tokenize
          List<List<String>> tokenizedTweets = Tokenizer
              .tokenizeTweets(subtestTweets);

          // Preprocess
          List<List<TaggedWord>> preprocessedTweets = preprocessor
              .preprocessTweets(tokenizedTweets);

          // POS Tagging
          List<List<TaggedWord>> taggedTweets = posTagger
              .tagTweets(preprocessedTweets);

          // Feature Vector Generation
          List<Map<Integer, Double>> featureVectors = fvg
              .generateFeatureVectors(taggedTweets);

          for (Map<Integer, Double> featureVector : featureVectors) {
            double predictedClass = SVM.evaluate(featureVector, svmModel,
                totalClasses, isc);
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
    System.out.println("Benchmark finished after " + totalTime + " ms");
    System.out.println("Total test tweets: " + testTweets.size());
    System.out.println("Tweets per second: "
        + (testTweets.size() / ((double) totalTime / 1000)));

    executorService.shutdown();
    svm.EXEC_SERV.shutdown();
  }
}
