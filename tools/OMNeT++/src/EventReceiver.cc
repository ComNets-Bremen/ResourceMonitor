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

#include "EventReceiver.h"
#include "BaseEventMessage_m.h"
#include "EventTypeDefinitions.h"

namespace eventsimulator {

Define_Module(EventReceiver);

void EventReceiver::initialize()
{
    EV_INFO << "Started" << endl;
}

void EventReceiver::handleMessage(cMessage *msg)
{

    BaseEventMessage *message = check_and_cast<BaseEventMessage *>(msg);
    EV_INFO << "Message type: " << message->getPayloadType() << endl;

    switch (message->getPayloadType())
    {
        case EVENT_TYPE_SCREEN:
            EV_INFO << "EVENT_TYPE_SCREEN" << endl;
            handleScreenEvent(check_and_cast<ScreenEventMessage *>(msg));
            break;
        default:
            EV_INFO << "Unknown Event" << endl;
    }

    delete msg;
}

void EventReceiver::handleScreenEvent(ScreenEventMessage *msg){
    EV_INFO << "This is the event: " << msg << endl;
}

}; // namespace
