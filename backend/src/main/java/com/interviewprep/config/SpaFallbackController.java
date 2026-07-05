package com.interviewprep.config;

import java.io.IOException;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controller that serves the Single Page Application (SPA) entry point for all non-static routes.
 *
 * <p>This controller ensures that client-side routing in the Angular/React frontend works correctly by returning
 * {@code index.html} for any URL path that does not match a static resource or API endpoint. Without this fallback,
 * refreshing a page at a deep link (e.g., {@code /interviews/123}) would result in a 404 from the server.</p>
 *
 * <h3>Routing Logic:</h3>
 * <ul>
 *   <li>{@code GET /} — Serves the main SPA entry point ({@code index.html}).</li>
 *   <li>{@code GET /{path}} (non-static) — Falls back to {@code index.html} for client-side route handling.
 *       The regex pattern {@code [^.]*} ensures that requests for static files (JS, CSS, images) are NOT caught here.</li>
 *   <li>{@code GET /favicon.ico} — Returns an empty 200 response to suppress browser favicon 404 errors.</li>
 * </ul>
 *
 * @see org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping
 */
@Controller
public class SpaFallbackController {

    /** The classpath location of the SPA's main HTML entry point. */
    private static final String INDEX_HTML = "static/browser/index.html";

    /**
     * Serves the SPA entry point at the root URL ({@code /}).
     *
     * @return The {@code index.html} resource as an HTTP 200 response, or a 500 error if the file is not found.
     * @throws IOException If the resource cannot be read from the classpath.
     */
    @GetMapping(value = "/", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> index() throws IOException {
        return resource(INDEX_HTML);
    }

    /**
     * Fallback handler for all non-static URL paths. Returns {@code index.html} to allow the client-side router
     * to handle navigation. The regex pattern {@code [^.]*} excludes requests ending with file extensions (e.g., .js, .css).
     *
     * @param path The captured URL path segment (excluding leading slash and any file extension).
     * @return The {@code index.html} resource as an HTTP 200 response, or a 500 error if the file is not found.
     * @throws IOException If the resource cannot be read from the classpath.
     */
    @GetMapping(value = "/{path:[^.]*}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> fallback(String path) throws IOException {
        return resource(INDEX_HTML);
    }

    /**
     * Handles favicon.ico requests by returning an empty 200 response.
     *
     * <p>This prevents the browser from logging 404 errors in server logs when requesting {@code /favicon.ico}.
     * The actual favicon is served as a static resource by Spring's resource handler.</p>
     *
     * @return An empty HTTP 200 response.
     */
    @GetMapping(value = "/favicon.ico")
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.ok().build();
    }

    /**
     * Loads a classpath resource and returns it as an HTTP 200 response if readable, or a 500 error otherwise.
     *
     * @param location The classpath location of the resource (e.g., {@code "static/browser/index.html"}).
     * @return A ResponseEntity containing the resource with HTML content type, or a 500 error status.
     * @throws IOException If the resource cannot be read from the classpath.
     */
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
