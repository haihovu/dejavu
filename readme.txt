Code is developed with Netbeans and some individual projects/apps can be built and tested
from Netbeans, but use the build scripts to build the distributable artifacts.

There is one view initialization script, initview.sh which should be run inside Unix-like shells such as cygwin bash or Linux shells.

There are two build scripts: buildMiDAS.sh and rebuildMiDAS.sh. These are bash scripts
meant for *nix environment, including Windows's cygwin (http://www.cygwin.com/).

Both scripts produce all the jars for all MiDAS packages plus WAR representing a deployable
web app in a container like Tomcat.

The end result are a bunch of jars under <CC_View>/SmallApps/MiDAS/distribution/jar,
plus a war file under <CC_View>/SmallApps/MiDAS/distribution.

- buildMiDAS.sh - Builds only things that have changed. To suppress javadoc generation, pass argument nogendoc=1 to script.
- rebuildMiDAS.sh - Cleans all existing artifacts and rebuild them all. To suppress javadoc generation, pass argument nogendoc=1 to script.

To develop code, use Netbeans 7.X and open project <CC_View>/SmallApps/MiDAS/proj/midasweb 
(with open required projects option) to open up all pertinent MiDAS projects.

Environment:
You'll need to set these up before you can build MiDAS:
- Install the latest JDK 1.6 if not already done so
- Install Ant 1.7+ if not already done so
- Install the latest Tomcat 6 if not already done so
- Do this once after creating a view for the first time: go to <CC_View>/SmallApps/MiDAS and execute initview.sh (from a cygwin or *Nix console)
- Make sure JAVA_HOME points to the JDK 1.6 installation, and that $JAVA_HOME/bin is in the PATH (in Windows it'd be %JAVA_HOME%/bin)
- Make sure CATALINA_HOME points to whereever you installed Tomcat
