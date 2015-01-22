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
import at.illecker.storm.examples.util.Dataset;
import at.illecker.storm.examples.util.io.SerializationUtils;
import at.illecker.storm.examples.util.preprocessor.Preprocessor;
import at.illecker.storm.examples.util.svm.classifier.IdentityScoreClassifier;
import at.illecker.storm.examples.util.svm.classifier.ScoreClassifier;
import at.illecker.storm.examples.util.svm.feature.CombinedFeatureVectorGenerator;
import at.illecker.storm.examples.util.svm.feature.FeatureVectorGenerator;
import at.illecker.storm.examples.util.svm.feature.SentimentFeatureVectorGenerator;
import at.illecker.storm.examples.util.svm.feature.TfIdfFeatureVectorGenerator;
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

public class SVM {
  public static final String SVM_PROBLEM_FILE = "svm_problem.txt";
  public static final String SVM_MODEL_FILE_SER = "svm_model.ser";
  private static final Logger LOG = LoggerFactory.getLogger(SVM.class);

  public static svm_parameter getDefaultParameter() {
    svm_parameter param = new svm_parameter();
    // type of SVM
    param.svm_type = svm_parameter.C_SVC; // default

    // type of kernel function
    param.kernel_type = svm_parameter.RBF; // default

    // degree in kernel function (default 3)
    param.degree = 3;

    // gamma in kernel function (default 1/num_features)
    // gamma = 2^−15, 2^−13, ..., 2^3
    param.gamma = Double.MIN_VALUE;

    // parameter C of C-SVC, epsilon-SVR, and nu-SVR (default 1)
    // C = 2^−5, 2^−3, ..., 2^15
    param.C = 1; // cost of constraints violation default 1

    // coef0 in kernel function (default 0)
    param.coef0 = 0;

    // parameter nu of nu-SVC, one-class SVM, and nu-SVR (default 0.5)
    param.nu = 0.5;

    // epsilon in loss function of epsilon-SVR (default 0.1)
    param.p = 0.1;

    // tolerance of termination criterion (default 0.001)
    param.eps = 0.001;

    // whether to use the shrinking heuristics, 0 or 1
    // (default 1)
    param.shrinking = 1;

    // whether to train a SVC or SVR model for probability estimates, 0 or 1
    // (default 0)
    // 1 means model with probability information is obtained
    param.probability = 1;

    // parameter C of class i to weight*C, for C-SVC (default 1)
    param.nr_weight = 0;
    param.weight_label = new int[0];
    param.weight = new double[0];

    // cache memory size in MB (default 100)
    param.cache_size = 2000;

    return param;
  }

