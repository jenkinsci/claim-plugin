Jenkins claim plugin
============

#Description

The original claim plug-in is used to claim a single failed build. This enhancement on the original claim plug-in allows users to not only claim the desired build but also to claim multiple builds.  User are given the flexibility to select a culprit from a list of culprits (culprits are those who are also responsible for the desired build’s failure).  Based on the selected culprit, a list of broken builds that belong to that culprit is displayed as a check list. Thus, users can claim not only their builds but also the broken builds of other developers.

#Prerequisites  

Updates to this plug-in require the following:

1.  Install Maven 3.x. Maven builds the `pom.xml` file, which contains configuration information as well as configurations of plug-ins that are used during the build process. Maven also develops a Jenkins plug-in by using the `.hpi` file. Every plug-in must have an `.hpi` extension.
2.  Prior knowledge of using GitHub. You must create an account and repository on github. Please follow the steps here: https://github.com. Refer to the four tutorials at https://help.github.com.  
3.  Prior knowledge of Jenkins job configuration and plug-in development.  Refer to the Jenkins plug-in tutorial at https://wiki.jenkins-ci.org/display/JENKINS/Plugin+tutorial.  You must have admin rights to the Jenkins server.
4.  You must have VPN access to Jenkins on the Swedish server.

#Claim Repositories

**Original claim plug-in:**  https://github.com/jenkinsci/claim-plugin

This is the repository of the actual open source Jenkins claim plug-in. This plug-in does not contain the modified enhancements. 

**Modified claim plug-in:**  https://github.com/hwp437/claim-plugin

This is the current repository for the modified claim plug-in. It contains the enhancements used by DreamGallery developers. 

**Making changes to the modified claim plug-in:**

Before you make changes to the modified claim plug-in, you must fork that repository to create your own repository and your own working (local) copy. Follow these steps:

1.  Click the **Fork** tab on the GitHub page of the project you wish to copy. This creates the project on GitHub.
2.	Run the following code to clone the project to your local machine:
    `git clone https://github.com/<yourusername>/<projectname>.git`
3.	Make modifications to the project. Refer to **Developer Details**, below. 

Refer to https://help.github.com/articles/fork-a-repo for further instructions if necessary.

#Developer Details

The following class names and functions describe what and where to make changes in the claim plug-in source code:

##Jelly Files

These files render the UI. They are located at `src/main/resources/hudson/plugins/claim` and include subfolder names that are based on the java class file to which they are associated (based on MVC pattern).

###summary.jelly
This file includes code for the claim options UI.  It is located at
   `src/main/resources/hudson/plugins/claim/AbstractClaimBuildAction` 
where `AbstractClaimBuildAction` is the associated java file that is responsible for rendering the contents of  `summary.jelly`.
Each option in the claim UI contains a blue question mark tag. Each tag must have a separate html file which, by convention, must be stored at `src/main/webapp` and must have a name that begins with “help-<name>”.
*Note:* Jelly files use an XHTML format. Thus, adding any JavaScript code that requires relational operators such as ‘<’,’<=’, ’>’, ‘>=’ does not work because the parser tries to interpret ‘<’ as an opening tag statement and so on. These must be used with escape sequences such as &lt and &gt.

##Java Files

###AbstractClaimBuildAction.java

This is the main java file that contains the core part of claiming functionality. The function that does the claiming part is `doClaim()`. Functions like `getCulprits()` and `getCulpritBuildMap()` are used to export the data as contents to be displayed on the browser. `getCulpritBuildMap()` returns a mapping of each culprit with its associated list of broken builds.

###ClaimBuildAction.java

This file extends `AbstractBuildAction.java` and is basically the same.

###ClaimPublisher.java

This is the descriptor class (as per the OSGi model) that initializes and associates the claim functionality instance to every build. The `perform()` function is called whenever the descriptor class is loaded which, in our case, is ClaimPublisher. This function initializes an instance of `AbstractClaimBuildAction` and adds it to the latest broken build of a project. (For more information on descriptors, refer to the Jenkins wiki.)

Any changes or enhancements that must be done before the initialization of claim instances but not part of claim functionality must be done in this function (prior to the new instantiation of the `ClaimBuildAction` class).

#Commit Your Changes to the Modified Plug-In

1. Use `git diff` from your root project to identify changed files.
2. Use `git add <path to the changed file/filename>` to  add the changed files that must be committed.
3. Use `git commit -m <your message>` to commit the changed files.
4. Use `git push` to push commits from the project on your local machine to your forked project:
     `git push https://github.com/<yourusername>/<projectname>.git`
    This action provide an additional backup of your changes. It is also necessary if you wish to pull your modifications to the original fork (pull request).
	
#Upload your modified plug-in to the Jenkins server

Compile your project by using the following command:
     `mvn hpi:run`
This creates the `claim.hpi` file in the `claim-plugin\target` folder.

The `claim.hpi` file must be uploaded to Jenkins. To do so, perform the following:

1. On Jenkin’s Home page, select **Manage Jenkins** from the left-hand navigation column.
2. Select **Manage Plugins** from the list of options that appear.
3. Select the **Advanced** tab from the page that appears. 
4. Go to the **Upload Plugin** section under the **Advanced** tab. Upload your `claim.hpi` file.
5. Restart Jenkins once the upload is complete. Do this by entering the following URL: https://ibuild.dreampark.se/safeRestart. This restarts Jenkins and installs the manually-uploaded plug-in.
