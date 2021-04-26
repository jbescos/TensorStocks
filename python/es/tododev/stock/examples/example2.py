import tensorflow as tf
import numpy as np
import os
from test.test_audioop import datas

os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'

x_input = np.random.sample((1,2))
print(x_input)

x = tf.placeholder(tf.float32, shape=[1,2], name = "X")

dataset = tf.data.Dataset.from_tensor_slices(x)

iterator = dataset.make_initializable_iterator()
get_next = iterator.get_next()

with tf.Session() as session:
    session.run(iterator.initializer, feed_dict={x : x_input})
    print(session.run(get_next))