maven-verify-dependencies-plugin
================================

Maven plugin to verify that you're using the latest/correct or not excessive dependencies in compile scope

 The Problem
 -----------

When deploying an a container such a JBoss EAP, too many people include all the dependencies inside their 
.war files making them very large, problematic for upgrades and takes a long time to deploy.


 The Solution
 ------------
This plugin can be configured with a list of "provided" scoped artifacts which are checked against in the build process, and it will warn you of
dependencies that are not necessary to include.

I considered using Plexus to lookup instead of using a file, however i was unable to get it to work in the time I allowed myself, so if anyone see's the value and
wants to contribute this feature then it would be greatfully received.


