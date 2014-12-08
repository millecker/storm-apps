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
package at.illecker.storm.examples.util.unsupervised;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.unsupervised.sentiwordnet.SentiWordNet;
import at.illecker.storm.examples.util.unsupervised.wordnet.WordNet;

public class UnsupervisedSentimentAnalysis {

  public static final String SENTIMENT_WORD_LIST1 = System
      .getProperty("user.dir")
      + File.separator
      + "resources"
      + File.separator
      + "wordlists" + File.separator + "AFINN-111.txt";
  public static final String SENTIMENT_WORD_LIST2 = System
      .getProperty("user.dir")
      + File.separator
      + "resources"
      + File.separator
      + "wordlists"
      + File.separator
      + "SentStrength_Data_Sept2011_EmotionLookupTable.txt";
  private static final Logger LOG = LoggerFactory
      .getLogger(UnsupervisedSentimentAnalysis.class);

  private static UnsupervisedSentimentAnalysis instance = new UnsupervisedSentimentAnalysis();
  private static WordNet m_wordnet;
  private static SentiWordNet m_sentiwordnet;
  private WordListMap<Double> m_wordRatings1; // AFINN
  private WordListMap<Double> m_wordRatings2; // SentStrength

  private UnsupervisedSentimentAnalysis() {
    m_wordnet = WordNet.getInstance();
    m_sentiwordnet = SentiWordNet.getInstance();

    try {
      m_wordRatings1 = WordListMap.loadWordRatings(new FileInputStream(
          SENTIMENT_WORD_LIST1));
      m_wordRatings2 = WordListMap.loadWordRatings(new FileInputStream(
          SENTIMENT_WORD_LIST2));

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    }
  }

  public static UnsupervisedSentimentAnalysis getInstance() {
    return instance;
  }

  public void close() {
    m_sentiwordnet.close();
  }

  public static void main(String[] args) {
    UnsupervisedSentimentAnalysis analysis = UnsupervisedSentimentAnalysis
        .getInstance();

    analysis.close();
  }
}
