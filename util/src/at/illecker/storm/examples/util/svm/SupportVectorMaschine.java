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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import libsvm.svm;
import libsvm.svm_model;
import libsvm.svm_node;
import libsvm.svm_parameter;
import libsvm.svm_problem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.Configuration;
import at.illecker.storm.examples.util.Tweet;
import at.illecker.storm.examples.util.io.FileUtil;
import at.illecker.storm.examples.util.tagger.POSTagger;
import at.illecker.storm.examples.util.tokenizer.Tokenizer;
import edu.stanford.nlp.ling.TaggedWord;

public class SupportVectorMaschine {

  public static final String DATASET_PATH = Configuration.getInstance()
      .getDataSetPath();
  public static final String DATASET3_PATH = DATASET_PATH + File.separator
      + "dataset3" + File.separator;
  public static final String TRAIN_DATA = DATASET3_PATH + "trainingInput.txt";
  public static final String TEST_DATA = DATASET3_PATH + "testingInput.txt";
  public static final String TRAIN_FILE = DATASET3_PATH + "trainingInput.ser";
  public static final String TEST_FILE = DATASET3_PATH + "testingInput.ser";

  private static final Logger LOG = LoggerFactory
      .getLogger(SupportVectorMaschine.class);
  private static final boolean LOGGING = false;

  public static svm_parameter svmParameter() {
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

  public static svm_problem svmProblem(List<Tweet> trainTweets) {
    int dataCount = trainTweets.size();

    svm_problem svmProb = new svm_problem();
    svmProb.y = new double[dataCount];
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

      // set end node
      svm_node node = new svm_node();
      node.index = -1;
      svmProb.x[i][features.length] = node;

      // set class
      svmProb.y[i] = tweet.getScore();
    }

    return svmProb;
  }

  public static svm_model svmTrain(svm_problem svmProb, svm_parameter svmParam) {
    String paramCheck = svm.svm_check_parameter(svmProb, svmParam);
    if (paramCheck != null) {
      LOG.error("svm_check_parameter: " + paramCheck);
    }

    return svm.svm_train(svmProb, svmParam);
  }

  public static double svmCrossValidation(svm_problem svmProb,
      svm_parameter svmParam) {
    double[] target = new double[svmProb.l];
    svm.svm_cross_validation(svmProb, svmParam, 3, target);

    double correctCounter = 0;
    for (int i = 0; i < target.length; i++) {
      if (target[i] == svmProb.y[i]) {
        correctCounter++;
      }
    }

    return correctCounter / (double) svmProb.l;
  }

  private static class FindParameterCallable implements Callable<double[]> {
    private svm_problem m_svmProb;
    private svm_parameter m_svmParam;
    private long m_i;
    private long m_j;

    public FindParameterCallable(svm_problem svmProb, svm_parameter svmParam,
        long i, long j) {
      m_svmProb = svmProb;
      m_svmParam = svmParam;
      m_i = i;
      m_j = j;
    }

    @Override
    public double[] call() throws Exception {
      long startTime = System.currentTimeMillis();
      double accuracy = svmCrossValidation(m_svmProb, m_svmParam);
      long estimatedTime = System.currentTimeMillis() - startTime;
      return new double[] { m_i, m_j, accuracy, m_svmParam.C, m_svmParam.gamma,
          estimatedTime };
    }
  }

  public static void findParamters(final svm_problem svmProb) {
    int cores = Runtime.getRuntime().availableProcessors();
    ExecutorService executorService = Executors.newFixedThreadPool(cores);
    Set<Callable<double[]>> callables = new HashSet<Callable<double[]>>();

    for (int i = 0; i < 10; i++) { // i < 11
      for (int j = 0; j < 9; j++) {
        svm_parameter svmParam = svmParameter();
        svmParam.C = Math.pow(2, -5 + (i * 2));
        svmParam.gamma = Math.pow(2, -15 + (j * 2));
        callables.add(new FindParameterCallable(svmProb, svmParam, i, j));
      }
    }

    try {
      List<Future<double[]>> futures = executorService.invokeAll(callables);
      for (Future<double[]> future : futures) {
        double[] result = future.get();
        LOG.info("findParamters[" + result[0] + "," + result[1] + "] C="
            + result[3] + " gamma=" + result[4] + " accuracy: " + result[2]
            + " time: " + result[5] + " ms");
      }
    } catch (InterruptedException e) {
      LOG.error(e.getMessage());
    } catch (ExecutionException e) {
      LOG.error(e.getMessage());
    }

    executorService.shutdown();
  }

  public static double svmEvaluate(Tweet tweet, svm_model svmModel,
      int totalClasses) {
    return SupportVectorMaschine.svmEvaluate(tweet, svmModel, totalClasses,
        false);
  }

  public static double svmEvaluate(Tweet tweet, svm_model svmModel,
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
      svm_parameter svmParam = svmParameter();
      svm_problem svmProb = svmProblem(trainTweets);

      findParamters(svmProb);
      // TODO
      System.exit(1);

      svm_model svmModel = svmTrain(svmProb, svmParam);

      long countMatches = 0;
      for (Tweet tweet : testTweets) {
        double predictedClass = svmEvaluate(tweet, svmModel, totalClasses);
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
