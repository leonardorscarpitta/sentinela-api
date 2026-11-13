package br.com.sentinelacidada;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SentinelaApplication {

	public static void main(String[] args) {
		SpringApplication.run(SentinelaApplication.class, args);
	}

}
