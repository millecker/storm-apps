###############################################################################
##### POS Tagger Example                                                  #####
###############################################################################

# Use Apache Ant to build and run example

# Clean all files
ant clean

# Build jar file
ant jar-cpu

# Submit Task to Storm
ant run-cpu [ -DtwitterDir=dir \
 -DconsumerKey=key \
 -DconsumerSecret=secret \
 -DaccessToken=token \
 -DaccessTokenSecret=secret]
 [-DkeyWords='word1 word2']

ant run-cpu -DtwitterDir=../../twitter

# Modify file conf/storm_env.ini
STORM_JAR_JVM_OPTS:-Xmx8g

###############################################################################