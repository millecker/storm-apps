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
import at.illecker.storm.examples.util.io.FileUtils;
import at.illecker.storm.examples.util.io.SerializationUtils;
import at.illecker.storm.examples.util.svm.classifier.IdentityScoreClassifier;
import at.illecker.storm.examples.util.svm.classifier.ScoreClassifier;
import at.illecker.storm.examples.util.svm.feature.FeatureVectorGenerator;
import at.illecker.storm.examples.util.svm.feature.SimpleFeatureVectorGenerator;
import at.illecker.storm.examples.util.tagger.POSTagger;
import at.illecker.storm.examples.util.tokenizer.Tokenizer;
import at.illecker.storm.examples.util.tweet.Tweet;
import edu.stanford.nlp.ling.TaggedWord;

public class SupportVectorMachine {
  public static final Configuration CONFIG = Configuration.getInstance();
  public static final String DATASET_PATH = CONFIG.getDataSetPath()
      + File.separator + "dataset3" + File.separator;
  public static final String TRAIN_DATA = DATASET_PATH + "trainingInput.txt";
  public static final String TEST_DATA = DATASET_PATH + "testingInput.txt";
  public static final String TRAIN_FILE = DATASET_PATH + "trainingInput.ser";
  public static final String TEST_FILE = DATASET_PATH + "testingInput.ser";

  private static final Logger LOG = LoggerFactory
      .getLogger(SupportVectorMachine.class);
  private static final boolean LOGGING = false;

  public static svm_parameter getDefaultParameter() {
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

  public static svm_problem generateProblem(List<? extends Tweet> trainTweets,
      ScoreClassifier scoreClassifier) {
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
      svmProb.y[i] = scoreClassifier.classfyScore(tweet.getScore());
    }

    return svmProb;
  }

  public static svm_model train(svm_problem svmProb, svm_parameter svmParam) {
    String paramCheck = svm.svm_check_parameter(svmProb, svmParam);
    if (paramCheck != null) {
      LOG.error("svm_check_parameter: " + paramCheck);
    }

    return svm.svm_train(svmProb, svmParam);
  }

  public static double crossValidate(svm_problem svmProb, svm_parameter svmParam) {
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

  public static void coarseGrainedParamterSearch(svm_problem svmProb,
      svm_parameter svmParam) {
    // coarse grained paramter search
    int maxC = 11;
    double[] c = new double[maxC];
    // C = 2^−5, 2^−3, ..., 2^15
    for (int i = 0; i < maxC; i++) {
      c[i] = Math.pow(2, -5 + (i * 2));
    }
    int maxGamma = 10;
    double[] gamma = new double[maxGamma];
    // gamma = 2^−15, 2^−13, ..., 2^3
    for (int j = 0; j < maxGamma; j++) {
      gamma[j] = Math.pow(2, -15 + (j * 2));
    }

    paramterSearch(svmProb, svmParam, c, gamma);
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
      double accuracy = crossValidate(m_svmProb, m_svmParam);
      long estimatedTime = System.currentTimeMillis() - startTime;
      return new double[] { m_i, m_j, accuracy, m_svmParam.C, m_svmParam.gamma,
          estimatedTime };
    }
  }

