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
package at.illecker.storm.examples.util;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import libsvm.svm_parameter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.io.FileUtils;
import at.illecker.storm.examples.util.tweet.Tweet;

public class Dataset {
  private static final Logger LOG = LoggerFactory.getLogger(Dataset.class);

  private String m_datasetPath;
  private String m_trainDataFile;
  private String m_testDataFile;

  private String m_delimiter;
  private int m_idIndex;
  private int m_labelIndex;
  private int m_textIndex;

  private String[] m_positiveLabels;
  private String[] m_neutralLabels;
  private String[] m_negativeLabels;

  private int m_positiveValue;
  private int m_neutralValue;
  private int m_negativeValue;

  private svm_parameter m_svmParam;

  private List<Tweet> m_trainTweets = null;
  private List<Tweet> m_testTweets = null;

  public Dataset(String datasetPath, String trainDataFile, String testDataFile,
      String delimiter, int idIndex, int labelIndex, int textIndex,
      String[] positiveLabels, String[] neutralLabels, String[] negativeLabels,
      int positiveValue, int neutralValue, int negativeValue,
      svm_parameter svmParam) {
    this.m_datasetPath = datasetPath;
    this.m_trainDataFile = trainDataFile;
    this.m_testDataFile = testDataFile;

    this.m_delimiter = delimiter;

    this.m_idIndex = idIndex;
    this.m_labelIndex = labelIndex;
    this.m_textIndex = textIndex;

    this.m_positiveLabels = positiveLabels;
    this.m_neutralLabels = neutralLabels;
    this.m_negativeLabels = negativeLabels;

    this.m_positiveValue = positiveValue;
    this.m_neutralValue = neutralValue;
    this.m_negativeValue = negativeValue;

    this.m_svmParam = svmParam;
  }

  public String getDatasetPath() {
    return m_datasetPath;
  }

  public String getTrainDataFile() {
    return m_datasetPath + File.separator + m_trainDataFile;
  }

  public String getTrainDataSerializationFile() {
    return m_datasetPath + File.separator + m_trainDataFile + ".ser";
  }

  public String getTestDataFile() {
    return m_datasetPath + File.separator + m_testDataFile;
  }

  public String getTestDataSerializationFile() {
    return m_datasetPath + File.separator + m_testDataFile + ".ser";
  }

  public String getDelimiter() {
    return m_delimiter;
  }

  public int getIdIndex() {
    return m_idIndex;
  }

  public int getLabelIndex() {
    return m_labelIndex;
  }

  public int getTextIndex() {
    return m_textIndex;
  }

  public String[] getPositiveLabels() {
    return m_positiveLabels;
  }

  public String[] getNeutralLabels() {
    return m_neutralLabels;
  }

  public String[] getNegativeLabels() {
    return m_negativeLabels;
  }

  public int getPositiveValue() {
    return m_positiveValue;
  }

  public int getNeutralValue() {
    return m_neutralValue;
  }

  public int getNegativeValue() {
    return m_negativeValue;
  }

  public svm_parameter getSVMParam() {
    return m_svmParam;
  }

  public List<Tweet> getTrainTweets() {
    if (m_trainTweets == null) {
      m_trainTweets = FileUtils.readTweets(this.getTrainDataFile(), this);
    }
    return m_trainTweets;
  }

  public List<Tweet> getTestTweets() {
    if (m_testTweets == null) {
      m_testTweets = FileUtils.readTweets(this.getTestDataFile(), this);
    }
    return m_testTweets;
  }

  public void printDatasetStats() {
    List<Tweet> trainTweets = getTrainTweets();
    List<Tweet> testTweets = getTestTweets();

    Map<Integer, Integer> trainCounts = new TreeMap<Integer, Integer>();
    for (Tweet tweet : trainTweets) {
      int key = tweet.getScore().intValue();
      Integer counts = trainCounts.get(key);
      trainCounts.put(key, ((counts != null) ? counts + 1 : 1));
    }

    Map<Integer, Integer> testCounts = new TreeMap<Integer, Integer>();
    for (Tweet tweet : testTweets) {
      int key = tweet.getScore().intValue();
      Integer counts = testCounts.get(key);
      testCounts.put(key, ((counts != null) ? counts + 1 : 1));
    }

    LOG.info("Dataset: " + getDatasetPath());
    LOG.info("Train Dataset: " + getTrainDataFile());
    int trainSum = 0;
    int trainMax = 0;
    for (Map.Entry<Integer, Integer> entry : trainCounts.entrySet()) {
      LOG.info("Class: \t" + entry.getKey() + "\t" + entry.getValue());
      trainSum += entry.getValue();
      if (entry.getValue() > trainMax) {
        trainMax = entry.getValue();
      }
    }
    LOG.info("Total: " + trainSum);

    LOG.info("Train Class Weights: ");
    for (Map.Entry<Integer, Integer> entry : trainCounts.entrySet()) {
      LOG.info("Class: \t" + entry.getKey() + "\t"
          + (trainMax / (double) entry.getValue()));
    }

    LOG.info("Test Dataset: " + getTestDataFile());
    int testSum = 0;
    int testMax = 0;
    for (Map.Entry<Integer, Integer> entry : testCounts.entrySet()) {
      LOG.info("Class: \t" + entry.getKey() + "\t" + entry.getValue());
      testSum += entry.getValue();
      if (entry.getValue() > testMax) {
        testMax = entry.getValue();
      }
    }
    LOG.info("Total: " + testSum);

    LOG.info("Test Class Weights: ");
    for (Map.Entry<Integer, Integer> entry : testCounts.entrySet()) {
      LOG.info("Class: \t" + entry.getKey() + "\t"
          + (testMax / (double) entry.getValue()));
    }
  }

  @Override
  public String toString() {
    return "DatasetProperty [datasetPath=" + m_datasetPath + ", trainDataFile="
        + m_trainDataFile + ", testDataFile=" + m_testDataFile + ", delimiter="
        + m_delimiter + ", idIndex=" + m_idIndex + ", textIndex=" + m_textIndex
        + ", labelIndex=" + m_labelIndex + ", positiveLabels="
        + Arrays.toString(m_positiveLabels) + ", neutralLabel="
        + Arrays.toString(m_neutralLabels) + ", negativeLabel="
        + Arrays.toString(m_negativeLabels) + ", positiveValue="
        + m_positiveValue + ", neutralValue=" + m_neutralValue
        + ", negativeValue=" + m_negativeValue + "]";
  }

  public static void main(String[] args) {
    Dataset dataSet2013Mixed = Configuration.getDataSetSemEval2013Mixed();
    dataSet2013Mixed.printDatasetStats();

    Dataset dataSet2013 = Configuration.getDataSetSemEval2013();
    dataSet2013.printDatasetStats();

    Dataset dataSet2014 = Configuration.getDataSetSemEval2014();
    dataSet2014.printDatasetStats();
  }
}
