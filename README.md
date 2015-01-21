Apache Storm - Applications
==========
This code-sink includes example applications of Apache Storm.

## Applications
  - [WordCount](wordcount)
  - [POS Tagger](pos_tagger) based on the [Stanford NLP](http://nlp.stanford.edu/software/corenlp.shtml) [MaxentTagger](http://nlp.stanford.edu/nlp/javadoc/javanlp/edu/stanford/nlp/tagger/maxent/MaxentTagger.html) and the [GATE Twitter model](https://gate.ac.uk/wiki/twitter-postagger.html)
  - [Sentiment Analysis](sentiment_analysis)

## Compilation and Execution
Use [Apache Ant](http://ant.apache.org) to build and run the applications.<br>
Further details are explained in the *README* file of each application. 

##### Clean all files
`ant clean`

##### Build a jar file
`ant jar-cpu`

##### Submit and run an application on Apache Storm
`ant run-cpu`

## Resources
The following resources are required for POS tagging and the sentiment analysis:

 - `gate-EN-twitter.model` from [GATE Twitter model](https://gate.ac.uk/wiki/twitter-postagger.html)
 - `gate-EN-twitter-fast.model` from [GATE Twitter model](https://gate.ac.uk/wiki/twitter-postagger.html)
 - `AFINN-111.txt` from [AFINN](http://www2.imm.dtu.dk/pubdb/views/publication_details.php?id=6010)

These files have to be placed into the [`resources`](resources) folder.



