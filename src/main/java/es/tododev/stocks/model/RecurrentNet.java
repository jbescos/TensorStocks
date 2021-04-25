package es.tododev.stocks.model;

import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.BackpropType;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.LSTM;
import org.deeplearning4j.nn.conf.layers.RnnOutputLayer;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.lossfunctions.LossFunctions;

public class RecurrentNet {

	private static final int SEED = 12345;

	private static final int LAYER1_SIZE = 256;
	private static final int LAYER2_SIZE = 256;
	private static final int DENSE_LAYER_SIZE = 32;
	private static final double DROPOUT_RATIO = 0.2;
	private static final int BPTT_LENGTH = 22;

	public static MultiLayerNetwork buildRecurrentNetwork(int nIn, int nOut) {
		MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder().seed(SEED)
				.optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT).weightInit(WeightInit.XAVIER)
				.updater(Updater.RMSPROP.getIUpdaterWithDefaultConfig()).l2(1e-4).list()
				.layer(0,
						new LSTM.Builder().nIn(nIn).nOut(LAYER1_SIZE).activation(Activation.TANH)
								.gateActivationFunction(Activation.HARDSIGMOID).dropOut(DROPOUT_RATIO).build())
				.layer(1,
						new LSTM.Builder().nIn(LAYER1_SIZE).nOut(LAYER2_SIZE).activation(Activation.TANH)
								.gateActivationFunction(Activation.HARDSIGMOID).dropOut(DROPOUT_RATIO).build())
				.layer(2,
						new DenseLayer.Builder().nIn(LAYER2_SIZE).nOut(DENSE_LAYER_SIZE).activation(Activation.RELU)
								.build())
				.layer(3,
						new RnnOutputLayer.Builder().nIn(DENSE_LAYER_SIZE).nOut(nOut).activation(Activation.IDENTITY)
								.lossFunction(LossFunctions.LossFunction.MSE).build())
				.backpropType(BackpropType.TruncatedBPTT).tBPTTForwardLength(BPTT_LENGTH)
				.tBPTTBackwardLength(BPTT_LENGTH).build();

		MultiLayerNetwork net = new MultiLayerNetwork(conf);
		net.init();
		net.setListeners(new ScoreIterationListener(100));
		return net;
	}
}
