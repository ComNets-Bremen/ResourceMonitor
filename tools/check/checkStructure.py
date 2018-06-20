#!/usr/bin/env python

"""
Perform a basic check if the ids are continuously increasing in the given json object
"""

import json
import argparse
import gzip

argparser = argparse.ArgumentParser(description="Analyze exported files from ResourceMonitor")
argparser.add_argument('file', metavar='filename', type=str, help="Filename to process")
argparser.add_argument('-z', "--gzip", action="store_true", help="Input file is gzip compressed")

args = argparser.parse_args()

opener = open
if args.gzip:
    print "Using gzip compressed json file"
    opener = gzip.open

with opener(args.file) as f:
    data = json.load(f)

    for item in data:
        if hasattr(data[item], "__iter__"):
            ids = []
            for dataset in data[item]:
                ids.append(dataset["_id"])
            checkset = range(min(ids), max(ids)+1)
            if len(ids) != len(checkset):
                print "Data mismatch with \"" + item + "\":"
                print "Missing:", list(set(checkset) - set(ids))

print "Check done"
