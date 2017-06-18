package de.uni_bremen.comnets.resourcemonitor;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Helper for database access
 */

public class EnergyMonitorDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 1;
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
}
