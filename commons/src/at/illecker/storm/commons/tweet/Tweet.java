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
import java.util.ArrayList;
import java.util.List;

public final class Tweet implements Serializable {
  private static final long serialVersionUID = 5246263296320608188L;
  private final Long m_id;
  private final String m_text;
  private final Double m_score;

  public Tweet(Long id, String text, Double score) {
    this.m_id = id;
    this.m_text = text;
    this.m_score = score;
  }

  public Tweet(Long id, String text) {
    this(id, text, null);
  }

  public Long getId() {
    return m_id;
  }

  public String getText() {
    return m_text;
  }

  public Double getScore() {
    return m_score;
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
    tweets.add(new Tweet(1L,
        "Gas by my house hit $3.39 !!!! I'm goin to Chapel Hill on Sat . :)",
        1.0));
    tweets
        .add(new Tweet(
            2L,
            "@oluoch @victor_otti @kunjand I just watched it ! Sridevi's comeback .... U remember her from the 90s ?? Sun mornings on NTA ;)",
            1.0));
    tweets
        .add(new Tweet(
            3L,
            "PBR & @mokbpresents bring you Jim White at the @Do317 Lounge on October 23rd at 7 pm ! http://t.co/7x8OfC56.",
            0.5));
    tweets
        .add(new Tweet(
            4L,
            "Why is it so hard to find the @TVGuideMagazine these days ? Went to 3 stores for the Castle cover issue . NONE . Will search again tomorrow ...",
            0.0));
    tweets.add(new Tweet(0L,
        "called in sick for the third straight day.  ugh.", 0.0));
    tweets
        .add(new Tweet(
            5L,
            "Here we go.  BANK FAAAAIL FRIDAY -- The FDIC says the Bradford Bank in Baltimore, Maryland has become the 82nd bank failure of the year.",
            0.0));
    tweets.add(new Tweet(0L,
        "Oh, I'm afraid your Windows-using friends will not survive.", 0.0));

    tweets
        .add(new Tweet(
            6L,
            "Excuse the connectivity of this live stream , from Baba Amr , so many activists using only one Sat Modem . LIVE http://t.co/U283IhZ5 #Homs"));
    tweets
        .add(new Tweet(
            7L,
            "Show your LOVE for your local field & it might win an award ! Gallagher Park #Bedlington current 4th in National Award http://t.co/WeiMDtQt"));
    tweets
        .add(new Tweet(
            8L,
            "@firecore Can you tell me when an update for the Apple TV 3rd gen becomes available ? The missing update holds me back from buying #appletv3"));
    tweets
        .add(new Tweet(
            9L,
            "My #cre blog Oklahoma Per Square Foot returns to the @JournalRecord blog hub tomorrow . I will have some interesting local data to share ."));
    tweets
        .add(new Tweet(
            10L,
            "\" &quot; @bbcburnsy : Loads from SB ; &quot;talks with Chester continue ; no deals 4 out of contract players ' til Jan ; Dev t Roth , Coops to Chest'ld #hcafc \""));
    return tweets;
  }

}
