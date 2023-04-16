package com.brunner.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ImportResource;
// import org.mybatis.spring.annotation.*;

@SpringBootApplication(scanBasePackages={"com.brunner.service", "com.brunner.server"})
@ImportResource("classpath:SpringConfig.xml")

// @MapperScan(basePackageClasses = BrunnerServerApplication.class)
public class BrunnerServerApplication {

	public static void main(String[] args) {
		System.setProperty("file.encoding", "UTF-8");
		SpringApplication.run(BrunnerServerApplication.class, args);

	}
}
