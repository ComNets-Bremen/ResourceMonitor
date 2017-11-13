Changelog
=========

### 2017-11-13, Version 0.31, Version Code 31

- Settings: Allow the upload on metered and unmetered connections
- Count number of failed uploads and show a warning message to the user
- Show the user that the automatic upload is enabled

### 2017-11-13, Version 0.30, Version Code 30

- Further tuned the upload timing parameters

### 2017-11-12, Version 0.29, Version Code 29

- Adapted upload timing parameters

### 2017-11-10, Version 0.28, Version Code 28

- Show licenses properly

### 2017-11-09, Version 0.27, Version Code 27

- Minor fix: do not display the manual each time the app is opened

### 2017-11-09, Version 0.26, Version Code 26

- Fixed some context memory leakage problems
- Added comments to the source code
- Don't show the upload button if the automatic upload is enabled
- Remove the upload button if the automatic upload is enabled
- Allow to show the manual dialog after important changes
- Adapt the manual for the recent changes

### 2017-11-08, Version 0.25, Version Code 25

- Updated string handling (format string)
- gradle and build tools updated
- minor fixes (context leakage)
- Upload background job
- Use firebase and play services to schedule jobs

### 2017-10-24, Version 0.24, Version Code 24

- Improved pie chart
- Bugfix in upload: timeout set to 30 seconds

### 2017-10-24, Version 0.23, Version Code 23

- Improved the time span shown in the UI

### 2017-10-16, Version 0.22, Version Code 22

- Minor performance improvements in the SQL queries

### 2017-10-15, Version 0.21, Version Code 21

- Minor change in battery percentage calculation

### 2017-10-14, Version 0.20, Version Code 20

- Improved the UI output regarding the discharge behaviour

### 2017-10-13, Version 0.19, Version Code 19

- Show the current discharge behaviour on the UI

### 2017-09-30, Version 0.18, Version Code 18

- Minor changes in the export function

### 2017-09-15, Version 0.17, Version Code 17

- Be smarter for the data upload: Only send data which has not been transmitted
  yet

### 2017-08-20, Version 0.16, Version Code 16

- Rename the abstract receiver class
- Show time of last data acquisition
- Allow user to disable the notification bar message

### 2017-07-20, Version 0.15, Version Code 15

- Removed setting for disabling the data collection (gaps datasets are always bad...)
- Show more information in the notification bar

### 2017-07-20, Version 0.14, Version Code 14

- Use Keyed-Hash Message Authentication Code

### 2017-07-16, Version 0.13, Version Code 13

- Tell the user how to use this app

### 2017-07-15, Version 0.12, Version Code 12

- Minor translation fixes

### 2017-07-15, Version 0.11, Version Code 11

- Give more feedback for the upload
- Show last successful upload date and time in UI
- Minor performance improvements and restructurings
- Restructured menu

### 2017-07-14, Version 0.10, Version Code 10

- Data upload via dataserver

### 2017-07-11, Version 0.9, Version Code 9

- Store app version number in json object

### 2017-06-28, Version 0.8, Version Code 8

- Move settings to separate activity
- Add icon to fill white space
- Restructured menu

### 2017-06-27, Version 0.7, Version Code 7

- Moved some options to sub menu
- Allow data export to any destination (just for the case the user is
  interested in analyzing the data)
- Add more information and contact details

### 2017-06-21, Version 0.6, Version Code 6

- Fixed several export issues

### 2017-06-19, Version 0.5, Version Code 5

- Store the timestamp when the data was exported into the json object
- Extended the filename for the export to distinguish between different files easily

### 2017-06-18, Version 0.4, Version Code 4

- Start App after upgrade
- Store BLE availablility in json object instead of table
- Store Bluetooth status on App start

### 2017-06-18, Version 0.3, Version Code 3

- Transmit device timezone

### 2017-06-18, Version 0.2, Version Code 2

- Minor changes in the translation

### 2017-06-18, Version 0.1, Version Code 1

- Initial version, code completely rewritten
