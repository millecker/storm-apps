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

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.sentimentanalysis.util.Tweet;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class PreprocessorBolt extends BaseRichBolt {
  private static final long serialVersionUID = -5313040244912981545L;
  private static final Logger LOG = LoggerFactory
      .getLogger(PreprocessorBolt.class);

  private OutputCollector m_collector;

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    declarer.declare(new Fields("preprocessedTweet")); // key of output tuples
  }

  public void prepare(Map config, TopologyContext context,
      OutputCollector collector) {
    this.m_collector = collector;
  }

  public void execute(Tuple tuple) {
    Tweet tweet = (Tweet) tuple.getValueByField("tweet");
    // LOG.info(tweet.toString());

    // STEP 1
    // First unify all URLs, e-mail addresses and user names by replacing them
    // with unique tokens.
    // All hash marks were stripped from words, and emoticons were mapped to
    // special tokens representing their emotion categories
    // These special tokens were then added to the polarity lexicons used by
    // SO-CAL.

    // STEP 2
    // Social media specific slang expressions and abbreviations like “2 b” (for
    // “to be”) or “im- sry” (for “I am sorry”) were translated to their ap-
    // propriate standard language forms. For this, we used a dictionary of
    // 5,424 expressions that we gathered from publicly available resources.
    // http://www.noslang.com/dictionary/
    // http://onlineslangdictionary.com/
    // http: //www.urbandictionary.com/

    // STEP 3
    // Tackles two typical spelling phenomena:
    // a) the omission of final g in gerund forms (goin), and
    // b) elongations of characters (suuuper).
    // For the former, we appended the character g to words ending with -in if
    // these words are unknown to vo- cabulary,4 while the corresponding
    // ‘g’-forms are in- vocabulary words (IVW). For the latter problem, we
    // first tried to subsequently remove each repeat- ing character until we
    // hit an IVW. For cases re- sisting this treatment, we adopted the method
    // sug- gested by (Brody/Diakopoulos, 2011) and generated a squeezed form of
    // the prolongated word, subse- quently looking it up in a probability table
    // that has previously been gathered from a training corpus.

    this.m_collector.emit(tuple, new Values(tweet));
    this.m_collector.ack(tuple);
  }
}
