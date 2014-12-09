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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;

public class Tweet implements Serializable {
  private static final long serialVersionUID = 4933096091291791733L;
  private long m_id;
  private String m_text = "";
  private double m_scoreAMT;
  private double m_scoreMislove;
  private double m_scoreAfinn;
  private double m_scoreSentiStrength;
  private double m_scoreSentiStrengthPos;
  private double m_scoreSentiStrengthNeg;
  private List<List<HasWord>> m_sentences;
  private List<List<TaggedWord>> m_taggedSentences;

  public Tweet(long id, String text, double scoreAMT, double scoreMislove,
      double scoreAfinn, double scoreSentiStrength,
      double scoreSentiStrengthPos, double scoreSentiStrengthNeg) {
    this.m_id = id;
    this.m_text = text;
    this.m_scoreAMT = scoreAMT;
    this.m_scoreMislove = scoreMislove;
    this.m_scoreAfinn = scoreAfinn;
    this.m_scoreSentiStrength = scoreSentiStrength;
    this.m_scoreSentiStrengthPos = scoreSentiStrengthPos;
    this.m_scoreSentiStrengthNeg = scoreSentiStrengthNeg;
    this.m_sentences = new ArrayList<List<HasWord>>();
    this.m_taggedSentences = new ArrayList<List<TaggedWord>>();
  }

  public long getId() {
    return m_id;
  }

  public String getText() {
    return m_text;
  }

  public double getScoreAMT() {
    return m_scoreAMT;
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

  public void addSentence(List<HasWord> sentence) {
    m_sentences.add(sentence);
  }

  public List<List<HasWord>> getSentences() {
    return m_sentences;
  }

  public void addTaggedSentence(List<TaggedWord> sentence) {
    m_taggedSentences.add(sentence);
  }

  public List<List<TaggedWord>> getTaggedSentences() {
    return m_taggedSentences;
  }

  @Override
  public String toString() {
    return "Tweet [id=" + m_id + ", text=" + m_text + ", scoreAMT="
        + m_scoreAMT + ", scoreMislove=" + m_scoreMislove + ", scoreAfinn="
        + m_scoreAfinn + ", scoreSentiStrength=" + m_scoreSentiStrength
        + ", scoreSentiStrengthPositive=" + m_scoreSentiStrengthPos
        + ", scoreSentiStrengthNegative=" + m_scoreSentiStrengthNeg + "]";
  }

}
