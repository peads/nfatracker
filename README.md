# nfatracker
Statistical analysis tools for NFATracker's data
## prerequisites to installation
### Ubuntu 14.04 Server+
#### Get sbt
Follow the instructions [here](http://www.scala-sbt.org/release/docs/Installing-sbt-on-Linux.html) to install SBT.
#### Install packages
    sudo apt install git default-jdk postgresql
#### Setup database
    sudo -u postgres createuser -E -P user_name
    sudo -u postgres createdb -O user_name database_name
## usage
    sbt dist
    sbt playGenerateSecret
    <Here an environment variable called PLAY_SECRET is saved via the preferred method.>
    <Unzip the dist archive>
    /path/to/unzipped/dist/bin/nfatracker -Dplay.http.secret.key=$PLAY_SECRET -Dhttp.agent="Mozilla/5.0"
More information about the Play ApplicationSecret can be found [here](https://www.playframework.com/documentation/2.6.x/ApplicationSecret).
