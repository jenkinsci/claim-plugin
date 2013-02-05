Jenkins Claim Plugin
=========================

A plugin for Jenkins CI that allows users to claim(take responsibility) for a failing build.<br>
See https://wiki.jenkins-ci.org/display/JENKINS/Claim+plugin for detailed instructions.

Change Log
----------

### v2.0 (5 Feb 2013)

- Store and display a date of when a claim was added.
- Support for annotations in claims.
- Improved Matrix claim information

### v1.7 (4 Maj 2010)

- Show user names in claims instead of ids.
- Support for claiming Matrix builds.

### v1.6 (28 Nov 2009)

- Updated claim plugin to match updated Jenkins API

### v1.5 (17 July 2011)

- Make sure that the claim plugin runs after all the other extensions that can change the build status run.