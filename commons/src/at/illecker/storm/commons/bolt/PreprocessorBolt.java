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

import at.illecker.storm.commons.dict.FirstNames;
import at.illecker.storm.commons.dict.SlangCorrection;
import at.illecker.storm.commons.util.RegexUtils;
import at.illecker.storm.commons.util.StringUtils;
import at.illecker.storm.commons.wordnet.WordNet;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;

public class PreprocessorBolt extends BaseRichBolt {
  public static final String ID = "preprocessor-bolt";
  public static final String CONF_LOGGING = ID + ".logging";
  private static final long serialVersionUID = -1623010654971791418L;
  private static final Logger LOG = LoggerFactory
      .getLogger(PreprocessorBolt.class);
  private boolean m_logging = false;
  private OutputCollector m_collector;

  private WordNet m_wordnet;
  private SlangCorrection m_slangCorrection;
  private FirstNames m_firstNames;

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
    // key of output tuples
    declarer.declare(new Fields("preprocessedTokens"));
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
    // Load WordNet
    m_wordnet = WordNet.getInstance();
    // Load Slang correction
    m_slangCorrection = SlangCorrection.getInstance();
    // Load FirstNames
    m_firstNames = FirstNames.getInstance();
  }

  public void execute(Tuple tuple) {
    List<String> tokens = (List<String>) tuple.getValueByField("tokens");

    // Preprocess
    List<String> preprocessedTokens = preprocess(tokens);

    if (m_logging) {
      LOG.info("Tweet: " + preprocessedTokens);
    }

    // Emit new tuples
    this.m_collector.emit(tuple, new Values(preprocessedTokens));
  }

  private List<String> preprocess(List<String> tokens) {
    List<String> preprocessedTokens = new ArrayList<String>();
    for (String token : tokens) {
      // identify token
      boolean tokenContainsPunctuation = StringUtils
          .consitsOfPunctuations(token);
      boolean tokenConsistsOfUnderscores = StringUtils
          .consitsOfUnderscores(token);
      boolean tokenIsEmoticon = StringUtils.isEmoticon(token);
      boolean tokenIsURL = StringUtils.isURL(token);
      boolean tokenIsNumeric = StringUtils.isNumeric(token);

      // Step 1) Unify Emoticons remove repeating chars
      if ((tokenIsEmoticon) && (!tokenIsURL) && (!tokenIsNumeric)) {
        Matcher m = RegexUtils.TWO_OR_MORE_REPEATING_CHARS_PATTERN
            .matcher(token);
        if (m.find()) {
          boolean isSpecialEmoticon = m.group(1).equals("^");
          String reducedToken = m.replaceAll("$1");
          if (isSpecialEmoticon) { // keep ^^
            reducedToken += "^";
          }
          preprocessedTokens.add(reducedToken);
          continue;
        }
      } else if ((tokenContainsPunctuation) || (tokenConsistsOfUnderscores)) {
        // If token is no Emoticon then there is no further
        // preprocessing for punctuations or underscores
        preprocessedTokens.add(token);
        continue;
      }

      // identify token further
      boolean tokenIsUser = StringUtils.isUser(token);
      boolean tokenIsHashTag = StringUtils.isHashTag(token);
      boolean tokenIsSlang = StringUtils.isSlang(token);
      boolean tokenIsEmail = StringUtils.isEmail(token);
      boolean tokenIsPhone = StringUtils.isPhone(token);
      boolean tokenIsSpecialNumeric = StringUtils.isSpecialNumeric(token);
      boolean tokenIsSeparatedNumeric = StringUtils.isSeparatedNumeric(token);

      // Step 2) Slang Correction
      if ((!tokenIsEmoticon) && (!tokenIsUser) && (!tokenIsHashTag)
          && (!tokenIsURL) && (!tokenIsNumeric) && (!tokenIsSpecialNumeric)
          && (!tokenIsSeparatedNumeric) && (!tokenIsEmail) && (!tokenIsPhone)) {
        String[] slangCorrection = m_slangCorrection.getCorrection(token
            .toLowerCase());
        if (slangCorrection != null) {
          for (int i = 0; i < slangCorrection.length; i++) {
            preprocessedTokens.add(slangCorrection[i]);
          }
          continue;
        }
      } else if (tokenIsSlang) {
        if (token.startsWith("w/")) {
          preprocessedTokens.add("with");
          preprocessedTokens.add(token.substring(2));
          continue;
        }
      }

      // Step 3) Check if there are punctuations between words
      // e.g., L.O.V.E
      if ((!tokenIsEmoticon) && (!tokenIsUser) && (!tokenIsHashTag)
          && (!tokenIsURL) && (!tokenIsNumeric) && (!tokenIsSpecialNumeric)
          && (!tokenIsSeparatedNumeric) && (!tokenIsEmail) && (!tokenIsPhone)) {
        // remove alternating letter dot pattern e.g., L.O.V.E
        Matcher m = RegexUtils.ALTERNATING_LETTER_DOT_PATTERN.matcher(token);
        if (m.matches()) {
          String newToken = token.replaceAll("\\.", "");
          if (m_wordnet.contains(newToken)) {
            preprocessedTokens.add(newToken);
            continue;
          }
        }
      }

      // Step 4) Add missing g in gerund forms e.g., goin
      if ((!tokenIsUser) && (!tokenIsHashTag) && (!tokenIsURL)
          && (token.endsWith("in")) && (!m_firstNames.isFirstName(token))
          && (!m_wordnet.contains(token.toLowerCase()))) {
        // append "g" if a word ends with "in" and is not in the vocabulary
        token = token + "g";
        preprocessedTokens.add(token);
        continue;
      }

      // Step 5) Remove elongations of characters (suuuper)
      // 'lollll' to 'loll' because 'loll' is found in dict
      // TODO 'AHHHHH' to 'AH'
      if ((!tokenIsEmoticon) && (!tokenIsUser) && (!tokenIsHashTag)
          && (!tokenIsURL) && (!tokenIsNumeric) && (!tokenIsSpecialNumeric)
          && (!tokenIsSeparatedNumeric) && (!tokenIsEmail) && (!tokenIsPhone)) {

        // remove repeating chars
        token = removeRepeatingChars(token);

        // Step 5b) Try Slang Correction again
        String[] slangCorrection = m_slangCorrection.getCorrection(token
            .toLowerCase());
        if (slangCorrection != null) {
          for (int i = 0; i < slangCorrection.length; i++) {
            preprocessedTokens.add(slangCorrection[i]);
          }
        }
      }
    }
    return preprocessedTokens;
  }

  private String removeRepeatingChars(String value) {
    // if there are three repeating equal chars
    // then remove one char until the word is found in the vocabulary
    // else if the word is not found reduce the repeating chars to one

    // collect matches for sub-token search
    List<int[]> matches = null;

    Matcher m = RegexUtils.THREE_OR_MORE_REPEATING_CHARS_PATTERN.matcher(value);
    while (m.find()) {
      if (matches == null) {
        matches = new ArrayList<int[]>();
      }

      int start = m.start();
      int end = m.end();
      // String c = m.group(1);
      // LOG.info("token: '" + value + "' match at start: " + start + " end: "
      // + end);

      // check if token is not in the vocabulary
      if (!m_wordnet.contains(value)) {
        // collect matches for subtoken check
        matches.add(new int[] { start, end });

        StringBuilder sb = new StringBuilder(value);
        for (int i = 0; i < end - start - 1; i++) {
          sb.deleteCharAt(start); // delete repeating char

          // LOG.info("check token: '" + sb.toString() + "'");
          // check if token is in the vocabulary
          if (m_wordnet.contains(sb.toString())) {
            return sb.toString();
          }

          // if the token is not in the vocabulary check all combinations
          // of prior matches
          // TODO really necessary?
          for (int j = 0; j < matches.size(); j++) {
            int startSub = matches.get(j)[0];
            int endSub = matches.get(j)[1];
            if (startSub != start) {
              StringBuilder subSb = new StringBuilder(sb);
              for (int k = 0; k < endSub - startSub - 1; k++) {
                subSb.deleteCharAt(startSub);

                // LOG.info("check subtoken: '" + subSb.toString() + "'");
                if (m_wordnet.contains(subSb.toString())) {
                  return subSb.toString();
                }
              }
            }
          }
        }
      }
    }

    // no match have been found
    // reduce all repeating chars
    if (matches != null) {
      String reducedToken = m.replaceAll("$1");
      value = reducedToken;
    }
    return value;
  }

}
