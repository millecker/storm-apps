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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import at.illecker.storm.examples.util.DatasetProperty;
import at.illecker.storm.examples.util.io.FileUtils;
import at.illecker.storm.examples.util.io.SerializationUtils;
import at.illecker.storm.examples.util.preprocessor.Preprocessor;
import at.illecker.storm.examples.util.svm.classifier.IdentityScoreClassifier;
import at.illecker.storm.examples.util.svm.classifier.ScoreClassifier;
import at.illecker.storm.examples.util.svm.feature.FeatureVectorGenerator;
import at.illecker.storm.examples.util.svm.feature.SentimentFeatureVectorGenerator;
import at.illecker.storm.examples.util.svm.feature.TfIdfFeatureVectorGenerator;
import at.illecker.storm.examples.util.tagger.POSTagger;
import at.illecker.storm.examples.util.tfidf.TfIdfNormalization;
import at.illecker.storm.examples.util.tfidf.TfType;
import at.illecker.storm.examples.util.tfidf.TweetTfIdf;
import at.illecker.storm.examples.util.tokenizer.Tokenizer;
import at.illecker.storm.examples.util.tweet.Tweet;

public class SVM {
  public static final String SVM_PROBLEM_FILE = "svm_problem.txt";
  public static final String SVM_MODEL_FILE_SER = "svm_model.ser";
  private static final Logger LOG = LoggerFactory.getLogger(SVM.class);

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
    param.cache_size = 2000; // kernel cache specified in megabytes

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
      Map<Integer, Double> features = tweet.getFeatureVector();

      // set feature vector
      svmProb.x[i] = new svm_node[features.size()];
      int j = 0;
      for (Map.Entry<Integer, Double> feature : features.entrySet()) {
        svm_node node = new svm_node();
        node.index = feature.getKey();
        node.value = feature.getValue();
        svmProb.x[i][j] = node;
        j++;
      }

