###############################################################################
##### Sentiment Analysis using Support Vector Machine Example             #####
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
ant clean && ant run

ant clean && ant run \
  -DconsumerKey=XXXX  -DconsumerSecret=XXXX \
  -DaccessToken=XXXX  -DaccessTokenSecret=XXXX


###############################################################################