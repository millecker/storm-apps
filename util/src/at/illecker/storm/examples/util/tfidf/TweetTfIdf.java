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
package at.illecker.storm.examples.util.tfidf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import at.illecker.storm.examples.util.tokenizer.Tokenizer;
import at.illecker.storm.examples.util.tweet.Tweet;

/**
 * Tweet Term Frequency - Inverse Document Frequency
 * 
 */
public class TweetTfIdf {

  public static Map<Tweet, Map<String, Double>> tf(List<Tweet> tweets,
      TfType type) {
    Map<Tweet, Map<String, Double>> termFrequency = new HashMap<Tweet, Map<String, Double>>();
    for (Tweet tweet : tweets) {
      Map<String, Double> tf = new HashMap<String, Double>();
      for (List<String> sentence : tweet.getSentences()) {
        tf = TfIdf.tf(tf, sentence);
      }
      tf = TfIdf.normalizeTf(tf, type);
      termFrequency.put(tweet, tf);
    }
    return termFrequency;
  }

  public static Map<String, Double> idf(Map<Tweet, Map<String, Double>> termFreq) {
    return TfIdf.idf(termFreq);
  }

  public static Map<Tweet, Map<String, Double>> tfIdf(
      Map<Tweet, Map<String, Double>> termFreqs,
      Map<String, Double> inverseDocFreq, TfIdfNormalization normalization) {

    Map<Tweet, Map<String, Double>> tfIdf = new HashMap<Tweet, Map<String, Double>>();
    // compute tfIdf for each document
    for (Map.Entry<Tweet, Map<String, Double>> doc : termFreqs.entrySet()) {
      tfIdf.put(doc.getKey(),
          TfIdf.tfIdf(doc.getValue(), inverseDocFreq, normalization));
    }

    return tfIdf;
  }

  public static void print(String title,
      Map<Tweet, Map<String, Double>> tweetData,
      Map<String, Double> inverseDocFreq) {
    // print title
    System.out.printf("%n=== %s ===%n", title);

    // print header
    System.out.printf("%15s", " ");
    for (Map.Entry<Tweet, Map<String, Double>> tweet : tweetData.entrySet()) {
      System.out.printf("%8s", "Tweet " + tweet.getKey().getId());
    }
    System.out.println();

    // print values
    for (Map.Entry<String, Double> term : inverseDocFreq.entrySet()) {
      System.out.printf("%15s", term.getKey());
      for (Map.Entry<Tweet, Map<String, Double>> tweet : tweetData.entrySet()) {
        System.out.printf("%8.4f", tweet.getValue().get(term.getKey()));
      }
      System.out.println();
    }
  }

  public static void main(String[] args) {
    List<Tweet> tweets = new ArrayList<Tweet>();
    tweets.add(new Tweet(1,
        "Human machine interface for computer applications", 0));
    tweets.add(new Tweet(2,
        "A survey of user opinion of computer system response time", 0));
    tweets.add(new Tweet(3, "The EPS user interface management system", 0));
    tweets.add(new Tweet(4,
        "System and human system engineering testing of EPS", 0));
    tweets.add(new Tweet(5,
        "The generation of random, binary and ordered trees", 0));
    tweets.add(new Tweet(6, "The intersection graph of paths in trees", 0));
    tweets.add(new Tweet(7, "Graph minors: A survey", 0));

    // Tokenize
    for (Tweet tweet : tweets) {
      tweet.addSentence(Tokenizer.tokenize(tweet.getText()));
    }

    Map<Tweet, Map<String, Double>> termFreqs = TweetTfIdf.tf(tweets,
        TfType.RAW);
    Map<String, Double> inverseDocFreq = TweetTfIdf.idf(termFreqs);

    Map<Tweet, Map<String, Double>> tfIdf = TweetTfIdf.tfIdf(termFreqs,
        inverseDocFreq, TfIdfNormalization.NONE);

    print("Term Frequency", termFreqs, inverseDocFreq);
    print("TF IDF", tfIdf, inverseDocFreq);
  }
}
