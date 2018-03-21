"""
General helper functions and settings

Jens Dede, ComNets, University of Bremen
jd@comnets.uni-bremen.de
"""

import numpy as np

# Return the value or null
def valueOrNone(val):
    if val == "null":
        return None
    try:
        return int(val)
    except ValueError:
        try:
            return float(val)
        except ValueError:
            return val

# Format the current value as percentage
def percentage_formatter(x, pos):
    return str(100.0*float(x))+"\%"

# Format the current value as hour
def hour_formatter(x, pos):
    return str(int(x/60.0/60.0))+"h"

# Format the current value as on (==1) or off (otherwise)
def onOff_formatter(x, pos):
    if x:
        return "On"
    else:
        return "Off"

# Format the current value as true or false
def trueFalse_formatter(x, pos):
    if x:
        return "True"
    else:
        return "False"

# Formatter for the y-axix (minutes per hour)
def minutePerHourFormatter(x, pos):
    return str(int(np.round(x*60.0)))#+"min/hour"

# Convert the weekday values (starting with 0) to human readable values
def toHumDate(weekday):
    weekdays = ["MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"]
    return weekdays[weekday]

# Convert seconds to hours
def hour_formatter(x, pos):
    return int(x/60/60)
