#!/usr/bin/env python3

"""
Convert the exported json data to a time-sorted list of events as XML

Jens Dede, ComNets University of Bremen

jd@comnets.uni-bremen.de
"""

import argparse
import sys
import gzip
import json
import datetime
from dateutil import parser as dtparser

import xml.etree.ElementTree as ET
import random

argparser = argparse.ArgumentParser(description="Convert the json data from the ResourceMonitor app to an event XML file.")
argparser.add_argument('infile', metavar='input', type=str, help="Input file name")
argparser.add_argument('outfile', metavar='output', type=str, help="output file name")
argparser.add_argument('-z', "--gzip", action="store_true", help="Input file is gzip compressed")

args = argparser.parse_args()

# How to open the file. Default: uncompressed json
opener = open
if args.gzip:
    print("Using gzip compressed json file")
    opener = gzip.open

allDatasets = []
configDict = {}
minTimestamp = None

print("Processing file", args.infile)
with opener(args.infile, "rb") as rf:
    data = json.load(rf)
    lastPercentages = {}
    for k in data.keys():
        if type(data[k]) in [list, tuple]:
            for eventLine in data[k]:
                attributes = {}
                attributes["data_type"] = k

                for eventAttribute in eventLine:
                    if eventAttribute.startswith("_"):
                        # Ignore _id etc.
                        continue
                    else:
                        if eventAttribute == "time":
                            dt = dtparser.parse(eventLine[eventAttribute])
                            attributes["timestamp_s"] = dt.timestamp()
                            attributes[eventAttribute] = eventLine[eventAttribute]
                            if minTimestamp == None:
                                minTimestamp = dt.timestamp()
                            else:
                                minTimestamp = min(minTimestamp, dt.timestamp())

                        if eventAttribute == "percentage":
                            if k not in lastPercentages:
                                lastPercentages[k] = eventLine[eventAttribute]

                            attributes[eventAttribute] = eventLine[eventAttribute]
                            attributes["delta_" + eventAttribute] = eventLine[eventAttribute] - lastPercentages[k]
                            lastPercentages[k] = eventLine[eventAttribute]

                        else:
                            attributes[eventAttribute] = eventLine[eventAttribute]

                allDatasets.append(attributes)
        else: # Not list our tuple
            configDict[k] = data[k]


configDict["timestamp_delta"] = minTimestamp

print("Got all datasets. Normalizing the times")

# Check structure, some fixes for XML
for i in range(len(allDatasets)):
    allDatasets[i]["timestamp_s"] = allDatasets[i]["timestamp_s"] - minTimestamp
    for d in allDatasets[i]:
        if type(allDatasets[i][d]) in (bool, float, int):
            allDatasets[i][d] = str(allDatasets[i][d])

print("Sorting the list")
orderedList = sorted(allDatasets, key=lambda k: k["timestamp_s"])

root = ET.Element("root")
doc = ET.SubElement(root, "events")

# The config
conf = ET.SubElement(root, "config")
for c in configDict:
    ET.SubElement(conf, c, {}).text = str(configDict[c])

print("Create XML document")
for l in orderedList:
    ET.SubElement(doc, "event", l).text = l["timestamp_s"]

print("Write XML to file", args.outfile)
tree = ET.ElementTree(root)
tree.write(args.outfile, xml_declaration=True, encoding='UTF-8')

