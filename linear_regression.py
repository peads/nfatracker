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
from statsmodels.sandbox.regression.predstd import wls_prediction_std

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
	parser.set_defaults(plot=False)
	return parser.parse_args()

def plot_fit(results, exog_idx, y_true=None, ax=None, **kwargs):
    fig, ax = utils.create_mpl_ax(ax)

    exog_name, exog_idx = utils.maybe_name_or_idx(exog_idx, results.model)
    results = maybe_unwrap_results(results)

    #maybe add option for wendog, wexog
    y = results.model.endog
    x1 = results.model.exog[:, exog_idx]
    x1_argsort = np.argsort(x1)
    y = y[x1_argsort]
    x1 = x1[x1_argsort]

    ax.plot(x1, y, 'bo', label=results.model.endog_names)
    if not y_true is None:
        ax.plot(x1, y_true[x1_argsort], 'b-', label='True values')
    title = 'Fitted values versus %s' % exog_name

    prstd, iv_l, iv_u = wls_prediction_std(results)
    ax.plot(x1, results.fittedvalues[x1_argsort], '-', color='r',
            label='fitted', **kwargs)

    ax.set_title(title)
    ax.set_xlabel(exog_name)
    ax.set_ylabel(results.model.endog_names)
    ax.legend(loc='best', numpoints=1)

    return fig

def main():
	args = parse_args()
	basedate = pd.Timestamp(args.basedate)
	date = pd.Timestamp(args.date)

	#Create partially applied functions	
	norm = ft.partial(normalize_date_to_base, basedate = basedate)
	outliers = ft.partial(filter_outliers, itemType = args.type)
	convert_back_to_datetime = ft.partial(pd.to_datetime, origin=basedate, unit='D')

	#Get data
	url = "http://www.nfatracker.com/wp-content/themes/smartsolutions/inc/export/"
	request = urllib2.Request(url, headers={ 'User-Agent': 'Mozilla/5.0' }) 
	response = urllib2.urlopen(request)
	df = pd.read_csv(response, header = 0)


	#Filter data
	df['CheckCashed'] = df['CheckCashed'].apply(norm)
	df['Approved'] = df['Approved'].apply(norm)
	df = df[df.apply(outliers, axis = 1)]

	#Create model
	X = df['CheckCashed']
	y = df['Approved']
	model = sm.OLS(y, X).fit()

	#Convert to date, and print
	date = norm(date)
	prediction = model.predict(date)
	print np.vectorize(convert_back_to_datetime)(prediction)

	if args.plot:
		ax = df.plot(x='CheckCashed',y='Approved',kind = 'scatter')
		fig = plot_fit(model, 0, ax=ax)
		ax.plot(date, prediction[0], marker='o', markerfacecolor='green', markersize=15)
		ax.annotate('Prediction', xy = (date, prediction[0]), textcoords='data')
		plt.show()
		#plot_regression(df, date, prediction[0])

if __name__ == "__main__":
	main()
