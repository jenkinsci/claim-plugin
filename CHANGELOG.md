CHANGELOG
=========
2.16 (13 Nov 2019)
------
* Fixed a Null Pointer Exception when used with [Build Failure Analyzer plugin] 1.23.0 and later [JENKINS-59204]

2.15 (03 May 2018)
------
* Added a link to the claimed test case in the claim mail when claiming a test [JENKINS-42090]

2.14.1 (14 Feb 2018)
------
* Fixed a Jenkins deadlock on startup in specific configurations [JENKINS-49038]

2.14 (30 Jan 2018)
------
* Groovy script is lost when upgrading the plugin [JENKINS-49015]
* Fixed obsolete data message following field removal

2.13.1 (07 Jan 2018)
------
* Fixed claim message inversion between assignee and claimer

2.13 (06 Jan 2018)
------
* Added an option to prevent autorefresh when claiming (Experimental)
* Allow propagating claims to following failed builds [JENKINS-12437]
* Use initial claim date for sticky/propagated claims [JENKINS-48037]

2.12 (30 Dec 2017)
------
* Fixed a Null Pointer Exception when migrating old data [JENKINS-18809]
* Use a dedicated claim icon eveywhere [JENKINS-6936]
* Made Build-Failure-Analyzer description text appear on form display, and be readonly [JENKINS-44066]
* Render run description using the configured formatter in Claim Report screen [JENKINS-42579]
* Made claim colum icon-size aware
* Standardized claim messages & improved them (user link, use of display name)
* Removed Claim Report broken rss links

2.11 (24 Dec 2017)
------
* Fixed a regression introduced in 2.10 preventing the plugin to start [JENKINS-47972]

2.10 (08 Nov 2017)
------
* Fixed an arbitrary code execution vulnerability [JENKINS-43811]
* Added an option to sort users by their full name in the claim page [JENKINS-47437]
* Fixed an Index Out of Bounds Exception when claiming a test failure [JENKINS-44024]

2.9 (14 Nov 2016)
------
* Fixed single quote failure introduced in 2.8 [JENKINS-28722]
* Removed unused json2.js [JENKINS-33078]
* Fix Class Cast Exception when claiming test [JENKINS-33715]
* Support new version of build failure plugin
* "Claim it" link for tests didn't work when BFA plugin isn't installed [JENKINS-39538]
* Claiming tests isn't compatible with Jenkins 2.0 pipelines [JENKINS-39537]

2.8 (14 Jan 2016)
------
* Integrate Build-Failure-Analyzer Plugin [JENKINS-28722]
* Add support for workflow (use SimpleBuildStep) [JENKINS-27206]

2.7 (12 May 2015)
------
* Fixed compatibility with Jenkins Core 1.577 and newer [JENKINS-27091]

2.6 (9 Mar 2015)
------
* Claim stickiness default value is now a globally configurable [JENKINS-27002]
* Added support for running a global groovy script whenever a claim is changed

2.5 (8 Dec 2014)
------
* You can now claim for other users than yourself [JENKINS-7824]

~~v2.4 (14 Oct 2014)~~
------

2.3 (18 Nov 2013)
------
* Add support for hierarchical projects [JENKINS-17734]

2.2 (27 Mar 2013)
------
* Added support for translations and added Chinese translation
* Added new icon to make claim plugin more visible
* All jobs are now visible in claim report [JENKINS-16801]

2.1 (12 Feb 2013)
------
* Fixed HTML rendering issue introduced in 2.0 [JENKINS-16766]

2.0 (6 Feb 2013)
------
* Store and display a date of when a claim was added.
* ~~Option to sticky claim test failures if the failure is "similiar"~~
* Improved Matrix claim information

1.7 (4 Maj 2010)
------
* Show user names in claims instead of ids.
* Support for claiming Matrix builds.

1.6 (28 Nov 2009)
------
* Updated claim plugin to match updated Jenkins API

1.5 (17 July 2011)
------
* Make sure that the claim plugin runs after all the other extensions that can change the build status run.

[JENKINS-59204]: https://issues.jenkins-ci.org/browse/JENKINS-59204
[JENKINS-42090]: https://issues.jenkins-ci.org/browse/JENKINS-42090
[JENKINS-49038]: https://issues.jenkins-ci.org/browse/JENKINS-49038
[JENKINS-49015]: https://issues.jenkins-ci.org/browse/JENKINS-49015
[JENKINS-48037]: https://issues.jenkins-ci.org/browse/JENKINS-48037
[JENKINS-47972]: https://issues.jenkins-ci.org/browse/JENKINS-47972
[JENKINS-47437]: https://issues.jenkins-ci.org/browse/JENKINS-47437
[JENKINS-44066]: https://issues.jenkins-ci.org/browse/JENKINS-44066
[JENKINS-44024]: https://issues.jenkins-ci.org/browse/JENKINS-44024
[JENKINS-43811]: https://issues.jenkins-ci.org/browse/JENKINS-43811
[JENKINS-42579]: https://issues.jenkins-ci.org/browse/JENKINS-42579
[JENKINS-39538]: https://issues.jenkins-ci.org/browse/JENKINS-39538
[JENKINS-39537]: https://issues.jenkins-ci.org/browse/JENKINS-39537
[JENKINS-33715]: https://issues.jenkins-ci.org/browse/JENKINS-33715
[JENKINS-33078]: https://issues.jenkins-ci.org/browse/JENKINS-33078
[JENKINS-28722]: https://issues.jenkins-ci.org/browse/JENKINS-28722
[JENKINS-27206]: https://issues.jenkins-ci.org/browse/JENKINS-27206
[JENKINS-27091]: https://issues.jenkins-ci.org/browse/JENKINS-27091
[JENKINS-27002]: https://issues.jenkins-ci.org/browse/JENKINS-27002
[JENKINS-18809]: https://issues.jenkins-ci.org/browse/JENKINS-18809
[JENKINS-17734]: https://issues.jenkins-ci.org/browse/JENKINS-17734
[JENKINS-16801]: https://issues.jenkins-ci.org/browse/JENKINS-16801
[JENKINS-16766]: https://issues.jenkins-ci.org/browse/JENKINS-16766
[JENKINS-12437]: https://issues.jenkins-ci.org/browse/JENKINS-12437
[JENKINS-7824]: https://issues.jenkins-ci.org/browse/JENKINS-7824
[JENKINS-6936]: https://issues.jenkins-ci.org/browse/JENKINS-6936
[Build Failure Analyzer plugin]: https://plugins.jenkins.io/build-failure-analyzer