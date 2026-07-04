package com.interviewprep.config;

import java.io.IOException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpaFallbackController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> index() throws IOException {
        return resource("static/browser/index.html");
    }

    @GetMapping(value = "/{path:[^.]*}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> fallback(String path) throws IOException {
        return resource("static/browser/index.html");
    }

    @GetMapping(value = "/favicon.ico")
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.ok().build();
    }

    private ResponseEntity<Resource> resource(String location) throws IOException {
        ClassPathResource res = new ClassPathResource(location);
        if (res.exists() && res.isReadable()) {
            return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .body(res);
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
