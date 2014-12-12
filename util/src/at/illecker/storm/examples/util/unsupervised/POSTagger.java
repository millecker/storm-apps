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
package at.illecker.storm.examples.util.unsupervised;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.tagger.maxent.TaggerConfig;

public class POSTagger {

  public static final String TAGGER_MODEL = System.getProperty("user.dir")
      + File.separator + "resources" + File.separator + "POSmodels"
      + File.separator + "gate-EN-twitter-fast.model";

  private static final Logger LOG = LoggerFactory.getLogger(POSTagger.class);
  private static final POSTagger instance = new POSTagger();

  // Standford POS Tagger
  private MaxentTagger m_posTagger;

  private POSTagger() {
    // Load POS Tagger
    LOG.info("Load POSTagger with model: " + TAGGER_MODEL);
    TaggerConfig posTaggerConf = new TaggerConfig("-model", TAGGER_MODEL);
    m_posTagger = new MaxentTagger(TAGGER_MODEL, posTaggerConf, false);
  }

  public static POSTagger getInstance() {
    return instance;
  }

  public List<TaggedWord> tagSentence(List<String> tokens) {
    List<TaggedWord> untaggedTokens = new ArrayList<TaggedWord>();

    for (String token : tokens) {
      TaggedWord preTaggedToken = new TaggedWord(token);

      if (token.indexOf("#") == 0) {
        preTaggedToken.setTag("HT");
      }
      if (token.indexOf("@") == 0) {
        preTaggedToken.setTag("USR");
      }
      if ((token.indexOf(".com") > -1) || (token.indexOf("http:") == 0)
          || (token.indexOf("www.") == 0)) {
        preTaggedToken.setTag("URL");
      }
      if ((token.toLowerCase().equals("rt"))
          || ((token.substring(0, 1).equals("R")) && (token.toLowerCase()
              .equals("retweet")))) {
        preTaggedToken.setTag("RT");
      }

      // Slang correction
/*
      String replacement;
      String token_lc = token.toLowerCase();
      if (corrections.containsKey(token_lc)) {
        replacement = (String) corrections.get(token_lc);
        System.err.println("Correcting " + token + " to " + replacement);
        token = replacement;
        preTaggedToken = new TaggedWord(replacement);
      }

      for (Pattern interjection_pattern : interjections) {
        Matcher m = interjection_pattern.matcher(token.toLowerCase());
        if (m.find()) {
          System.err.println("Interjection labelled for " + token);
          preTaggedToken.setTag("UH");
          break;
        }
      }

      String token_lc = token.toLowerCase();
      if (nes.contains(token_lc)) {
        System.out.println("NE labelled for " + token);
        preTaggedToken.setTag("NNP");
      }
*/
      untaggedTokens.add(preTaggedToken);
    }

    return m_posTagger.tagSentence(untaggedTokens, true);
  }

  public static void main(String[] args) {
    String text = "Gas by my house hit $3.39 !!!! I'm going to Chapel Hill on Sat . :)";
    System.out.println("text: '" + text + "'");

    POSTagger posTagger = POSTagger.getInstance();

    List<String> tokens = Tokenizer.tokenize(text);
    List<TaggedWord> taggedSentence = posTagger.tagSentence(tokens);
    for (TaggedWord w : taggedSentence) {
      System.out.println("token: '" + w.word() + "' tag: '" + w.tag() + "'");
    }
  }
}
