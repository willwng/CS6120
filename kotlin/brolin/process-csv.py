import pandas as pd

# Read the CSV into a DataFrame
df = pd.read_csv('out.csv', header=None, names=['benchmark', 'run', 'result'])

# Pivot the DataFrame to reorganize it
result_df = df.pivot(index='benchmark', columns='run')['result'].reset_index()
result_df.drop(columns=[result_df.columns[-1]], inplace=True)

# Rename the columns
result_df.columns = ['benchmark', 'baseline', 'ours']

# Fill NaN values with 0 if needed
result_df = result_df.fillna(0)

# Speedup
result_df['baseline'] = pd.to_numeric(result_df['baseline'])
result_df['ours'] = pd.to_numeric(result_df['ours'])
result_df['% speedup'] = ((result_df['baseline'] - result_df['ours']) / result_df['baseline']*100).round(1)

# Save the result to a new CSV file
result_df.to_csv('results.csv', index=False)
