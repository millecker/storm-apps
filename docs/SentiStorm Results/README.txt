###############################################################################
##### BenchmarkResults R Script                                           #####
###############################################################################

# Command-line Arguments

./BenchmarkResults.R <CSVInputFile> <TotalTuples>
   [<Title>]
   [<XaxisDescription>]
   [<YaxisDescription>]
   [XticksIncrement]
   [YticksIncrement]
   [XticksStart]
   [YticksStart]

# Examples

./BenchmarkResults.R 38130Tuples_c3.8xlarge.csv 38130 "SentiStorm Multicore Performance for 38130 test tweets on one c3.8xlarge instance" "Threads" "Tweets / Second" 1 200 1 200

