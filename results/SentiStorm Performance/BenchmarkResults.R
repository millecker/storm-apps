#!/usr/bin/Rscript
# library(rjson) # install.packages("rjson")
# library(reshape2) # install.packages("reshape2")
# suppressPackageStartupMessages(library(sqldf)) # install.packages("sqldf")
library(ggplot2) # install.packages("ggplot2")
# library(scales) # percentage for efficiency plot # install.packages("scales")
library(extrafont) # extra font "LM Roman" # install.packages("extrafont") and font_import()
# library(stringr) # str_locate # install.packages("stringr")

###############################################################################
# Defaults
###############################################################################
printTitle <- TRUE

# font type and sizes
fontType <- "LM Roman 10"
legendFontSize <- 20
axisTitleFontSize <- 22
axisTicksFontSize <- 20
axisTicksFontColor <- "black"

# colors
defaultRedColor <- "#BE1621" # red
defaultGreenColor <- "#006532" # green

defaultBarColor <- "gray"
defaultGeomLineColor <- "#BE1621" # red
defaultSpeedupGeomLineColor <- "#302683"
defaultEfficiencyGeomLineColor <- "#F39200"

#BE1621 red
#E6332A red_light
#006532 green
#5C9630 green_light
#302683 blue
#1D70B7 blue_light
#F39200 orange
#F9B233 orange_light

squareShape <- 15 # square
circleShape <- 16 # circle

defaultXAxisTextAngle <- 0

###############################################################################
# Check command-line arguments
###############################################################################
# arg1: <csvInputFile>
# arg2: <totalTuples>
# arg3: [<Title>]
# arg4: [<XaxisDescription>]
# arg5: [<YaxisDescription>]
# arg6: [XticksIncrement]
# arg7: [YticksIncrement]
# arg8: [XticksStart]
# arg9: [YticksStart]

args <- commandArgs(trailingOnly = TRUE)
if (is.na(args[1])) {
  stop("Input CSV file is missing!")
}

# argument totalTuples
if (is.na(args[2])) {
  stop("Total tuples argument is missing!")
} else {
  totalTuples <- as.numeric(args[2]);
}

# argument title
if (is.na(args[3])) {
  title <- NA
} else {
  title <- args[3];
}

# argument XaxisDescription
if (is.na(args[4])) {
  XaxisDescription <- NA
} else {
  XaxisDescription <- args[4];
}

# argument YaxisDescription
if (is.na(args[5])) {
  YaxisDescription <- NA
} else {
  YaxisDescription <- args[5];
}

# argument XticksIncrement
if (is.na(args[6])) {
  XticksIncrement <- 1
} else {
  XticksIncrement <- as.numeric(args[6]);
}

# argument YticksIncrement
if (is.na(args[7])) {
  YticksIncrement <- 1
} else {
  YticksIncrement <- as.numeric(args[7]);
}

# argument XticksStart
if (is.na(args[8])) {
  XticksStart <- NA
} else {
  XticksStart <- as.numeric(args[8]);
}

# argument YticksStart
if (is.na(args[9])) {
  YticksStart <- NA
} else {
  YticksStart <- as.numeric(args[9]);
}

###############################################################################
# load result file
###############################################################################
csvInputFile <- args[1]
# csvInputFile <- "~/workspace/storm-apps/results/SentiStorm Performance/38130Tuples_c3.8xlarge.csv"
measurement.df <- read.csv(csvInputFile)
if (is.null(measurement.df)) {
  message <- paste("CSV input file was not found!",csvInputFile)
  stop(message)
}

# str(measurementTable)

###############################################################################
# Generate measurement.df
###############################################################################
measurements <- length(measurement.df) - 1

# Create sub table
measurement.df.sub <- measurement.df[, 1:measurements+1]
# measurement.df.sub

# Add Sum
Sum <- as.numeric()
for(r in row.names(measurement.df.sub)) {
  Sum <- append(Sum, sum(measurement.df.sub[r,], na.rm = TRUE)) 
}
measurement.df <- cbind(measurement.df, Sum)
rm(r)
rm(Sum)
rm(measurement.df.sub)

# Add Average
measurement.df$Avg <- measurement.df$Sum / measurements

# Add tuples / second
measurement.df$TuplesPerMilli <- totalTuples / measurement.df$Avg
measurement.df$TuplesPerSec <- measurement.df$TuplesPerMilli * 1000
measurement.df

###############################################################################
# Generate Figures
###############################################################################

if ((isTRUE(printTitle)) && (!is.na(title))) {
  title <- title
} else {
  title <- ""
}

###############################################################################
# Generate geom Line plot
###############################################################################
if (is.na(XaxisDescription)) {
  XaxisDescription <- "Threads"
}

if (is.na(YaxisDescription)) {
  YaxisDescription <- "Tuples / Second"
}

# min and max for X axis ticks
if (!is.na(XticksStart)) {
  minX <- XticksStart
} else {
  minX <- round(min(measurement.df$Threads))
}
maxX <- round(max(measurement.df$Threads))

# min and max for Y axis ticks
if (!is.na(YticksStart)) {
  minY <- YticksStart
} else {
  minY <- round(min(measurement.df$TuplesPerSec))
}
maxY <- round(max(measurement.df$TuplesPerSec)) + YticksIncrement

ggplot(measurement.df, aes(x=Threads,y=TuplesPerSec)) + 
  geom_point(size=5,color=defaultGeomLineColor) +
  geom_line(color=defaultGeomLineColor) +
  scale_x_continuous(breaks = round(seq(minX, maxX, by = XticksIncrement), 1)) +
  scale_y_continuous(breaks = round(seq(minY, maxY, by = YticksIncrement), 1)) +
  xlab(XaxisDescription) +
  ylab(YaxisDescription) +
  ggtitle(title) +
  theme(text=element_text(family=fontType),
        legend.position = "none",
        plot.title =  element_text(face="bold", vjust=1, size=axisTitleFontSize),
        axis.title.x = element_text(face="bold", vjust=-0.5, size=axisTitleFontSize),
        axis.text.x  = element_text(angle=defaultXAxisTextAngle, vjust=0.5, size=axisTicksFontSize, colour=axisTicksFontColor),
        axis.title.y = element_text(face="bold", vjust=1, size=axisTitleFontSize),
        axis.text.y  = element_text(vjust=0.5, size=axisTicksFontSize, colour=axisTicksFontColor))

outputfile <- paste(csvInputFile, "_geom_line_not_embeded_fonts.pdf", sep="")
outputfileEmbeded <- paste(csvInputFile, "_geom_line.pdf", sep="")

ggsave(file=outputfile, scale=2)
embed_fonts(outputfile, outfile=outputfileEmbeded)
file.remove(outputfile)
message <- paste("Info: Saved GeomLine Plot in ",outputfileEmbeded,"\n",sep="")
cat(message)

# Delete temporary created plot file
unlink("Rplots.pdf")
