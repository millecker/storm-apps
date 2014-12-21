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
package at.illecker.storm.examples.util.svm.classifier;

import java.util.Random;

public class DynamicScoreClassifier implements ScoreClassifier {

  private int m_totalClasses;
  private double m_maxValue;
  private double m_minValue;
  private double m_factor;

  public DynamicScoreClassifier(int totalClasses, double minValue,
      double maxValue) {
    this.m_totalClasses = totalClasses;
    this.m_minValue = minValue;
    this.m_maxValue = maxValue;
    this.m_factor = (maxValue - minValue + 1) / totalClasses;
  }

  @Override
  public int classfyScore(double score) {
    if (score > m_maxValue) {
      return m_totalClasses - 1;
    } else if (score < m_minValue) {
      return 0;
    } else {
      return (int) (score / m_factor);
    }
  }

  public int getTotalClasses() {
    return m_totalClasses;
  }

  public void setTotalClasses(int totalClasses) {
    this.m_totalClasses = totalClasses;
  }

  public double getMaxValue() {
    return m_maxValue;
  }

  public void setMaxValue(double maxValue) {
    this.m_maxValue = maxValue;
  }

  public double getMinValue() {
    return m_minValue;
  }

  public void setMinValue(double minValue) {
    this.m_minValue = minValue;
  }

  @Override
  public String toString() {
    return "DynamicScoreClassifier [totalClasses=" + m_totalClasses
        + ", maxValue=" + m_maxValue + ", minValue=" + m_minValue + "]";
  }

  public static void main(String[] args) {
    Random rand = new Random();
    DynamicScoreClassifier dsc = new DynamicScoreClassifier(5, 1, 9);
    for (int i = 0; i < 10; i++) {
      double score = rand.nextInt(9) + 1 + rand.nextDouble();
      int clazz = dsc.classfyScore(score);
      System.out.println("score=" + score + " class=" + clazz);
    }
  }
}
