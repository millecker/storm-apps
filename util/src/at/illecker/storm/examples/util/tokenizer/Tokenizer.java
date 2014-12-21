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

import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.process.PTBTokenizer.PTBTokenizerFactory;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class Tokenizer {

  public static List<String> tokenize(String text) {
    List<String> tokens = new ArrayList<String>();
    // trim text
    text = text.trim();
    // split at one ore more blanks
    String[] inputTokens = text.split("\\s+");
    for (String inputToken : inputTokens) {
      tokens.add(inputToken);
    }
    return tokens;
  }

  public static List<List<HasWord>> tokenizeSentences(String text) {
    TokenizerFactory<Word> tokenizer = PTBTokenizerFactory
        .newTokenizerFactory();
    tokenizer.setOptions("ptb3Escaping=false");

    return MaxentTagger.tokenizeText(new StringReader(text), tokenizer);
  }

  public static void main(String[] args) {
    String text = "Gas by my house hit $3.39 !!!! I'm going to Chapel Hill on Sat . :)";
    System.out.println("text: '" + text + "'");
    List<String> tokens = Tokenizer.tokenize(text);
    for (String token : tokens) {
      System.out.println("token: '" + token + "'");
    }
  }
}
