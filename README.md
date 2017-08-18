# nfatracker
Statistical analysis tools for NFATracker's data
## prerequisites to installation
### Ubuntu 14.04 Server+
#### Get sbt
Follow the instructions [here](http://www.scala-sbt.org/release/docs/Installing-sbt-on-Linux.html) to install SBT.
#### Install packages
    sudo apt install git default-jdk
## usage
    Usage: sbt "run -b <date> -d <date> -t <type> [options]"
      -b, --baseDate <value>   Date around which to normalize data (i.e. earliest data used in prediction). Format: yyyy-MM-DD
      -d, --date <value>       Date check was cashed by the NFA and for which the prediction is made. Format: yyyy-MM-DD
      -t, --nfa-item-type <value>
                               Type of NFA item on which to resrict data.
      --plot-regression        Plot a linear regression of data normalized around BASEDATE.
      -v, --verbose            Print verbose information during execution.
      --help                   prints this usage text

