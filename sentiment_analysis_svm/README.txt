###############################################################################
##### Sentiment Analysis using Support Vector Machine Application         #####
###############################################################################

# Use Apache Ant to build and run example

# Clean all files
ant clean

# Build jar file
ant jar

# Submit Task to Storm
ant run [ -DconsumerKey=key \
 -DconsumerSecret=secret \
 -DaccessToken=token \
 -DaccessTokenSecret=secret]
 [-DkeyWords='word1 word2']

# Example
ant run

ant run \
  -DconsumerKey=XXXX  -DconsumerSecret=XXXX \
  -DaccessToken=XXXX  -DaccessTokenSecret=XXXX

# Benchmark
ant bench -DbenchTimeLimit='--time-limit 10000s' \
  -DbenchInstrument='--instrument macro' \
  -DbenchMacroMeasurements='-Cinstrument.macro.options.measurements=5' \
  -DbenchMacroWarmup='-Cinstrument.macro.options.warmup=30s'
  [-DbenchTrials='--trials 1']

###############################################################################