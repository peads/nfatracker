# nfatracker
Statistical analysis tools for NFATracker's data

usage: linear_regression.py [-h] -b BASEDATE -d DATE [-t TYPE]
                            [--plot-regression] [--no-plot-regression]

Predict approval date of a given NFA item based on NFATracker data.

optional arguments:
  -h, --help            show this help message and exit
  -b BASEDATE, --base-date BASEDATE
                        Date around which to normalize data. Format: yyyy-MM-
                        DD
  -d DATE, --check-cashed-date DATE
                        Date check was cashed by the NFA and for which the
                        prediction is made. Format: yyyy-MM-DD
  -t TYPE, --nfa-item-type TYPE
                        Type of NFA item on which to resrict data.
  --plot-regression     Plot a linear regression of data normalized around
                        BASEDATE.
  --no-plot-regression  Default: does not plot linear regression.

