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
package at.illecker.storm.examples.util.svm.examples;

import java.io.File;
import java.util.List;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_parameter;
import libsvm.svm_problem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.Configuration;
import at.illecker.storm.examples.util.io.FileUtils;
import at.illecker.storm.examples.util.io.IOUtils;
import at.illecker.storm.examples.util.io.SerializationUtils;
import at.illecker.storm.examples.util.svm.SVM;
import at.illecker.storm.examples.util.svm.classifier.IdentityScoreClassifier;
import at.illecker.storm.examples.util.svm.feature.ExtendedFeatureVectorGenerator;
import at.illecker.storm.examples.util.svm.feature.FeatureVectorGenerator;
import at.illecker.storm.examples.util.svm.feature.SimpleFeatureVectorGenerator;
import at.illecker.storm.examples.util.tagger.POSTagger;
import at.illecker.storm.examples.util.tfidf.TweetTfIdf;
import at.illecker.storm.examples.util.tokenizer.Tokenizer;
import at.illecker.storm.examples.util.tweet.Tweet;
import edu.stanford.nlp.ling.TaggedWord;

public class DataSet3SVM {
  public static final String DATASET_PATH = Configuration.getDataSetPath()
      + File.separator + "dataset3" + File.separator;
  public static final String TRAIN_DATA = DATASET_PATH + "trainingInput.txt";
  public static final String TEST_DATA = DATASET_PATH + "testingInput.txt";
  public static final String TRAIN_SER = DATASET_PATH + "trainingInput.ser";
  public static final String TEST_SER = DATASET_PATH + "testingInput.ser";
  public static final String SVM_PROBLEM_SER = DATASET_PATH + "svmProblem.txt";
  public static final String SVM_MODEL_SER = DATASET_PATH + "svmModel.ser";

  private static final Logger LOG = LoggerFactory.getLogger(DataSet3SVM.class);
  private static final boolean LOGGING = false;

  public static void tokenizeTweets(List<Tweet> tweets) {
    for (Tweet tweet : tweets) {
      List<String> tokens = Tokenizer.tokenize(tweet.getText());
      tweet.addSentence(tokens);
    }
  }

  public static void tagTweets(POSTagger posTagger, List<Tweet> tweets) {
    for (Tweet tweet : tweets) {
      for (List<String> sentence : tweet.getSentences()) {
        List<TaggedWord> taggedSentence = posTagger.tagSentence(sentence);
        tweet.addTaggedSentence(taggedSentence);
      }
    }
  }

  public static void featureGenTweets(FeatureVectorGenerator fvg,
      List<Tweet> tweets) {
    for (Tweet tweet : tweets) {
      // Generate Feature Vector
      tweet.genFeatureVector(fvg);

      if (LOGGING) {
        LOG.info("Tweet: " + tweet);
        LOG.info("FeatureVector: " + tweet.getFeatureVector());
      }
    }
  }

