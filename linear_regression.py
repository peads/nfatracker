import numpy as np
import warnings

warnings.simplefilter(action='ignore', category=FutureWarning)
import pandas as pd
import statsmodels.api as sm
import matplotlib.pyplot as plt
import urllib2
import functools as ft
from argparse import ArgumentParser


def normalize_date_to_base(date, basedate):
    try:
        return (pd.to_datetime(date) - basedate).days
    except (pd._libs.tslib.OutOfBoundsDatetime, ValueError):
        return normalize_date_to_base(pd.Timestamp.min, basedate)


def filter_outliers(row, item_type):
    return row['CheckCashed'] >= 0 and row['Approved'] - row['CheckCashed'] >= 14 \
           and row['NFAItem'] == item_type \
           and row['FormType'] is not "Form 3 To Dealer"


def parse_args():
    parser = ArgumentParser(description="Predict approval date of a given NFA item based on NFATracker data.")
    parser.add_argument("-b", "--base-date", dest="basedate", required=True,
                        help="Date around which to normalize data. Format: yyyy-MM-DD", metavar="BASEDATE")
    parser.add_argument("-d", "--check-cashed-date", dest="date", required=True,
                        help="Date check was cashed by the NFA and for which the prediction is made. Format: yyyy-MM-DD",
                        metavar="DATE")
    parser.add_argument("-t", "--nfa-item-type", dest="type", default='Suppressor',
                        choices=['Suppressor', 'SBR', 'SBS', 'MG', 'AOW'],
                        help="Type of NFA item on which to resrict data.", metavar="TYPE")
    parser.add_argument('--plot-regression', dest='plot', action='store_true',
                        help="Plot a linear regression of data normalized around BASEDATE.")
    parser.add_argument('--no-plot-regression', dest='plot', action='store_false',
                        help="Default: does not plot linear regression.")
    parser.add_argument("-v", '--verbose', dest='verbose', action='store_true',
                        help="Print verbose information during execution.")
    parser.add_argument('--no-verbose', dest='verbose', action='store_false',
                        help="Default: does not print verbose information during execution.")
    parser.set_defaults(plot=False, verbose=False)
    return parser.parse_args()


def generate_dataframe(url):
    request = urllib2.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    response = urllib2.urlopen(request)
    return pd.read_csv(response, header=0)


def filter_dataframe(df, normalize_dates, filter_outliers):
    df.dropna(subset=['CheckCashed', 'Approved', 'NFAItem'], how='any', inplace=True)
    df['CheckCashed'] = df['CheckCashed'].apply(normalize_dates)
    df['Approved'] = df['Approved'].apply(normalize_dates)
    return df[df.apply(filter_outliers, axis=1)]


def plot_model_and_prediction(df, mean_prediction, date, *predictions):
    # Plot data
    ax = df.plot(x='CheckCashed', y='Approved', kind='scatter')

    # Plot prediction
    ax_plot = ft.partial(ax.plot, marker='d')
    ax_annotate = ft.partial(ax.annotate, textcoords='data')

    for i, prediction in enumerate(predictions):
        xy = (date, prediction)
        ax_plot(*xy)
        ax_annotate(xy=xy, s=' Prediction %d' % (i + 1))

    xy = (date, mean_prediction)
    ax_plot(*xy)
    ax_annotate(xy=xy, s='Mean Prediction')

    plt.show()


def generate_models(df):
    y, X = df['Approved'], df['CheckCashed']
    ols_model = sm.OLS(y, X, missing='raise')
    ols_model_intercept = sm.OLS(y, sm.add_constant(X, has_constant='raise'), missing='raise')
    return ols_model, ols_model_intercept


def predict_scalar_or_not(result, date):
    try:
        return result.predict(date)
    except ValueError:
        return result.predict((1, date))


def main():
    args = parse_args()
    basedate = pd.Timestamp(args.basedate)

    # Create partially applied functions
    norm = ft.partial(normalize_date_to_base, basedate=basedate)
    outliers = ft.partial(filter_outliers, item_type=args.type)
    convert_back_to_datetime = ft.partial(pd.to_datetime, origin=basedate, unit='D')
    date = norm(pd.Timestamp(args.date))
    predict = ft.partial(predict_scalar_or_not, date=date)

    # Get data
    df = generate_dataframe("http://www.nfatracker.com/wp-content/themes/smartsolutions/inc/export/")

    # Filter data
    df = filter_dataframe(df, norm, outliers)

    # Fit, print and plot as specified
    results = tuple(map(lambda x: x.fit(), generate_models(df)))

    predictions = tuple(map(predict, results))
    if args.verbose:
        for result, prediction in zip(results, tuple(map(np.vectorize(convert_back_to_datetime), predictions))):
            print result.summary()
            print 'Predicted approval date:', prediction

    prediction = np.mean(predictions)

    print 'Predicted approval date:', np.vectorize(convert_back_to_datetime)(prediction)

    if args.plot:
        plot_model_and_prediction(df, prediction, date, *predictions)


if __name__ == "__main__":
    main()
