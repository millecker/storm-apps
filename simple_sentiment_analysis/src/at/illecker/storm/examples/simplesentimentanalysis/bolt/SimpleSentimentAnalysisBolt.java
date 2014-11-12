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
package at.illecker.storm.examples.simplesentimentanalysis.bolt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import twitter4j.Status;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;

/**
 * Simple Sentiment Analysis Bolt is based on AFINN from Finn Årup Nielsen
 *
 * http://www2.imm.dtu.dk/pubdb/views/publication_details.php?id=6010
 * 
 */
public class SimpleSentimentAnalysisBolt extends BaseRichBolt {
  private static final long serialVersionUID = -5959679287318116521L;
  private static final Logger LOG = LoggerFactory
      .getLogger(SimpleSentimentAnalysisBolt.class);
  private OutputCollector m_collector;
  private Map<String, Integer> m_wordRatings;

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
  }

  public void prepare(Map config, TopologyContext context,
      OutputCollector collector) {
    this.m_collector = collector;
    m_wordRatings = new TreeMap<String, Integer>();

    if (config.get("afinn.sentiment.file") != null) {
      String afinnFile = config.get("afinn.sentiment.file").toString();
      // Load AFINN word ratings
      BufferedReader reader = null;
      try {
        reader = new BufferedReader(new InputStreamReader(
            ClassLoader.getSystemResourceAsStream(afinnFile)));
        String str = "";
        while ((str = reader.readLine()) != null) {
          if (str.trim().length() == 0) {
            continue;
          }
          String[] values = str.split("\t");
          // LOG.info("prepare word: " + values[0] + " rating: " + values[1]);
          m_wordRatings.put(values[0], Integer.parseInt(values[1]));
        }
        LOG.info("Loaded " + m_wordRatings.size() + " words and ratings");
      } catch (IOException e) {
        LOG.error(e.getMessage());
      } finally {
        try {
          if (reader != null) {
            reader.close();
          }
        } catch (IOException e) {
          LOG.error(e.getMessage());
        }
      }
    } else {
      throw new RuntimeException("afinn.sentiment.file property was not set!");
    }
  }

  public void execute(Tuple tuple) {
    Status status = (Status) tuple.getValueByField("tweet");
    String tweetText = status.getText();
    int tweetSentiment = 0;
    LOG.info("User: " + status.getUser().getScreenName() + " Tweet: "
        + tweetText);

    // Cleanup Tweet (remove @user #hashtag RT)
    // \\n - newlines
    // @\\w* - @users
    // #\\w* - hashtags
    // \\bRT\\b - Retweets RT
    // \\p{Punct} - removes smileys
    // [^@#\\p{L}\\p{N} ]+
    // URL
    // (https?:\\/\\/)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([\\/\\w \\.-]*)*\\/? -
    Pattern pattern = Pattern
        .compile("\\n|@\\w*|#\\w*|\\bRT\\b|"
            + "(https?:\\/\\/)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([\\/\\w \\.-]*)*\\/?");
    tweetText = pattern.matcher(tweetText).replaceAll(" ");

    // Tweet to lower case
    tweetText = tweetText.toLowerCase();

    LOG.info("User: " + status.getUser().getScreenName() + " Tweet: "
        + tweetText);

    // TODO check smileys

    // TODO Remove \\p{Punct}

    String[] words = tweetText.split(" ");
    for (String word : words) {
      word = word.trim();
      if (!word.isEmpty()) {
        Integer rating = m_wordRatings.get(word);
        // LOG.info("execute word: " + word + " rating: " + rating);
        if (rating != null) {
          tweetSentiment += rating;
        }
      }
    }

    LOG.info("Tweet: " + tweetText + " sentiment: " + tweetSentiment);
    this.m_collector.ack(tuple);
  }
}
