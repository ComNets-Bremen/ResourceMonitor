ResourceMonitor
===============

Introduction
------------

ResourceMonitor collects a variety of different phone usage parameters for
further analysis.

ResourceMonitor is available on [Google Play](https://play.google.com/store/apps/details?id=de.uni_bremen.comnets.resourcemonitor).

Known Issues
------------

- None

TODO
----

- Nicer UI
- Automatic upload:
    - Is a WiFi network available?
    - Upload every n hours
    - Show log of last upload
    - Only upload new datasets
- Benefit for the user:
    - Set an email address and send automatic reports like "This is your
      pattern from the last week" as a pdf document.

Data Format
-----------

ResourceMonitor exports the collected data as gzip compressed json. The format
is described in detail in [DataFormat.md](DataFormat.md).

Changelog
---------

The changelog is located in [CHANGELOG.md](CHANGELOG.md).

Contact
-------

If you have any questions or comments, please write to
Jens Dede (jd@comnets.uni-bremen.de)
