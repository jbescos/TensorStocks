package es.tododev.stocks;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import es.tododev.stocks.chart.ChartGenerator;
import es.tododev.stocks.model.IModel;
import es.tododev.stocks.model.SimpleModel;
import es.tododev.stocks.utils.Utils;
import es.tododev.stocks.yahoo.CsvGenerator;

public class Main {

	private static final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
	private static final String ARG_GENERATE_CSV = "-generateCsv";
	private static final String ARG_HELP = "-help";
	private static final String ARG_YAHOO_API_KEY = "-yahooApiKey";
	private static final String ARG_CSV_FOLDER = "-csvFolder";
	private static final String ARG_GENERATE_CHART = "-generateChart";
	private static final String ARG_FROM = "-from";
	private static final String ARG_CREATEMODEL = "-createModel";
	
	public static void main(String[] args) throws Exception {
		parseArguments(args);
	}
	
	private static void parseArguments(String[] args) throws Exception {
		if (args.length == 0) {
			throw new IllegalArgumentException("No arguments provided. Run -help to see the options.");
		}
		String first = args[0];
		if (ARG_HELP.equals(first)) {
			// -help
		} else if (ARG_GENERATE_CSV.equals(first)) {
			// -generateCsv -yahooApiKey XXXXXX -csvFolder data
			String yahooApiKey = requireArg(args, 1, ARG_YAHOO_API_KEY, true);
			File output = new File(requireArg(args, 3, ARG_CSV_FOLDER, true));
			CsvGenerator csvGenerator = new CsvGenerator(yahooApiKey);
			csvGenerator.generateCsv(Utils.getSymbols(), output);
			System.out.println("CSVs generated under " + output.getAbsolutePath());
		} else if (ARG_GENERATE_CHART.equals(first)) {
			// -generateChart -csvFolder data -from 2021-01-01
			File csvRootFolder = new File(requireArg(args, 1, ARG_CSV_FOLDER, true));
			String from = requireArg(args, 3, ARG_FROM, true);
			ChartGenerator chartGenerator = new ChartGenerator(csvRootFolder);
			chartGenerator.generateChart(DATE_FORMAT.parse(from), new Date(Long.MAX_VALUE), "yyyy-MM-dd");
		} else if (ARG_CREATEMODEL.equals(first)) {
			// -createModel -csvFolder data
			File csvRootFolder = new File(requireArg(args, 1, ARG_CSV_FOLDER, true));
			IModel model = new SimpleModel(csvRootFolder);
			model.generateModel();
		} else {
			throw new IllegalArgumentException("Unkown argument: " + first + ". Run -help to see the options.");
		}
	}
	
	private static String requireArg(String[] args, int position, String required, boolean withValue) {
		if (args.length <= position) {
			throw new IllegalArgumentException("Missing argument: " + required);
		} else {
			String arg = args[position];
			if (!required.equals(arg)) {
				throw new IllegalArgumentException("Unexpected argument: " + arg + ". It is required: " + required);
			} else {
				if (!withValue) {
					return arg;
				} else {
					if (args.length <= position + 1) {
						throw new IllegalArgumentException("Argument: " + required + " requires a value after it. " + required + "<value>");
					} else {
						return args[position + 1];
					}
				}
			}
		}
	}


}
