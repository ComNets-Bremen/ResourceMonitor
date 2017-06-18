package de.uni_bremen.comnets.resourcemonitor;

import android.provider.BaseColumns;

/**
 * Contract for the database tables
 */

public final class EnergyMonitorContract {
    private EnergyMonitorContract() {} // We can instantiate this class

    /**
     * Battery status table
     */
    public static class BatteryStatusEntry implements BaseColumns {
        public static final String TABLE_NAME = "BatteryStatus";
        public static final String COLUMN_NAME_PERCENTAGE = "percentage";
        public static final String COLUMN_NAME_IS_CHARGING = "is_charging";
        public static final String COLUMN_NAME_CHG_USB = "chg_usb";
        public static final String COLUMN_NAME_CHG_AC = "chg_ac";
        public static final String COLUMN_NAME_CHG_WIRELESS = "chg_wireless";
        public static final String COLUMN_NAME_TIME = "time";

        public static final String CREATE_TABLE =
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY, " +
                        COLUMN_NAME_TIME + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, " +
                        COLUMN_NAME_PERCENTAGE + " REAL, " +
                        COLUMN_NAME_IS_CHARGING + " INTEGER, " +
                        COLUMN_NAME_CHG_AC + " INTEGER, " +
                        COLUMN_NAME_CHG_USB + " INTEGER, " +
                        COLUMN_NAME_CHG_WIRELESS + " INTEGER" +
                        ")";

        public static final String DELETE_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }


    /**
     * Screen status table
     */
    public static class ScreenStatusEntry implements BaseColumns {
        public static final String TABLE_NAME = "ScreenStatus";
        public static final String COLUMN_NAME_SCREEN_STATUS = "screen_status";
        public static final String COLUMN_NAME_TIME = "time";

        public static final String CREATE_TABLE =
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY, " +
                        COLUMN_NAME_TIME + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, " +
                        COLUMN_NAME_SCREEN_STATUS + " INTEGER " +
                        ")";

        public static final String DELETE_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    /**
     * WiFi status table
     */
    public static class WiFiStatusEntry implements BaseColumns {
        public static final String TABLE_NAME = "WiFiStatus";
        public static final String COLUMN_NAME_WIFI_STATUS = "wifi_status";
        public static final String COLUMN_NAME_TIME = "time";

        public static final String CREATE_TABLE =
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY, " +
                        COLUMN_NAME_TIME + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, " +
                        COLUMN_NAME_WIFI_STATUS + " INTEGER " +
                        ")";

        public static final String DELETE_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    /**
     * Airplanemode status table
     */
    public static class AirplaneModeEntry implements BaseColumns {
        public static final String TABLE_NAME = "AirplaneModeStatus";
        public static final String COLUMN_NAME_AIRPLANE_MODE = "airplane_mode";
        public static final String COLUMN_NAME_TIME = "time";

        public static final String CREATE_TABLE =
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY, " +
                        COLUMN_NAME_TIME + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, " +
                        COLUMN_NAME_AIRPLANE_MODE + " INTEGER " +
                        ")";

        public static final String DELETE_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    /**
     * Traffic statistics table
     */
    public static class TrafficStatsEntry implements BaseColumns {
        public static final String TABLE_NAME = "TrafficStatistics";
        public static final String COLUMN_NAME_MOBILE_TX = "mobile_tx";
        public static final String COLUMN_NAME_MOBILE_RX = "mobile_rx";
        public static final String COLUMN_NAME_TOTAL_TX = "total_tx";
        public static final String COLUMN_NAME_TOTAL_RX = "total_rx";
        public static final String COLUMN_NAME_TIME = "time";

        public static final String CREATE_TABLE =
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY, " +
                        COLUMN_NAME_TIME + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, " +
                        COLUMN_NAME_MOBILE_TX + " INTEGER, " +
                        COLUMN_NAME_MOBILE_RX + " INTEGER, " +
                        COLUMN_NAME_TOTAL_RX + " INTEGER, " +
                        COLUMN_NAME_TOTAL_TX + " INTEGER " +
                        ")";

        public static final String DELETE_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    /**
     * Bluetooth status table
     */
    public static class BluetoothStatusEntry implements BaseColumns {
        public static final String TABLE_NAME = "BluetoothStatus";
        public static final String COLUMN_NAME_BLUETOOTH_STATUS = "bluetooth_status";
        public static final String COLUMN_NAME_BLE_AVAILABLE = "ble_avail";
        public static final String COLUMN_NAME_TIME = "time";

        public static final String CREATE_TABLE =
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY, " +
                        COLUMN_NAME_TIME + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, " +
                        COLUMN_NAME_BLUETOOTH_STATUS + " INTEGER, " +
                        COLUMN_NAME_BLE_AVAILABLE + " INTEGER " +
                        ")";

        public static final String DELETE_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;
    }

    /**
     * Cellular network status
     */
    public static class CellularStatusEntry implements BaseColumns {
        public static final String TABLE_NAME = "CellularStatus";
        public static final String COLUMN_NAME_TYPE = "cellular_type";
        public static final String COLUMN_NAME_STATE = "cellular_state";
        public static final String COLUMN_NAME_TIME = "time";

        public static final String CREATE_TABLE =
                "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                        _ID + " INTEGER PRIMARY KEY, " +
                        COLUMN_NAME_TIME + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP NOT NULL, " +
                        COLUMN_NAME_TYPE + " TEXT, " +
                        COLUMN_NAME_STATE + " INTEGER " +
                        ")";

        public static final String DELETE_TABLE =
                "DROP TABLE IF EXISTS " + TABLE_NAME;

    }
}
