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
package at.illecker.storm.examples.util.dictionaries;

import java.util.ArrayList;
import java.util.List;

public class SentimentResult {
  private int m_posCount;
  private int m_neutralCount;
  private int m_negCount;
  private double m_sum;
  private double m_maxPos;
  private double m_maxNeg;
  private double m_initialMaxPos;
  private double m_initialMaxNeg;
  private List<Double> m_scores;

  public SentimentResult() {
    this.m_posCount = 0;
    this.m_neutralCount = 0;
    this.m_negCount = 0;
    this.m_sum = 0;
    this.m_maxPos = 0;
    this.m_maxNeg = 0;
    this.m_initialMaxPos = 0;
    this.m_initialMaxNeg = 0;
    this.m_scores = new ArrayList<Double>();
  }

  public SentimentResult(double initialMaxPos, double initialMaxNeg) {
    this();
    this.m_initialMaxPos = initialMaxPos;
    this.m_maxPos = initialMaxPos;
    this.m_initialMaxNeg = initialMaxNeg;
    this.m_maxNeg = initialMaxNeg;
  }

  public int getPosCount() {
    return m_posCount;
  }

  public double getAvgPosCount() {
    return m_posCount / (double) m_scores.size();
  }

  public void incPosCount() {
    this.m_posCount++;
  }

  public int getNeutralCount() {
    return m_neutralCount;
  }

  public double getAvgNeutralCount() {
    return m_neutralCount / (double) m_scores.size();
  }

  public void incNeutralCount() {
    this.m_neutralCount++;
  }

  public int getNegCount() {
    return m_negCount;
  }

  public double getAvgNegCount() {
    return m_negCount / (double) m_scores.size();
  }

  public void incNegCount() {
    this.m_negCount++;
  }

  public double getSum() {
    return m_sum;
  }

  public double getAvgSum() {
    return m_sum / (double) m_scores.size();
  }

  public int getCount() {
    return m_scores.size();
  }

  public double getMaxPos() {
    return m_maxPos;
  }

  public void setMaxPos(double maxPos) {
    this.m_maxPos = maxPos;
  }

  public double getInitalMaxPos() {
    return m_initialMaxPos;
  }

  public double getMaxNeg() {
    return m_maxNeg;
  }

  public void setMaxNeg(double maxNeg) {
    this.m_maxNeg = maxNeg;
  }

  public double getInitalMaxNeg() {
    return m_initialMaxNeg;
  }

  public void addScore(double value) {
    this.m_scores.add(value);
    this.m_sum += value;
  }

  public void add(SentimentResult sentimentResult) {
    this.m_posCount += sentimentResult.getPosCount();
    this.m_neutralCount += sentimentResult.getNeutralCount();
    this.m_negCount += sentimentResult.getNegCount();

    this.m_sum += sentimentResult.getSum();

    double maxPos = sentimentResult.getMaxPos();
    if (maxPos != sentimentResult.getInitalMaxPos()) {
      this.m_maxPos = maxPos;
    }

    double maxNeg = sentimentResult.getMaxNeg();
    if (maxNeg != sentimentResult.getInitalMaxNeg()) {
      this.m_maxNeg = maxNeg;
    }

    this.m_scores.addAll(sentimentResult.m_scores);
  }

  @Override
  public String toString() {
    return "SentimentResult [posCount=" + m_posCount + ", neutralCount="
        + m_neutralCount + ", negCount=" + m_negCount + ", sum=" + m_sum
        + ", count=" + m_scores.size() + ", maxPos=" + m_maxPos + ", maxNeg="
        + m_maxNeg + ", scores=" + m_scores.toString() + "]";
  }
}
