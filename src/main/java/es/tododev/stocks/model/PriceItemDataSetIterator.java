package es.tododev.stocks.model;

import java.util.List;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.DataSetPreProcessor;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.factory.Nd4j;

import es.tododev.stocks.chart.CsvColumns;
import es.tododev.stocks.yahoo.PriceItem;

public class PriceItemDataSetIterator implements DataSetIterator {

	private static final long serialVersionUID = 1L;
	private final List<PriceItem> training;
	private final int batch;
	// Only 1 is supported. More than 1 will require implementation changes in next(int num)
	private final int nextRelationLenght = 1;
	private int idx = 0;

	public PriceItemDataSetIterator(List<PriceItem> training, int batch) {
		this.training = training;
		this.batch = batch;
	}

	@Override
	public boolean hasNext() {
		return idx < training.size();
	}

	@Override
	public DataSet next() {
		return next(batch);
	}

	@Override
	public DataSet next(int num) {
		int next = idx + num;
		if (next >= training.size()) {
			next = training.size();
		}
		INDArray input = Nd4j.create(new int[] {(next - idx), inputColumns(), nextRelationLenght});
		INDArray output = Nd4j.create(new int[] {(next - idx), totalOutcomes(), nextRelationLenght});
		for (int i = 0; i < next - nextRelationLenght; i++) {
			PriceItem currentItem = training.get(i);
			PriceItem nextItem = training.get(i + nextRelationLenght);
			//  FIXME normalize it
			input.putScalar(new int[] {i, CsvColumns.date.getColumnIdx(), idx}, currentItem.getDate());
			input.putScalar(new int[] {i, CsvColumns.adjclose.getColumnIdx(), idx}, currentItem.getAdjclose());
			input.putScalar(new int[] {i, CsvColumns.close.getColumnIdx(), idx}, currentItem.getClose());
			input.putScalar(new int[] {i, CsvColumns.high.getColumnIdx(), idx}, currentItem.getHigh());
			input.putScalar(new int[] {i, CsvColumns.low.getColumnIdx(), idx}, currentItem.getLow());
			input.putScalar(new int[] {i, CsvColumns.open.getColumnIdx(), idx}, currentItem.getOpen());
			input.putScalar(new int[] {i, CsvColumns.volume.getColumnIdx(), idx}, currentItem.getVolume());
//			input.putScalar(new int[] {i, CsvColumns.symbol.getColumnIdx(), idx}, currentItem.getSymbol());
			output.putScalar(new int[] {i, CsvColumns.adjclose.getColumnIdx(), idx}, nextItem.getAdjclose());
			output.putScalar(new int[] {i, CsvColumns.close.getColumnIdx(), idx}, nextItem.getClose());
			output.putScalar(new int[] {i, CsvColumns.high.getColumnIdx(), idx}, nextItem.getHigh());
			output.putScalar(new int[] {i, CsvColumns.low.getColumnIdx(), idx}, nextItem.getLow());
			output.putScalar(new int[] {i, CsvColumns.open.getColumnIdx(), idx}, nextItem.getOpen());
			output.putScalar(new int[] {i, CsvColumns.volume.getColumnIdx(), idx}, nextItem.getVolume());
			idx++;
		}
		return new DataSet(input, output);
	}

	@Override
	public int inputColumns() {
		return CsvColumns.values().length;
	}

	@Override
	public int totalOutcomes() {
		// Output contains every value of PriceItem except the 'symbol' and 'date'
		return CsvColumns.values().length - 2;
	}

	@Override
	public boolean resetSupported() {
		return true;
	}

	@Override
	public boolean asyncSupported() {
		return false;
	}

	@Override
	public void reset() {
		idx = 0;
	}

	@Override
	public int batch() {
		return batch;
	}

	@Override
	public void setPreProcessor(DataSetPreProcessor preProcessor) {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public DataSetPreProcessor getPreProcessor() {
		throw new UnsupportedOperationException("Not implemented");
	}

	@Override
	public List<String> getLabels() {
		throw new UnsupportedOperationException("Not implemented");
	}

}
