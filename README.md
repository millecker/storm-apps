Apache Storm - Applications
==========
This code-sink includes example applications of Apache Storm.

## Applications
  - [WordCount](wordcount)
  - [POS Tagger](pos_tagger) based on the [Stanford NLP](http://nlp.stanford.edu/software/corenlp.shtml) and the [GATE Twitter model](https://gate.ac.uk/wiki/twitter-postagger.html)
  - [Simple Sentiment Analysis](simple_sentiment_analysis) based on [AFINN](http://www2.imm.dtu.dk/pubdb/views/publication_details.php?id=6010) affective word list
  - [N-Gram (Bigram)](n_gram)

## Compilation and Execution
Use Apache Ant to build and run the applications.
Further details are explained in the README of each application. 

##### Clean all files
`ant clean`

##### Build jar file
`ant jar-cpu`

##### Submit and run the Application on Apache Storm
`ant run-cpu`

## Resources
The following resources are required for POS tagging and the Sentiment Analysis:

 - `gate-EN-twitter.model` from [GATE Twitter model](https://gate.ac.uk/wiki/twitter-postagger.html)
 - `gate-EN-twitter-fast.model` from [GATE Twitter model](https://gate.ac.uk/wiki/twitter-postagger.html)
 - `AFINN-111.txt` from [AFINN](http://www2.imm.dtu.dk/pubdb/views/publication_details.php?id=6010)

These files have to be placed into the `resources` folder.



