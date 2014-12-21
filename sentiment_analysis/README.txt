###############################################################################
##### Sentiment Analysis                                                  #####
###############################################################################

# Use Apache Ant to build and run example

# Clean all files
ant clean

# Build jar file
ant jar-cpu

# Submit Task to Storm
ant run-cpu [ -DreferenceFile=file.json \
 -DconsumerKey=key \
 -DconsumerSecret=secret \
 -DaccessToken=token \
 -DaccessTokenSecret=secret]
 [-DkeyWords='word1 word2']

# Example
ant clean && ant run-cpu -DreferenceFile=../resources/datasets/dataset2/mislove_1000tweets_with_sentistrength_and_afinn.json

# Modify file conf/storm_env.ini
STORM_JAR_JVM_OPTS:-Xmx8g

# Modify file conf/storm.yaml
worker.childopts: "-Xmx8g"

###############################################################################
