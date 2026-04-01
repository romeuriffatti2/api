package com.example.cert;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.CommandLineRunner;
import com.example.cert.repository.MagazineRepository;

@SpringBootApplication
public class CertApplication {

	static {
		Dotenv dotenv = Dotenv.configure()
				.directory("./")
				.ignoreIfMalformed()
				.ignoreIfMissing()
				.load();
		dotenv.entries().forEach(entry -> {
			if (System.getProperty(entry.getKey()) == null) {
				System.setProperty(entry.getKey(), entry.getValue());
			}
		});
	}

	public static void main(String[] args) {
		SpringApplication.run(CertApplication.class, args);
	}

	@Bean
	public CommandLineRunner initMockCnpj(MagazineRepository magazineRepository) {
		return args -> {
			magazineRepository.findAll().forEach(magazine -> {
				if (magazine.getCnpj() == null || magazine.getCnpj().isEmpty()) {
					magazine.setCnpj("00.000.000/0000-00");
					magazineRepository.save(magazine);
				}
			});
		};
	}
}
