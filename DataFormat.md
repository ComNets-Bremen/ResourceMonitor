The Dataformat
==============

ResourceMonitor exports the collected data as gzip compressed json. The json
structure is described in this document. Please be aware that the format is
subject to change!

- `HAS_BLE`: True if the device supports Bluetooth Low Energy
- `EXPORT_TIMESTAMP`: Unix timestamp of export
- `UUID`: Random id created during App installation to distinguish different
  users
- `TIMEZONE`: The timezone the data was collected
- `CellularStatus`: Array of the statuses of the cellular connection
    - `_id`: Database row id
    - `cellular_type`: The type of the connection
    - `time`: Time when the data was acquired
    - `cellular_state`: Connection state
-`BatteryStatus`: Array of the statuses of the battery
    - `is_charging`: Is the device charging?
    - `chrg_usb`: Is the device charging via USB?
    - `time`: Time when the data was acquired
    - `chg_wireless`: Is the device charging wirelessly?
    - `_id`: Database row id
    - `percentage`: Battery level in percent
    - `chg_ac`: Is the device charging via an AC power plug?
- `WiFiStatus`: Array of the statuses of the WiFi network
    - `_id`: Database row id
    - `time`: Time when the data was acquired
    - `wifi_status`: Status code for wireless connection
- `ScreenStatus`: Array of the statuses of the screen
    - `_id`: Database row id
    - `time`: Time when the data was acquired
    - `screen_status`: 1 if the screen is on, 0 otherwise
- `BluetoothStatus`: Array of the statuses of the Bluetooth interface
    - `_id`: Database row id
    - `time`: Time when the data was acquired
    - `bluetooth_status`: connection status of the Bluetooth interface
- `TrafficStatistics`: Array of the number of transmitted bytes:
    - `mobile_tx`: Transmitted bytes via mobile data
    - `time`: Time when the data was acquired
    - `_id`: Database row id
    - `total_tx`: Total number of transmitted bytes
    - `total_rx`: Total number of received bytes
    - `mobile_rx`: Received bytes via mobile data
