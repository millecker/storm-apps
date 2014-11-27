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
package at.illecker.storm.examples.sentimentanalysis.bolt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.sentimentanalysis.util.Tweet;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import edu.stanford.nlp.ling.TaggedWord;

/**
 * PolarityDetectorBolt
 * 
 */
public class PolarityDetectionBolt extends BaseRichBolt {
  public static String CONF_WORD_LIST_FILE = "word.list.file";
  private static final long serialVersionUID = -549704444828609491L;
  private static final Logger LOG = LoggerFactory
      .getLogger(PolarityDetectionBolt.class);

  private OutputCollector m_collector;
  private Map<String, Double> m_wordRatings;

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
  }

  public void prepare(Map config, TopologyContext context,
      OutputCollector collector) {
    this.m_collector = collector;
    m_wordRatings = new TreeMap<String, Double>();

    if (config.get(CONF_WORD_LIST_FILE) != null) {
      String wordlist = config.get(CONF_WORD_LIST_FILE).toString();
      // Load AFINN word ratings
      BufferedReader reader = null;
      try {
        reader = new BufferedReader(new InputStreamReader(
            ClassLoader.getSystemResourceAsStream(wordlist)));
        String str = "";
        while ((str = reader.readLine()) != null) {
          if (str.trim().length() == 0) {
            continue;
          }
          String[] values = str.split("\t");
          // LOG.info("prepare word: " + values[0] + " rating: " + values[1]);
          m_wordRatings.put(values[0], Double.parseDouble(values[1]));
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
      throw new RuntimeException(CONF_WORD_LIST_FILE + " property was not set!");
    }
  }

  public void execute(Tuple tuple) {
    Tweet tweet = (Tweet) tuple.getValueByField("taggedTweet");
    LOG.info(tweet.toString());

    // Cleanup Tweet (remove @user #hashtag RT)
    // \\n - newlines
    // @\\w* - @users
    // #\\w* - hashtags
    // \\bRT\\b - Retweets RT
    // \\p{Punct} - removes smileys
    // [^@#\\p{L}\\p{N} ]+
    // URL
    // (https?:\\/\\/)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([\\/\\w \\.-]*)*\\/? -

    double tweetSentiment = 0;
    for (List<TaggedWord> taggedSentence : tweet.getTaggedSentences()) {
      LOG.info("TaggedTweet: " + taggedSentence.toString());

      String sentimentSentence = "";
      double sentenceSentiment = 0;
      for (TaggedWord taggedWord : taggedSentence) {
        String word = taggedWord.word().toLowerCase().trim();
        Double rating = m_wordRatings.get(word);
        sentimentSentence += word + "/" + ((rating != null) ? rating : "NA")
            + " ";
        if (rating != null) {
          sentenceSentiment += rating;
        }
      }
      tweetSentiment += sentenceSentiment;
      LOG.info("SentimentSentence: " + sentimentSentence
          + " totalTweetSentiment: " + sentenceSentiment);
    }

    this.m_collector.ack(tuple);
  }
}
