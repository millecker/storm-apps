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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import libsvm.svm;
import libsvm.svm_model;
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
    List<TaggedTweet> taggedTrainTweets = SerializationUtils
        .deserialize(dataset.getTrainTaggedDataSerializationFile());
    TweetTfIdf tweetTfIdf = new TweetTfIdf(taggedTrainTweets, TfType.RAW,
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
    System.out.println("Benchmark finished after " + totalTime + " ms");
    System.out.println("Total test tweets: " + testTweets.size());
    System.out.println("Tweets per second: "
        + (testTweets.size() / ((double) totalTime / 1000)));

    executorService.shutdown();
    svm.EXEC_SERV.shutdown();
  }
}
