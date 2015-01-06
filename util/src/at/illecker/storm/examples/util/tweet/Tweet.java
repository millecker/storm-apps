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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.illecker.storm.examples.util.svm.feature.FeatureVectorGenerator;
import edu.stanford.nlp.ling.TaggedWord;

public class Tweet implements Serializable {
  private static final long serialVersionUID = 4547113316137760131L;
  private long m_id;
  private String m_text = "";
  private double m_score = 0;

  private List<List<String>> m_sentences;
  private List<List<TaggedWord>> m_taggedSentences;
  private Map<Integer, Double> m_featureVector; // store only non-zero values

  public Tweet() {
    this.m_sentences = new ArrayList<List<String>>();
    this.m_taggedSentences = new ArrayList<List<TaggedWord>>();
    this.m_featureVector = new HashMap<Integer, Double>();
  }

  public Tweet(long id, String text, double score) {
    this();
    this.m_id = id;
    this.m_text = text;
    this.m_score = score;
  }

  public long getId() {
    return m_id;
  }

  public String getText() {
    return m_text;
  }

  public double getScore() {
    return m_score;
  }

  public Map<Integer, Double> getFeatureVector() {
    return m_featureVector;
  }

  public void addSentence(List<String> sentence) {
    m_sentences.add(sentence);
  }

  public List<List<String>> getSentences() {
    return m_sentences;
  }

  public void replaceSentences(List<List<String>> sentences) {
    m_sentences = sentences;
  }

  public void addTaggedSentence(List<TaggedWord> sentence) {
    m_taggedSentences.add(sentence);
  }

  public List<List<TaggedWord>> getTaggedSentences() {
    return m_taggedSentences;
  }

  public void genFeatureVector(FeatureVectorGenerator featureVectorGen) {
    m_featureVector = featureVectorGen.calculateFeatureVector(this);
  }

  @Override
  public String toString() {
    return "Tweet [id=" + m_id + ", text=" + m_text + ", score=" + m_score
        + "]";
  }
}
