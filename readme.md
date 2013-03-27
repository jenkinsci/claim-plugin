Jenkins Claim Plugin
=========================

A plugin for Jenkins CI that allows users to claim(take responsibility) for a failing build.<br>
Look at [wiki] for detailed instructions.

Change Log
----------

### v2.2 (27 Mar 2013)
- Added support for translations and added Chinese translation
- Added new icon to make claim plugin more visible
- All jobs are now visible in claim report [JENKINS-16801]

### v2.1 (12 Feb 2013)

- Fixed HTML rendering issue introduced in 2.0 [JENKINS-16766]

### v2.0 (6 Feb 2013)

- Store and display a date of when a claim was added.
- ~~Option to sticky claim test failures if the failure is "similiar"~~
- Improved Matrix claim information

### v1.7 (4 Maj 2010)

- Show user names in claims instead of ids.
- Support for claiming Matrix builds.

### v1.6 (28 Nov 2009)

- Updated claim plugin to match updated Jenkins API

### v1.5 (17 July 2011)

- Make sure that the claim plugin runs after all the other extensions that can change the build status run.

[JENKINS-16766]: https://issues.jenkins-ci.org/browse/JENKINS-16766
[wiki]: https://wiki.jenkins-ci.org/display/JENKINS/Claim+plugin