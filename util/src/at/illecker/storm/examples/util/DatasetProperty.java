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

public class DatasetProperty {
  private String m_datasetPath;
  private String m_trainDataFile;
  private String m_testDataFile;

  private String m_delimiter;
  private int m_idIndex;
  private int m_textIndex;
  private int m_labelIndex;

  private String m_positiveLabel;
  private String m_neutralLabel;
  private String m_negativeLabel;

  private int m_positiveValue;
  private int m_neutralValue;
  private int m_negativeValue;

  public DatasetProperty(String datasetPath, String trainDataFile,
      String testDataFile, String delimiter, int idIndex, int textIndex,
      int labelIndex, String positiveLabel, String neutralLabel,
      String negativeLabel, int positiveValue, int neutralValue,
      int negativeValue) {
    this.m_datasetPath = datasetPath;
    this.m_trainDataFile = trainDataFile;
    this.m_testDataFile = testDataFile;

    this.m_delimiter = delimiter;
    this.m_idIndex = idIndex;
    this.m_textIndex = textIndex;
    this.m_labelIndex = labelIndex;

    this.m_positiveLabel = positiveLabel;
    this.m_neutralLabel = neutralLabel;
    this.m_negativeLabel = negativeLabel;

    this.m_positiveValue = positiveValue;
    this.m_neutralValue = neutralValue;
    this.m_negativeValue = negativeValue;
  }

  public DatasetProperty(String datasetPath, String trainDataFile,
      String testDataFile, String delimiter, int idIndex, int textIndex,
      int labelIndex, String positiveLabel, String neutralLabel,
      String negativeLabel) {
    this(datasetPath, trainDataFile, testDataFile, delimiter, idIndex,
        textIndex, labelIndex, positiveLabel, neutralLabel, negativeLabel, 0,
        1, 2);
  }

  public DatasetProperty(String datasetPath, String trainDataFile,
      String testDataFile, String delimiter, int idIndex, int textIndex,
      int labelIndex) {
    this(datasetPath, trainDataFile, testDataFile, delimiter, idIndex,
        textIndex, labelIndex, "positive", "neutral", "negative");
  }

  public DatasetProperty(String datasetPath, String trainDataFile,
      String testDataFile) {
    this(datasetPath, trainDataFile, testDataFile, "\t", 0, 1, 2);
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

  public int getTextIndex() {
    return m_textIndex;
  }

  public int getLabelIndex() {
    return m_labelIndex;
  }

  public String getPositiveLabel() {
    return m_positiveLabel;
  }

  public String getNeutralLabel() {
    return m_neutralLabel;
  }

  public String getNegativeLabel() {
    return m_negativeLabel;
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

  @Override
  public String toString() {
    return "DatasetProperty [datasetPath=" + m_datasetPath + ", trainDataFile="
        + m_trainDataFile + ", testDataFile=" + m_testDataFile + ", delimiter="
        + m_delimiter + ", idIndex=" + m_idIndex + ", textIndex=" + m_textIndex
        + ", labelIndex=" + m_labelIndex + ", positiveLabel=" + m_positiveLabel
        + ", neutralLabel=" + m_neutralLabel + ", negativeLabel="
        + m_negativeLabel + ", positiveValue=" + m_positiveValue
        + ", neutralValue=" + m_neutralValue + ", negativeValue="
        + m_negativeValue + "]";
  }
}
