from tensorflow.python.keras.layers import Bidirectional, Dropout, Activation, Dense, LSTM

from tensorflow.keras.models import Sequential
from tensorflow.python.keras.callbacks import EarlyStopping

from sklearn.preprocessing import MinMaxScaler

from datetime import date

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd

data = pd.read_csv('C:/Users/jorge/Downloads/bitstampUSD_1-min_data_2012-01-01_to_2021-03-31.csv')

data['date'] = pd.to_datetime(data['Timestamp'], unit='s').dt.date

group = data.groupby('date')

day_price = group['Weighted_Price'].mean()

date1 = date(2012, 1, 1)
date2 = date(2022, 6, 25)

delta = date2 - date1
days_look = delta.days + 1


data = day_price[len(day_price) - days_look:len(day_price)]

scl = MinMaxScaler()

data = data.values.reshape(data.shape[0], 1)
scale_data = scl.fit_transform(data)

SEQ_LEN = 50
WINDOW_SIZE = SEQ_LEN - 1

BATCH_SIZE=64

DROPOUT = 0.2

def load_data(data_raw, seq_len):
    data = []

    for index in range(len(data_raw) - seq_len):
        data.append(data_raw[index: index + seq_len])

    data = np.array(data)
    train_split = 0.8

    num_data = data.shape[0]

    num_train = int(train_split * num_data)

    data = np.array(data);

    x_train = data[:num_train, :-1, :]

    y_train = data[:num_train, -1, :]

    x_test = data[num_train:, :-1, :]
    y_test = data[num_train:, -1, :]

    return [x_train, y_train, x_test, y_test]


x_train, y_train, x_test, y_test = load_data(scale_data, SEQ_LEN)

model = Sequential()

# First Layer
model.add(Bidirectional(LSTM(WINDOW_SIZE, return_sequences=True),
                        input_shape=(WINDOW_SIZE, x_train.shape[-1])))
model.add(Dropout(DROPOUT))

# Second Layer
model.add(Bidirectional(LSTM((WINDOW_SIZE * 2), return_sequences=True)))
model.add(Dropout(DROPOUT))

# Third Layer
model.add(Bidirectional(LSTM(WINDOW_SIZE, return_sequences=False)))

model.add(Dense(units=1))

# Set activation function
model.add(Activation('linear'))

# compile and fit the model
model.compile(loss='mean_squared_error', optimizer='adam')

history = model.fit(x_train, y_train, epochs=100, batch_size=BATCH_SIZE, shuffle=False,
                    validation_data=(x_test, y_test),
                    callbacks=[EarlyStopping(monitor='val_loss', min_delta=5e-5, patience=20, verbose=1)])

predict_prices = model.predict(x_test)

plt.plot(scl.inverse_transform(y_test), label="Actual Values", color='green')
plt.plot(scl.inverse_transform(predict_prices), label="Predicted Values", color='red')

plt.title('BitCoin price Prediction')
plt.xlabel('time [days]')
plt.ylabel('Price')
plt.legend(loc='best')

plt.show()