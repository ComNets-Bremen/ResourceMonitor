package de.uni_bremen.comnets.resourcemonitor;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

/**
 * Helper for database access
 */

public class EnergyMonitorDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "EnergyMonitor.db";
    public static final String TAG = EnergyMonitorDbHelper.class.getSimpleName();

    public EnergyMonitorDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(EnergyMonitorContract.BatteryStatusEntry.CREATE_TABLE);
        db.execSQL(EnergyMonitorContract.ScreenStatusEntry.CREATE_TABLE);
        db.execSQL(EnergyMonitorContract.WiFiStatusEntry.CREATE_TABLE);
        db.execSQL(EnergyMonitorContract.AirplaneModeEntry.CREATE_TABLE);
        db.execSQL(EnergyMonitorContract.TrafficStatsEntry.CREATE_TABLE);
        db.execSQL(EnergyMonitorContract.BluetoothStatusEntry.CREATE_TABLE);
        db.execSQL(EnergyMonitorContract.CellularStatusEntry.CREATE_TABLE);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(EnergyMonitorContract.BatteryStatusEntry.DELETE_TABLE);
        db.execSQL(EnergyMonitorContract.ScreenStatusEntry.DELETE_TABLE);
        db.execSQL(EnergyMonitorContract.WiFiStatusEntry.DELETE_TABLE);
        db.execSQL(EnergyMonitorContract.AirplaneModeEntry.DELETE_TABLE);
        db.execSQL(EnergyMonitorContract.TrafficStatsEntry.DELETE_TABLE);
        db.execSQL(EnergyMonitorContract.BluetoothStatusEntry.DELETE_TABLE);
        db.execSQL(EnergyMonitorContract.CellularStatusEntry.DELETE_TABLE);

        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    /**
     * Return the statistics shown in the user interface
     * @param db the database
     * @return a string with some information
     */
    public static String getDbStatistics(SQLiteDatabase db){
        String s = "";
        s += EnergyMonitorContract.BatteryStatusEntry.TABLE_NAME + ": " + db.query(EnergyMonitorContract.BatteryStatusEntry.TABLE_NAME, null, null, null, null, null, null).getCount() + "\n";
        s += EnergyMonitorContract.ScreenStatusEntry.TABLE_NAME + ": " + db.query(EnergyMonitorContract.ScreenStatusEntry.TABLE_NAME, null, null, null, null, null, null).getCount() + "\n";
        s += EnergyMonitorContract.WiFiStatusEntry.TABLE_NAME + ": " + db.query(EnergyMonitorContract.WiFiStatusEntry.TABLE_NAME, null, null, null, null, null, null).getCount() + "\n";
        s += EnergyMonitorContract.AirplaneModeEntry.TABLE_NAME + ": " + db.query(EnergyMonitorContract.AirplaneModeEntry.TABLE_NAME, null, null, null, null, null, null).getCount() + "\n";
        s += EnergyMonitorContract.TrafficStatsEntry.TABLE_NAME + ": " + db.query(EnergyMonitorContract.TrafficStatsEntry.TABLE_NAME, null, null, null, null, null, null).getCount() + "\n";
        s += EnergyMonitorContract.BluetoothStatusEntry.TABLE_NAME + ": " + db.query(EnergyMonitorContract.BluetoothStatusEntry.TABLE_NAME, null, null, null, null, null, null).getCount() + "\n";
        s += EnergyMonitorContract.CellularStatusEntry.TABLE_NAME + ": " + db.query(EnergyMonitorContract.CellularStatusEntry.TABLE_NAME, null, null, null, null, null, null).getCount();
        return s;
    }

    /**
     * Return a list of battery change states containing the start and the end of the current phase. The phases can be limited by unix timestamps
     *
     * @param db        A database instance to read the data from
     * @param minTime   Minimum time, -1 if unused
     * @param maxTime   Maximum time, -1 if unused
     * @return          A list of @BatteryChangeObject
     */
    public static List<BatteryChangeObject> getDischargeBehaviour(SQLiteDatabase db, long minTime, long maxTime){

        if (minTime > 0)
            Log.d(TAG, "Min: " + new Date(minTime));
        if (maxTime > 0)
            Log.d(TAG, "Max: " + new Date(maxTime));

        // We need a raw query to select a proper time period (string date from SQL vs. epoch time).
        // So let's create some SQL here...

        String selectQuery = "SELECT " +
                "_id, " +
                EnergyMonitorContract.BatteryStatusEntry.COLUMN_NAME_PERCENTAGE + ", " +
                EnergyMonitorContract.BatteryStatusEntry.COLUMN_NAME_IS_CHARGING + ","  +
                "strftime(\"%s\", " + EnergyMonitorContract.BatteryStatusEntry.COLUMN_NAME_TIME + ") * 1000.0 AS " + EnergyMonitorContract.BatteryStatusEntry.COLUMN_NAME_TIME +
                " FROM " + EnergyMonitorContract.BatteryStatusEntry.TABLE_NAME;

        if (minTime > 0 && maxTime > 0) {  // Both are valid
            selectQuery += " WHERE " + EnergyMonitorContract.BatteryStatusEntry.COLUMN_NAME_TIME + " < DATETIME(" + maxTime + "/1000.0, \"unixepoch\") AND " + EnergyMonitorContract.BatteryStatusEntry.COLUMN_NAME_TIME + " > DATETIME(" + minTime + "/1000.0, \"unixepoch\")";
        } else if(minTime < 0 && maxTime > 0){ // Only limited by maxTime
            selectQuery += " WHERE " + EnergyMonitorContract.BatteryStatusEntry.COLUMN_NAME_TIME + " < DATETIME(" + maxTime + "/1000.0, \"unixepoch\")";
        } else if (minTime > 0 && maxTime < 0) { // Only limited by minTime
            selectQuery += " WHERE " + EnergyMonitorContract.BatteryStatusEntry.COLUMN_NAME_TIME + " > DATETIME(" + minTime + "/1000.0, \"unixepoch\")";
        }

        //Log.d(TAG, selectQuery);

        // We need the raw query to convert the string date to a timestamp
        Cursor cur = db.rawQuery(selectQuery, null);

        cur.moveToFirst();

        // Store the items to the BatteryChangeObjects List and perform all further calculations using this list / object
        List<BatteryChangeObject> battery = new ArrayList<>();

        long lastTimestamp = -1;
        double lastPercentage = -1;
        boolean lastCharging = false;

        while(!cur.isAfterLast()){
            long timestamp = cur.getLong(cur.getColumnIndex(EnergyMonitorContract.BatteryStatusEntry.COLUMN_NAME_TIME));
            double percentage = cur.getDouble(cur.getColumnIndex(EnergyMonitorContract.BatteryStatusEntry.COLUMN_NAME_PERCENTAGE));
            boolean isCharging = cur.getInt(cur.getColumnIndex(EnergyMonitorContract.BatteryStatusEntry.COLUMN_NAME_IS_CHARGING)) != 0;

            if (lastTimestamp < 0){
                // First dataset
                lastTimestamp = timestamp;
                lastPercentage = percentage;
                lastCharging = isCharging;
            } else {
                battery.add(new BatteryChangeObject(lastCharging, lastTimestamp, timestamp, lastPercentage, percentage));
                lastPercentage = percentage;
                lastTimestamp = timestamp;
                lastCharging = isCharging;
            }
            cur.moveToNext();
        }

        cur.close();

        return  battery;
    }

    /**
     * Convert a database cursor to an json array
     * @param cur the database cursor
     * @return JSONArray of the cursor
     */
    public static JSONArray cursorToJson(Cursor cur){
        JSONArray ret = new JSONArray();
        cur.moveToFirst();
        while(!cur.isAfterLast()) {
            JSONObject row = new JSONObject();

            try {
                for (int column = 0; column < cur.getColumnCount(); column++) {
                    switch (cur.getType(column)){
                        case Cursor.FIELD_TYPE_FLOAT:
                            row.put(cur.getColumnName(column), cur.getFloat(column));
                            break;
                        case Cursor.FIELD_TYPE_INTEGER:
                            row.put(cur.getColumnName(column), cur.getLong(column));
                            break;
                        case Cursor.FIELD_TYPE_STRING:
                            row.put(cur.getColumnName(column), cur.getString(column));
                            break;
                        default:
                            row.put(cur.getColumnName(column), cur.getString(column));
                    }
                }
            } catch (Exception e) {
                Log.d(TAG, e.getMessage()  );
            }
            ret.put(row);
            cur.moveToNext();
        }
        return ret;
    }

    /**
     * Helper function to use the result array and to reduce the amount of transmitted data
     *
     * @param range     The range object from the server
     * @param tableName The table name
     * @return          The where part of the query object. null if not successful / no data available.
     */
    public static String getWhereForQuery(JSONObject range, String tableName){
        if ((range != null ) && (range.has(tableName))){
            long maximum = -1;
            try {
                JSONObject tableJsonObject = range.getJSONObject(tableName);
                if (tableJsonObject != null && !tableJsonObject.isNull("max")){
                    maximum = tableJsonObject.getLong("max");
                } else {
                    maximum = -1;
                }
            } catch (org.json.JSONException e) {
                e.printStackTrace();
                maximum = -1;
            }
            Log.d(TAG, "Max in table " + tableName + ": " +  maximum);
            if (maximum > 0){
                return "_id > " + maximum;
            }
        }
        return null;
    }
}
