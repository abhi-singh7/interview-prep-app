package com.interviewprep;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.bind.annotation.CrossOrigin;

/**
 * Main entry point for the Interview Prep Platform application.
 * This class initializes the Spring Boot application, enabling retry mechanisms
 * and scheduled tasks.
 */
@SpringBootApplication
@EnableRetry
@EnableScheduling
public class InterviewPrepPlatformApplication {

    /**
     * Main method to launch the Spring Boot application.
     *
     * @param args Command-line arguments.
     */
    public static void main(String[] args) {
        SpringApplication.run(InterviewPrepPlatformApplication.class, args);
    }
}
