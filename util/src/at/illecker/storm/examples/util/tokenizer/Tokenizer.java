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
package at.illecker.storm.examples.util.tokenizer;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.Configuration;
import at.illecker.storm.examples.util.Dataset;
import at.illecker.storm.examples.util.HtmlUtils;
import at.illecker.storm.examples.util.RegexUtils;
import at.illecker.storm.examples.util.UnicodeUtils;
import at.illecker.storm.examples.util.tweet.Tweet;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.process.PTBTokenizer.PTBTokenizerFactory;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class Tokenizer {
  private static final Logger LOG = LoggerFactory.getLogger(Tokenizer.class);

  public static List<String> tokenize(String str) {
    // Step 1) Trim text
    str = str.trim();

    // Step 2) Replace Unicode symbols \u0000
    if (UnicodeUtils.containsUnicode(str)) {
      String replacedText = UnicodeUtils.replaceUnicodeSymbols(str);
      // LOG.info("Replaced Unicode symbols from '" + str + "' to '"
      // + replacedText + "'");
      if (replacedText.equals(str)) {
        LOG.error("Unicode symbols could not be replaced: '" + str + "'");
      }
      str = replacedText;
    }

    // Step 3) Replace HTML symbols &#[0-9];
    if (HtmlUtils.containsHtml(str)) {
      String replacedText = HtmlUtils.replaceHtmlSymbols(str);
      // LOG.info("Replaced HTML symbols from '" + text + "' to '"
      // + replacedText + "'");
      if (replacedText.equals(str)) {
        LOG.error("HTML symbols could not be replaced: '" + str + "'");
      }
      str = replacedText;
    }

    // Step 4) Tokenize string by multiple regex patterns
    List<String> resultTokens = new ArrayList<String>();
    Matcher m = RegexUtils.TOKENIZER_PATTERN.matcher(str);
    while (m.find()) {
      resultTokens.add(m.group());
    }
    return resultTokens;
  }

  public static List<List<HasWord>> tokenizeTreebank(String str) {
    TokenizerFactory<Word> tokenizer = PTBTokenizerFactory
        .newTokenizerFactory();
    tokenizer.setOptions("ptb3Escaping=false");

    return MaxentTagger.tokenizeText(new StringReader(str), tokenizer);
  }

  public static void tokenizeTweets(List<Tweet> tweets) {
    for (Tweet tweet : tweets) {
      List<String> tokens = tokenize(tweet.getText());
      tweet.addSentence(tokens);
    }
  }

  public static void main(String[] args) {
    boolean extendedTest = true;
    List<Tweet> tweets = null;

    // load tweets
    if (extendedTest) {
      Dataset dataset = Configuration.getDataSetSemEval2013();
      tweets = dataset.getTrainTweets(true);
    } else { // test tweets
      tweets = Tweet.getTestTweets();
      tweets
          .add(new Tweet(
              0,
              ":-))) xDD XDD ;) :) :-) :) :D :o) :] :3 :c) :> =] 8) =) :} :^) :-D 8-D 8D x-D xD X-D XD =-D =D =-3 =3"));
      tweets
          .add(new Tweet(12,
              ">:[ :-( :(  :-c :c :-< :< :-[ :[ :{ ;( :-|| :@ >:( :'-( :'( :'-) :') \\m/"));
      tweets
          .add(new Tweet(
              0,
              "\"All the money you've spent on my credit card I'm taking it out of your account\".... Hi I'm Sydney and I'm filing bankruptcy\ud83d\ude05\ud83d\ude05\ud83d\ude05\ud83d\ude05 \ud83d\ude05"));
      tweets
          .add(new Tweet(
              0,
              "word:-) Moon:Oct Jobs! Thursday:... http://t.co/TZZzrrKa +1 (000) 123-4567, (000) 123-4567, and 123-4567 "));
      tweets
          .add(new Tweet(
              0,
              "t/m k/o w/my b/slisten Rt/follow S/o S/O O/U O/A w/ w/Biden w/deals w/you w/the w/her"));
      tweets
          .add(new Tweet(0, "5pm 5Am 5% $5 5-6am 7am-10pm U.S. U.K. L.O.V.E"));
    }

    // Tokenize tweets
    long startTime = System.currentTimeMillis();
    for (Tweet tweet : tweets) {
      List<String> tokens = Tokenizer.tokenize(tweet.getText());
      LOG.info("Tweet: '" + tweet + "'");
      LOG.info("Tokens: " + tokens);
    }
    LOG.info("Tokenize finished after "
        + (System.currentTimeMillis() - startTime) + " ms");
  }
}
