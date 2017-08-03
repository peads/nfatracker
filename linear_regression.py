import numpy as np
import pandas as pd
import statsmodels.api as sm
import seaborn as sns
import matplotlib.pyplot as plt
import urllib2

def normalize_dates_to(basedate, date):
	try:
		return (pd.to_datetime(date) - basedate).days
	except (pd._libs.tslib.OutOfBoundsDatetime, ValueError):
		return normalize_dates_to(basedate, pd.Timestamp.min)

url = "http://www.nfatracker.com/wp-content/themes/smartsolutions/inc/export/"
response = urllib2.urlopen(url)
df = pd.read_csv(response, header = 0)
basedate = pd.Timestamp('2016-01-01')
today = pd.to_datetime('today')

#Filter data
df['CheckCashed'] = df['CheckCashed'].apply(lambda x: normalize_dates_to(basedate, x))
df['Approved'] = df['Approved'].apply(lambda x: normalize_dates_to(basedate, x))

df = df[df.apply(lambda x: x['CheckCashed'] >= 0 and x['Approved'] >= 0 and x['CheckCashed'] < x['Approved'] and x['NFAItem'] == 'Suppressor', axis = 1)]

#Create model
X = df['CheckCashed']
y = df['Approved']
model = sm.OLS(y, X).fit()
predictions = model.predict(X)

#Plot linear regression
sns.lmplot(x='CheckCashed',y='Approved',data=df,fit_reg=True) 
plt.show()


