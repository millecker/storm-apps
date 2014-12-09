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
package at.illecker.storm.examples.util.supervised;

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
import at.illecker.storm.examples.util.FileUtil;
import at.illecker.storm.examples.util.Tweet;

public class SupportVectorMaschine {

  public static final String TRAIN_DATA = System.getProperty("user.dir")
      + File.separator + "resources" + File.separator + "tweets"
      + File.separator + "svm" + File.separator + "trainingInput.txt";
  public static final String TEST_DATA = System.getProperty("user.dir")
      + File.separator + "resources" + File.separator + "tweets"
      + File.separator + "svm" + File.separator + "testingInput.txt";

  public static void test() {
    // http://stackoverflow.com/questions/10792576/libsvm-java-implementation
    double[][] train = new double[100][];
    double[][] test = new double[10][];

    for (int i = 0; i < train.length; i++) {
      if (i + 1 > (train.length / 2)) { // 50% positive
        double[] vals = { 1, 0, i + i };
        train[i] = vals;
      } else {
        double[] vals = { 0, 0, i - i - i - 2 }; // 50% negative
        train[i] = vals;
      }
    }

    System.out.println("Training Set:");
    for (int i = 0; i < train.length; i++) {
      System.out.println("  " + Arrays.toString(train[i]));
    }

    for (int i = 0; i < test.length; i++) {
      if (i + 1 > (test.length / 2)) { // 50% positive
        double[] vals = { 1, 0, i + i + train.length };
        test[i] = vals;
      } else {
        double[] vals = { 0, 0, i - i - i - 2 }; // 50% negative
        test[i] = vals;
      }
    }

    System.out.println("Testing Set:");
    for (int i = 0; i < test.length; i++) {
      System.out.println("  " + Arrays.toString(test[i]));
    }

    System.out.println("Train model...");
    svm_model model = svmTrain(train);

    System.out.println("Evaluate model...");
    for (int i = 0; i < test.length; i++) {
      evaluate(test[i], model);
    }
  }

  private static svm_model svmTrain(double[][] train) {
    svm_problem prob = new svm_problem();
    int dataCount = train.length;
    prob.y = new double[dataCount];
    prob.l = dataCount;
    prob.x = new svm_node[dataCount][];

    for (int i = 0; i < dataCount; i++) {
      double[] features = train[i]; // feature vector
      prob.x[i] = new svm_node[features.length - 1];
      for (int j = 1; j < features.length; j++) {
        svm_node node = new svm_node();
        node.index = j;
        node.value = features[j];
        prob.x[i][j - 1] = node;
      }
      prob.y[i] = features[0];
    }

    svm_parameter param = new svm_parameter();
    param.probability = 1;
    param.gamma = 0.5;
    param.nu = 0.5;
    param.C = 1;
    param.svm_type = svm_parameter.C_SVC;
    param.kernel_type = svm_parameter.LINEAR;
    param.cache_size = 20000;
    param.eps = 0.001;

    svm_model model = svm.svm_train(prob, param);

    return model;
  }

  private static double evaluate(double[] features, svm_model model) {
    svm_node[] nodes = new svm_node[features.length - 1];
    for (int i = 1; i < features.length; i++) {
      svm_node node = new svm_node();
      node.index = i;
      node.value = features[i];

      nodes[i - 1] = node;
    }

    int totalClasses = 2;
    int[] labels = new int[totalClasses];
    svm.svm_get_labels(model, labels);

    double[] prob_estimates = new double[totalClasses];
    double v = svm.svm_predict_probability(model, nodes, prob_estimates);

    for (int i = 0; i < totalClasses; i++) {
      System.out.print("(" + labels[i] + ":" + prob_estimates[i] + ")");
    }
    System.out.println("(Actual:" + features[0] + " Prediction:" + v + ")");

    return v;
  }

  public static void main(String[] args) {
    // test();

    try {
      List<Tweet> trainTweets = FileUtil.readTweets(new FileInputStream(
          TRAIN_DATA));
      List<Tweet> testTweets = FileUtil.readTweets(new FileInputStream(
          TEST_DATA));

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }

  }
}
