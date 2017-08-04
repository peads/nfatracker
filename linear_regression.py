import numpy as np
import warnings
warnings.simplefilter(action='ignore', category=FutureWarning)
import pandas as pd
import statsmodels.api as sm
import seaborn as sns
import matplotlib.pyplot as plt
import urllib2
import functools as ft
from argparse import ArgumentParser


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
	parser = ArgumentParser(description="Predict approval data of a given NFA item based on NFATracker data.")
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
	
def plot_regression(df):
	#Plot linear regression
	sns.lmplot(x='CheckCashed',y='Approved',data=df,fit_reg=True) 
	#sns.regplot(x='CheckCashed',y='Approved', data=df, order=2)
	plt.show()

def main():
	args = parse_args()
	basedate = pd.Timestamp(args.basedate)
	
	#Create partial functions	
	norm = ft.partial(normalize_date_to_base, basedate = basedate)
	outliers = ft.partial(filter_outliers, itemType = args.type)
	convert_back_to_datetime = ft.partial(pd.to_datetime, origin=basedate, unit='D')

	#Get data
	url = "http://www.nfatracker.com/wp-content/themes/smartsolutions/inc/export/"
	response = urllib2.urlopen(url)
	df = pd.read_csv(response, header = 0)


	#Filter data
	df['CheckCashed'] = df['CheckCashed'].apply(norm)
	df['Approved'] = df['Approved'].apply(norm)
	df = df[df.apply(outliers, axis = 1)]

	#Create model
	X = df['CheckCashed']
	y = df['Approved']
	model = sm.OLS(y, X).fit()
	predictions = model.predict(X)

	#Convert to date
	print np.vectorize(convert_back_to_datetime)(model.predict(norm(pd.Timestamp(args.date))))

	if args.plot:
		plot_regression(df)

if __name__ == "__main__":
	main()