  public static void main(String[] args) {
    FeatureVectorGenerator fvg = null;
    POSTagger posTagger = null;
    boolean useExtendedFeatureVectorGen = false;
    boolean parameterSearch = false;

    // Prepare Train tweets
    LOG.info("Prepare Train data...");
    List<Tweet> trainTweets = SerializationUtils.deserialize(TRAIN_SER);
    if (trainTweets == null) {
      LOG.info("Read train tweets from " + TRAIN_DATA);
      trainTweets = FileUtils.readTweets(IOUtils.getInputStream(TRAIN_DATA));

      // Tokenize
      LOG.info("Tokenize train tweets...");
      tokenizeTweets(trainTweets);

      // POS Tagging
      LOG.info("POS Tagging of train tweets...");
      if (posTagger == null) {
        posTagger = POSTagger.getInstance();
      }
      tagTweets(posTagger, trainTweets);

      if (fvg == null) {
        if (useExtendedFeatureVectorGen) {
          LOG.info("Load ExtendedFeatureVectorGenerator...");
          fvg = new ExtendedFeatureVectorGenerator(new TweetTfIdf(trainTweets,
              true));
        } else {
          LOG.info("Load SimpleFeatureVectorGenerator...");
          fvg = SimpleFeatureVectorGenerator.getInstance();
        }
      }

      // Feature Vector Generation
      LOG.info("Feature Vector Generation of train tweets...");
      featureGenTweets(fvg, trainTweets);

      // Serialize training data
      SerializationUtils.serializeList(trainTweets, TRAIN_SER);
    }

    // Prepare Test tweets
    LOG.info("Prepare Test data...");
    List<Tweet> testTweets = SerializationUtils.deserialize(TEST_SER);
    if (testTweets == null) {
      if ((fvg == null) || (posTagger == null)) {
        LOG.error("Train and test data must use the same FeatureVectorGenerator!");
        System.exit(1);
      }
      LOG.info("Read test tweets from " + TEST_DATA);
      testTweets = FileUtils.readTweets(IOUtils.getInputStream(TEST_DATA));

      // Tokenize
      LOG.info("Tokenize test tweets...");
      tokenizeTweets(testTweets);

      // POS Tagging
      LOG.info("POS Tagging of test tweets...");
      tagTweets(posTagger, testTweets);

      // Feature Vector Generation
      LOG.info("Feature Vector Generation of test tweets...");
      featureGenTweets(fvg, testTweets);

      // Serialize test data
      SerializationUtils.serializeList(testTweets, TEST_SER);
    }

    // Optional parameter search of C and gamma
    if (parameterSearch) {
      svm_parameter svmParam = SVM.getDefaultParameter();
      LOG.info("Generate SVM problem...");
      svm_problem svmProb = SVM.generateProblem(trainTweets,
          new IdentityScoreClassifier());

      // 1) coarse grained paramter search
      SVM.coarseGrainedParamterSearch(svmProb, svmParam);

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

      LOG.info("SVM paramterSearch...");
      SVM.paramterSearch(svmProb, svmParam, c, gamma);

    } else {

      int totalClasses = 3;
      // classes 1 = positive, 0 = neutral, -1 = negative
      IdentityScoreClassifier isc = new IdentityScoreClassifier();

      // deserialize svmModel
      LOG.info("Try loading SVM model...");
      svm_model svmModel = SerializationUtils.deserialize(SVM_MODEL_SER);
      if (svmModel == null) {
        svm_parameter svmParam = SVM.getDefaultParameter();
        LOG.info("Generate SVM problem...");
        svm_problem svmProb = SVM.generateProblem(trainTweets, isc);

        // save svm problem in libSVM format
        SVM.saveProblem(svmProb, SVM_PROBLEM_SER);

        // after parameter search use best C and gamma values
        svmParam.C = Math.pow(2, 6);
        svmParam.gamma = Math.pow(2, -5);

        // train model
        LOG.info("Train SVM model...");
        long startTime = System.currentTimeMillis();
        svmModel = SVM.train(svmProb, svmParam);
        LOG.info("Train SVM model finished after "
            + (System.currentTimeMillis() - startTime) + " ms");
        SerializationUtils.serialize(svmModel, SVM_MODEL_SER);
      }

      long countMatches = 0;
      LOG.info("Evaluate test tweets...");
      long startTime = System.currentTimeMillis();
      for (Tweet tweet : testTweets) {
        double predictedClass = SVM
            .evaluate(tweet, svmModel, totalClasses, isc);
        if (predictedClass == isc.classfyScore(tweet.getScore())) {
          countMatches++;
        }
      }

      LOG.info("Evaluate finished after "
          + (System.currentTimeMillis() - startTime) + " ms");
      LOG.info("Total test tweets: " + testTweets.size());
      LOG.info("Matches: " + countMatches);
      double accuracy = (double) countMatches / (double) testTweets.size();
      LOG.info("Accuracy: " + accuracy);

      svm.EXEC_SERV.shutdown();
    }
  }
}
