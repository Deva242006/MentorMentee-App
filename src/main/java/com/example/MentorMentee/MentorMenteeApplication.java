package com.example.MentorMentee;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MentorMenteeApplication {

	public static void main(String[] args) {
		SpringApplication.run(MentorMenteeApplication.class, args);
	}

}
