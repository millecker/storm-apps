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
package at.illecker.storm.examples.postagger.bolt;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import twitter4j.Status;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.tuple.Tuple;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations.TreeAnnotation;
import edu.stanford.nlp.util.CoreMap;

public class POSTaggerBolt extends BaseRichBolt {
  private static final long serialVersionUID = 4980829201072337802L;
  private static final Logger LOG = LoggerFactory
      .getLogger(POSTaggerBolt.class);
  private OutputCollector m_collector;

  public void declareOutputFields(OutputFieldsDeclarer declarer) {
  }

  public void prepare(Map config, TopologyContext context,
      OutputCollector collector) {
    this.m_collector = collector;
  }

  public void execute(Tuple tuple) {
    Status status = (Status) tuple.getValue(0);
    String tweetText = status.getText();
    LOG.info("@" + status.getUser().getScreenName() + " - " + tweetText);

    // Use StanfordNLP library together with gate-EN-twitter.model
    // http://nlp.stanford.edu/software/corenlp.shtml
    // http://nlp.stanford.edu:8080/corenlp/process
    // https://gate.ac.uk/wiki/twitter-postagger.html

    Properties props = new Properties();
    // tokenize, ssplit, pos, lemma, ner, parse, dcoref
    props.put("annotators", "tokenize, ssplit, pos");
    props.put("pos.model", "gate-EN-twitter.model");
    props.put("dcoref.score", true);
    StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

    // create an empty Annotation just with the given text
    Annotation document = new Annotation(tweetText);

    // run all Annotators on this text
    pipeline.annotate(document);

    PrintWriter out = new PrintWriter(System.out);
    pipeline.prettyPrint(document, out);

    // these are all the sentences in this document
    // a CoreMap is essentially a Map that uses class objects as keys and has
    // values with custom types
    List<CoreMap> sentences = document.get(SentencesAnnotation.class);

    for (CoreMap sentence : sentences) {
      // traversing the words in the current sentence
      // a CoreLabel is a CoreMap with additional token-specific methods
      for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
        // this is the text of the token
        String word = token.get(TextAnnotation.class);
        // this is the POS tag of the token
        String pos = token.get(PartOfSpeechAnnotation.class);
        // this is the NER label of the token
        // String ne = token.get(NamedEntityTagAnnotation.class);
      }

      // this is the parse tree of the current sentence
      Tree tree = sentence.get(TreeAnnotation.class);

      // this is the Stanford dependency graph of the current sentence
      SemanticGraph dependencies = sentence
          .get(CollapsedCCProcessedDependenciesAnnotation.class);
    }

    this.m_collector.ack(tuple);
  }
}
