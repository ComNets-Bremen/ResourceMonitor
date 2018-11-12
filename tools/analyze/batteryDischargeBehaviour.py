#!/usr/bin/env python
"""
Analyze the battery discharge behaviour and create a graph out of it

Jens Dede, ComNets University of Bremen

jd@comnets.uni-bremen.de

"""

import gzip
import json
from datetime import datetime
import argparse
import matplotlib
matplotlib.rcParams['text.usetex'] = True
matplotlib.rcParams['text.latex.unicode'] = True
import matplotlib.pyplot as plt
import numpy as np
import matplotlib.dates as mdates

from dateutil import parser as dparser

from matplotlib.ticker import MultipleLocator, FormatStrFormatter, NullLocator, FuncFormatter

from resourceMonitor import graphHelper
from resourceMonitor import resourceHelpers

argparser = argparse.ArgumentParser(description="Analyze exported files from ResourceMonitor")
argparser.add_argument('file', metavar='filename', type=str, help="Filename to process")
argparser.add_argument('-v', "--verbose", action="store_true", help="Increase debug output")
argparser.add_argument('--show', action="store_true", help="Show results instead of plotting them into an output file.")
argparser.add_argument('--xmin', default=None, type=str, help="Minimum datetime for x axis and analysis")
argparser.add_argument('--xmax', default=None, type=str, help="Minimum datetime for x axis and analysis")
argparser.add_argument('-z', "--gzip", action="store_true", help="Input file is gzip compressed")

args = argparser.parse_args()

# How to open the file. Default: uncompressed json
opener = open
if args.gzip:
    print "Using gzip compressed json file"
    opener = gzip.open

resourceDataHandlers = None
xMin = None
xMax = None

# Load data into resourceDataHandlers
with opener(args.file, "rb") as rf:

    data = json.load(rf)
    resourceDataHandler = resourceHelpers.ResourceDataHandler(data, args.verbose)

    graphMin, graphMax = resourceDataHandler.getMinMax()

    if xMin == None:
        xMin = graphMin
    if xMax == None:
        xMax = graphMax

    xMin = max(xMin, graphMin)
    xMax = min(xMax, graphMax)

# Check if mMin / xMax were set via CLI

if args.xmin != None:
    xMin = dparser.parse(args.xmin).replace(tzinfo=None)
    print "xMin set to", xMin, "via CLI"

if args.xmax != None:
    xMax = dparser.parse(args.xmax).replace(tzinfo=None)
    print "xMax set to", xMax, "via CLI"

print "Using xMin", xMin, "and xMax", xMax

## Get and cleanup battery values

batteryValues = resourceDataHandler.getField("BatteryStatus");
sanitizedBatteryValues = []

for bv in batteryValues:
    time = dparser.parse(bv["time"])
    if time.year == 2015:
        # There seem to be some problems with the internal clock of some phones
        continue

    value = bv["percentage"]
    is_charging = bv["is_charging"]
    sanitizedBatteryValues.append({
        "time" : time,
        "value" : value,
        "is_charging": is_charging
        })

sortedValues = sorted(sanitizedBatteryValues, key=lambda k: k['time'])

# In this point, the values are sorted and cleaned up. Next step: split by
# discharging periods

dischargingRanges = {}
discharge_range_number = 0;

# go through the sorted values and add the discharging values to a new list.
# Create a new entry whenever the device state changes from charging to
# discharging
for n, v in enumerate(sortedValues):
    if v["is_charging"]:
        continue
    if n == 0:
        continue
    if (not v["is_charging"] and sortedValues[n-1]["is_charging"]) or \
        ((v["value"] - 0.1) > sortedValues[n-1]["value"]):
        discharge_range_number += 1
        dischargingRanges["run_"+str(discharge_range_number)] = []

    # Skip incomplete runs, i.e. not starting with charging -> not charging
    if "run_"+str(discharge_range_number) in dischargingRanges:
        dischargingRanges["run_"+str(discharge_range_number)].append(v)

descriptions = {}   # Needed later on for plotting / labelling
xy_axis = {}        # Values we can use for plotting

# Fill the descriptions and normalize the timestamps so we can plot them nicely
for k in dischargingRanges:

    descriptions[k] = {
            "minDate" : dischargingRanges[k][0]["time"],
            "maxDate" : dischargingRanges[k][-1]["time"],
            "len"     : len(dischargingRanges[k])
            }

    # Skip too short values
    if len(dischargingRanges[k]) < 5:
        continue

    xy_axis[k] = {}

    for pair in dischargingRanges[k]:
        key = (pair["time"] - dischargingRanges[k][0]["time"]).total_seconds()
        if key in xy_axis[k]:
            print "Double Value", key, xy_axis[k][key], pair["value"]
            #raise ValueError('Double date!!!')
            continue
        xy_axis[k][key] = pair["value"]


# Start the plotting
fig = plt.figure(figsize=(12, 6)) # size of resulting figure in inches
ax = fig.add_subplot(111)

# Use the descriptions to sort the keys to get the correct order
keys = xy_axis.keys()
keys = sorted(keys, key=lambda k: descriptions[k]['minDate'])

# Plotting
for k in keys:
    # Get the data (x = key, y = value) and sort it by the key.
    lists = sorted(xy_axis[k].items())
    x, y = zip(*lists)
    d = descriptions[k]
    print k, d
    l = d["minDate"].strftime("%b-%d") + " - " + d["maxDate"].strftime("%b-%d") + " (" + str((d["maxDate"] - d["minDate"]).days) + " days)"
    ax.plot(x, y, label=l)

# Formatting etc.
ax.yaxis.set_major_formatter(FuncFormatter(graphHelper.percentage_formatter))
ax.yaxis.set_major_locator(MultipleLocator(0.25))
ax.yaxis.set_minor_locator(MultipleLocator(0.1))

ax.xaxis.set_major_formatter(FuncFormatter(graphHelper.day_formatter))
ax.xaxis.set_major_locator(MultipleLocator(60*60*24))
ax.xaxis.set_minor_locator(MultipleLocator(60*60*1))

plt.ylabel("Battery Level")
plt.xlabel("Time")

ax.set_title("Battery Discharging Behaviour")

fig.tight_layout()

if len(keys) < 10:
    # Do not show overfilled legends
    plt.legend(loc="best")

if args.show:
    plt.show()
else:
    plt.savefig(args.file + ".pdf")

