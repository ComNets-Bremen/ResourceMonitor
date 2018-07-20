package de.uni_bremen.comnets.resourcemonitor.BroadcastReceiver;

/**
 * Class describing a broadcast receiver in detail. Used to better inform the user about the
 * collected data.
 */
public class BroadcastReceiverDescriptor {
    private  String title, description;

    public BroadcastReceiverDescriptor(String title, String description){
        this.description = description;
        this.title = title;
    }

    /**
     * Get the title of the receiver
     *
     * @return String with the title
     */
    public String getTitle(){
        return this.title;
    }

    /**
     * Get the description of the receiver
     *
     * @return String with the description
     */
    public String getDescription(){
        return this.description;
    }
}
