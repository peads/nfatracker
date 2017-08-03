import numpy as np
import pandas as pd
import statsmodels.api as sm
import seaborn as sns
import matplotlib.pyplot as plt
import urllib2
import functools as ft

def normalize_date_to_base(date, basedate):
	try:
		return (pd.to_datetime(date) - basedate).days
	except (pd._libs.tslib.OutOfBoundsDatetime, ValueError):
		return normalize_date_to_base(pd.Timestamp.min, basedate)
def filter_outliers(row):
	return row['CheckCashed'] >= 0 and row['Approved'] >= 0 \
		and row['CheckCashed'] < row['Approved'] \
		and row['NFAItem'] == 'Suppressor' \
		and row['Approved'] - row['CheckCashed'] >= 14 \
		and row['FormType'] is not "Form 3 To Dealer"

basedate = pd.Timestamp('2016-01-01')
norm = ft.partial(normalize_date_to_base, basedate = basedate)
url = "http://www.nfatracker.com/wp-content/themes/smartsolutions/inc/export/"
response = urllib2.urlopen(url)
df = pd.read_csv(response, header = 0)


#Filter data
df['CheckCashed'] = df['CheckCashed'].apply(norm)
df['Approved'] = df['Approved'].apply(norm)
df = df[df.apply(filter_outliers, axis = 1)]

#Create model
X = df['CheckCashed']
y = df['Approved']
model = sm.OLS(y, X).fit()
predictions = model.predict(X)

#Plot linear regression
sns.lmplot(x='CheckCashed',y='Approved',data=df,fit_reg=True) 
#sns.regplot(x='CheckCashed',y='Approved', data=df, order=2)
plt.show()


