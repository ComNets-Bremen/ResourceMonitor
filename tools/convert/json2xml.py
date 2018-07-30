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

import numpy as np

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
batteryStats = []
minTimestamp = None

print("Processing file", args.infile)
with opener(args.infile, "rb") as rf:
    data = json.load(rf)
    lastPercentages = {}
    sequentialNumber = 0
    for k in data.keys():
        if type(data[k]) in [list, tuple]:
            for eventLine in data[k]:
                attributes = {}
                attributes["data_type"] = k
                attributes["sequential_number"] = sequentialNumber
                sequentialNumber += 1

                if "time" in eventLine:
                    dt = dtparser.parse(eventLine["time"])
                    attributes["timestamp_s"] = dt.timestamp()
                    attributes["time"] = eventLine["time"]
                    if minTimestamp == None:
                        minTimestamp = dt.timestamp()
                    else:
                        minTimestamp = min(minTimestamp, dt.timestamp())

                if k == "BatteryStatus":
                    batteryStats.append({
                            "timestamp_s": attributes["timestamp_s"],
                            "percentage" : eventLine["percentage"],
                            "is_charging": eventLine["is_charging"],
                        })


                for eventAttribute in eventLine:
                    if eventAttribute.startswith("_") or\
                            eventAttribute == "time":
                        # Ignore _id etc.
                        # Ignore time as it was handled before
                        continue
                    else:
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

print("Got all datasets")

print("Calculating battery statistics")

# Sorting function is stable: It does not change the order of equal values
orderedBatteryStats = sorted(batteryStats, key=lambda k: k["timestamp_s"])
batCharge = []
batDischarge = []
for n, v in enumerate(orderedBatteryStats):
    if n > 1:
        percentageDelta = v["percentage"] - orderedBatteryStats[n-1]["percentage"]
        timeDelta = v["timestamp_s"] - orderedBatteryStats[n-1]["timestamp_s"]

        if timeDelta == 0:
            continue

        deltaPerHour = percentageDelta * 60.0 * 60.0 / timeDelta

        if orderedBatteryStats[n-1]["is_charging"]:
            batCharge.append(deltaPerHour)
        else:
            batDischarge.append(deltaPerHour)

print("Normalizing datasets")

# Check structure, some fixes for XML
for i in range(len(allDatasets)):
    allDatasets[i]["timestamp_s"] = allDatasets[i]["timestamp_s"] - minTimestamp

print("Sorting the list")
orderedList = sorted(allDatasets, key=lambda k: k["timestamp_s"])

for i in range(len(allDatasets)):
    for d in allDatasets[i]:
        if type(allDatasets[i][d]) in (bool, float, int):
            allDatasets[i][d] = str(allDatasets[i][d])

root = ET.Element("root")
doc = ET.SubElement(root, "events")

# The config
conf = ET.SubElement(root, "config")
for c in configDict:
    ET.SubElement(conf, c, {}).text = str(configDict[c])

# The parameters
par = ET.SubElement(root, "parameters")
if len(batCharge):
    ET.SubElement(par, "battery_charge_min", {}).text = str(np.min(batCharge))
    ET.SubElement(par, "battery_charge_max", {}).text = str(np.max(batCharge))
    ET.SubElement(par, "battery_charge_median", {}).text = str(np.median(batCharge))
    ET.SubElement(par, "battery_charge_mean", {}).text = str(np.mean(batCharge))

if len(batDischarge):
    ET.SubElement(par, "battery_discharge_min", {}).text = str(np.min(batDischarge))
    ET.SubElement(par, "battery_discharge_max", {}).text = str(np.max(batDischarge))
    ET.SubElement(par, "battery_discharge_median", {}).text = str(np.median(batDischarge))
    ET.SubElement(par, "battery_discharge_mean", {}).text = str(np.mean(batDischarge))

print("Create XML document")
for l in orderedList:
    ET.SubElement(doc, "event", l).text = l["timestamp_s"]

print("Write XML to file", args.outfile)
tree = ET.ElementTree(root)
tree.write(args.outfile, xml_declaration=True, encoding='UTF-8')

