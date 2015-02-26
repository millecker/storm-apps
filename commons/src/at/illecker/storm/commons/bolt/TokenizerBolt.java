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
package at.illecker.storm.commons.bolt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.commons.util.HtmlUtils;
import at.illecker.storm.commons.util.RegexUtils;
import at.illecker.storm.commons.util.UnicodeUtils;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class TokenizerBolt extends BaseRichBolt {
  public static final String ID = "tokenizer-bolt";
  public static final String CONF_LOGGING = ID + ".logging";
  private static final long serialVersionUID = 7134328814020366549L;
  private static final Logger LOG = LoggerFactory
      .getLogger(TokenizerBolt.class);
  private boolean m_logging = false;
  private OutputCollector m_collector;

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    // key of output tuples
    declarer.declare(new Fields("tokens"));
  }

  public void prepare(Map config, TopologyContext context,
      OutputCollector collector) {
    this.m_collector = collector;
    // Optional set logging
    if (config.get(CONF_LOGGING) != null) {
      m_logging = (Boolean) config.get(CONF_LOGGING);
    } else {
      m_logging = false;
    }
  }

  public void execute(Tuple tuple) {
    String text = tuple.getStringByField("text");

    // Step 1) Trim text
    text = text.trim();

    // Step 2) Replace Unicode symbols \u0000
    if (UnicodeUtils.containsUnicode(text)) {
      text = UnicodeUtils.replaceUnicodeSymbols(text);
    }

    // Step 3) Replace HTML symbols &#[0-9];
    if (HtmlUtils.containsHtml(text)) {
      text = HtmlUtils.replaceHtmlSymbols(text);
    }

    // Step 4) Tokenize
    List<String> tokens = new ArrayList<String>();
    Matcher m = RegexUtils.TOKENIZER_PATTERN.matcher(text);
    while (m.find()) {
      tokens.add(m.group());
    }

    if (m_logging) {
      LOG.info("Tweet: \"" + text + "\" Tokenized: " + tokens);
    }

    // Emit new tuples
    this.m_collector.emit(tuple, new Values(tokens));
    this.m_collector.ack(tuple);
  }

}
