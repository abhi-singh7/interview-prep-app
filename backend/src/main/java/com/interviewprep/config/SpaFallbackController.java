package com.interviewprep.config;

import java.io.IOException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.function.ServerResponse;

@Controller
public class SpaFallbackController {

    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public ServerResponse index() {
        return resource("static/browser/index.html");
    }

    @GetMapping(value = "/{path:[^.]*}", produces = MediaType.TEXT_HTML_VALUE)
    public ServerResponse fallback(String path) {
        return resource("static/browser/index.html");
    }

    private ServerResponse resource(String location) {
        try {
            ClassPathResource res = new ClassPathResource(location);
            if (res.exists()) {
                return ServerResponse.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .body(res.getInputStream());
            }
        } catch (IOException e) {
            // fall through to not found
        }
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}
