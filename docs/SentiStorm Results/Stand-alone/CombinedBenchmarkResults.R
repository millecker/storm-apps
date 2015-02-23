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
printTitle <- FALSE

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

ArkColor <- "#F39200"
GateColor <- "#302683"
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
# arg1: <csvInputFile1>
# arg2: <csvInputFile2>
# arg3: <totalTuples>
# arg4: [<Title>]
# arg5: [<XaxisDescription>]
# arg6: [<YaxisDescription>]
# arg7: [XticksIncrement]
# arg8: [YticksIncrement]
# arg9: [XticksStart]
# arg10: [YticksStart]

args <- commandArgs(trailingOnly = TRUE)
if (is.na(args[1])) {
  stop("Input CSV file1 is missing!")
}

if (is.na(args[2])) {
  stop("Input CSV file2 is missing!")
}

# argument totalTuples
if (is.na(args[3])) {
  stop("Total tuples argument is missing!")
} else {
  totalTuples <- as.numeric(args[3]);
}

# argument title
if (is.na(args[4])) {
  title <- NA
} else {
  title <- args[4];
}

# argument XaxisDescription
if (is.na(args[5])) {
  XaxisDescription <- NA
} else {
  XaxisDescription <- args[5];
}

# argument YaxisDescription
if (is.na(args[6])) {
  YaxisDescription <- NA
} else {
  YaxisDescription <- args[6];
}

# argument XticksIncrement
if (is.na(args[7])) {
  XticksIncrement <- 1
} else {
  XticksIncrement <- as.numeric(args[7]);
}

# argument YticksIncrement
if (is.na(args[8])) {
  YticksIncrement <- 1
} else {
  YticksIncrement <- as.numeric(args[8]);
}

# argument XticksStart
if (is.na(args[9])) {
  XticksStart <- NA
} else {
  XticksStart <- as.numeric(args[9]);
}

# argument YticksStart
if (is.na(args[10])) {
  YticksStart <- NA
} else {
  YticksStart <- as.numeric(args[10]);
}

###############################################################################
# load result file
###############################################################################
csvInputFile1 <- args[1]
# csvInputFile1 <- "~/workspace/storm-apps/docs/SentiStorm\ Results/Stand-alone//38130_tuples_ark_c3.4xlarge.csv"
csvInputFile2 <- args[2]
# csvInputFile2 <- "~/workspace/storm-apps/docs/SentiStorm\ Results/Stand-alone//38130_tuples_gate_c3.4xlarge.csv"

measurement.df1 <- read.csv(csvInputFile1)
measurement.df2 <- read.csv(csvInputFile2)

if (is.null(measurement.df1)) {
  message <- paste("First CSV input file was not found!",csvInputFile1)
  stop(message)
}
if (is.null(measurement.df2)) {
  message <- paste("First CSV input file was not found!",csvInputFile2)
  stop(message)
}

# str(measurementTable)


###############################################################################
# Generate measurement.df
###############################################################################
measurements1 <- length(measurement.df1) - 1
measurements2 <- length(measurement.df2) - 1

# Create sub table
measurement.df1.sub <- measurement.df1[, 1:measurements1+1]
measurement.df2.sub <- measurement.df2[, 1:measurements2+1]
# measurement.df.sub

# Add Sum
Sum <- as.numeric()
for(r in row.names(measurement.df1.sub)) {
  Sum <- append(Sum, sum(measurement.df1.sub[r,], na.rm = TRUE)) 
}
measurement.df1 <- cbind(measurement.df1, Sum)
Sum <- as.numeric()
for(r in row.names(measurement.df2.sub)) {
  Sum <- append(Sum, sum(measurement.df2.sub[r,], na.rm = TRUE)) 
}
measurement.df2 <- cbind(measurement.df2, Sum)
rm(r)
rm(Sum)
rm(measurement.df1.sub)
rm(measurement.df2.sub)

