package com.interviewprep;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.CrossOrigin;

@SpringBootApplication
@EnableRetry
@EnableScheduling
public class InterviewPrepPlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(InterviewPrepPlatformApplication.class, args);
    }
}
