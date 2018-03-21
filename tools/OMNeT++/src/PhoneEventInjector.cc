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

#include "PhoneEventInjector.h"
#include "ScreenEventMessage_m.h"
#include "EventTypeDefinitions.h"

namespace eventsimulator {

Define_Module(PhoneEventInjector);

void PhoneEventInjector::initialize()
{
    cXMLElement *xmlEvents = par("eventFilename").xmlValue();

    if (xmlEvents == nullptr)
        throw cRuntimeError("No event xml file");

    cXMLElementList allEvents = xmlEvents->getElementsByTagName("event");

    EV_INFO << allEvents.size() << " events in file"<< endl;

    for (auto const& value:allEvents){
        EV_INFO << "Type:" << value->getAttribute("type") << " Status:" << value->getAttribute("status") << " Value:" <<
        value->getNodeValue() << endl;
        simtime_t eventTime = SimTime(strtod(value->getNodeValue(), NULL)*1000, SimTimeUnit::SIMTIME_MS);

        EV_INFO << "Next event: " << eventTime << endl;
        ScreenEventMessage *msg = new ScreenEventMessage("ScreenEvent");

        msg->setInjectionTime(eventTime);
        msg->setPayloadType(EVENT_TYPE_SCREEN);


        std::string statusString = value->getAttribute("status");

        EV_INFO << "STATUS: " << statusString << endl;

        if (statusString == "on") msg->setScreenOn(true);
        else if (statusString == "off") msg->setScreenOn(false);
        else throw cRuntimeError("Not an On Off value");

        scheduleAt(eventTime, msg);
    }
}

void PhoneEventInjector::handleMessage(cMessage *msg)
{
    EV_INFO << "MSG to out" << endl;
    for (int i = 0; i<gateSize("out"); i++ )
        send(msg, "out", i);
}

}; // namespace
