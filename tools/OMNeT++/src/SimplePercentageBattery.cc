//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
// 
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
// 
// You should have received a copy of the GNU Lesser General Public License
// along with this program.  If not, see http://www.gnu.org/licenses/.
// 

#include "SimplePercentageBattery.h"

namespace eventsimulator {

SimplePercentageBattery::SimplePercentageBattery() {
    batteryPercentage = 50.0d;
}

SimplePercentageBattery::~SimplePercentageBattery() {
    // TODO Auto-generated destructor stub
}

double SimplePercentageBattery::getBatteryPercentage(){
    return batteryPercentage;
}

void SimplePercentageBattery::setBatteryPercentage(double percentage){
    batteryPercentage = percentage;
}

bool SimplePercentageBattery::checkBatteryPercentageValid(){
    return (batteryPercentage < 100.0d && batteryPercentage > 0.0d);
}

void SimplePercentageBattery::incrementalBatteryChange(double percentage){
    batteryPercentage += percentage;
}

bool SimplePercentageBattery::isInitialized(){
    return initialized;
}

void SimplePercentageBattery::initialize(){
    // TODO: Init?
    initialized = true;
}

} /* namespace eventsimulator */
