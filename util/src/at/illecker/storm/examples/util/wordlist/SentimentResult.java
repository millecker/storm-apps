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
package at.illecker.storm.examples.util.wordlist;

public class SentimentResult {
  private long m_posCount;
  private long m_neutralCount;
  private long m_negCount;
  private double m_sum;
  private long m_count;
  private double m_maxPos;
  private double m_maxNeg;
  private double m_initialMaxPos;
  private double m_initialMaxNeg;

  public SentimentResult() {
    this.m_posCount = 0;
    this.m_neutralCount = 0;
    this.m_negCount = 0;
    this.m_sum = 0;
    this.m_count = 0;
    this.m_maxPos = 0;
    this.m_maxNeg = 0;
    this.m_initialMaxPos = 0;
    this.m_initialMaxNeg = 0;
  }

  public SentimentResult(double initialMaxPos, double initialMaxNeg) {
    this();
    this.m_initialMaxPos = initialMaxPos;
    this.m_maxPos = initialMaxPos;
    this.m_initialMaxNeg = initialMaxNeg;
    this.m_maxNeg = initialMaxNeg;
  }

  public SentimentResult(long posCount, long neutralCount, long negCount,
      double sum, long count, double initialMaxPos, double initialMaxNeg) {
    this(initialMaxPos, initialMaxNeg);
    this.m_posCount = posCount;
    this.m_neutralCount = neutralCount;
    this.m_negCount = negCount;
    this.m_sum = sum;
    this.m_count = count;
  }

  public long getPosCount() {
    return m_posCount;
  }

  public void incPosCount() {
    this.m_posCount++;
  }

  public long getNeutralCount() {
    return m_neutralCount;
  }

  public void incNeutralCount() {
    this.m_neutralCount++;
  }

  public long getNegCount() {
    return m_negCount;
  }

  public void incNegCount() {
    this.m_negCount++;
  }

  public double getSum() {
    return m_sum;
  }

  public void addSum(double value) {
    this.m_sum += value;
  }

  public long getCount() {
    return m_count;
  }

  public void incCount() {
    this.m_count++;
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

  public void add(SentimentResult sentimentResult) {
    this.m_posCount += sentimentResult.getPosCount();
    this.m_neutralCount += sentimentResult.getNeutralCount();
    this.m_negCount += sentimentResult.getNegCount();

    this.m_sum += sentimentResult.getSum();
    this.m_count += sentimentResult.getCount();

    double maxPos = sentimentResult.getMaxPos();
    if (maxPos != sentimentResult.getInitalMaxPos()) {
      this.m_maxPos = maxPos;
    }

    double maxNeg = sentimentResult.getMaxNeg();
    if (maxNeg != sentimentResult.getInitalMaxNeg()) {
      this.m_maxNeg = maxNeg;
    }
  }

  @Override
  public String toString() {
    return "SentimentResult [posCount=" + m_posCount + ", neutralCount="
        + m_neutralCount + ", negCount=" + m_negCount + ", sum=" + m_sum
        + ", count=" + m_count + ", maxPos=" + m_maxPos + ", maxNeg="
        + m_maxNeg + "]";
  }
}
