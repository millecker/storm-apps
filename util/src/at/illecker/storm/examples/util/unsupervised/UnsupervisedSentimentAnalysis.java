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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.unsupervised.util.SentimentWordLists;
import at.illecker.storm.examples.util.unsupervised.util.sentiwordnet.SentiWordNet;

public class UnsupervisedSentimentAnalysis {
  private static final Logger LOG = LoggerFactory
      .getLogger(UnsupervisedSentimentAnalysis.class);
  private static final UnsupervisedSentimentAnalysis instance = new UnsupervisedSentimentAnalysis();

  private SentimentWordLists m_sentimentWordLists;
  private SentiWordNet m_sentiWordNet;

  private UnsupervisedSentimentAnalysis() {
    m_sentimentWordLists = SentimentWordLists.getInstance();
    m_sentiWordNet = SentiWordNet.getInstance();
  }

  public static UnsupervisedSentimentAnalysis getInstance() {
    return instance;
  }

  public void close() {
    m_sentiWordNet.close();
  }

  public static void main(String[] args) {
    UnsupervisedSentimentAnalysis analysis = UnsupervisedSentimentAnalysis
        .getInstance();

    analysis.close();
  }
}
