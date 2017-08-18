# nfatracker
Statistical analysis tools for NFATracker's data
## prerequisites to installation
### Ubuntu 14.04 Server+
    sudo apt install git libfreetype6-dev libxft-dev libpng-dev python-dev python-tk gcc g++ python-pip gfortran libblas-dev liblapack-dev
    pip install --user numpy scipy matplotlib pandas patsy statsmodels
## usage
    Usage: sbt "run -b <date> -d <date> -t <type> [options]"
      -b, --baseDate <value>   Date around which to normalize data (i.e. earliest data used in prediction). Format: yyyy-MM-DD
      -d, --date <value>       Date check was cashed by the NFA and for which the prediction is made. Format: yyyy-MM-DD
      -t, --nfa-item-type <value>
                               Type of NFA item on which to resrict data.
      -v, --verbose            Print verbose information during execution.
      --help                   prints this usage text

