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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.Tweet;
import at.illecker.storm.examples.util.io.FileUtil;
import at.illecker.storm.examples.util.tagger.POSTagger;
import at.illecker.storm.examples.util.tokenizer.Tokenizer;
import edu.stanford.nlp.ling.TaggedWord;

public class SupportVectorMaschine {

  public static final String SVM_DIR = System.getProperty("user.dir")
      + File.separator + "resources" + File.separator + "tweets"
      + File.separator + "svm" + File.separator;
  public static final String TRAIN_DATA = SVM_DIR + "trainingInput.txt";
  public static final String TEST_DATA = SVM_DIR + "testingInput.txt";
  public static final String TRAIN_FILE = SVM_DIR + "trainingInput.ser";
  public static final String TEST_FILE = SVM_DIR + "testingInput.ser";

  private static final Logger LOG = LoggerFactory
      .getLogger(SupportVectorMaschine.class);
  private static final boolean LOGGING = false;

  public static svm_parameter getSVMParameter() {
    svm_parameter param = new svm_parameter();
    param.svm_type = svm_parameter.C_SVC; // default
    param.kernel_type = svm_parameter.RBF;
    // 1 means model with probability information is obtained
    param.probability = 1;
    // C = 2^−5, 2^−3, ..., 2^15
    param.C = 10; // cost of constraints violation default 1
    // gamma = 2^−15, 2^−13, ..., 2^3
    param.gamma = 0.5; // default 1/num_features
    param.nu = 0.5; // default 0.5
    param.eps = 0.001; // stopping criterion
    param.cache_size = 20000; // kernel cache specified in megabytes

    return param;
  }

  public static svm_model svmTrain(List<Tweet> trainTweets,
      svm_parameter svmParam) {
    int dataCount = trainTweets.size();

    svm_problem svmProb = new svm_problem();
    svmProb.y = new double[dataCount]; // classes
    svmProb.l = dataCount;
    svmProb.x = new svm_node[dataCount][];

    for (int i = 0; i < dataCount; i++) {
      Tweet tweet = trainTweets.get(i);
      double[] features = tweet.getFeatureVector();

      // set feature vector
      svmProb.x[i] = new svm_node[features.length + 1];
      for (int j = 0; j < features.length; j++) {
        svm_node node = new svm_node();
        node.index = j;
        node.value = features[j];
        svmProb.x[i][j] = node;
      }

      // end node
      svm_node node = new svm_node();
      node.index = -1;
      svmProb.x[i][features.length] = node;

      // set class
      svmProb.y[i] = tweet.getScore();
    }

    String paramCheck = svm.svm_check_parameter(svmProb, svmParam);
    if (paramCheck != null) {
      LOG.error("svm_check_parameter: " + paramCheck);
    }

    return svm.svm_train(svmProb, svmParam);
  }

  public static double evaluate(Tweet tweet, svm_model svmModel,
      int totalClasses) {
    return SupportVectorMaschine.evaluate(tweet, svmModel, totalClasses, false);
  }

  public static double evaluate(Tweet tweet, svm_model svmModel,
      int totalClasses, boolean logging) {

    double[] features = tweet.getFeatureVector();
    svm_node[] nodes = new svm_node[features.length];
    for (int i = 0; i < features.length; i++) {
      svm_node node = new svm_node();
      node.index = i;
      node.value = features[i];
      nodes[i] = node;
    }

    int[] labels = new int[totalClasses];
    svm.svm_get_labels(svmModel, labels);

    double[] probEstimates = new double[totalClasses];
    double predictedClass = svm.svm_predict_probability(svmModel, nodes,
        probEstimates);

    if (logging) {
      for (int i = 0; i < totalClasses; i++) {
        LOG.info("Label[" + i + "]: " + labels[i] + " Probability: "
            + probEstimates[i]);
      }
      LOG.info("TweetScore:" + tweet.getScore() + " Prediction:"
          + predictedClass);
    }

    return predictedClass;
  }

  public static void processTweets(POSTagger posTagger,
      FeatureVectorGenerator fvg, List<Tweet> tweets) {
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
      tweet.genFeatureVector(fvg);
      if (LOGGING) {
        LOG.info("FeatureVector: " + Arrays.toString(tweet.getFeatureVector()));
      }
    }
  }

  public static void main(String[] args) {
    SimpleFeatureVectorGenerator sfvg = null;
    POSTagger posTagger = null;
    try {

      // Prepare Train tweets
      List<Tweet> trainTweets = null;
      LOG.info("Prepare Train data...");
      List<Tweet> trainedTweets = FileUtil.deserializeTweets(TRAIN_FILE);
      if (trainedTweets == null) {
        // Generate feature vectors
        if (sfvg == null) {
          LOG.info("Load SimpleFeatureVectorGenerator...");
          sfvg = SimpleFeatureVectorGenerator.getInstance();
        }
        // Load POS Tagger
        if (posTagger == null) {
          posTagger = POSTagger.getInstance();
        }
        LOG.info("Read train tweets from " + TRAIN_DATA);
        trainTweets = FileUtil.readTweets(new FileInputStream(TRAIN_DATA));
        processTweets(posTagger, sfvg, trainTweets);
        FileUtil.serializeTweets(trainTweets, TRAIN_FILE);
      } else {
        trainTweets = trainedTweets;
      }

      // Prepare Test tweets
      List<Tweet> testTweets = null;
      LOG.info("Prepare Test data...");
      List<Tweet> testedTweets = FileUtil.deserializeTweets(TEST_FILE);
      if (trainedTweets == null) {
        // Generate feature vectors
        if (sfvg == null) {
          LOG.info("Load SimpleFeatureVectorGenerator...");
          sfvg = SimpleFeatureVectorGenerator.getInstance();
        }
        // Load POS Tagger
        if (posTagger == null) {
          posTagger = POSTagger.getInstance();
        }
        LOG.info("Read test tweets from " + TEST_DATA);
        testTweets = FileUtil.readTweets(new FileInputStream(TEST_DATA));
        processTweets(posTagger, sfvg, testTweets);
        FileUtil.serializeTweets(testTweets, TEST_FILE);
      } else {
        testTweets = testedTweets;
      }

      // classes 1 = positive, 0 = neutral, -1 = negative
      int totalClasses = 3;
      svm_parameter svmParam = getSVMParameter();
      svm_model svmModel = svmTrain(trainTweets, svmParam);

      long countMatches = 0;
      for (Tweet tweet : testTweets) {
        double predictedClass = evaluate(tweet, svmModel, totalClasses);
        if (predictedClass == tweet.getScore()) {
          countMatches++;
        }
      }

      LOG.info("Total test tweets: " + testTweets.size());
      LOG.info("Matches: " + countMatches);
      double accuracy = (double) countMatches / (double) testTweets.size();
      LOG.info("Accuracy: " + accuracy);

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } finally {
      if (sfvg != null) {
        sfvg.close();
      }
    }
  }
}
