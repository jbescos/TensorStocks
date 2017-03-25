package es.tododev.stocks.resource;

import java.util.concurrent.Executors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.tododev.stocks.tensor.LabelImage;

@RestController
public class Resource {

    @RequestMapping("/off")
	public ResponseEntity<String> switchOff(){
		Executors.newSingleThreadScheduledExecutor().execute(() -> {
			try {
				Thread.sleep(1000);
			} catch (Exception e) {}
			System.exit(0);
		});
		return ResponseEntity.ok("OK");
	}
    
    @RequestMapping("/test")
    public ResponseEntity<?> images(@RequestParam("model") String model, @RequestParam("image") String image) {
    	String result = LabelImage.test(new String[]{model, image});
    	return ResponseEntity.ok(result);
    }
	
}
