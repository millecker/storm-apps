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
package at.illecker.storm.commons.tweet;

import java.io.Serializable;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SentimentTweet implements Serializable {
  private static final long serialVersionUID = -3755946597513993340L;
  private static final Logger LOG = LoggerFactory
      .getLogger(SentimentTweet.class);
  private final Tweet m_tweet;
  private final Double m_scoreMislove;
  private final Double m_scoreAfinn;
  private final Double m_scoreSentiStrength;
  private final Double m_scoreSentiStrengthPos;
  private final Double m_scoreSentiStrengthNeg;

  public SentimentTweet(long id, String text, double score,
      double scoreMislove, double scoreAfinn, double scoreSentiStrength,
      double scoreSentiStrengthPos, double scoreSentiStrengthNeg) {
    this.m_tweet = new Tweet(id, text, score);
    this.m_scoreMislove = scoreMislove;
    this.m_scoreAfinn = scoreAfinn;
    this.m_scoreSentiStrength = scoreSentiStrength;
    this.m_scoreSentiStrengthPos = scoreSentiStrengthPos;
    this.m_scoreSentiStrengthNeg = scoreSentiStrengthNeg;
  }

  public Double getScoreMislove() {
    return m_scoreMislove;
  }

  public Double getScoreAFINN() {
    return m_scoreAfinn;
  }

  public Double getScoreSentiStrength() {
    return m_scoreSentiStrength;
  }

  public Double getScoreSentiStrengthPos() {
    return m_scoreSentiStrengthPos;
  }

  public Double getScoreSentiStrengthNeg() {
    return m_scoreSentiStrengthNeg;
  }

  @Override
  public String toString() {
    return "Tweet [id=" + m_tweet.getId() + ", text=" + m_tweet.getText()
        + ", score=" + m_tweet.getScore() + ", scoreMislove=" + m_scoreMislove
        + ", scoreAfinn=" + m_scoreAfinn + ", scoreSentiStrength="
        + m_scoreSentiStrength + ", scoreSentiStrengthPositive="
        + m_scoreSentiStrengthPos + ", scoreSentiStrengthNegative="
        + m_scoreSentiStrengthNeg + "]";
  }

  public static SentimentTweet fromJsonElement(Map<String, Object> element) {
    long id = Long.parseLong((String) element.get("id"));
    double score_amt = (Double) element.get("score_amt");
    double score_amt2 = (Double) element.get("score_amt_wrong");
    double score_mislove = (Double) element.get("score_mislove");
    double score_mislove2 = (Double) element.get("score_mislove2");
    double score_afinn = (Double) element.get("sentiment_afinn");
    double score_sentistrength = (Double) element.get("sentistrength");
    double score_sentistrength_pos = (Double) element
        .get("sentistrength_positive");
    double score_sentistrength_neg = (Double) element
        .get("sentistrength_negative");
    // sentiment_afinn_nonzero=-2.0,
    // sentiment_afinn_quant=-1.0,
    // sentiment_afinn_extreme=-2.0,
    // sentiment_afinn_sum=-4.0

    if ((score_amt != score_amt2) || (score_mislove != score_mislove2)) {
      LOG.error("Inconsistency: " + element.toString());
    }

    return new SentimentTweet(id, (String) element.get("text"), score_amt,
        score_mislove, score_afinn, score_sentistrength,
        score_sentistrength_pos, score_sentistrength_neg);
  }
}
