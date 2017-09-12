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
    Usage: sbt run

