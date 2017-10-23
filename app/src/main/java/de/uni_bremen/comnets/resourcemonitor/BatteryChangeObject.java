package de.uni_bremen.comnets.resourcemonitor;

import android.content.Context;
import android.util.Log;

import java.util.Date;
import java.util.List;

/**
 * Handling class for battery statistics / general battery information.
 * The instances are initiated with start and stop times. The class can calculate several
 * metices from that.
 *
 * The object has a start and a end time. Measurements are calculated in this range
 */

class BatteryChangeObject {
    public static final String TAG = BatteryChangeObject.class.getSimpleName();

    private long startTime = -1;
    private long stopTime = -1;
    private double startPercentage = -1.0;
    private double stopPercentage = -1.0;
    private boolean isCharging = false;

    private static double HOUR_MILLISECONDS = 60*60*1000;
    private static double CRITICAL_PERCENTAGE = 0.01;

    /**
     * Constructor to set the main properties of this object
     *
     * @param isCharging        Is the device being charged in this period?
     * @param startTime         Start time of this period in millisecond epoch time
     * @param stopTime          Stop time of this period in milliseconds epoch time
     * @param startPercentage   Start percentage of this period
     * @param stopPercentage    End percentage of this period
     */
    public BatteryChangeObject(boolean isCharging, long startTime, long stopTime, double startPercentage, double stopPercentage){
        this.isCharging = isCharging;
        this.startTime = startTime;
        this.stopTime = stopTime;
        this.startPercentage = startPercentage;
        this.stopPercentage = stopPercentage;
        //Log.d(TAG, "ADDED, Valid: " + this.isValid() + " Charging: " + this.isCharging() + " start=" + new Date(startTime) + " / " + this.startPercentage + " stop=" + new Date(stopTime) + " / " + this.stopPercentage);
    }


    /**
     * Set the values if the default constructor is not used / values have to be set differntly
     *
     * @param isCharging        Is the device being charged in this period?
     * @param startTime         Start time of this period in millisecond epoch time
     * @param stopTime          Stop time of this period in milliseconds epoch time
     * @param startPercentage   Start percentage of this period
     * @param stopPercentage    End percentage of this period
     */
    public void set(boolean isCharging, long startTime, long stopTime, double startPercentage, double stopPercentage){
        this.isCharging = isCharging;
        this.startTime = startTime;
        this.stopTime = stopTime;
        this.startPercentage = startPercentage;
        this.stopPercentage = stopPercentage;
    }

    /**
     * Get the start time of the current interval
     *
     * @return the start time as a java timestamp
     */
    public long getStartTime(){
        return this.startTime;
    }

    /**
     * Get the stop time of the current interval
     *
     * @return the stop time as a java timestamp
     */
    public long getStopTime(){
        return this.stopTime;
    }

    /**
     * Get the average discharge per hour for the period
     *
     * @return the discharge per hour in percent
     */
    public double getPercentPerHour(){
        return (getDeltaPercent()* HOUR_MILLISECONDS)/(double) getDeltaT();
    }

    /**
     * Get the difference of the percentage
     *
     * @return  Difference of the percentage
     */
    public double getDeltaPercent(){
        return this.stopPercentage - this.startPercentage;
    }

    /**
     * Get the difference in time
     *
     * @return difference in time as milliseconds
     */
    public long getDeltaT(){
        return this.stopTime - this.startTime;
    }

    /**
     * Is the devices being chagred in this period of time?
     *
     * @return true if charged
     */
    public boolean isCharging() {
        return isCharging;
    }

    /**
     * Is the current object valid? Several tests are performed:
     * - Time difference is unequal to zero (division error)
     * - Percentage is not too low (less than 1%) to overcome problems with shut down phones
     *
     * @return
     */
    public boolean isValid(){
        // TODO more tests?
        return ((stopTime - startTime) != 0.0) &&                   // prevent division by zero
                (startPercentage > CRITICAL_PERCENTAGE && stopPercentage > CRITICAL_PERCENTAGE);  // empty battery state
    }

    /**
     * Get period of time covered by the given list of object
     *
     * @param   changeObjects a list of @BatteryChangeObject
     * @return  The time covered by the object in milliseconds
     */
    static long getTotalDischargeTime(List<BatteryChangeObject> changeObjects){
        long totalTimeMillis = 0;
        for (BatteryChangeObject bco : changeObjects){
            if (bco.isValid() && bco.getDeltaPercent() <= 0.0 && !bco.isCharging()){
                totalTimeMillis += bco.getDeltaT();
            }
        }
        return totalTimeMillis;
    }

    /**
     * Return the total time of the given list
     *
     * @param changeObjects a list of @BatteryChangeObjects
     * @return The timespan in milliseconds
     */
    static long getTotalTime(List<BatteryChangeObject> changeObjects){
        long minTime = -1;
        long maxTime = -1;
        for (BatteryChangeObject bco : changeObjects){
            if (bco.isValid()){
                if (minTime < 0){
                    minTime = bco.getStartTime();
                }
                if (maxTime < 0){
                    maxTime = bco.getStopTime();
                }

                minTime = Math.min(minTime, bco.getStartTime());
                maxTime = Math.max(maxTime, bco.getStopTime());
            }
        }
        if (minTime > 0 && maxTime > 0){
            return maxTime - minTime;
        }
        // Error. No valid values
        return -1;
    }

    /**
     * Calculate the average discharge of all given objects
     *
     * @param chargeObjects A list of charge objects
     * @return The average discharge as percent per hour
     */
    static double averageDischarge(List<BatteryChangeObject> chargeObjects){
        long totalTimeMillis = getTotalDischargeTime(chargeObjects);

        double percentageTime = 0;

        for (BatteryChangeObject bco : chargeObjects){
            if (bco.isValid() && bco.getDeltaPercent() <= 0.0 && !bco.isCharging()){
                double percentageOfTotal = (double) bco.getDeltaT() / (double) totalTimeMillis;
                percentageTime += bco.getPercentPerHour() * percentageOfTotal;
            }
        }

        return percentageTime;
    }

    /**
     * Try to get the battery capacity from an internal API. Might not work...
     *
     * @param context   The application context
     * @return          The capacity in mAh, -1 if an error occurred
     */
    static double getBatteryCapacity(Context context){
        Object mPowerProfile = null;
        final String POWER_PROFILE_CLASS = "com.android.internal.os.PowerProfile";
         try {
            mPowerProfile = Class.forName(POWER_PROFILE_CLASS)
                .getConstructor(Context.class).newInstance(context);
        } catch (Exception e) {
             // Private class, might not work
             //e.printStackTrace();
             Log.e(TAG, "Class not found");
             return -1;
        }

        try {
            double capacity = (Double) Class
                    .forName(POWER_PROFILE_CLASS)
                    .getMethod("getAveragePower", java.lang.String.class)
                    .invoke(mPowerProfile, "battery.capacity");

            Log.d(TAG, "Got capacity: " + capacity + " mAh");
            return capacity;
        } catch (Exception e){
            //e.printStackTrace();
            Log.e(TAG, "Class not found");
            return -1;
        }
    }

    @Override
    public String toString() {
        return "Min: " + new Date(this.startTime) + " Max: " + new Date(this.stopTime) + " percent per hour: " + getPercentPerHour() + " Percent: " + this.stopPercentage;
    }

}
