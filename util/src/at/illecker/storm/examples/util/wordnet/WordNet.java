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
package at.illecker.storm.examples.util.wordnet;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.FileUtil;
import edu.mit.jwi.IDictionary;
import edu.mit.jwi.IRAMDictionary;
import edu.mit.jwi.RAMDictionary;
import edu.mit.jwi.data.ILoadPolicy;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.ISynsetID;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.item.Pointer;
import edu.mit.jwi.morph.WordnetStemmer;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.DocumentPreprocessor;

public class WordNet {

  public static final String WORD_NET_DICT_PATH = System
      .getProperty("user.dir")
      + File.separator
      + "resources"
      + File.separator
      + "wn3.1.dict.tar.gz";
  private static final Logger LOG = LoggerFactory.getLogger(WordNet.class);

  private static WordNet instance = new WordNet(); // singleton
  private IRAMDictionary m_dict;
  private File m_wordNetDir;
  private WordnetStemmer m_wordnetStemmer;

  private WordNet() {
    try {
      File wordNetDict = new File(WORD_NET_DICT_PATH);
      m_wordNetDir = new File(wordNetDict.getParent() + File.separator + "dict");
      LOG.info("wordNetDictionary: " + wordNetDict.getAbsolutePath());
      LOG.info("wordNetExtractLocation: " + m_wordNetDir.getAbsolutePath());

      // check if extract location does exist
      if (m_wordNetDir.exists()) {
        FileUtil.delete(m_wordNetDir);
      }

      // extract tar.gz file
      FileUtil.extractTarGz(wordNetDict.getAbsolutePath(),
          m_wordNetDir.getParent());

      m_dict = new RAMDictionary(m_wordNetDir, ILoadPolicy.NO_LOAD);
      m_dict.open();

      // load into memory
      long t = System.currentTimeMillis();
      m_dict.load(true);
      LOG.info("Loaded Wordnet into memory in (%1d msec)\n",
          System.currentTimeMillis() - t);

      m_wordnetStemmer = new WordnetStemmer(m_dict);

    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  public static WordNet getInstance() {
    return instance;
  }

  public void close() throws IOException {
    m_dict.close();
    FileUtil.delete(m_wordNetDir);
  }

  public List<String> findStems(String wordString, POS wordPOS) {
    return m_wordnetStemmer.findStems(wordString, wordPOS);
  }

  public IIndexWord getIndexWord(String wordString, POS wordPOS) {
    List<String> stems = findStems(wordString, wordPOS);
    if (stems != null) {
      if (stems.size() > 1) {
        System.out.println("Be careful: the lemma \"" + wordString
            + "\" has several lemmatized form.");
      }

      for (String stem : stems) {
        return m_dict.getIndexWord(stem, wordPOS); // return random stem
      }
    }
    return null;
  }

  public Set<IIndexWord> getAllIndexWords(String wordString) {
    Set<IIndexWord> indexWords = new HashSet<IIndexWord>();
    for (POS pos : POS.values()) {
      IIndexWord indexWord = m_dict.getIndexWord(wordString, pos);
      if (indexWord != null) {
        indexWords.add(indexWord);
      }
    }
    return indexWords;
  }

  public Set<ISynset> convertIndexWordToSynsetSet(IIndexWord indexWord) {
    Set<ISynset> synsets = new HashSet<ISynset>();
    for (IWordID wordID : indexWord.getWordIDs()) {
      synsets.add(m_dict.getSynset(wordID.getSynsetID()));
    }
    return synsets;
  }

  public Set<ISynset> convertIndexWordSetToSynsetSet(Set<IIndexWord> indexWords) {
    Set<ISynset> synsets = new HashSet<ISynset>();
    for (IIndexWord indexWord : indexWords) {
      for (IWordID wordID : indexWord.getWordIDs()) {
        synsets.add(m_dict.getSynset(wordID.getSynsetID()));
      }
    }
    return synsets;
  }

  public List<ISynsetID> getAllAncestors(ISynset synset) {
    List<ISynsetID> list = new ArrayList<ISynsetID>();
    list.addAll(synset.getRelatedSynsets(Pointer.HYPERNYM));
    list.addAll(synset.getRelatedSynsets(Pointer.HYPERNYM_INSTANCE));
    return list;
  }

  public List<ISynsetID> getAllChildren(ISynset synset) {
    List<ISynsetID> list = new ArrayList<ISynsetID>();
    list.addAll(synset.getRelatedSynsets(Pointer.HYPONYM));
    list.addAll(synset.getRelatedSynsets(Pointer.HYPONYM_INSTANCE));
    return list;
  }

  public List<List<ISynset>> getPathsToRoot(ISynset synset) {
    List<List<ISynset>> pathsToRoot = null;
    List<ISynsetID> ancestors = getAllAncestors(synset);

    if (ancestors.isEmpty()) {
      pathsToRoot = new ArrayList<List<ISynset>>();
      List<ISynset> pathToRoot = new ArrayList<ISynset>();
      pathToRoot.add(synset);
      pathsToRoot.add(pathToRoot);

    } else if (ancestors.size() == 1) {
      pathsToRoot = getPathsToRoot(m_dict.getSynset(ancestors.get(0)));

      for (List<ISynset> pathToRoot : pathsToRoot) {
        pathToRoot.add(0, synset);
      }

    } else {
      pathsToRoot = new ArrayList<List<ISynset>>();
      for (ISynsetID ancestor : ancestors) {
        ISynset ancestorSynset = m_dict.getSynset(ancestor);
        List<List<ISynset>> pathsToRootLocal = getPathsToRoot(ancestorSynset);

        for (List<ISynset> pathToRoot : pathsToRootLocal) {
          pathToRoot.add(0, synset);
        }

        pathsToRoot.addAll(pathsToRootLocal);
      }
    }

    return pathsToRoot;
  }

  private ISynset findCCPFromVector(List<ISynset> pathToRoot1,
      List<ISynset> pathToRoot2) {
    int i = 0;
    int j = 0;

    if (pathToRoot1.size() > pathToRoot2.size()) {
      i = pathToRoot1.size() - pathToRoot2.size();
      j = 0;

    } else if (pathToRoot1.size() < pathToRoot2.size()) {
      i = 0;
      j = pathToRoot2.size() - pathToRoot1.size();
    }

    do {
      ISynset synset1 = pathToRoot1.get(i++);
      ISynset synset2 = pathToRoot2.get(j++);

      if (synset1.equals(synset2)) {
        return synset1;
      }

    } while (i < pathToRoot1.size());

    return null;
  }

  public ISynset findCCP(ISynset synset1, ISynset synset2) {
    if (synset1.equals(synset2)) {
      return synset1;
    }

    List<List<ISynset>> pathsToRoot1 = getPathsToRoot(synset1);
    List<List<ISynset>> pathsToRoot2 = getPathsToRoot(synset2);
    ISynset resultSynset = null;
    int i = 0;

    for (List<ISynset> pathToRoot1 : pathsToRoot1) {
      for (List<ISynset> pathToRoot2 : pathsToRoot2) {

        ISynset synset = findCCPFromVector(pathToRoot1, pathToRoot2);

        int j = pathToRoot1.size() - (pathToRoot1.indexOf(synset) + 1);
        if (j >= i) {
          i = j;
          resultSynset = synset;
        }
      }
    }

    return resultSynset;
  }

  public ISynset disambiguateWordSenses(String sentence, String wordString,
      POS wordPOS) {
    IIndexWord indexWord = getIndexWord(wordString, wordPOS);
    Set<ISynset> synsets = convertIndexWordToSynsetSet(indexWord);
    ISynset resultSynset = null;
    double bestScore = 0;
    for (ISynset synset : synsets) {
      DocumentPreprocessor dp = new DocumentPreprocessor(new StringReader(
          sentence));
      for (List<HasWord> words : dp) {
        for (HasWord word : words) {
          double score = 0;
          IIndexWord indexWordLocal = getIndexWord(word.word(), wordPOS); // TODO
                                                                          // wordPOS!
          Set<ISynset> synsetsLocal = convertIndexWordToSynsetSet(indexWordLocal);
          for (ISynset synsetLocal : synsetsLocal) {
            double sim = similarity(synsetLocal, synset);
            if (sim > 0) {
              score += sim;
            }
          }
          if (score > bestScore) {
            bestScore = score;
            resultSynset = synset;
          }
        }
      }
    }
    return resultSynset;
  }

  public int depth(ISynset synset) {
    if (synset == null) {
      return 0;
    }

    List<ISynsetID> ancestors = getAllAncestors(synset);
    if (ancestors.isEmpty()) {
      return 0;
    }

    int i = 0;
    for (ISynsetID ancestor : ancestors) {
      ISynset ancestorSynset = m_dict.getSynset(ancestor);
      int j = depth(ancestorSynset);
      i = i > j ? i : j;
    }

    return i + 1;
  }

  public int distance(ISynset synset1, ISynset synset2) {
    ISynset ccp = findCCP(synset1, synset2);

    System.out.println(depth(synset1));
    System.out.println(depth(synset2));
    System.out.println(depth(ccp));

    return depth(synset1) + depth(synset2) - 2 * depth(ccp);
  }

  public double similarity(ISynset synset1, ISynset synset2) {
    int MAX_DEPTH = 16;
    double distance = distance(synset1, synset2);
    int depth1 = depth(synset1);
    int depth2 = depth(synset2);
    double t = distance / (depth1 + depth2);

    return Math.log(t) / Math.log(1 / (2 * (MAX_DEPTH + 1)));
  }

  /**
   * Computes a score denoting how similar two word senses are, based on the
   * shortest path that connects the senses in the is-a (hypernym/hypnoym)
   * taxonomy.
   *
   * @param indexWord1
   * @param indexWord2
   * @return Returns a similarity score is in the range 0 to 1. A score of 1
   *         represents identity i.e. comparing a sense with itself will return
   *         1.
   */

  public double similarity(String word1String, POS word1POS,
      String word2String, POS word2POS) {
    IIndexWord idxWord1 = getIndexWord(word1String, word1POS);
    Set<ISynset> synsets1 = convertIndexWordToSynsetSet(idxWord1);

    IIndexWord idxWord2 = getIndexWord(word2String, word2POS);
    Set<ISynset> synsets2 = convertIndexWordToSynsetSet(idxWord2);

    double maxSim = 0;
    for (ISynset synset1 : synsets1) {
      for (ISynset synset2 : synsets2) {
        double sim = similarity(synset1, synset2);
        if ((sim > 0) && (sim > maxSim)) {
          maxSim = sim;
        }
      }
    }
    return maxSim;
  }

  public void testWordNet() {
    // treking across Wordnet
    trek(m_dict);

    System.out.println("wn.synsets('dog')");
    Set<IIndexWord> indexWords = getAllIndexWords("dog");
    for (IIndexWord indexWord : indexWords) {
      for (IWordID wordID : indexWord.getWordIDs()) {
        IWord word = m_dict.getWord(wordID);
        System.out.println(word.getSynset());
      }
    }

    System.out.println("wn.synsets('dog', pos=wn.VERB)");
    IIndexWord indexWord = m_dict.getIndexWord("dog", POS.VERB);
    IWordID wordID = indexWord.getWordIDs().get(0);
    IWord word = m_dict.getWord(wordID);
    System.out.println("Id = " + wordID);
    System.out.println("Lemma = " + word.getLemma());
    System.out.println("Gloss = " + word.getSynset().getGloss());

    String stemmingWord = "gone";
    List<String> test = m_wordnetStemmer.findStems(stemmingWord, POS.NOUN);
    for (int i = 0; i < test.size(); i++) {
      System.out.println("stems of +" + stemmingWord + ": " + test.get(i));
    }
  }

  public void trek(IDictionary dict) {
    int tickNext = 0;
    int tickSize = 20000;
    int seen = 0;
    System.out.print("Treking across Wordnet");
    long t = System.currentTimeMillis();
    for (POS pos : POS.values()) {
      for (Iterator<IIndexWord> i = dict.getIndexWordIterator(pos); i.hasNext();) {
        for (IWordID wid : i.next().getWordIDs()) {
          seen += dict.getWord(wid).getSynset().getWords().size();
          if (seen > tickNext) {
            System.out.print(".");
            tickNext = seen + tickSize;
          }
        }
      }
    }
    System.out.printf("done (%1d msec)\n", System.currentTimeMillis() - t);
    System.out.println("In my trek I saw " + seen + " words");
  }

  public static void main(String[] args) {
    try {
      WordNet.getInstance().testWordNet();
      WordNet.getInstance().close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
