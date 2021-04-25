package es.tododev.stocks.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.util.ModelSerializer;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;

import es.tododev.stocks.chart.CsvColumns;
import es.tododev.stocks.chart.CsvParser;
import es.tododev.stocks.yahoo.PriceItem;

public class RecursiveModel implements IModel {

	private static final float TRAIN_CHUNK = 0.9f;
	private static final int EPOCHS = 100;
	private final File csvRootFolder;
	private final List<PriceItem> testing = new ArrayList<>();
	private final List<PriceItem> training = new ArrayList<>();

	public RecursiveModel(File csvRootFolder) {
		this.csvRootFolder = csvRootFolder;
	}

	@Override
	public void generateModel() throws IOException {
		loadData();
		DataSetIterator iterator = new PriceItemDataSetIterator(training, 50);
		MultiLayerNetwork net = RecurrentNet.buildRecurrentNetwork(iterator.inputColumns(), iterator.totalOutcomes());
		net.fit(iterator, EPOCHS);
		ModelSerializer.writeModel(net, csvRootFolder.getAbsolutePath() + "/" + new Date().getTime() + ".zip", false);
	}

	private void loadData() throws IOException {
		File[] symbols = csvRootFolder.listFiles();
		for (File symbol : symbols) {
			if (symbol.isDirectory()) {
				List<PriceItem> results = new LinkedList<>();
				for (File csv : symbol.listFiles()) {
					if (csv.getName().endsWith(".csv")) {
						List<PriceItem> csvList = CsvParser.getRows(csv, true, Integer.MAX_VALUE, line -> {
							String[] cols = line.split(",");
							PriceItem item = new PriceItem();
							for (CsvColumns csvCol : CsvColumns.values()) {
								csvCol.addValue(item, cols);
							}
							return item;
						});
						results.addAll(csvList);
					}
				}
				setTrainAndTest(results);
			}
		}
		Collections.shuffle(training);
		Collections.shuffle(testing);
	}
	
	private void setTrainAndTest(List<PriceItem> results) {
		Collections.shuffle(results);
		int trainingList = (int) (results.size() * TRAIN_CHUNK);
		training.addAll(results.subList(0, trainingList));
		testing.addAll(results.subList(trainingList, results.size()));
	}
}
