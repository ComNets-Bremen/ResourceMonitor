"""
A class to handle the data from the ResourceMonitor 2.0

Jens Dede, ComNets, University of Bremen
jd@comnets.uni-bremen.de
"""

from dateutil import parser,tz
from datetime import datetime, timedelta

class ResourceDataHandler():
    json = None
    timezone = None
    export_time = None
    verbose = False
    timeMin = None
    timeMax = None

    def __init__(self, json, verbose=False):
        self.json = json
        self.timezone = None
        self.verbose = verbose
        if "TIMEZONE" in self.json:
            self.timezone = tz.gettz(json["TIMEZONE"])
            if self.verbose:
                print "Using timezone", self.timezone

        if "EXPORT_TIMESTAMP" in self.json and self.verbose:
            print "Timestamp", datetime.fromtimestamp(int(self.json["EXPORT_TIMESTAMP"]) / 1000.0)

        # Calc min and max time values for later calculation and plotting of
        # multiple graphs
        for key in self.getArrayFields():
            dt = self.getTimes(key)
            if self.verbose:
                print "Min and Max dt",key,  min(dt), max(dt)
            if self.timeMin == None:
                self.timeMin = min(dt)
            if self.timeMax == None:
                self.timeMax = max(dt)

            self.timeMax = max(self.timeMax, max(dt))
            self.timeMin = min(self.timeMin, min(dt))


    # Return the keys of the current json object
    def getKeys(self):
        return self.json.keys()

    # Convert the date to a normalized timezone
    def convertDate(self, datestring):
        if self.timezone:
            dt = parser.parse(datestring)
            dt = dt.replace(tzinfo=tz.gettz("UTC"))
            normalizedTime = dt.astimezone(self.timezone)
            return normalizedTime.replace(tzinfo=None)
        else:
            return parser.parse(datestring)

    # Return a all array-like fields from the json object
    def getArrayFields(self, empty=False):
        fields = []
        for key in self.json:
            if type(self.json[key]) == list and\
            ( empty or (\
                        not empty and len(self.json[key]) > 0\
                    )):
                fields.append(key)
        return fields

    # Get a special field from the json object
    def getField(self, field):
        return self.json[field]

    # Get field keys from a special field
    def getFieldTypes(self, field):
        data = self.getField(field)
        return data[0].keys()

    # Get the Time values for a certain field
    def getTimes(self, field):
        dt = []
        datas = self.getField(field)

        for data in datas:
            dt.append(self.convertDate(data["time"]))
        return dt

    # Get complete datasets (value and time) for a certain field
    def getDatasets(self, field, fieldType):
        datas = self.getField(field)
        x = []
        y = []
        for data in datas:
            x.append(self.convertDate(data["time"]))
            y.append(data[fieldType])

        return x, y

    # Return smoothed datasets, i.e., the lines in the graph are perpendicular
    # to the x-axis
    def getSmoothedDatasets(self, field, fieldType):
        datas = zip(*self.getDatasets(field, fieldType))
        lastDataset = None
        lastTimestamp = None
        newData = []

        for data in datas:
            if lastDataset == None:
                lastDataset = data[1]
                lastTimestamp = data[0]
            if (data[0] - lastTimestamp) > timedelta(0, 30):
                newData.append([data[0] - timedelta(0, 1), lastDataset])
            newData.append([data[0], data[1]])

            lastDataset = data[1]
            lastTimestamp = data[0]

        if self.timeMax > lastTimestamp:
            newData.append([self.timeMax, lastDataset])

        x = []
        y = []
        for r in newData:
            x.append(r[0])
            y.append(r[1])

        return x, y

    # Get minimum and maximum timestamp from all datasets
    def getMinMax(self):
        return self.timeMin, self.timeMax

    # Get the percentages the device was in a certain state
    def getStatePercentages(self, field, fieldType, minDate, maxDate):
        completeTimespan = maxDate - minDate
        stateStatistics = {}
        x, y = self.getSmoothedDatasets(field, fieldType)
        for value in set(y):
            stateStatistics[str(value)] = 0.0

        datasets = zip(x, y)
        lastDatetime = None
        lastDataset = None

        for dataset in datasets:
            if lastDataset == None:
                lastDataset = dataset[1]

            if dataset[0] > minDate and dataset[0] < maxDate:
                if lastDatetime == None:
                    lastDatetime = minDate
                if lastDataset != dataset[1]:
                    timespan = dataset[0] - lastDatetime
                    stateStatistics[str(lastDataset)] += timespan.total_seconds()
                    lastDatetime = dataset[0]
                    lastDataset = dataset[1]

            if dataset[0] >= maxDate:
                timespan = maxDate - lastDatetime
                stateStatistics[str(lastDataset)] += timespan.total_seconds()
                ret = {}
                for key in stateStatistics:
                    ret[key] = stateStatistics[key] / completeTimespan.total_seconds()
                return ret

