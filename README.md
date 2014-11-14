Apache Storm - Applications
==========
This code-sink includes example applications of Apache Storm.

## Examples
  - WordCount
  - POS Tagger
  - Simple Sentiment Analysis based on [AFINN](http://www2.imm.dtu.dk/pubdb/views/publication_details.php?id=6010) affective word list
  - N-Gram (Bigram)

## Compilation and Execution
Use Apache Ant to build and run examples.

##### Clean all files
`ant clean`

##### Build jar file
`ant jar-cpu`

##### Submit Task to Storm
`ant run-cpu`
