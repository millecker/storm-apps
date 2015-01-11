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

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_parameter;
import libsvm.svm_problem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.Configuration;
import at.illecker.storm.examples.util.DatasetProperty;
import at.illecker.storm.examples.util.io.FileUtils;
import at.illecker.storm.examples.util.io.SerializationUtils;
import at.illecker.storm.examples.util.preprocessor.Preprocessor;
import at.illecker.storm.examples.util.svm.classifier.IdentityScoreClassifier;
import at.illecker.storm.examples.util.svm.feature.FeatureVectorGenerator;
import at.illecker.storm.examples.util.svm.feature.SentimentFeatureVectorGenerator;
import at.illecker.storm.examples.util.svm.feature.TfIdfFeatureVectorGenerator;
import at.illecker.storm.examples.util.tagger.POSTagger;
import at.illecker.storm.examples.util.tfidf.TfIdfNormalization;
import at.illecker.storm.examples.util.tfidf.TfType;
import at.illecker.storm.examples.util.tfidf.TweetTfIdf;
import at.illecker.storm.examples.util.tokenizer.Tokenizer;
import at.illecker.storm.examples.util.tweet.Tweet;

public class DataSet3SVM {
  public static final String SVM_PROBLEM_FILE = "svmProblem.txt";
  public static final String SVM_MODEL_FILE_SER = "svmModel.ser";
  private static final Logger LOG = LoggerFactory.getLogger(DataSet3SVM.class);

  public static void main(String[] args) {
    FeatureVectorGenerator fvg = null;
    Preprocessor preprocessor = null;
    POSTagger posTagger = null;
    boolean useTfIdfFeatureVectorGen = true;
    boolean parameterSearch = false;
    DatasetProperty dataset3Prop = Configuration.getDataSet3();

    // Prepare Train tweets
    LOG.info("Prepare Train data...");
    List<Tweet> trainTweets = SerializationUtils.deserialize(dataset3Prop
        .getTrainDataSerializationFile());

    if (trainTweets == null) {
      // Read train tweets
      trainTweets = FileUtils.readTweets(dataset3Prop.getTrainDataFile(),
          dataset3Prop);
      LOG.info("Read train tweets from " + dataset3Prop.getTrainDataFile());

      // Tokenize
      LOG.info("Tokenize train tweets...");
      Tokenizer.tokenizeTweets(trainTweets);

      // Preprocess
      preprocessor = Preprocessor.getInstance();
      LOG.info("Preprocess train tweets...");
      preprocessor.preprocessTweets(trainTweets);

      // POS Tagging
      posTagger = POSTagger.getInstance();
      LOG.info("POS Tagging of train tweets...");
      posTagger.tagTweets(trainTweets);

      if (useTfIdfFeatureVectorGen) {
        TweetTfIdf tweetTfIdf = new TweetTfIdf(trainTweets, TfType.RAW,
            TfIdfNormalization.COS, true);
        LOG.info("Load TfIdfFeatureVectorGenerator...");
        fvg = new TfIdfFeatureVectorGenerator(tweetTfIdf);
      } else {
        LOG.info("Load SentimentFeatureVectorGenerator...");
        fvg = new SentimentFeatureVectorGenerator();
      }

      // Feature Vector Generation
      LOG.info("Generate Feature Vectors for train tweets...");
      fvg.generateFeatureVectors(trainTweets);

      // Serialize training data
      SerializationUtils.serializeList(trainTweets,
          dataset3Prop.getTrainDataSerializationFile());
    }

    // Prepare Test tweets
    LOG.info("Prepare Test data...");
    List<Tweet> testTweets = SerializationUtils.deserialize(dataset3Prop
        .getTestDataSerializationFile());

    if (testTweets == null) {
      if (fvg == null) {
        LOG.error("Train and test data must use the same FeatureVectorGenerator!");
        System.exit(1);
      }

      // read test tweets
      testTweets = FileUtils.readTweets(dataset3Prop.getTestDataFile(),
          dataset3Prop);
      LOG.info("Read test tweets from " + dataset3Prop.getTestDataFile());

      // Tokenize
      LOG.info("Tokenize test tweets...");
      Tokenizer.tokenizeTweets(testTweets);

      // Preprocess
      LOG.info("Preprocess test tweets...");
      preprocessor.preprocessTweets(trainTweets);

      // POS Tagging
      LOG.info("POS Tagging of test tweets...");
      posTagger.tagTweets(testTweets);

      // Feature Vector Generation
      LOG.info("Generate Feature Vectors for test tweets...");
      fvg.generateFeatureVectors(trainTweets);

      // Serialize test data
      SerializationUtils.serializeList(testTweets,
          dataset3Prop.getTestDataSerializationFile());
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
      svm_model svmModel = SerializationUtils.deserialize(dataset3Prop
          .getDatasetPath() + File.separator + SVM_MODEL_FILE_SER);
      if (svmModel == null) {
        LOG.info("Generate SVM problem...");
        svm_problem svmProb = SVM.generateProblem(trainTweets, isc);

        // save svm problem in libSVM format
        SVM.saveProblem(svmProb, dataset3Prop.getDatasetPath() + File.separator
            + SVM_PROBLEM_FILE);

        // after parameter search use best C and gamma values
        svm_parameter svmParam = SVM.getDefaultParameter();
        svmParam.kernel_type = svm_parameter.LINEAR;
        svmParam.C = 0.5;
        // svmParam.C = Math.pow(2, 6);
        // svmParam.gamma = Math.pow(2, -5);

        // train model
        LOG.info("Train SVM model...");
        long startTime = System.currentTimeMillis();
        svmModel = SVM.train(svmProb, svmParam);
        LOG.info("Train SVM model finished after "
            + (System.currentTimeMillis() - startTime) + " ms");

        // serialize svm model
        SerializationUtils.serialize(svmModel, dataset3Prop.getDatasetPath()
            + File.separator + SVM_MODEL_FILE_SER);

        // Evaluate n-fold
        LOG.info("Run n-fold cross validation...");
        startTime = System.currentTimeMillis();
        double accuracy = SVM.crossValidate(svmProb, svmParam, 3);
        LOG.info("CrossValidation finished after "
            + (System.currentTimeMillis() - startTime) + " ms");
        LOG.info("Cross Validation Accurancy: " + accuracy);
      }

      // Evaluate test tweets
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