  public static svm_problem generateProblem(List<FeaturedTweet> trainTweets,
      ScoreClassifier scoreClassifier) {
    int dataCount = trainTweets.size();

    svm_problem svmProb = new svm_problem();
    svmProb.y = new double[dataCount];
    svmProb.l = dataCount;
    svmProb.x = new svm_node[dataCount][];

    for (int i = 0; i < dataCount; i++) {
      FeaturedTweet tweet = trainTweets.get(i);
      Map<Integer, Double> featureVector = tweet.getFeatureVector();
      // set feature nodes
      svmProb.x[i] = new svm_node[featureVector.size()];
      int j = 0;
      for (Map.Entry<Integer, Double> feature : featureVector.entrySet()) {
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
    // save problem in libSVM format
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
    // set gamma to default 1/num_features if not specified
    if (svmParam.gamma == Double.MIN_VALUE) {
      svmParam.gamma = 1 / (double) svmProb.l;
    }

    String paramCheck = svm.svm_check_parameter(svmProb, svmParam);
    if (paramCheck != null) {
      LOG.error("svm_check_parameter: " + paramCheck);
    }

    return svm.svm_train(svmProb, svmParam);
  }

  public static double crossValidate(svm_problem svmProb,
      svm_parameter svmParam, int nFold) {
    return crossValidate(svmProb, svmParam, nFold, false);
  }

  public static double crossValidate(svm_problem svmProb,
      svm_parameter svmParam, int nFold, boolean printStats) {

    // set gamma to default 1/num_features if not specified
    if (svmParam.gamma == Double.MIN_VALUE) {
      svmParam.gamma = 1 / (double) svmProb.l;
    }

    double[] target = new double[svmProb.l];
    svm.svm_cross_validation(svmProb, svmParam, nFold, target);

    double correctCounter = 0;
    for (int i = 0; i < svmProb.l; i++) {
      if (target[i] == svmProb.y[i]) {
        correctCounter++;
      }
    }

    double accuracy = correctCounter / (double) svmProb.l;
    LOG.info("Cross Validation Accuracy: " + (100.0 * accuracy));

    if (printStats) {
      printStats(getConfusionMatrix(svmProb.y, target));
    }

    return accuracy;
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

  public static double evaluate(FeaturedTweet tweet, svm_model svmModel,
      int totalClasses, ScoreClassifier scoreClassifier) {
    return evaluate(tweet, svmModel, totalClasses, scoreClassifier, false);
  }

  public static double evaluate(FeaturedTweet tweet, svm_model svmModel,
      int totalClasses, ScoreClassifier scoreClassifier, boolean logging) {

    Map<Integer, Double> featureVector = tweet.getFeatureVector();
    // set feature nodes
    svm_node[] testNodes = new svm_node[featureVector.size()];
    int i = 0;
    for (Map.Entry<Integer, Double> feature : featureVector.entrySet()) {
      svm_node node = new svm_node();
      node.index = feature.getKey();
      node.value = feature.getValue();
      testNodes[i] = node;
      i++;
    }

    double predictedClass = svm.svm_predict(svmModel, testNodes);

    if (logging) {
      int[] labels = new int[totalClasses];
      svm.svm_get_labels(svmModel, labels);

      double[] probEstimates = new double[totalClasses];
      double predictedClassProb = svm.svm_predict_probability(svmModel,
          testNodes, probEstimates);

      for (i = 0; i < totalClasses; i++) {
        LOG.info("Label[" + i + "]: " + labels[i] + " Probability: "
            + probEstimates[i]);
      }
      LOG.info("TweetClass: " + scoreClassifier.classfyScore(tweet.getScore())
          + " Prediction: " + predictedClassProb);
    }

    return predictedClass;
  }

  public static int[][] getConfusionMatrix(double[] actualClass,
      double[] predictedClass) {
    if (actualClass.length != predictedClass.length) {
      return null;
    }

    // find the total number of classes
    int maxClassNum = 0;
    for (int i = 0; i < actualClass.length; i++) {
      if (actualClass[i] > maxClassNum)
        maxClassNum = (int) actualClass[i];
    }
    // add 1 because of class zero
    maxClassNum++;

    // create confusion matrix
    // rows represent the instances in an actual class
    // cols represent the instances in a predicted class
    int[][] confusionMatrix = new int[maxClassNum][maxClassNum];
    for (int i = 0; i < actualClass.length; i++) {
      confusionMatrix[(int) actualClass[i]][(int) predictedClass[i]]++;
    }
    return confusionMatrix;
  }

  public static void printStats(int[][] confusionMatrix) {
    int totalClasses = confusionMatrix.length;
    int total = 0;
    int totalCorrect = 0;

    int[] rowSum = new int[totalClasses];
    int[] colSum = new int[totalClasses];
    for (int i = 0; i < totalClasses; i++) {
      for (int j = 0; j < totalClasses; j++) {
        total += confusionMatrix[i][j];
        rowSum[i] += confusionMatrix[i][j];
        colSum[i] += confusionMatrix[j][i];
      }
      totalCorrect += confusionMatrix[i][i];
    }

    LOG.info("Confusion Matrix:");
    // print header
    StringBuffer sb = new StringBuffer();
    sb.append("\t\t");
    for (int i = 0; i < totalClasses; i++) {
      sb.append("\t").append(i);
    }
    sb.append("\t").append("total");
    LOG.info(sb.toString());
    // print matrix
    for (int i = 0; i < totalClasses; i++) {
      int[] predictedClasses = confusionMatrix[i];
      sb = new StringBuffer();
      sb.append("Class:\t" + i);
      for (int j = 0; j < predictedClasses.length; j++) {
        sb.append("\t").append(predictedClasses[j]);
      }
      sb.append("\t" + rowSum[i]);
      LOG.info(sb.toString());
    }
    sb = new StringBuffer();
    sb.append("total").append("\t");
    for (int i = 0; i < totalClasses; i++) {
      sb.append("\t").append(colSum[i]);
    }
    LOG.info(sb.toString() + "\n");

    LOG.info("Total: " + total);
    LOG.info("Correct: " + totalCorrect);
    LOG.info("Accuracy: " + (totalCorrect / (double) total));

    LOG.info("Scores per class:");
    double[] FScores = new double[totalClasses];
    for (int i = 0; i < totalClasses; i++) {
      int correctHitsPerClass = confusionMatrix[i][i];

      double precision = correctHitsPerClass / (double) colSum[i];
      double recall = correctHitsPerClass / (double) rowSum[i];
      FScores[i] = 2 * ((precision * recall) / (precision + recall));

      LOG.info("Class: " + i + " Precision: " + precision + " Recall: "
          + recall + " F-Score: " + FScores[i]);
    }

    // FScoreWeighted is a weighted average of the classes' f-scores, weighted
    // by the proportion of how many elements are in each class.
    double FScoreWeighted = 0;
    for (int i = 0; i < totalClasses; i++) {
      FScoreWeighted += FScores[i] * colSum[i];
    }
    FScoreWeighted /= total;
    LOG.info("F-Score weighted: " + FScoreWeighted);

    // F-Score average of positive and negative
    double FScoreAveragePosNeg = (FScores[0] + FScores[1]) / 2;
    LOG.info("F-Score average(pos,neg): " + FScoreAveragePosNeg);

    // Macro-average: Average precision, recall, or F1 over the classes of
    // interest.

    // Micro-average: Sum corresponding cells to create a 2 x 2 confusion
    // matrix, and calculate precision in terms of the new matrix.
    // (In this set-up, precision, recall, and F1 are all the same.)
  }

  public static void svm(Dataset dataset,
      Class<? extends FeatureVectorGenerator> featureVectorGenerator,
      int nFoldCrossValidation, boolean parameterSearch,
      boolean useSerialization) {

    FeatureVectorGenerator fvg = null;
    Preprocessor preprocessor = null;
    POSTagger posTagger = null;

    // Prepare Train tweets
    LOG.info("Prepare Train data...");
    List<FeaturedTweet> trainFeaturedTweets = null;
    if (useSerialization) {
      trainFeaturedTweets = SerializationUtils.deserialize(dataset
          .getTrainDataSerializationFile());
    }

    if (trainFeaturedTweets == null) {
      // Read train tweets
      List<Tweet> trainTweets = dataset.getTrainTweets(true);
      LOG.info("Read train tweets from " + dataset.getTrainDataFile());
      Dataset.printTweetStats(trainTweets);

      // Tokenize
      LOG.info("Tokenize train tweets...");
      List<TokenizedTweet> tokenizedTweets = Tokenizer
          .tokenizeTweets(trainTweets);

      // Preprocess
      preprocessor = Preprocessor.getInstance();
      LOG.info("Preprocess train tweets...");
      List<PreprocessedTweet> preprocessedTweets = preprocessor
          .preprocessTweets(tokenizedTweets);

      // POS Tagging
      posTagger = POSTagger.getInstance();
      LOG.info("POS Tagging of train tweets...");
      List<TaggedTweet> taggedTweets = posTagger.tagTweets(preprocessedTweets);

      // Serialize training data including POS tags
      if (useSerialization) {
        SerializationUtils.serializeList(taggedTweets,
            dataset.getTrainTaggedDataSerializationFile());
      }

      // Feature Vector Generation
      if (featureVectorGenerator.equals(SentimentFeatureVectorGenerator.class)) {
        LOG.info("Load SentimentFeatureVectorGenerator...");
        fvg = new SentimentFeatureVectorGenerator();

      } else if (featureVectorGenerator
          .equals(TfIdfFeatureVectorGenerator.class)) {
        TweetTfIdf tweetTfIdf = new TweetTfIdf(taggedTweets, TfType.RAW,
            TfIdfNormalization.COS, true);
        LOG.info("Load TfIdfFeatureVectorGenerator...");
        fvg = new TfIdfFeatureVectorGenerator(tweetTfIdf);

      } else if (featureVectorGenerator
          .equals(CombinedFeatureVectorGenerator.class)) {
        TweetTfIdf tweetTfIdf = new TweetTfIdf(taggedTweets, TfType.RAW,
            TfIdfNormalization.COS, true);
        LOG.info("Load CombinedFeatureVectorGenerator...");
        fvg = new CombinedFeatureVectorGenerator(tweetTfIdf);

      } else {
        throw new UnsupportedOperationException("FeatureVectorGenerator '"
            + featureVectorGenerator.getName() + "' is not supported!");
      }

      // Feature Vector Generation
      LOG.info("Generate Feature Vectors for test tweets...");
      trainFeaturedTweets = fvg.generateFeatureVectors(taggedTweets);

      // Serialize training data including feature vectors
      if (useSerialization) {
        SerializationUtils.serializeList(trainFeaturedTweets,
            dataset.getTrainDataSerializationFile());
      }
    }

    // Prepare Test tweets
    LOG.info("Prepare Test data...");
    List<FeaturedTweet> testFeaturedTweets = null;
    if (useSerialization) {
      testFeaturedTweets = SerializationUtils.deserialize(dataset
          .getTestDataSerializationFile());
    }

    if (testFeaturedTweets == null) {
      if (fvg == null) {
        LOG.error("Train and test data must use the same FeatureVectorGenerator!");
        System.exit(1);
      }

      // read test tweets
      List<Tweet> testTweets = dataset.getTestTweets();
      LOG.info("Read test tweets from " + dataset.getTestDataFile());

      // Tokenize
      List<TokenizedTweet> tokenizedTweets = Tokenizer
          .tokenizeTweets(testTweets);

      // Preprocess
      List<PreprocessedTweet> preprocessedTweets = preprocessor
          .preprocessTweets(tokenizedTweets);

      // POS Tagging
      List<TaggedTweet> taggedTweets = posTagger.tagTweets(preprocessedTweets);

      // Feature Vector Generation
      LOG.info("Generate Feature Vectors for test tweets...");
      testFeaturedTweets = fvg.generateFeatureVectors(taggedTweets);

      // Serialize test data
      if (useSerialization) {
        SerializationUtils.serializeList(testFeaturedTweets,
            dataset.getTestDataSerializationFile());
      }
    }

    // Optional parameter search of C and gamma
    if (parameterSearch) {
      svm_parameter svmParam = getDefaultParameter();
      LOG.info("Generate SVM problem...");
      svm_problem svmProb = generateProblem(trainFeaturedTweets,
          new IdentityScoreClassifier());

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

      LOG.info("SVM paramterSearch...");
      paramterSearch(svmProb, svmParam, c, gamma);

    } else {

      int totalClasses = 3;
      // classes 0 = negative, 1 = neutral, 2 = positive
      IdentityScoreClassifier isc = new IdentityScoreClassifier();

      svm_model svmModel = null;
      LOG.info("Try loading SVM model...");
      // deserialize svmModel
      if (useSerialization) {
        svmModel = SerializationUtils.deserialize(dataset.getDatasetPath()
            + File.separator + SVM_MODEL_FILE_SER);
      }

      if (svmModel == null) {
        LOG.info("Generate SVM problem...");
        svm_problem svmProb = generateProblem(trainFeaturedTweets, isc);

        // save svm problem in libSVM format
        saveProblem(svmProb, dataset.getDatasetPath() + File.separator
            + SVM_PROBLEM_FILE);

        // train model
        LOG.info("Train SVM model...");
        long startTime = System.currentTimeMillis();
        svmModel = train(svmProb, dataset.getSVMParam());
        LOG.info("Train SVM model finished after "
            + (System.currentTimeMillis() - startTime) + " ms");

        // serialize svm model
        if (useSerialization) {
          SerializationUtils.serialize(svmModel, dataset.getDatasetPath()
              + File.separator + SVM_MODEL_FILE_SER);
        }

        // Run n-fold cross validation
        if (nFoldCrossValidation > 1) {
          LOG.info("Run n-fold cross validation...");
          startTime = System.currentTimeMillis();
          double accuracy = crossValidate(svmProb, dataset.getSVMParam(),
              nFoldCrossValidation, true);
          LOG.info("Cross Validation finished after "
              + (System.currentTimeMillis() - startTime) + " ms");
          LOG.info("Cross Validation Accurancy: " + accuracy);
        }
      }

      // Evaluate test tweets
      long countMatches = 0;
      int[][] confusionMatrix = new int[totalClasses][totalClasses];
      LOG.info("Evaluate test tweets...");

      long startTime = System.currentTimeMillis();
      for (FeaturedTweet tweet : testFeaturedTweets) {
        double predictedClass = evaluate(tweet, svmModel, totalClasses, isc);
        int actualClass = isc.classfyScore(tweet.getScore());
        if (predictedClass == actualClass) {
          countMatches++;
        }
        confusionMatrix[actualClass][(int) predictedClass]++;
      }

      LOG.info("Evaluate finished after "
          + (System.currentTimeMillis() - startTime) + " ms");
      LOG.info("Total test tweets: " + testFeaturedTweets.size());
      LOG.info("Matches: " + countMatches);
      double accuracy = (double) countMatches
          / (double) testFeaturedTweets.size();
      LOG.info("Accuracy: " + accuracy);

      printStats(confusionMatrix);

      svm.EXEC_SERV.shutdown();
    }
  }

  public static void main(String[] args) {
    int nFoldCrossValidation = 3;
    int featureVectorLevel = 2;
    Dataset dataSet = Configuration.getDataSetSemEval2013();
    boolean parameterSearch = false;
    boolean useSerialization = true;

    if (featureVectorLevel == 0) {
      SVM.svm(dataSet, SentimentFeatureVectorGenerator.class,
          nFoldCrossValidation, parameterSearch, useSerialization);
    } else if (featureVectorLevel == 1) {
      SVM.svm(dataSet, TfIdfFeatureVectorGenerator.class, nFoldCrossValidation,
          parameterSearch, useSerialization);
    } else {
      SVM.svm(dataSet, CombinedFeatureVectorGenerator.class,
          nFoldCrossValidation, parameterSearch, useSerialization);
    }
  }
}
