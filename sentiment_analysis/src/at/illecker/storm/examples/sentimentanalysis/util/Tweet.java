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
package at.illecker.storm.examples.sentimentanalysis.util;

import java.io.Serializable;

public class Tweet implements Serializable {
  private static final long serialVersionUID = 4933096091291791733L;
  private long m_id;
  private String m_text = "";
  private double m_score_amt;
  private double m_score_mislove;
  private double m_score_afinn;
  private double m_score_sentistrength;
  private double m_score_sentistrength_pos;
  private double m_score_sentistrength_neg;

  public Tweet(long id, String text, double score_amt, double score_mislove,
      double score_afinn, double score_sentistrength,
      double score_sentistrength_pos, double score_sentistrength_neg) {
    this.m_id = id;
    this.m_text = text;
    this.m_score_amt = score_amt;
    this.m_score_mislove = score_mislove;
    this.m_score_afinn = score_afinn;
    this.m_score_sentistrength = score_sentistrength;
    this.m_score_sentistrength_pos = score_sentistrength_pos;
    this.m_score_sentistrength_neg = score_sentistrength_neg;
  }

  public long getId() {
    return m_id;
  }

  public String getText() {
    return m_text;
  }

  public double getScoreAMT() {
    return m_score_amt;
  }

  public double getScoreMislove() {
    return m_score_mislove;
  }

  public double getScoreAFINN() {
    return m_score_afinn;
  }

  public double getScoreSentiStrength() {
    return m_score_sentistrength;
  }

  public double getScoreSentiStrengthPos() {
    return m_score_sentistrength_pos;
  }

  public double getScoreSentiStrengthNeg() {
    return m_score_sentistrength_neg;
  }

  @Override
  public String toString() {
    return "Tweet [m_id=" + m_id + ", m_text=" + m_text + ", m_score_amt="
        + m_score_amt + ", m_score_mislove=" + m_score_mislove
        + ", m_score_afinn=" + m_score_afinn + ", m_score_sentistrength="
        + m_score_sentistrength + ", m_score_sentistrength_pos="
        + m_score_sentistrength_pos + ", m_score_sentistrength_neg="
        + m_score_sentistrength_neg + "]";
  }

}
