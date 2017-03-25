package es.tododev.stocks;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.tensorflow.TensorFlow;

public class Main {

	private static final String IMAGE = "/home/jbescos/Pictures/photo5953936953277786097.jpg";
	
	public static void main(String[] args) throws IOException {
		List<File> files = Libraries.addNativeLibs("/libtensorflow_jni.so");
		for(File file : files){
			System.out.println("Included: "+file.getAbsolutePath());
		}
		System.out.println("Loading library folder: "+System.getProperty("java.library.path"));
		System.out.println("I'm using TensorFlow version: " +  TensorFlow.version());
		testImageLabel();
	}
	
	public static void testImageLabel(){
		LabelImage.main(new String[]{"/home/jbescos/workspace/TensorStocks/model/inception5h", IMAGE});
	}

}