# Add Average
measurement.df1$Avg <- measurement.df1$Sum / measurements1
measurement.df2$Avg <- measurement.df2$Sum / measurements2

# Add tuples / second
# totalTuples <- 38130
measurement.df1$TuplesPerMilli <- totalTuples / measurement.df1$Avg
measurement.df1$TuplesPerSec <- measurement.df1$TuplesPerMilli * 1000
measurement.df1

measurement.df2$TuplesPerMilli <- totalTuples / measurement.df2$Avg
measurement.df2$TuplesPerSec <- measurement.df2$TuplesPerMilli * 1000
measurement.df2

###############################################################################
# Transpose data
###############################################################################

# transposedMeasurement.df1 <- as.data.frame(t(measurement.df1))
# colnames(transposedMeasurement.df1) <- transposedMeasurement.df1[1, ]
# transposedMeasurement.df1 <- transposedMeasurement.df1[-1, ]
# transposedMeasurement.df1$id<-seq.int(nrow(transposedMeasurement.df1))
# transposedMeasurement.df1$type <- "a"

# transposedMeasurement.df2 <- as.data.frame(t(measurement.df2))
# colnames(transposedMeasurement.df2) <- transposedMeasurement.df2[1, ]
# transposedMeasurement.df2 <- transposedMeasurement.df2[-1, ]
# transposedMeasurement.df2$id<-seq.int(nrow(transposedMeasurement.df2))
# transposedMeasurement.df2$type <- "b"

measurement.df1$Type <- "ARK POS Tagger"
measurement.df2$Type <- "GATE POS Tagger"
combined <- rbind(measurement.df1, measurement.df2)

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
  minX <- round(min(combined$Threads))
}
maxX <- round(max(combined$Threads))

# min and max for Y axis ticks
if (!is.na(YticksStart)) {
  minY <- YticksStart
} else {
  minY <- round(min(combined$TuplesPerSec))
}
maxY <- round(max(combined$TuplesPerSec)) + YticksIncrement

ggplot(data=combined,  aes(x=Threads,y=TuplesPerSec,group=Type,shape=Type,colour=Type)) + 
  geom_point(size=5) +
  geom_line() +
  scale_x_continuous(breaks = round(seq(minX, maxX, by = XticksIncrement), 1)) +
  scale_y_continuous(breaks = round(seq(minY, maxY, by = YticksIncrement), 1)) +
  xlab(XaxisDescription) +
  ylab(YaxisDescription) +
  ggtitle(title) +
  scale_colour_manual(values=c(ArkColor, GateColor)) +
  theme(text=element_text(family=fontType),
        legend.position = "bottom",
        legend.title =  element_text(face="bold", vjust=1, size=axisTitleFontSize),
        legend.text =  element_text(vjust=1, size=axisTitleFontSize),
        plot.title =  element_text(face="bold", vjust=1, size=axisTitleFontSize),
        axis.title.x = element_text(face="bold", vjust=-0.5, size=axisTitleFontSize),
        axis.text.x  = element_text(angle=defaultXAxisTextAngle, vjust=0.5, size=axisTicksFontSize, colour=axisTicksFontColor),
        axis.title.y = element_text(face="bold", vjust=1, size=axisTitleFontSize),
        axis.text.y  = element_text(vjust=0.5, size=axisTicksFontSize, colour=axisTicksFontColor))

outputfile <- paste(csvInputFile1, "_combined_geom_line_not_embeded_fonts.pdf", sep="")
outputfileEmbeded <- paste(csvInputFile1, "_combined_geom_line.pdf", sep="")

ggsave(file=outputfile, scale=2)
embed_fonts(outputfile, outfile=outputfileEmbeded)
file.remove(outputfile)
message <- paste("Info: Saved GeomLine Plot in ",outputfileEmbeded,"\n",sep="")
cat(message)

# Delete temporary created plot file
unlink("Rplots.pdf")