  public static void paramterSearch(svm_problem svmProb,
      svm_parameter svmParam, double[] c, double[] gamma) {
    int cores = Runtime.getRuntime().availableProcessors();
    ExecutorService executorService = Executors.newFixedThreadPool(cores);
    Set<Callable<double[]>> callables = new HashSet<Callable<double[]>>();

    for (int i = 0; i < c.length; i++) {
      for (int j = 0; j < gamma.length; j++) {
        svm_parameter param = (svm_parameter) svmParam.clone();
        param.C = c[i];
        param.gamma = gamma[j];
        callables.add(new FindParameterCallable(svmProb, param, i, j));
      }
    }

    try {
      long startTime = System.currentTimeMillis();
      List<Future<double[]>> futures = executorService.invokeAll(callables);
      for (Future<double[]> future : futures) {
        double[] result = future.get();
        LOG.info("findParamters[" + result[0] + "," + result[1] + "] C="
            + result[3] + " gamma=" + result[4] + " accuracy: " + result[2]
            + " time: " + result[5] + " ms");
      }
      long estimatedTime = System.currentTimeMillis() - startTime;
      LOG.info("findParamters total execution time: " + estimatedTime
          + " ms - " + (estimatedTime / 1000) + " sec");

      // output CSV file
      LOG.info("CSV file of paramterSearch with C=" + Arrays.toString(c)
          + " gamma=" + Arrays.toString(gamma));
      LOG.info("i;j;C;gamma;accuracy;time_ms");
      for (Future<double[]> future : futures) {
        double[] result = future.get();
        LOG.info(result[0] + ";" + result[1] + ";" + result[3] + ";"
            + result[4] + ";" + result[2] + ";" + result[5]);
      }

    } catch (InterruptedException e) {
      LOG.error(e.getMessage());
    } catch (ExecutionException e) {
      LOG.error(e.getMessage());
    }

    executorService.shutdown();
  }

  public static double evaluate(Tweet tweet, svm_model svmModel,
      int totalClasses, ScoreClassifier scoreClassifier) {
    return SupportVectorMachine.evaluate(tweet, svmModel, totalClasses,
        scoreClassifier, false);
  }

  public static double evaluate(Tweet tweet, svm_model svmModel,
      int totalClasses, ScoreClassifier scoreClassifier, boolean logging) {

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
      LOG.info("TweetClass:" + scoreClassifier.classfyScore(tweet.getScore())
          + " Prediction:" + predictedClass);
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
    boolean parameterSearch = false;
    try {
      // Prepare Train tweets
      LOG.info("Prepare Train data...");
      List<Tweet> trainTweets = SerializationUtils.deserialize(TRAIN_FILE);
      if (trainTweets == null) {
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
        trainTweets = FileUtils.readTweets(new FileInputStream(TRAIN_DATA));
        processTweets(posTagger, sfvg, trainTweets);
        SerializationUtils.serializeList(trainTweets, TRAIN_FILE);
      }

      // Prepare Test tweets
      LOG.info("Prepare Test data...");
      List<Tweet> testTweets = SerializationUtils.deserialize(TEST_FILE);
      if (testTweets == null) {
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
        testTweets = FileUtils.readTweets(new FileInputStream(TEST_DATA));
        processTweets(posTagger, sfvg, testTweets);
        SerializationUtils.serializeList(testTweets, TEST_FILE);
      }

      svm_parameter svmParam = getDefaultParameter();
      svm_problem svmProb = generateProblem(trainTweets,
          new IdentityScoreClassifier());

      // Optional parameter search of C and gamma
      if (parameterSearch) {
        // 1) coarse grained paramter search
        coarseGrainedParamterSearch(svmProb, svmParam);

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

        paramterSearch(svmProb, svmParam, c, gamma);

      } else {

        int totalClasses = 3;
        // classes 1 = positive, 0 = neutral, -1 = negative
        IdentityScoreClassifier isc = new IdentityScoreClassifier();

        // after parameter search use best C and gamma values
        svmParam.C = Math.pow(2, 6);
        svmParam.gamma = Math.pow(2, -5);

        // train model
        svm_model model = train(svmProb, svmParam);

        long countMatches = 0;
        for (Tweet tweet : testTweets) {
          double predictedClass = evaluate(tweet, model, totalClasses, isc);
          if (predictedClass == isc.classfyScore(tweet.getScore())) {
            countMatches++;
          }
        }

        LOG.info("Total test tweets: " + testTweets.size());
        LOG.info("Matches: " + countMatches);
        double accuracy = (double) countMatches / (double) testTweets.size();
        LOG.info("Accuracy: " + accuracy);
      }

    } catch (FileNotFoundException e) {
      LOG.error("FileNotFoundException: " + e.getMessage());
    } finally {
      if (sfvg != null) {
        sfvg.close();
      }
    }
  }
}
