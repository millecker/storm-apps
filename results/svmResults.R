#!/usr/bin/Rscript
library(scales) # install.packages("scales")
library(ggplot2) # install.packages("ggplot2")
library(extrafont) # extra font "LM Roman" # install.packages("extrafont") and font_import()
# font_import()
# fonts() or fonttable() #Show entire table

# global variables
fontType <- "LM Roman 10"
legendFontSize <- 16
axisTitleFontSize <- 26
axisTitleFontColor <- "black"
axisTicksFontSize <- 22
axisTicksFontColor <- "black"

# check parameters
args <- commandArgs(trailingOnly = TRUE)

# argument csv file
if (is.na(args[1])) {
  stop("Input CSV file is missing!")
} else {
  inputCSV <- args[1]
}

# argument roundPrecision
if (is.na(args[2])) {
  roundPrecision <- 2
} else {
  roundPrecision <- as.numeric(args[2]);
}

# read CSV file
svmResults <- read.csv(inputCSV, sep=";")

# order by accuracy
orderedsvmResults <- svmResults[order(-svmResults$accuracy),] 
# convert msec to seconds
orderedsvmResults <- transform(orderedsvmResults, time_sec = orderedsvmResults$time_ms / 1000)
# round accuracy to build groups
orderedsvmResults$accuracy_group <- round(orderedsvmResults$accuracy, digits = roundPrecision)
orderedsvmResults

ggplot(orderedsvmResults, aes(x=C, y=gamma, colour=accuracy_group)) + 
  geom_point(size=16) +
 # geom_line(aes(group=accuracy_group),size=3) +
  scale_x_continuous(trans = log2_trans(),
                     breaks = trans_breaks("log2", function(x) 2^x),
                     labels = trans_format("log2", math_format(2^.x))) +
  scale_y_continuous(trans = log2_trans(),
                     breaks = trans_breaks("log2", function(x) 2^x),
                     labels = trans_format("log2", math_format(2^.x))) +
  theme(text=element_text(family=fontType),
        legend.text=element_text(size=legendFontSize),
        axis.title.x = element_text(face="bold", colour=axisTicksFontColor, size=axisTitleFontSize),
        axis.text.x  = element_text(angle=0, vjust=0.5, colour=axisTicksFontColor, size=axisTicksFontSize),
        axis.title.y = element_text(face="bold", colour=axisTicksFontColor, size=axisTitleFontSize),
        axis.text.y  = element_text(angle=0, vjust=0.5, colour=axisTicksFontColor, size=axisTicksFontSize))

outputfile <- paste(inputCSV, "_not_embeded_fonts.pdf", sep="")
outputfileEmbeded <- paste(inputCSV, "_.pdf", sep="")
ggsave(file=outputfile, scale=2)
embed_fonts(outputfile, outfile=outputfileEmbeded)
file.remove(outputfile)
message <- paste("Info: Plot saved to ",outputfileEmbeded,"\n")
cat(message)

# Heat Map
# orderedsvmResults
subOrderedsvmResults <- orderedsvmResults[, c("C", "gamma", "accuracy", "accuracy_group")]
# subOrderedsvmResults
# str(subOrderedsvmResults)
subOrderedsvmResults$C <- sapply(subOrderedsvmResults$C, as.factor)
subOrderedsvmResults$gamma <- sapply(subOrderedsvmResults$gamma, as.factor)
# str(subOrderedsvmResults)
# subOrderedsvmResults

ggplot(subOrderedsvmResults, aes(C,gamma)) + 
  geom_tile(aes(fill = accuracy_group), colour =   "white") +
  scale_fill_gradient(low = "white", high = "steelblue") +
  theme(text=element_text(family=fontType),
        legend.text=element_text(size=legendFontSize),
        axis.title.x = element_text(face="bold", colour=axisTicksFontColor, size=axisTitleFontSize),
        axis.text.x  = element_text(angle=0, vjust=0.5, colour=axisTicksFontColor, size=axisTicksFontSize),
        axis.title.y = element_text(face="bold", colour=axisTicksFontColor, size=axisTitleFontSize),
        axis.text.y  = element_text(angle=0, vjust=0.5, colour=axisTicksFontColor, size=axisTicksFontSize))

outputfile <- paste(inputCSV, "_heatmap_not_embeded_fonts.pdf", sep="")
outputfileEmbeded <- paste(inputCSV, "_heatmap.pdf", sep="")
ggsave(file=outputfile, scale=2)
embed_fonts(outputfile, outfile=outputfileEmbeded)
file.remove(outputfile)
message <- paste("Info: Plot saved to ",outputfileEmbeded,"\n")
cat(message)

# Delete temporary created plot file
unlink("Rplots.pdf")
