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
    Dataset dataset = Configuration.getDataSetSemEval2013();
    int totalClasses = 3;
    // classes 0 = negative, 1 = neutral, 2 = positive
    IdentityScoreClassifier isc = new IdentityScoreClassifier();

    // Load Preprocessor
    Preprocessor preprocessor = Preprocessor.getInstance();

    // Load POS Tagger
    POSTagger posTagger = POSTagger.getInstance();

    // Load TF-IDF
    List<TaggedTweet> taggedTrainTweets = SerializationUtils
        .deserialize(dataset.getTrainTaggedDataSerializationFile());
    TweetTfIdf tweetTfIdf = new TweetTfIdf(taggedTrainTweets, TfType.RAW,
        TfIdfNormalization.COS, true);

    // Load Feature Vector Generator
    LOG.info("Load CombinedFeatureVectorGenerator...");
    FeatureVectorGenerator fvg = new CombinedFeatureVectorGenerator(tweetTfIdf);

    // Load SVM Model
    LOG.info("Loading SVM model...");
    svm_model svmModel = SerializationUtils.deserialize(dataset
        .getDatasetPath() + File.separator + SVM.SVM_MODEL_FILE_SER);

    // Start Benchmark
    LOG.info("Start Benchmark...");
    long startTime = System.currentTimeMillis();

    // 1) Load test tweets
    List<Tweet> testTweets = dataset.getTestTweets();

    // 2) Tokenize
    List<TokenizedTweet> tokenizedTweets = Tokenizer.tokenizeTweets(testTweets);

    // 3) Preprocess
    List<PreprocessedTweet> preprocessedTweets = preprocessor
        .preprocessTweets(tokenizedTweets);

    // 4) POS Tagging
    List<TaggedTweet> taggedTweets = posTagger.tagTweets(preprocessedTweets);

    // 5) Feature Vector Generation
    List<FeaturedTweet> featuredTweets = fvg
        .generateFeatureVectors(taggedTweets);

    for (FeaturedTweet tweet : featuredTweets) {
      double predictedClass = SVM.evaluate(tweet, svmModel, totalClasses, isc);
    }

    // End Benchmark
    long totalTime = System.currentTimeMillis() - startTime;
    LOG.info("Benchmark finished after " + totalTime + " ms");
    LOG.info("Total test tweets: " + testTweets.size());
    LOG.info("Tweets per second: "
        + (testTweets.size() / ((double) totalTime / 1000)));
  }
}
