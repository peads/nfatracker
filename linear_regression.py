import numpy as np
import pandas as pd
import statsmodels.api as sm
import seaborn as sns
import matplotlib.pyplot as plt
import urllibl

url = "http://www.nfatracker.com/wp-content/themes/smartsolutions/inc/export"
urllib.urlretrieve(url, "trasnfers.csv")

input_file = "/home/peads/Downloads/transfers_filter_filter_filter.csv"
df = pd.read_csv(input_file, header = 0)
basedate = pd.Timestamp('2016-01-01')
today = pd.to_datetime('today')

#Filter data
df['CheckCashed'] = df['CheckCashed'].apply(lambda x: (pd.to_datetime(x) - basedate).days)
df['Approved'] = df['Approved'].apply(lambda x: (pd.to_datetime(x) - basedate).days)
df = df[df.apply(lambda x: x['CheckCashed'] >= 0 and x['Approved'] >= 0 and x['CheckCashed'] < x['Approved'] and x['NFAItem'] == 'Suppressor', axis = 1)]

#Create model
X = df['CheckCashed']
y = df['Approved']
model = sm.OLS(y, X).fit()
predictions = model.predict(X)

#Plot linear regression
sns.lmplot(x='CheckCashed',y='Approved',data=df,fit_reg=True) 
plt.show()


