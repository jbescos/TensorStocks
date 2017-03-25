package es.tododev.stocks;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Libraries {
	
	private final static String TEMP_FOLDER = System.getProperty("java.io.tmpdir")+File.separator+"nlib";
	
	static{
		try {
			Path path = Paths.get(TEMP_FOLDER);
			Files.createDirectories(path);
		} catch (IOException e) {
			throw new ExceptionInInitializerError(e);
		}
	}

	public static List<File> addNativeLibs(String ... libClassPaths) throws IOException{
		List<File> files = new ArrayList<>();
		for(String libClassPath : libClassPaths){
			File nativeLib = new File(TEMP_FOLDER+File.separator+libClassPath);
			nativeLib.deleteOnExit();
			try(InputStream input = Libraries.class.getResourceAsStream(libClassPath); OutputStream output = new FileOutputStream(nativeLib.getAbsolutePath());){
				int read = 0;
				byte[] bytes = new byte[1024];
				while ((read = input.read(bytes)) != -1) {
					output.write(bytes, 0, read);
				}
				System.load(nativeLib.getAbsolutePath());
				files.add(nativeLib);
			}
		}
		return files;
	}
	
}
