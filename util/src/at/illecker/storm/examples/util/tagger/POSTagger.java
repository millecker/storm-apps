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
package at.illecker.storm.examples.util.tagger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.Configuration;
import at.illecker.storm.examples.util.io.preprocessor.Preprocessor;
import at.illecker.storm.examples.util.tokenizer.Tokenizer;
import at.illecker.storm.examples.util.wordlist.Interjections;
import at.illecker.storm.examples.util.wordlist.NameEntities;
import edu.stanford.nlp.io.RuntimeIOException;
import edu.stanford.nlp.ling.TaggedWord;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import edu.stanford.nlp.tagger.maxent.TaggerConfig;

public class POSTagger {
  private static final Logger LOG = LoggerFactory.getLogger(POSTagger.class);
  private static final boolean LOGGING = false;
  private static final POSTagger instance = new POSTagger();

  private Configuration m_conf;
  private MaxentTagger m_posTagger; // Standford POS Tagger
  private NameEntities m_nameEntities;
  private Interjections m_interjections;

  private POSTagger() {
    m_conf = Configuration.getInstance();

    // Load POS Tagger
    String taggingModel = m_conf.getPOSTaggingModel();
    try {
      LOG.info("Load POSTagger with model: " + taggingModel);
      TaggerConfig posTaggerConf = new TaggerConfig("-model", taggingModel);
      m_posTagger = new MaxentTagger(taggingModel, posTaggerConf, false);
    } catch (RuntimeIOException e) {
      LOG.error(e.getMessage());
    }

    // Load NameEntities
    m_nameEntities = NameEntities.getInstance();

    // Load Interjections
    m_interjections = Interjections.getInstance();
  }

  public static POSTagger getInstance() {
    return instance;
  }

  public List<TaggedWord> tagSentence(List<String> tokens) {
    // LOG.info("tagSentence: " + tokens.toString());
    List<TaggedWord> untaggedTokens = new ArrayList<TaggedWord>();

    Iterator<String> iter = tokens.iterator();
    while (iter.hasNext()) {
      String token = iter.next();
      TaggedWord preTaggedToken = new TaggedWord(token);
      String tokenLowerCase = token.toLowerCase();

      // set custom tags
      if (token.indexOf("#") == 0) {
        preTaggedToken.setTag("HT");
        if ((token.length() == 1) && (iter.hasNext())) {
          String nextToken = iter.next();
          preTaggedToken.setWord(preTaggedToken.word() + nextToken);
          token = nextToken;
        }
      }
      if (token.indexOf("@") == 0) {
        preTaggedToken.setTag("USR");
        if ((token.length() == 1) && (iter.hasNext())) {
          String nextToken = iter.next();
          preTaggedToken.setWord(preTaggedToken.word() + nextToken);
          token = nextToken;
          if (preTaggedToken.word().indexOf("#") == 0) {
            preTaggedToken.setTag("HT");
          }
        }
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

      // Name entities
      if (m_nameEntities.isNameEntity(tokenLowerCase)) {
        if (LOGGING) {
          LOG.info("NameEntity labelled for " + token);
        }
        preTaggedToken.setTag("NNP");
      }

      // Interjections
      if (m_interjections.isInterjection(tokenLowerCase)) {
        if (LOGGING) {
          LOG.info("Interjection labelled for " + token);
        }
        preTaggedToken.setTag("UH");
      }

      untaggedTokens.add(preTaggedToken);
    }

    if (LOGGING) {
      LOG.info("tagSentence: " + untaggedTokens.toString());
    }
    return m_posTagger.tagSentence(untaggedTokens, true);
  }

  public static void main(String[] args) {
    String text = "Gas by my house hit $3.39 !!!! I'm going to Chapel Hill on Sat . lol :)";
    System.out.println("text: '" + text + "'");

    POSTagger posTagger = POSTagger.getInstance();
    Preprocessor preProcessor = Preprocessor.getInstance();

    List<String> tokens = Tokenizer.tokenize(text);
    List<String> preprocessedTokens = preProcessor.preprocess(tokens);

    List<TaggedWord> taggedSentence = posTagger.tagSentence(preprocessedTokens);
    for (TaggedWord w : taggedSentence) {
      System.out.println("token: '" + w.word() + "' tag: '" + w.tag() + "'");
    }
  }
}
