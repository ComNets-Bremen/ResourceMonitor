#!/usr/bin/env python
"""
A simple data analysis script for the ResourceMonitor 2.0

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

LINESTYLE = ":"
MARKER = None
LINEWIDTH = 0.8

trueMin = -0.1
trueMax = 1.1

argparser = argparse.ArgumentParser(description="Analze exported files from ResourceMonitor")
argparser.add_argument('files', metavar='filename', type=str, nargs="+", help="Filename to process")
argparser.add_argument('-v', "--verbose", action="store_true", help="Increase debug output")
argparser.add_argument('--show', action="store_true", help="Show results instead of plotting them into an output file.")
argparser.add_argument('--objects', action="store_true", help="Print json object structure.")
argparser.add_argument('--xmin', default=None, type=str, help="Minimum datetime for x axis and analysis")
argparser.add_argument('--xmax', default=None, type=str, help="Minimum datetime for x axis and analysis")
argparser.add_argument('-z', "--gzip", action="store_true", help="Input file is gzip compressed")
argparser.add_argument('-d', "--date", action="store_true", help="Show dates in x axis")


#argparser.add_argument('--label', metavar="Label to use", default=None, type=str, help="Add label to the output")
#argparser.add_argument('--timelimit', metavar='timelimit', type=float, default=None, help="Stop reading values after n seconds")

args = argparser.parse_args()

yAxisMajorLocator = MultipleLocator(1)

fig, (batteryAxis, chargingAxis, screenAxis, wifiAxis, mobiledataAxis) = \
        plt.subplots(nrows=5, figsize=(6,5), sharex=True)

xMin = xMax = None

# How to open the file. Default: uncompressed json
opener = open
if args.gzip:
    print "Using gzip compressed json file"
    opener = gzip.open

resourceDataHandlers = {}

# Load data into resourceDataHandlers
for f in args.files:
    with opener(f, "rb") as rf:

        data = json.load(rf)
        resourceDataHandlers[f] = resourceHelpers.ResourceDataHandler(data, args.verbose)

        graphMin, graphMax = resourceDataHandlers[f].getMinMax()

        if xMin == None:
            xMin = graphMin
        if xMax == None:
            xMax = graphMax

        xMin = max(xMin, graphMin)
        xMax = min(xMax, graphMax)

# Check if mMin / xMax were set via CLI

if args.xmin != None:
    xMin = dparser.parse(args.xmin)
    print "xMin set to", xMin, "via CLI"

if args.xmax != None:
    xMax = dparser.parse(args.xmax)
    print "xMax set to", xMax, "via CLI"

print "Using xMin", xMin, "and xMax", xMax

# Print structure of json object?
if args.objects:
    for f in resourceDataHandlers:
        for af in resourceDataHandlers[f].getArrayFields():
            print "Field:", af
            for d in resourceDataHandlers[f].getFieldTypes(af):
                print "*", d

# Create graph
for f in resourceDataHandlers:
    ## Battery level
    x, y = resourceDataHandlers[f].getDatasets("BatteryStatus", "percentage")
    batteryAxis.plot(x, y, linestyle=LINESTYLE, marker=MARKER, linewidth=LINEWIDTH)

    # Battery charging
    x, y = resourceDataHandlers[f].getSmoothedDatasets("BatteryStatus", "is_charging")
    chargingAxis.plot(x, y, linestyle=LINESTYLE, marker=MARKER, linewidth=LINEWIDTH)

    # Screen status
    x, y = resourceDataHandlers[f].getSmoothedDatasets("ScreenStatus", "screen_status")
    screenAxis.plot(x, y, linestyle=LINESTYLE, marker=MARKER, linewidth=LINEWIDTH)

    # WiFi status
    x, y = resourceDataHandlers[f].getSmoothedDatasets("WiFiStatus", "wifi_status")
    wifiData = zip(x,y)
    convertedData = []

    for line in wifiData:
        # authenticating, connected, connecting, obtaining ip, scanning
        if line[1] == 1 or \
                line[1] == 4 or \
                line[1] == 5 or \
                line[1] == 9 or \
                line[1] == 10:
            convertedData.append([line[0], 1])
        else:
            convertedData.append([line[0], 0])

    x, y = zip(*convertedData)
    wifiAxis.plot(x, y, linestyle=LINESTYLE, marker=MARKER, linewidth=LINEWIDTH)

    # Cellular network status
    x, y = resourceDataHandlers[f].getSmoothedDatasets("CellularStatus", "cellular_type")
    cellData = zip(x, y)

    convertedData = []

    for line in cellData:
        if line[1] == "LTE" or\
                line[1] == "EDGE" or\
                line[1] == "HSPA+" or\
                line[1] == "GPRS" or \
                line[1] == "HSUPA":
                    convertedData.append([line[0], 1])
                    #print line[1]

        elif line[1] == "NONE" or line[1] == "UNKNOWN":
            convertedData.append([line[0], 0])
        else:
            pass

    x, y = zip(*convertedData)
    mobiledataAxis.plot(x, y, linestyle=LINESTYLE, marker=MARKER, linewidth=LINEWIDTH)

# Plotting
batteryAxis.yaxis.set_major_formatter(FuncFormatter(graphHelper.percentage_formatter))
batteryAxis.set_title("Battery Level")

chargingAxis.yaxis.set_major_locator(yAxisMajorLocator)
chargingAxis.yaxis.set_minor_locator(NullLocator())
chargingAxis.yaxis.set_major_formatter(FuncFormatter(graphHelper.trueFalse_formatter))
chargingAxis.set_title("Device Charging")
chargingAxis.set_ylim([trueMin, trueMax])

screenAxis.yaxis.set_major_locator(yAxisMajorLocator)
screenAxis.yaxis.set_minor_locator(NullLocator())
screenAxis.yaxis.set_major_formatter(FuncFormatter(graphHelper.onOff_formatter))
screenAxis.set_title("Screen Status")
screenAxis.set_ylim([trueMin, trueMax])

wifiAxis.yaxis.set_major_locator(yAxisMajorLocator)
wifiAxis.yaxis.set_minor_locator(NullLocator())
wifiAxis.yaxis.set_major_formatter(FuncFormatter(graphHelper.trueFalse_formatter))
wifiAxis.set_title("WiFi Connected")
wifiAxis.set_ylim([trueMin, trueMax])

mobiledataAxis.yaxis.set_major_locator(yAxisMajorLocator)
mobiledataAxis.yaxis.set_minor_locator(NullLocator())
mobiledataAxis.yaxis.set_major_formatter(FuncFormatter(graphHelper.trueFalse_formatter))
mobiledataAxis.set_title("Mobile Data Connected")
mobiledataAxis.set_ylim([trueMin, trueMax])

if args.date:
    mobiledataAxis.xaxis.set_major_formatter(mdates.DateFormatter('%d.%m. %H:%M'))
else:
    mobiledataAxis.xaxis.set_major_formatter(mdates.DateFormatter('%H:%M'))

mobiledataAxis.set_xlim([xMin, xMax])

fig.autofmt_xdate()
fig.tight_layout()

if args.show:
    plt.show()
else:
    plt.savefig("example-resources.pdf")

print "Statistics"
for key in resourceDataHandlers:
    print "Processing file", key
    print "BatteryStatus/is_charging", resourceDataHandlers[key].getStatePercentages("BatteryStatus", "is_charging", xMin, xMax)
    print "ScreenStatus", "screen_status", resourceDataHandlers[key].getStatePercentages("ScreenStatus", "screen_status", xMin, xMax)
    print "WiFiStatus/wifi_status", resourceDataHandlers[key].getStatePercentages("WiFiStatus", "wifi_status", xMin, xMax)
    print "CellularStatus/cellular_type", resourceDataHandlers[key].getStatePercentages("CellularStatus", "cellular_type", xMin, xMax)
    print "Max Battery gap", resourceDataHandlers[key].getMaxBatteryGap()

    # WiFi Codes:
    # AUTHENTICATING: 1
    # BLOCKED: 2
    # CAPTIVE_PORTAL_CHECK: 3
    # CONNECTED: 4
    # CONNECTING: 5
    # DISCONNECTED: 6
    # FAILED: 7
    # IDLE: 8
    # OBTAINING_IPADDR: 9
    # SCANNING: 10
    # SUSPENDED: 11
    # VERIFYING_POOR_LINK: 12

