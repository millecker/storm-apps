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
import at.illecker.storm.commons.postagger.ArkPOSTagger;
import at.illecker.storm.commons.postagger.GatePOSTagger;
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
import cmu.arktweetnlp.Tagger.TaggedToken;
import edu.stanford.nlp.ling.TaggedWord;

public class SVMBenchmark {

  public static void main(String[] args) {
    final boolean useArkPOSTagger;
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
        if (args.length > 2) {
          useArkPOSTagger = Boolean.parseBoolean(args[2]);
          if (useArkPOSTagger) {
            System.out.println("Using Ark POS Tagger...");
          } else {
            System.out.println("Using Gate POS Tagger...");
          }
        } else {
          useArkPOSTagger = true;
        }
      } else {
        useArkPOSTagger = true;
        inputCount = 1;
      }
    } else {
      useArkPOSTagger = true;
      numberOfThreads = 1;
      inputCount = 1;
    }

    Dataset dataset = Configuration.getDataSetSemEval2013();
    final int totalClasses = 3;
    // classes 0 = negative, 1 = neutral, 2 = positive

    final IdentityScoreClassifier isc = new IdentityScoreClassifier();

    // Load featured tweets
    List<FeaturedTweet> featuredTrainTweets = SerializationUtils
        .deserialize(dataset.getTrainDataSerializationFile());

    // Load Preprocessor
    final Preprocessor preprocessor = Preprocessor.getInstance();
    final ArkPOSTagger arkPOSTagger;
    final GatePOSTagger gatePOSTagger;
    final FeatureVectorGenerator fvg;

    if (useArkPOSTagger) {
      // Load Ark POS Tagger
      gatePOSTagger = null;
      arkPOSTagger = ArkPOSTagger.getInstance();

      // Load TF-IDF
      TweetTfIdf tweetTfIdf = TweetTfIdf.createFromTaggedTokens(
          FeaturedTweet.getTaggedTokensFromTweets(featuredTrainTweets),
          TfType.RAW, TfIdfNormalization.COS, true);

      // Load Feature Vector Generator
      System.out.println("Load CombinedFeatureVectorGenerator...");
      fvg = new CombinedFeatureVectorGenerator(false, true, tweetTfIdf);

    } else {
      // Load Gate POS Tagger
      arkPOSTagger = null;
      gatePOSTagger = GatePOSTagger.getInstance();

      // Load TF-IDF
      TweetTfIdf tweetTfIdf = TweetTfIdf.createFromTaggedWords(
          FeaturedTweet.getTaggedWordsFromTweets(featuredTrainTweets),
          TfType.RAW, TfIdfNormalization.COS, true);

      // Load Feature Vector Generator
      System.out.println("Load CombinedFeatureVectorGenerator...");
      fvg = new CombinedFeatureVectorGenerator(true, true, tweetTfIdf);
    }

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

          List<Map<Integer, Double>> featureVectors = null;
          if (useArkPOSTagger) {
            // Preprocess only
            List<List<String>> preprocessedTweets = preprocessor
                .preprocessTweets(tokenizedTweets);

            // Ark POS Tagging
            List<List<TaggedToken>> taggedTweets = arkPOSTagger
                .tagTweets(preprocessedTweets);

            // Feature Vector Generation
            featureVectors = fvg
                .generateFeatureVectorsFromTaggedTokens(taggedTweets);

          } else {
            // Preprocess and tag
            List<List<TaggedWord>> preprocessedTweets = preprocessor
                .preprocessAndTagTweets(tokenizedTweets);

            // Gate POS Tagging
            List<List<TaggedWord>> taggedTweets = gatePOSTagger
                .tagTweets(preprocessedTweets);

            // Feature Vector Generation
            featureVectors = fvg
                .generateFeatureVectorsFromTaggedWords(taggedTweets);
          }

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
