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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import at.illecker.storm.examples.util.Configuration;
import at.illecker.storm.examples.util.io.IOUtils;
import edu.mit.jwi.IRAMDictionary;
import edu.mit.jwi.RAMDictionary;
import edu.mit.jwi.data.ILoadPolicy;
import edu.mit.jwi.item.IIndexWord;
import edu.mit.jwi.item.ISenseEntry;
import edu.mit.jwi.item.ISynset;
import edu.mit.jwi.item.ISynsetID;
import edu.mit.jwi.item.IWord;
import edu.mit.jwi.item.IWordID;
import edu.mit.jwi.item.POS;
import edu.mit.jwi.item.Pointer;
import edu.mit.jwi.morph.WordnetStemmer;
import edu.stanford.nlp.ling.TaggedWord;

public class WordNet {
  public static final int MAX_DEPTH_OF_HIERARCHY = 16;
  private static final Logger LOG = LoggerFactory.getLogger(WordNet.class);
  private static final WordNet instance = new WordNet();

  private IRAMDictionary m_dict;
  private File m_wordNetDir;
  private WordnetStemmer m_wordnetStemmer;

  private WordNet() {
    try {
      String wordNetDictPath = Configuration.getWordNetDict();
      LOG.info("WordNet Dictionary: " + wordNetDictPath);
      m_wordNetDir = new File(Configuration.TEMP_DIR_PATH + File.separator
          + "dict");
      LOG.info("WordNet Extract Location: " + m_wordNetDir.getAbsolutePath());

      // check if extract location does exist
      if (m_wordNetDir.exists()) {
        IOUtils.delete(m_wordNetDir);
      }

      // extract tar.gz file
      IOUtils.extractTarGz(wordNetDictPath, m_wordNetDir.getParent());

      m_dict = new RAMDictionary(m_wordNetDir, ILoadPolicy.NO_LOAD);
      m_dict.open();

      // load into memory
      long t = System.currentTimeMillis();
      m_dict.load(true);
      LOG.info("Loaded Wordnet into memory in "
          + (System.currentTimeMillis() - t) + " msec");

      m_wordnetStemmer = new WordnetStemmer(m_dict);

    } catch (IOException e) {
      LOG.error("IOException: " + e.getMessage());
    } catch (InterruptedException e) {
      LOG.error("InterruptedException: " + e.getMessage());
    }
  }

  public static WordNet getInstance() {
    return instance;
  }

  public void close() {
    if (m_dict != null) {
      m_dict.close();
    }
    try {
      IOUtils.delete(m_wordNetDir);
    } catch (IOException e) {
      LOG.error("IOException: " + e.getMessage());
    }
  }

  public boolean contains(String word) {
    for (POS pos : POS.values()) {
      for (String stem : m_wordnetStemmer.findStems(word, pos)) {
        IIndexWord indexWord = m_dict.getIndexWord(stem, pos);
        if (indexWord != null)
          return true;
      }
    }
    return false;
  }

  public boolean isNoun(String word) {
    return m_dict.getIndexWord(word, POS.NOUN) != null;
  }

  public boolean isAdjective(String word) {
    return m_dict.getIndexWord(word, POS.ADJECTIVE) != null;
  }

  public boolean isAdverb(String word) {
    return m_dict.getIndexWord(word, POS.ADVERB) != null;
  }

  public boolean isVerb(String word) {
    return m_dict.getIndexWord(word, POS.VERB) != null;
  }

  public synchronized POS findPOS(String word) {
    int maxCount = 0;
    POS mostLikelyPOS = null;
    for (POS pos : POS.values()) {
      // From JavaDoc: The surface form may or may not contain whitespace or
      // underscores, and may be in mixed case.
      word = word.replaceAll("\\s", "").replaceAll("_", "");

      List<String> stems = m_wordnetStemmer.findStems(word, pos);
      for (String stem : stems) {
        IIndexWord indexWord = m_dict.getIndexWord(stem, pos);
        if (indexWord != null) {
          int count = 0;
          for (IWordID wordId : indexWord.getWordIDs()) {
            IWord aWord = m_dict.getWord(wordId);
            ISenseEntry senseEntry = m_dict.getSenseEntry(aWord.getSenseKey());
            count += senseEntry.getTagCount();
          }

          if (count > maxCount) {
            maxCount = count;
            mostLikelyPOS = pos;
          }
        }
      }
    }

    return mostLikelyPOS;
  }

