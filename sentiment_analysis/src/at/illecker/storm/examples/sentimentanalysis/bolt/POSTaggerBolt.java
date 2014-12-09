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

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.Tweet;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

/**
 * POSTaggerBolt is based on the Stanford NLP library and the
 * gate-EN-twitter-fast.model
 *
 * http://nlp.stanford.edu/software/corenlp.shtml
 * http://nlp.stanford.edu:8080/corenlp/process
 * https://gate.ac.uk/wiki/twitter-postagger.html
 * 
 */
public class POSTaggerBolt extends BaseRichBolt {
  public static String CONF_TAGGER_MODEL_FILE = "tagger.model.file";
  private static final long serialVersionUID = -2524877140212376549L;
  private static final Logger LOG = LoggerFactory
      .getLogger(POSTaggerBolt.class);

  private OutputCollector m_collector;
  private MaxentTagger m_tagger;

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declare(new Fields("taggedTweet")); // key of output tuples
  }

  public void prepare(Map config, TopologyContext context,
      OutputCollector collector) {
    this.m_collector = collector;

    if (config.get(CONF_TAGGER_MODEL_FILE) != null) {
      String model = config.get(CONF_TAGGER_MODEL_FILE).toString();
      // Load Tagger and model
      m_tagger = new MaxentTagger(model);
    } else {
      throw new RuntimeException(CONF_TAGGER_MODEL_FILE
          + " property was not set!");
    }
  }

  public void execute(Tuple tuple) {
    Tweet tweet = (Tweet) tuple.getValueByField("splittedTweet");
    // LOG.info(tweet.toString());

    // POS tagging of sentences
    for (List<HasWord> sentence : tweet.getSentences()) {
      List<TaggedWord> taggedSentence = m_tagger.tagSentence(sentence);
      tweet.addTaggedSentence(taggedSentence);
      // LOG.info("Tweet: " + sentence.toString() + " TaggedTweet: "
      // + taggedSentence.toString());
    }

    this.m_collector.emit(tuple, new Values(tweet));
    this.m_collector.ack(tuple);
  }
}
