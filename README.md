ResourceMonitor
===============

Introduction
------------

ResourceMonitor collects a variety of different phone usage parameters for
further analysis.

ResourceMonitor is available on [Google Play](https://play.google.com/store/apps/details?id=de.uni_bremen.comnets.resourcemonitor).
You can access the [Developing Version of the App here](https://play.google.com/apps/testing/de.uni_bremen.comnets.resourcemonitor).

Known Issues
------------

- None

TODO
----

- Nicer UI
- Benefit for the user:
    - Set an email address and send automatic reports like "This is your
      pattern from the last week" as a pdf document.
- Upload data should be limited to e.g. 2.5 MB per upload

Data Format
-----------

ResourceMonitor exports the collected data as gzip compressed json. The format
is described in detail in [DataFormat.md](DataFormat.md).

Data Analysis
-------------

A simple analysis script can be found in the directory [analyze](analyze). The
script is just kind of proof of concept. Feel free to adapt and extend!

Changelog
---------

The changelog is located in [CHANGELOG.md](CHANGELOG.md).

Author / Contact
----------------

If you have any questions or comments, please write to
Jens Dede (jd@comnets.uni-bremen.de), [Sustainable Communication Networks,
University of Bremen](https://www.comnets.uni-bremen.de/), Germany
