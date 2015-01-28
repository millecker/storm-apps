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
package at.illecker.storm.commons.sentiwordnet;

public class SentiValue {

  private double m_posScore = 0;
  private double m_negScore = 0;
  private double m_objScore = 0; // objScore = 1 - (PosScore + NegScore)
  private double m_score = 0; // posScore - negScore

  public SentiValue(double posScore, double negScore) {
    this.m_posScore = posScore;
    this.m_negScore = negScore;
    this.m_objScore = 1 - (posScore + negScore);
    this.m_score = posScore - negScore;
  }

  public double getPosScore() {
    return m_posScore;
  }

  public double getNegScore() {
    return m_negScore;
  }

  public double getObjScore() {
    return m_objScore;
  }

  public double getScore() {
    return m_score;
  }

  @Override
  public String toString() {
    return "SentiValue [posScore=" + m_posScore + ", negScore=" + m_negScore
        + ", objScore=" + m_objScore + ", score=" + m_score + "]";
  }
}
