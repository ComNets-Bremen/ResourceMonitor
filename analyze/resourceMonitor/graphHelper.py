"""
General helper functions and settings

Jens Dede, ComNets, University of Bremen
jd@comnets.uni-bremen.de
"""

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
    return str(100*int(x))+"\%"

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

