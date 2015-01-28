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
import java.util.List;

public final class TokenizedTweet implements Serializable {
  private static final long serialVersionUID = 399139537435347839L;
  private final Tweet m_tweet;
  private final List<List<String>> m_sentences;

  public TokenizedTweet(Long id, String text, Double score,
      List<List<String>> sentences) {
    this.m_tweet = new Tweet(id, text, score);
    this.m_sentences = sentences;
  }

  public Long getId() {
    return m_tweet.getId();
  }

  public String getText() {
    return m_tweet.getText();
  }

  public Double getScore() {
    return m_tweet.getScore();
  }

  public List<List<String>> getSentences() {
    return m_sentences;
  }

  @Override
  public boolean equals(Object obj) {
    return m_tweet.equals(obj);
  }

  @Override
  public String toString() {
    return m_tweet.toString();
  }
}