      // set class / label
      svmProb.y[i] = scoreClassifier.classfyScore(tweet.getScore());
    }

    return svmProb;
  }

  public static void saveProblem(svm_problem svmProb, String file) {
    // save problem in LIBSVM format
    // <label> <index1>:<value1> <index2>:<value2> ...
    try {
      BufferedWriter br = new BufferedWriter(new FileWriter(file));
      for (int i = 0; i < svmProb.l; i++) {
        // <label>
        br.write(Double.toString(svmProb.y[i]));
        for (int j = 0; j < svmProb.x[i].length; j++) {
          if (svmProb.x[i][j].value != 0) {
            // <index>:<value>
            br.write(" " + svmProb.x[i][j].index + ":" + svmProb.x[i][j].value);
          }
        }
        br.newLine();
        br.flush();
      }
      br.close();
      LOG.info("saved svm_problem in " + file);
    } catch (IOException e) {
      LOG.error("IOException: " + e.getMessage());
    }
  }

  public static svm_model train(svm_problem svmProb, svm_parameter svmParam) {
    String paramCheck = svm.svm_check_parameter(svmProb, svmParam);
    if (paramCheck != null) {
      LOG.error("svm_check_parameter: " + paramCheck);
    }

    return svm.svm_train(svmProb, svmParam);
  }

  public static double crossValidate(svm_problem svmProb,
      svm_parameter svmParam, int nFold) {
    double[] target = new double[svmProb.l];
    svm.svm_cross_validation(svmProb, svmParam, nFold, target);

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
      double accuracy = crossValidate(m_svmProb, m_svmParam, 10);
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
      LOG.error("InterruptedException: " + e.getMessage());
    } catch (ExecutionException e) {
      LOG.error("ExecutionException: " + e.getMessage());
    }

    executorService.shutdown();
  }

  public static double evaluate(Tweet tweet, svm_model svmModel,
      int totalClasses, ScoreClassifier scoreClassifier) {
    return evaluate(tweet, svmModel, totalClasses, scoreClassifier, false);
  }

  public static double evaluate(Tweet tweet, svm_model svmModel,
      int totalClasses, ScoreClassifier scoreClassifier, boolean logging) {

    Map<Integer, Double> features = tweet.getFeatureVector();
    svm_node[] nodes = new svm_node[features.size()];
    int i = 0;
    for (Map.Entry<Integer, Double> feature : features.entrySet()) {
      svm_node node = new svm_node();
      node.index = feature.getKey();
      node.value = feature.getValue();
      nodes[i] = node;
      i++;
    }

    int[] labels = new int[totalClasses];
    svm.svm_get_labels(svmModel, labels);

    double[] probEstimates = new double[totalClasses];
    double predictedClass = svm.svm_predict_probability(svmModel, nodes,
        probEstimates);

    if (logging) {
      for (i = 0; i < totalClasses; i++) {
        LOG.info("Label[" + i + "]: " + labels[i] + " Probability: "
            + probEstimates[i]);
      }
      LOG.info("TweetClass:" + scoreClassifier.classfyScore(tweet.getScore())
          + " Prediction:" + predictedClass);
    }

    return predictedClass;
  }

  public static void svm(DatasetProperty datasetProperty,
      Class<? extends FeatureVectorGenerator> featureVectorGenerator,
      int nFoldCrossValidation, boolean parameterSearch) {
    FeatureVectorGenerator fvg = null;
    Preprocessor preprocessor = null;
    POSTagger posTagger = null;

    // Prepare Train tweets
    LOG.info("Prepare Train data...");
    List<Tweet> trainTweets = SerializationUtils.deserialize(datasetProperty
        .getTrainDataSerializationFile());

    if (trainTweets == null) {
      // Read train tweets
      trainTweets = FileUtils.readTweets(datasetProperty.getTrainDataFile(),
          datasetProperty);
      LOG.info("Read train tweets from " + datasetProperty.getTrainDataFile());

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

      if (featureVectorGenerator.equals(TfIdfFeatureVectorGenerator.class)) {
        TweetTfIdf tweetTfIdf = new TweetTfIdf(trainTweets, TfType.RAW,
            TfIdfNormalization.COS, true);
        LOG.info("Load TfIdfFeatureVectorGenerator...");
        fvg = new TfIdfFeatureVectorGenerator(tweetTfIdf);

      } else if (featureVectorGenerator
          .equals(SentimentFeatureVectorGenerator.class)) {
        LOG.info("Load SentimentFeatureVectorGenerator...");
        fvg = new SentimentFeatureVectorGenerator();

      } else {
        throw new UnsupportedOperationException("FeatureVectorGenerator '"
            + featureVectorGenerator.getName() + "' is not supported!");
      }

      // Feature Vector Generation
      LOG.info("Generate Feature Vectors for train tweets...");
      fvg.generateFeatureVectors(trainTweets);

      // Serialize training data
      SerializationUtils.serializeList(trainTweets,
          datasetProperty.getTrainDataSerializationFile());
    }

    // Prepare Test tweets
    LOG.info("Prepare Test data...");
    List<Tweet> testTweets = SerializationUtils.deserialize(datasetProperty
        .getTestDataSerializationFile());

    if (testTweets == null) {
      if (fvg == null) {
        LOG.error("Train and test data must use the same FeatureVectorGenerator!");
        System.exit(1);
      }

      // read test tweets
      testTweets = FileUtils.readTweets(datasetProperty.getTestDataFile(),
          datasetProperty);
      LOG.info("Read test tweets from " + datasetProperty.getTestDataFile());

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
          datasetProperty.getTestDataSerializationFile());
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
      svm_model svmModel = SerializationUtils.deserialize(datasetProperty
          .getDatasetPath() + File.separator + SVM_MODEL_FILE_SER);
      if (svmModel == null) {
        LOG.info("Generate SVM problem...");
        svm_problem svmProb = SVM.generateProblem(trainTweets, isc);

        // save svm problem in libSVM format
        SVM.saveProblem(svmProb, datasetProperty.getDatasetPath()
            + File.separator + SVM_PROBLEM_FILE);

        // train model
        LOG.info("Train SVM model...");
        long startTime = System.currentTimeMillis();
        svmModel = SVM.train(svmProb, datasetProperty.getSVMParam());
        LOG.info("Train SVM model finished after "
            + (System.currentTimeMillis() - startTime) + " ms");

        // serialize svm model
        SerializationUtils.serialize(svmModel, datasetProperty.getDatasetPath()
            + File.separator + SVM_MODEL_FILE_SER);

        // Run n-fold cross validation
        if (nFoldCrossValidation > 0) {
          LOG.info("Run n-fold cross validation...");
          startTime = System.currentTimeMillis();
          double accuracy = SVM.crossValidate(svmProb,
              datasetProperty.getSVMParam(), nFoldCrossValidation);
          LOG.info("CrossValidation finished after "
              + (System.currentTimeMillis() - startTime) + " ms");
          LOG.info("Cross Validation Accurancy: " + accuracy);
        }
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

  public static void main(String[] args) {
    // SVM.svm(Configuration.getDataSet3(),
    // SentimentFeatureVectorGenerator.class,
    // 3, false);

    SVM.svm(Configuration.getDataSet3(), TfIdfFeatureVectorGenerator.class, 3,
        false);
  }
}