  public List<String> findStems(String word, POS pos) {
    return m_wordnetStemmer.findStems(word, pos);
  }

  public IIndexWord getIndexWord(String word, POS pos) {
    List<String> stems = findStems(word, pos);
    if (stems != null) {
      if (stems.size() > 1) {
        LOG.info("Be careful the word '" + word
            + "' has several lemmatized forms.");
      }

      for (String stem : stems) {
        return m_dict.getIndexWord(stem, pos); // return first stem
      }
    }
    return null;
  }

  public String getLemma(ISynset synset) {
    return synset.getWord(0).getLemma();
  }

  public String getLemma(ISynsetID synsetID) {
    return getLemma(m_dict.getSynset(synsetID));
  }

  public Set<IIndexWord> getAllIndexWords(String word) {
    Set<IIndexWord> indexWords = new HashSet<IIndexWord>();
    for (POS pos : POS.values()) {
      IIndexWord indexWord = m_dict.getIndexWord(word, pos);
      if (indexWord != null) {
        indexWords.add(indexWord);
      }
    }
    return indexWords;
  }

  public ISynset getSynset(String word, POS pos) {
    IIndexWord indexWord = m_dict.getIndexWord(word, pos);
    if (indexWord != null) {
      IWordID wordID = indexWord.getWordIDs().get(0); // use first WordID
      return m_dict.getWord(wordID).getSynset();
    }
    return null;
  }

  public Set<ISynset> getSynsets(IIndexWord indexWord) {
    Set<ISynset> synsets = new HashSet<ISynset>();
    for (IWordID wordID : indexWord.getWordIDs()) {
      synsets.add(m_dict.getSynset(wordID.getSynsetID()));
    }
    return synsets;
  }

  public Set<ISynset> getSynsets(Set<IIndexWord> indexWords) {
    Set<ISynset> synsets = new HashSet<ISynset>();
    for (IIndexWord indexWord : indexWords) {
      for (IWordID wordID : indexWord.getWordIDs()) {
        synsets.add(m_dict.getSynset(wordID.getSynsetID()));
      }
    }
    return synsets;
  }

  public List<ISynsetID> getAncestors(ISynset synset) {
    List<ISynsetID> list = new ArrayList<ISynsetID>();
    list.addAll(synset.getRelatedSynsets(Pointer.HYPERNYM));
    list.addAll(synset.getRelatedSynsets(Pointer.HYPERNYM_INSTANCE));
    return list;
  }

  public List<ISynsetID> getChildren(ISynset synset) {
    List<ISynsetID> list = new ArrayList<ISynsetID>();
    list.addAll(synset.getRelatedSynsets(Pointer.HYPONYM));
    list.addAll(synset.getRelatedSynsets(Pointer.HYPONYM_INSTANCE));
    return list;
  }

