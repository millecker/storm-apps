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
package at.illecker.storm.examples.util.io.preprocessor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.tokenizer.Tokenizer;
import at.illecker.storm.examples.util.wordlist.SlangCorrection;

public class Preprocessor {
  private static final Logger LOG = LoggerFactory.getLogger(Preprocessor.class);
  private static final boolean LOGGING = false;
  private static final Preprocessor instance = new Preprocessor();

  private SlangCorrection m_slangCorrection;

  private Preprocessor() {
    // Load SlangCorrection
    m_slangCorrection = SlangCorrection.getInstance();
  }

  public static Preprocessor getInstance() {
    return instance;
  }

  public List<String> preprocess(List<String> tokens) {
    // LOG.info("preprocess: " + tokens.toString());
    List<String> processedTokens = new ArrayList<String>();

    Iterator<String> iter = tokens.iterator();
    while (iter.hasNext()) {
      String token = iter.next();
      String tokenLowerCase = token.toLowerCase();

      // Slang correction
      String correction = m_slangCorrection.getCorrection(tokenLowerCase);
      if (correction != null) {
        if (LOGGING) {
          LOG.info("SlangCorrecting from " + token + " to " + correction);
        }
        token = correction;
      }

      processedTokens.add(token);
    }

    if (LOGGING) {
      LOG.info("preprocessed: " + processedTokens.toString());
    }

    return processedTokens;
  }

  public static void main(String[] args) {
    String text = "2moro afaik bbq hf lol";
    System.out.println("text: '" + text + "'");

    Preprocessor preprocessor = Preprocessor.getInstance();
    List<String> tokens = Tokenizer.tokenize(text);
    List<String> processedTokens = preprocessor.preprocess(tokens);

    System.out.println("preprocessed: '" + processedTokens + "'");
  }
}
