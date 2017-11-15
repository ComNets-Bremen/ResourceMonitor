"""
A class to handle the data from the ResourceMonitor 2.0

Jens Dede, ComNets, University of Bremen
jd@comnets.uni-bremen.de
"""

from dateutil import parser,tz
from datetime import datetime, timedelta
from graphHelper import toHumDate

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

    # Get some hour statistics: Percentage of function x is True (display,
    # charging etc.)
    # Weekdays can be specified as a list of weekdays (0-6)
    def getHourStatistics(self, field, fieldType, weekdays=range(7)):
        datas = zip(*self.getDatasets(field, fieldType))
        dataiter = iter(datas)

        print "Weekdays: ", ", ".join([toHumDate(w) for w in weekdays])

        newData = {}
        for i in range(0,24):
            newData[str(i)+"-"+str((i+1)%24)] = []

        beginTimestamp = self.timeMin.replace(minute=0, second=0, microsecond=0) + timedelta(hours=1)
        endTimestamp = self.timeMax.replace(minute=0, second=0, microsecond=0)

        print endTimestamp-beginTimestamp

        if endTimestamp - beginTimestamp < timedelta(hours=1):
            # Does not make sense to calculate these stats for less than one
            # hour
            return None

        # We store on and off times to proof that nothing got lost.
        # The sum of both should be equal to 3600 (seconds in an hour)
        onTimedeltas = []
        offTimedeltas = []

        currentTimestamp = beginTimestamp

        # Find first valid value from the datasets, i.e. larger than the start
        # value
        currentDataset = next(dataiter)
        lastDataset = currentDataset
        while(currentDataset[0] < beginTimestamp):
            lastDataset = currentDataset
            currentDataset = next(dataiter)

        # Iterate over the timestamp spans
        while(currentTimestamp < endTimestamp):
            periodOnValues  = []
            periodOffValues = []

            # Iterate over the data points
            while(currentDataset[0] > currentTimestamp and currentDataset[0] < currentTimestamp + timedelta(hours=1)):
                if len(periodOnValues) == 0 and  len(periodOffValues) == 0:
                    # First datapoint in this period. Set begin of period as
                    # starting time
                    if lastDataset[1] == True:
                        periodOnValues.append(currentDataset[0] - currentTimestamp)
                    else:
                        periodOffValues.append(currentDataset[0] - currentTimestamp)
                else:
                    # Got another value within this period
                    if lastDataset[1] == True:
                        periodOnValues.append(currentDataset[0] - lastDataset[0])
                    else:
                        periodOffValues.append(currentDataset[0] - lastDataset[0])

                lastDataset = currentDataset
                currentDataset = next(dataiter)

            # Handled all datapoints in the current period
            if len(periodOnValues) == 0 and len(periodOffValues) == 0:
                # period is empty. Set it to the values of the last known
                # dataset (i.e. always on / always off)
                if lastDataset[1] == True:
                    periodOnValues.append(timedelta(hours=1))
                else:
                    periodOffValues.append(timedelta(hours=1))
            else:
                # Not empty: We have to finish the last dataset to ensure the
                # time over the last period is complete (i.e. no time missing)
                if lastDataset[1] == True:
                    periodOnValues.append(currentTimestamp+timedelta(hours=1) - lastDataset[0])
                else:
                    periodOffValues.append(currentTimestamp+timedelta(hours=1) - lastDataset[0])

            onTimes = 0
            offTimes = 0

            # Sum up all data in the timestamp span
            for t in periodOnValues:
                onTimes += t.total_seconds()
            for t in periodOffValues:
                offTimes += t.total_seconds()

            #print "Interval", str(currentTimestamp.hour)+"-"+str((currentTimestamp + timedelta(hours=1)).hour)
            #print onTimes, offTimes, onTimes+offTimes, timedelta(hours=1).total_seconds()

            # Ignore values not in the weekdays array
            if currentTimestamp.weekday() in weekdays:
                newData[str(currentTimestamp.hour)+"-"+str((currentTimestamp + timedelta(hours=1)).hour)].append(onTimes/(onTimes+offTimes))

            currentTimestamp += timedelta(hours=1)

        return newData

    # Get minimum and maximum timestamp from all datasets
    def getMinMax(self):
        return self.timeMin, self.timeMax

    # Returns the maximum gap between two battery percentage values in seconds
    def getMaxBatteryGap(self):
        x, y = self.getDatasets("BatteryStatus", "percentage")
        delta = 0
        lastx = x[0]
        for v in x:
            delta = max(delta, (v - lastx).total_seconds())
            lastx = v

        return delta

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