  public List<List<ISynset>> getPathsToRoot(ISynset synset) {
    List<List<ISynset>> pathsToRoot = null;
    List<ISynsetID> ancestors = getAncestors(synset);

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

  private ISynset findClosestCommonParent(List<ISynset> pathToRoot1,
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

  public ISynset findClosestCommonParent(ISynset synset1, ISynset synset2) {
    if ((synset1 == null) || (synset2 == null)) {
      return null;
    }
    if (synset1.equals(synset2)) {
      return synset1;
    }

    List<List<ISynset>> pathsToRoot1 = getPathsToRoot(synset1);
    List<List<ISynset>> pathsToRoot2 = getPathsToRoot(synset2);
    ISynset resultSynset = null;
    int i = 0;

    for (List<ISynset> pathToRoot1 : pathsToRoot1) {
      for (List<ISynset> pathToRoot2 : pathsToRoot2) {

        ISynset synset = findClosestCommonParent(pathToRoot1, pathToRoot2);

        int j = pathToRoot1.size() - (pathToRoot1.indexOf(synset) + 1);
        if (j >= i) {
          i = j;
          resultSynset = synset;
        }
      }
    }

    return resultSynset;
  }

  /**
   * maxDepth
   * 
   * @param synset
   * @return The length of the longest hypernym path from this synset to the
   *         root.
   */
  public int maxDepth(ISynset synset) {
    if (synset == null) {
      return 0;
    }

    List<ISynsetID> ancestors = getAncestors(synset);
    if (ancestors.isEmpty()) {
      return 0;
    }

    int i = 0;
    for (ISynsetID ancestor : ancestors) {
      ISynset ancestorSynset = m_dict.getSynset(ancestor);
      int j = maxDepth(ancestorSynset);
      i = (i > j) ? i : j;
    }

    return i + 1;
  }

  /**
   * Shortest Path Distance
   * 
   * Returns the distance of the shortest path linking the two synsets (if one
   * exists).
   * 
   * For each synset, all the ancestor nodes and their distances are recorded
   * and compared. The ancestor node common to both synsets that can be reached
   * with the minimum number of traversals is used. If no ancestor nodes are
   * common, null is returned. If a node is compared with itself 0 is returned.
   * 
   * @param synset1
   * @param synset2
   * @return The number of edges in the shortest path connecting the two nodes,
   *         or null if no path exists.
   */
  public Integer shortestPathDistance(ISynset synset1, ISynset synset2) {
    Integer distance = null;
    if (synset1.equals(synset2)) {
      return 0;
    }

    ISynset ccp = findClosestCommonParent(synset1, synset2);
    if (ccp != null) {
      distance = maxDepth(synset1) + maxDepth(synset2) - 2 * maxDepth(ccp);

      // Debug
      String w1 = synset1.getWords().get(0).getLemma();
      String w2 = synset2.getWords().get(0).getLemma();
      String w3 = ccp.getWords().get(0).getLemma();
      System.out.println("maxDepth(" + w1 + "): " + maxDepth(synset1));
      System.out.println("maxDepth(" + w2 + "): " + maxDepth(synset2));
      System.out.println("maxDepth(" + w3 + "): " + maxDepth(ccp));
      System.out.println("distance(" + w1 + "," + w2 + "): " + distance);
    }
    return distance;
  }

  /**
   * Path Distance Similarity
   * 
   * Return a score denoting how similar two word senses are, based on the
   * shortest path that connects the senses in the is-a (hypernym/hypnoym)
   * taxonomy.
   * 
   * The score is in the range 0 to 1, except in those cases where a path cannot
   * be found (will only be true for verbs as there are many distinct verb
   * taxonomies), in which case null is returned.
   * 
   * A score of 1 represents identity i.e. comparing a sense with itself will
   * return 1.
   * 
   * @param synset1
   * @param synset2
   * @return A score denoting the similarity of the two ``Synset`` objects,
   *         normally between 0 and 1. null is returned if no connecting path
   *         could be found. 1 is returned if a ``Synset`` is compared with
   *         itself.
   */
  public Double pathSimilarity(ISynset synset1, ISynset synset2) {
    Integer distance = shortestPathDistance(synset1, synset2);
    Double pathSimilarity = null;
    if (distance != null) {
      if (distance < 0) {
        throw new IllegalArgumentException("Distance value is negative!");
      }
      pathSimilarity = 1 / ((double) distance + 1);

      // Debug
      String w1 = synset1.getWords().get(0).getLemma();
      String w2 = synset2.getWords().get(0).getLemma();
      System.out.println("maxDepth(" + w1 + "): " + maxDepth(synset1));
      System.out.println("maxDepth(" + w2 + "): " + maxDepth(synset2));
      System.out.println("distance: " + distance);
      System.out.println("pathSimilarity(" + w1 + "," + w2 + "): "
          + pathSimilarity);
    } else {
      // TODO simulate_root=True
    }
    return pathSimilarity;
  }

  /**
   * Leacock Chodorow Similarity
   * 
   * Return a score denoting how similar two word senses are, based on the
   * shortest path that connects the senses and the maximum depth of the
   * taxonomy in which the senses occur. The relationship is given as -log(p/2d)
   * where p is the shortest path length and d is the taxonomy depth.
   * 
   * lch(c1,c2) = - log(minPathLength(c1,c2) / 2 * depth of the hierarchy)
   * lch(c1,c2) = - log(minPL(c1,c2) / 2 * depth) = log(2*depth / minPL(c1,c2))
   * 
   * minPathLength is measured in nodes, i.e. the distance of a node to itself
   * is 0! This would cause a logarithm error (or a division by zero)). Thus we
   * changed the behaviour in order to return a distance of 1, if the nodes are
   * equal or neighbors.
   * 
   * double relatedness = Math.log( (2*depthOfHierarchy) / (pathLength + 1) );
   * 
   * @param synset1
   * @param synset2
   * @return A score denoting the similarity of the two ``Synset`` objects,
   *         normally greater than 0. null is returned if no connecting path
   *         could be found. If a ``Synset`` is compared with itself, the
   *         maximum score is returned, which varies depending on the taxonomy
   *         depth.
   */
  public Double lchSimilarity(ISynset synset1, ISynset synset2) {
    Integer distance = shortestPathDistance(synset1, synset2);
    Double lchSimilarity = null;
    if (distance != null) {
      if (distance < 0) {
        throw new IllegalArgumentException("Distance value is negative!");
      }
      if (distance == 0) {
        distance = 1;
      }

      lchSimilarity = Math.log((2 * MAX_DEPTH_OF_HIERARCHY)
          / ((double) distance));

      // Debug
      String w1 = synset1.getWords().get(0).getLemma();
      String w2 = synset2.getWords().get(0).getLemma();
      System.out.println("lchSimilarity(" + w1 + "," + w2 + "): "
          + lchSimilarity);
    } else {
      // TODO simulate_root=True
    }
    return lchSimilarity;
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
    IIndexWord indexWord1 = getIndexWord(word1String, word1POS);
    Set<ISynset> synsets1 = getSynsets(indexWord1);

    IIndexWord indexWord2 = getIndexWord(word2String, word2POS);
    Set<ISynset> synsets2 = getSynsets(indexWord2);

    double maxSim = 0;
    for (ISynset synset1 : synsets1) {
      for (ISynset synset2 : synsets2) {
        double sim = pathSimilarity(synset1, synset2);
        if ((sim > 0) && (sim > maxSim)) {
          maxSim = sim;
        }
      }
    }
    return maxSim;
  }

  public ISynset disambiguateWordSenses(List<TaggedWord> sentence, String word,
      POS pos) {
    IIndexWord indexWord = getIndexWord(word, pos);
    Set<ISynset> synsets = getSynsets(indexWord);

    ISynset resultSynset = null;
    double bestScore = 0;
    for (ISynset synset : synsets) {
      for (TaggedWord taggedWord : sentence) {
        double score = 0;
        IIndexWord indexWordLocal = getIndexWord(taggedWord.word(),
            POSTag.convertString(taggedWord.tag()));
        Set<ISynset> synsetsLocal = getSynsets(indexWordLocal);
        for (ISynset synsetLocal : synsetsLocal) {
          double sim = shortestPathDistance(synsetLocal, synset);
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
    return resultSynset;
  }

  /**
   * testWordNet implementation
   */
  public void testWordNet() {
    // Performance test - treking across Wordnet
    trek();

    // ************************************************************************
    // Misc Tests
    // ************************************************************************
    System.out.println("\nwn.synsets('dog')");
    Set<IIndexWord> indexWords = getAllIndexWords("dog");
    for (IIndexWord indexWord : indexWords) {
      System.out.println("indexWords: ");
      for (IWordID wordID : indexWord.getWordIDs()) {
        IWord word = m_dict.getWord(wordID);
        System.out.println(word.getSynset());
      }
    }

    System.out.println("\nwn.synsets('dog', pos=wn.NOUN)");
    IIndexWord indexWord = m_dict.getIndexWord("dog", POS.NOUN);
    IWordID wordID = indexWord.getWordIDs().get(0);
    IWord word = m_dict.getWord(wordID);
    System.out.println("Id = " + wordID);
    System.out.println("Lemma = " + word.getLemma());
    System.out.println("Gloss = " + word.getSynset().getGloss());

    System.out
        .println("\ndog = wn.synset('dog.n.01') dog.hypernyms - ancestors");
    List<ISynsetID> ancestors = getAncestors(getSynset("dog", POS.NOUN));
    for (ISynsetID ancestor : ancestors) {
      ISynset synset = m_dict.getSynset(ancestor);
      System.out.println(synset);
    }

    System.out.println("\ndog = wn.synset('dog.n.01') dog.hyponyms - children");
    List<ISynsetID> children = getChildren(getSynset("dog", POS.NOUN));
    for (ISynsetID child : children) {
      ISynset synset = m_dict.getSynset(child);
      System.out.println(synset);
    }

    // ************************************************************************
    // Test similarities
    // ************************************************************************
    Double pathSimilarity = null;
    Double lchSimilarity = null;

    System.out.println("\nwn.path_similarity(dog, dog) = 1.0");
    pathSimilarity = pathSimilarity(getSynset("dog", POS.NOUN),
        getSynset("dog", POS.NOUN));
    System.out.println("pathSimilarity: " + pathSimilarity);

    System.out.println("\nwn.lch_similarity(dog, dog) = 3.63758");
    lchSimilarity = lchSimilarity(getSynset("dog", POS.NOUN),
        getSynset("dog", POS.NOUN));
    System.out.println("lchSimilarity: " + lchSimilarity);

    System.out.println("\nwn.path_similarity(dog, cat) = 0.2");
    pathSimilarity = pathSimilarity(getSynset("dog", POS.NOUN),
        getSynset("cat", POS.NOUN));
    System.out.println("pathSimilarity: " + pathSimilarity);

    System.out.println("\nwn.lch_similarity(dog, cat) = 2.02814");
    lchSimilarity = lchSimilarity(getSynset("dog", POS.NOUN),
        getSynset("cat", POS.NOUN));
    System.out.println("lchSimilarity: " + lchSimilarity);

    System.out.println("\nwn.path_similarity(hit, slap) = 0.14285");
    pathSimilarity = pathSimilarity(getSynset("hit", POS.VERB),
        getSynset("slap", POS.VERB));
    System.out.println("pathSimilarity: " + pathSimilarity);

    System.out.println("\nwn.lch_similarity(hit, slap) = 1.31218");
    lchSimilarity = lchSimilarity(getSynset("hit", POS.VERB),
        getSynset("slap", POS.VERB));
    System.out.println("lchSimilarity: " + lchSimilarity);

    // ************************************************************************
    // Test stemming
    // ************************************************************************
    System.out.println("\nStemming test...");
    String[] stemmingWords = { "cats", "running", "ran", "cactus", "cactuses",
        "community", "communities", "going" };
    POS[] stemmingPOS = { POS.NOUN, POS.VERB, POS.VERB, POS.NOUN, POS.NOUN,
        POS.NOUN, POS.NOUN, POS.VERB };
    String[] stemmingResults = { "cat", "run", "run", "cactus", "cactus",
        "community", "community", "go" };

    for (int i = 0; i < stemmingWords.length; i++) {
      List<String> stemResults = m_wordnetStemmer.findStems(stemmingWords[i],
          stemmingPOS[i]);
      for (String stemResult : stemResults) {
        System.out.println("stems of \"" + stemmingWords[i] + "\": "
            + stemResult);
        // verify result
        if (!stemResult.equals(stemmingResults[i])) {
          System.err.println("Wrong stemming result of \"" + stemmingWords[i]
              + "\" result: \"" + stemResult + "\" expected: \""
              + stemmingResults[i] + "\"");
        }
      }
    }

  }

  /**
   * Treking across Wordnet for performance measurements
   * 
   */
  private void trek() {
    int tickNext = 0;
    int tickSize = 20000;
    int seen = 0;
    System.out.print("Treking across Wordnet");
    long t = System.currentTimeMillis();
    for (POS pos : POS.values()) {
      for (Iterator<IIndexWord> i = m_dict.getIndexWordIterator(pos); i
          .hasNext();) {
        for (IWordID wid : i.next().getWordIDs()) {
          seen += m_dict.getWord(wid).getSynset().getWords().size();
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
    WordNet wordNet = WordNet.getInstance();
    wordNet.testWordNet();
    wordNet.close();
  }
}
