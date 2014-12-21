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
package at.illecker.storm.examples.util.tweet;

public class SentimentTweet extends Tweet {
  private static final long serialVersionUID = 5040446561123932812L;
  private double m_scoreMislove;
  private double m_scoreAfinn;
  private double m_scoreSentiStrength;
  private double m_scoreSentiStrengthPos;
  private double m_scoreSentiStrengthNeg;

  public SentimentTweet(long id, String text, double score,
      double scoreMislove, double scoreAfinn, double scoreSentiStrength,
      double scoreSentiStrengthPos, double scoreSentiStrengthNeg) {
    super(id, text, score);
    this.m_scoreMislove = scoreMislove;
    this.m_scoreAfinn = scoreAfinn;
    this.m_scoreSentiStrength = scoreSentiStrength;
    this.m_scoreSentiStrengthPos = scoreSentiStrengthPos;
    this.m_scoreSentiStrengthNeg = scoreSentiStrengthNeg;
  }

  public double getScoreMislove() {
    return m_scoreMislove;
  }

  public double getScoreAFINN() {
    return m_scoreAfinn;
  }

  public double getScoreSentiStrength() {
    return m_scoreSentiStrength;
  }

  public double getScoreSentiStrengthPos() {
    return m_scoreSentiStrengthPos;
  }

  public double getScoreSentiStrengthNeg() {
    return m_scoreSentiStrengthNeg;
  }

  @Override
  public String toString() {
    return "Tweet [id=" + getId() + ", text=" + getText() + ", score="
        + getScore() + ", scoreMislove=" + m_scoreMislove + ", scoreAfinn="
        + m_scoreAfinn + ", scoreSentiStrength=" + m_scoreSentiStrength
        + ", scoreSentiStrengthPositive=" + m_scoreSentiStrengthPos
        + ", scoreSentiStrengthNegative=" + m_scoreSentiStrengthNeg + "]";
  }
}
