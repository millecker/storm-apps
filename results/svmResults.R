#!/usr/bin/Rscript
library(scales) # install.packages("scales")
library(ggplot2) # install.packages("ggplot2")
library(extrafont) # extra font "LM Roman" # install.packages("extrafont") and font_import()
# font_import()
# fonts() or fonttable() #Show entire table

# global variables
fontType <- "LM Roman 10"
legendTitleFontSize <- 20
legendTextFontSize <- 22
legendFontColor <- "black"
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

# output
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
  xlab("C") +
  ylab("Gamma") +
  labs(colour = "Accuracy") +
  theme(text=element_text(family=fontType),
        legend.position="right",
        legend.title = element_text(face="bold", colour=legendFontColor, size=legendTitleFontSize),
        legend.text=element_text(colour=legendFontColor, size=legendTextFontSize),
        axis.title.x = element_text(face="bold", colour=axisTicksFontColor, size=axisTitleFontSize),
        axis.text.x  = element_text(angle=0, vjust=0.5, colour=axisTicksFontColor, size=axisTicksFontSize),
        axis.title.y = element_text(face="bold", colour=axisTicksFontColor, size=axisTitleFontSize),
        axis.text.y  = element_text(angle=0, vjust=0.5, colour=axisTicksFontColor, size=axisTicksFontSize))
#  guides(colour = guide_legend(title.hjust = 0.5))

outputfile <- paste(inputCSV, "_geom_point_not_embeded_fonts.pdf", sep="")
outputfileEmbeded <- paste(inputCSV, "_geom_point.pdf", sep="")
ggsave(file=outputfile, scale=2)
embed_fonts(outputfile, outfile=outputfileEmbeded)
file.remove(outputfile)
message <- paste("Info: Plot saved to ",outputfileEmbeded,"\n")
cat(message)

# Heat Map
# orderedsvmResults
subOrderedsvmResults <- orderedsvmResults[, c("C", "gamma", "accuracy", "accuracy_group")]

# convert C to factor
# subOrderedsvmResults$C_factor <- sapply(subOrderedsvmResults$C, as.factor)

# gamma C to factor
# subOrderedsvmResults$gamma_factor <- sapply(subOrderedsvmResults$gamma, as.factor)

# debug
# str(subOrderedsvmResults)
# subOrderedsvmResults

ggplot(subOrderedsvmResults, aes(factor(C),factor(gamma))) +
  geom_tile(aes(fill = accuracy_group)) +
#  scale_x_discrete(limits=(subOrderedsvmResults$C_factor)[order(subOrderedsvmResults$C)]) +
#  scale_y_discrete(limits=(subOrderedsvmResults$gamma_factor)[order(subOrderedsvmResults$gamma)]) +
  scale_fill_gradient(low = "white", high = "steelblue") +
  xlab("C") +
  ylab("Gamma") +
  labs(fill = "Accuracy") +
  theme(text=element_text(family=fontType),
        legend.position="right",
        legend.title = element_text(face="bold", colour=legendFontColor, size=legendTitleFontSize),
        legend.text=element_text(colour=legendFontColor, size=legendTextFontSize),
        axis.title.x = element_text(face="bold", colour=axisTicksFontColor, size=axisTitleFontSize),
        axis.text.x  = element_text(angle=0, vjust=0.5, colour=axisTicksFontColor, size=axisTicksFontSize),
        axis.title.y = element_text(face="bold", colour=axisTicksFontColor, size=axisTitleFontSize),
        axis.text.y  = element_text(angle=0, vjust=0.5, colour=axisTicksFontColor, size=axisTicksFontSize))

outputfile <- paste(inputCSV, "_geom_tile_not_embeded_fonts.pdf", sep="")
outputfileEmbeded <- paste(inputCSV, "_geom_tile.pdf", sep="")
ggsave(file=outputfile, scale=2)
embed_fonts(outputfile, outfile=outputfileEmbeded)
file.remove(outputfile)
message <- paste("Info: Plot saved to ",outputfileEmbeded,"\n")
cat(message)

# Delete temporary created plot file
unlink("Rplots.pdf")
