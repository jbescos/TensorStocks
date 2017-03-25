package es.tododev.stocks;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.tensorflow.TensorFlow;

import es.tododev.stocks.util.Libraries;

@SpringBootApplication
public class DemoApplication {
	
	private final static Logger log = LogManager.getLogger();
	private static final String IMAGE = "/home/jbescos/Pictures/photo5953936953277786097.jpg";

	public static void main(String[] args) throws IOException {
		SpringApplication.run(DemoApplication.class, args);
		List<File> files = Libraries.addNativeLibs("/libtensorflow_jni.so");
		for(File file : files){
			log.debug("Included: "+file.getAbsolutePath());
		}
		log.debug("Loading library folder: "+System.getProperty("java.library.path"));
		log.debug("I'm using TensorFlow version: " +  TensorFlow.version());
	}
}
