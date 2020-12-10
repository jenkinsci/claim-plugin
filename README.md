Claim plugin
====================

[![Jenkins Plugin](https://img.shields.io/jenkins/plugin/v/claim.svg)](https://plugins.jenkins.io/claim)
[![Jenkins Plugin Installs](https://img.shields.io/jenkins/plugin/i/claim.svg?color=blue)](https://plugins.jenkins.io/claim)

## About this plugin

The claim plugin is meant to allow users to claim failing builds and tests from [Jenkins](https://jenkins.io) to signal they are taking responsibility in fixing them.

Supported features

* Claiming **failed builds**.
* Claiming **failed tests**.
* Marking claims as **sticky**, so that future failing builds or tests will keep the declared claim
* Ensuring claim **propagate to following builds**, if claim is not done on the last build
* Running a **Groovy script** on claim
* Integrating with [Build Failure Analyzer plugin] to **describe claims using BFA knowledge base**
* Integrating with [Mailer plugin] to **notify claims by mail**

## Usage

### Installing and enabling the plugin

The Claim plugin can be installed from any Jenkins installation connected to the Internet using the **Plugin Manager** screen.

You can then access the **Manage Jenkins** > **Configure System** > **Claim** section to configure global settings and default options for the plugin.

### Allow claiming of failed builds

To allow claiming of failed builds, add a **Allow broken build claiming** post-build action to your project.

![The Claim build post-build action](./docs/images/build-config-claim.png)

### Claiming a build

Steps | Screenshot
----- | ----------
Failed builds will then display a **Claim it** option. | ![The "Claim it" action for a build](./docs/images/build-claim-action.png)
Upon clicking the **Claim it** link, a logged-in user can then claim the build. | ![The claim details for a build](./docs/images/build-claim-details.png)
The build will then display the claim information, along with options to **Drop the claim** or **Reassign it**. | ![The claim info for a build](./docs/images/build-claim-info.png)
The claim information will also be displayed on the **Build History** widget, with information about the claim. | ![The claim history for a build](./docs/images/build-history-claim-info.png)

### Allow claiming of failed tests

To allow claiming of failed tests, add a **Allow claiming of failed test** additional test report features to your test results report post-build action.

![The Claim test test result report feature](./docs/images/test-config-claim.png)

### Claiming a test

Steps | Screenshot
----- | ----------
Failed tests will then display a **Claim it** option. | ![The "Claim it" action for a test](./docs/images/test-claim-action.png)
Upon clicking the **Claim it** link, a logged-in user can then claim the test. | ![The claim details for a test](./docs/images/test-claim-details.png)
The test will then display the claim information, along with options to **Drop the claim** or **Reassign it**. | ![The claim info for a test](./docs/images/test-claim-info.png)
The claim information will also be displayed on the **Test Result** page, with a tooltipinformation about the claim. | ![The claim history for a test](./docs/images/test-claim-summary.png)

### Integration with Build Failure Analyzer
This feature is supported from v2.8 forward.

Given the [Build Failure Analyzer plugin] is installed and enabled, users can use failure causes stored in the BFA plugin knowledge base to describe their claim, and this cause will be stored with the claimed build or test.

![The Claim menu when BFA is installed](./docs/images/claim-bfa-enabled.png)

To claim a build with a failure cause:

Upon claiming the build, the "Error" drop-down will be populated with all failure causes defined in the knowledge base.
Selecting a failure cause will update the "Description" field with the cause description
By default, selecting "None" will result in the default claim plugin behaviour.
If there are already failure causes automatically detected by BFA, the claimed cause will be added to the list of identified problems.
Claiming without a failure cause selected will result in no additional causes specified under "Identified Problems"
The user may also input a claim reason, which is saved in the claim report as per default functionality.

### Pipeline integration

#### Allow claiming of broken builds

The plugin provides the property `allowBrokenBuildClaiming()` to allow claiming of failed builds.

Example:

```
properties([
  allowBrokenBuildClaiming()
  ...
])
```

#### Allow claiming of failed tests

A pipeline job needs to be configured with `ClaimTestDataPublisher` as JUnit testDataPlublisher to allow claiming of failed tests.

Example:

```
junit testDataPublishers: [[$class: 'ClaimTestDataPublisher']], testResults: '**/target/*-reports/TEST*.xml'
```

### Limitations
As the plugin uses a post build action to allow claiming, build or tests marked as failed priori to enabling the plugin will not have any claim action proposed.

## License

[MIT License](./LICENSE.md)

## More information

[Changelog](./CHANGELOG.md)

[Build Failure Analyzer plugin]: https://plugins.jenkins.io/build-failure-analyzer
[Mailer plugin]: https://plugins.jenkins.io/mailer
