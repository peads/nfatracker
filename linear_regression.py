import numpy as np
import warnings

warnings.simplefilter(action='ignore', category=FutureWarning)
import pandas as pd
import statsmodels.api as sm
import matplotlib.pyplot as plt
import urllib2
import functools as ft
from argparse import ArgumentParser
from statsmodels.graphics import utils
from statsmodels.tools.tools import maybe_unwrap_results


def normalize_date_to_base(date, basedate):
    try:
        return (pd.to_datetime(date) - basedate).days
    except (pd._libs.tslib.OutOfBoundsDatetime, ValueError):
        return normalize_date_to_base(pd.Timestamp.min, basedate)


def filter_outliers(row, itemType):
    return row['CheckCashed'] >= 0 and row['Approved'] >= 0 \
           and row['CheckCashed'] < row['Approved'] \
           and row['NFAItem'] == itemType \
           and row['Approved'] - row['CheckCashed'] >= 14 \
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
    parser.set_defaults(plot=False)
    return parser.parse_args()


def plot_fit(results, exog_idx, y_true=None, ax=None, fittedname=None, **kwargs):
    fig, ax = utils.create_mpl_ax(ax)

    exog_name, exog_idx = utils.maybe_name_or_idx(exog_idx, results.model)
    results = maybe_unwrap_results(results)

    # maybe add option for wendog, wexog
    y = results.model.endog
    x1 = results.model.exog[:, exog_idx]
    x1_argsort = np.argsort(x1)
    y = y[x1_argsort]
    x1 = x1[x1_argsort]

    if not y_true is None:
        ax.plot(x1, y_true[x1_argsort], '-', label='True values')
    title = 'Fitted values versus %s' % exog_name

    # prstd, iv_l, iv_u = wls_prediction_std(results)
    ax.plot(x1, results.fittedvalues[x1_argsort], '-',
            label='fitted' if fittedname is None else fittedname, **kwargs)

    ax.set_title(title)
    ax.set_xlabel(exog_name)
    ax.set_ylabel(results.model.endog_names)
    ax.legend(loc='best', numpoints=1)

    return fig


def generate_dataframe(url):
    request = urllib2.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    response = urllib2.urlopen(request)
    return pd.read_csv(response, header=0)


def filter_dataframe(df, normalize_dates, filter_outliers):
    df['CheckCashed'] = df['CheckCashed'].apply(normalize_dates)
    df['Approved'] = df['Approved'].apply(normalize_dates)
    return df[df.apply(filter_outliers, axis=1)]


def plot_model_and_prediction(df, models, prediction, date):
    # Plot data
    ax = df.plot(x='CheckCashed', y='Approved', kind='scatter')

    # Plot models
    for i, model in enumerate(models):
        plot_fit(model, 0, ax=ax, fittedname="Fitted %d" % (i+1))
    ax_plot = ft.partial(ax.plot, marker='d')
    ax_annotate = ft.partial(ax.annotate, s='Prediction', textcoords='data')

    # Plot prediction
    xy = (date, prediction)
    ax_plot(*xy)
    ax_annotate(xy=xy)

    plt.show()


def generate_models(df):
    y, X = df['Approved'], df['CheckCashed']
    return (sm.OLS(y, X), sm.RLM(y, X, M=sm.robust.norms.HuberT()), sm.RLM(y, X, M=sm.robust.norms.AndrewWave()))


def main():
    args = parse_args()
    basedate = pd.Timestamp(args.basedate)

    # Create partially applied functions
    norm = ft.partial(normalize_date_to_base, basedate=basedate)
    outliers = ft.partial(filter_outliers, itemType=args.type)
    convert_back_to_datetime = ft.partial(pd.to_datetime, origin=basedate, unit='D')

    date = norm(pd.Timestamp(args.date))

    # Get data
    df = generate_dataframe("http://www.nfatracker.com/wp-content/themes/smartsolutions/inc/export/")

    # Filter data
    df = filter_dataframe(df, norm, outliers)

    # Fit, print and plot as specified
    models = tuple(map(lambda x: x.fit(), generate_models(df)))
    predictions = tuple(map(lambda model: model.predict(date), models))

    if args.verbose:
        for model, prediction in zip(models, tuple(map(np.vectorize(convert_back_to_datetime), predictions))):
            print model.summary()
            print 'Predicted approval date:', prediction

    prediction = np.mean(predictions)

    print 'Predicted approval date:', np.vectorize(convert_back_to_datetime)(prediction)

    if args.plot:
        plot_model_and_prediction(df, models, prediction, date)

if __name__ == "__main__":
    main()
