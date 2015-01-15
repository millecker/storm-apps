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
  private Double m_score = null;

  private List<List<String>> m_sentences;
  private List<List<String>> m_preprocessedSentences;
  private List<List<TaggedWord>> m_taggedSentences;
  private Map<Integer, Double> m_featureVector; // store only non-zero values

  public Tweet() {
    this.m_sentences = new ArrayList<List<String>>();
    this.m_preprocessedSentences = new ArrayList<List<String>>();
    this.m_taggedSentences = new ArrayList<List<TaggedWord>>();
    this.m_featureVector = new HashMap<Integer, Double>();
  }

  public Tweet(long id, String text) {
    this();
    this.m_id = id;
    this.m_text = text;
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

  public Double getScore() {
    return m_score;
  }

  public List<List<String>> getSentences() {
    return m_sentences;
  }

  public void addSentence(List<String> sentence) {
    m_sentences.add(sentence);
  }

  public List<List<String>> getPreprocessedSentences() {
    return m_preprocessedSentences;
  }

  public void addPreprocessedSentence(List<String> preprocessedSentence) {
    m_preprocessedSentences.add(preprocessedSentence);
  }

  public List<List<TaggedWord>> getTaggedSentences() {
    return m_taggedSentences;
  }

  public void addTaggedSentence(List<TaggedWord> sentence) {
    m_taggedSentences.add(sentence);
  }

  public void genFeatureVector(FeatureVectorGenerator featureVectorGen) {
    m_featureVector = featureVectorGen.calculateFeatureVector(this);
  }

  public Map<Integer, Double> getFeatureVector() {
    return m_featureVector;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final Tweet other = (Tweet) obj;
    // check if id is matching
    if (this.m_id != other.getId()) {
      return false;
    }
    // check if text is matching
    if (!this.m_text.equals(other.getText())) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return "Tweet [id=" + m_id + ", text=" + m_text
        + ((m_score != null) ? ", score=" + m_score : "") + "]";
  }

  public static List<Tweet> getTestTweets() {
    List<Tweet> tweets = new ArrayList<Tweet>();
    tweets
        .add(new Tweet(
            1,
            "Gas by my house hit $3.39 !!!! I'm goin to Chapel Hill on Sat . :)",
            1));
    tweets
        .add(new Tweet(
            2,
            "@oluoch @victor_otti @kunjand I just watched it ! Sridevi's comeback .... U remember her from the 90s ?? Sun mornings on NTA ;)",
            1));
    tweets
        .add(new Tweet(
            3,
            "PBR & @mokbpresents bring you Jim White at the @Do317 Lounge on October 23rd at 7 pm ! http://t.co/7x8OfC56",
            0.5));
    tweets
        .add(new Tweet(
            4,
            "Why is it so hard to find the @TVGuideMagazine these days ? Went to 3 stores for the Castle cover issue . NONE . Will search again tomorrow ...",
            0));
    tweets.add(new Tweet(0, "called in sick for the third straight day.  ugh.",
        0));
    tweets
        .add(new Tweet(
            5,
            "Here we go.  BANK FAAAAIL FRIDAY -- The FDIC says the Bradford Bank in Baltimore, Maryland has become the 82nd bank failure of the year.",
            0));
    tweets.add(new Tweet(0,
        "Oh, I'm afraid your Windows-using friends will not survive.", 0));

    tweets
        .add(new Tweet(
            6,
            "Excuse the connectivity of this live stream , from Baba Amr , so many activists using only one Sat Modem . LIVE http://t.co/U283IhZ5 #Homs"));
    tweets
        .add(new Tweet(
            7,
            "Show your LOVE for your local field & it might win an award ! Gallagher Park #Bedlington current 4th in National Award http://t.co/WeiMDtQt"));
    tweets
        .add(new Tweet(
            8,
            "@firecore Can you tell me when an update for the Apple TV 3rd gen becomes available ? The missing update holds me back from buying #appletv3"));
    tweets
        .add(new Tweet(
            9,
            "My #cre blog Oklahoma Per Square Foot returns to the @JournalRecord blog hub tomorrow . I will have some interesting local data to share ."));
    tweets
        .add(new Tweet(
            10,
            "\" &quot; @bbcburnsy : Loads from SB ; &quot;talks with Chester continue ; no deals 4 out of contract players ' til Jan ; Dev t Roth , Coops to Chest'ld #hcafc \""));

    tweets.add(new Tweet(11, ":-))) xDD XDD ;) :)"));
    tweets
        .add(new Tweet(
            12,
            "\"All the money you've spent on my credit card I'm taking it out of your account\".... Hi I'm Sydney and I'm filing bankruptcy\ud83d\ude05\ud83d\ude05\ud83d\ude05\ud83d\ude05 \ud83d\ude05"));

    return tweets;
  }
}
