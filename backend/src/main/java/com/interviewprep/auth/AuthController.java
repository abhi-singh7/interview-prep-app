package com.interviewprep.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import com.interviewprep.config.JwtConfig;
import com.interviewprep.config.JwtService;
import com.interviewprep.domain.*;
import org.springframework.http.HttpHeaders;
import java.util.Map;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtConfig jwtConfig;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                          JwtService jwtService, JwtConfig jwtConfig) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtConfig = jwtConfig;
    }

    /**
     * Returns the authentication status of the current user.
     * 
     * <p>Checks for a valid JWT token in cookies and returns whether the request is authenticated.</p>
     * 
     * @param request HTTP servlet request containing cookies
     * @return ResponseEntity with AuthStatusResponse indicating authentication state (200 if authenticated, 401 if not)
     */
    @GetMapping("/status")
    public ResponseEntity<AuthStatusResponse> getAuthStatus(HttpServletRequest request) {
        String token = extractJwtToken(request);
        boolean authenticated = token != null && jwtService.isTokenValid(token);
        
        if (authenticated) {
            return ResponseEntity.ok(new AuthStatusResponse(true));
        } else {
            return ResponseEntity.status(401).body(new AuthStatusResponse(false));
        }
    }

    /**
     * Registers a new user account and returns authentication credentials.
     * 
     * <p>Creates a new User entity with encoded password, generates JWT token, and sets it as HttpOnly cookie.</p>
     * 
     * @param request Registration request containing name, email, and password
     * @return ResponseEntity with AuthResponse (200) or error message (400 if email already exists)
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        // FIX: blank name/email/password used to be accepted — an empty password is a valid
        // bcrypt hash that a later blank-password login would match. Reject blank fields and
        // malformed emails up front (frontend form validators are trivially bypassable).
        if (request.getName() == null || request.getName().isBlank()
                || request.getEmail() == null || request.getEmail().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()
                || !EMAIL_SHAPE.matcher(request.getEmail()).matches()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Name, a valid email and a non-empty password are required"));
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already in use"));
        }

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(user);

        // Generate JWT and set httpOnly cookie
        String token = jwtService.generateToken(request.getEmail(), user.getId());
        ResponseCookie cookie = ResponseCookie.from("jwt_token", token)
                .httpOnly(true)
                .secure(false)  // Set true in production with HTTPS
                .path("/")
                .maxAge((long) jwtConfig.getExpirationHours() * 60 * 60)
                .sameSite("Strict")
                .build();

        AuthResponse authResponse = new AuthResponse();
        authResponse.setUserId(user.getId());
        authResponse.setEmail(user.getEmail());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(authResponse);
    }

    /**
     * Authenticates a user with email and password credentials.
     * 
     * <p>Validates credentials against stored hash, generates JWT token, and sets it as HttpOnly cookie.</p>
     * 
     * @param request Login request containing email and password
     * @return ResponseEntity with AuthResponse (200) or error message (400 if invalid credentials)
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        // FIX: a blank password must never be compared against a stored hash.
        if (request.getEmail() == null || request.getEmail().isBlank()
                || request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid credentials"));
        }
        User user = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid credentials"));
        }

        String token = jwtService.generateToken(user.getEmail(), user.getId());
        ResponseCookie cookie = ResponseCookie.from("jwt_token", token)
                .httpOnly(true)
                .secure(false)  // Set true in production with HTTPS
                .path("/")
                .maxAge((long) jwtConfig.getExpirationHours() * 60 * 60)
                .sameSite("Strict")
                .build();

        AuthResponse authResponse = new AuthResponse();
        authResponse.setUserId(user.getId());
        authResponse.setEmail(user.getEmail());
        authResponse.setName(user.getName());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(authResponse);
    }

    /**
     * Logs out the current user by clearing the JWT token cookie.
     * 
     * <p>Sets the jwt_token cookie to expire immediately (maxAge=0), effectively removing it from the browser.</p>
     * 
     * @return ResponseEntity with success message (200) and expired cookie header
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        ResponseCookie deleteCookie = ResponseCookie.from("jwt_token", "")
                .httpOnly(true)
                .secure(false)
                .path("/")
                .maxAge(0)  // Expire immediately
                .sameSite("Strict")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, deleteCookie.toString())
                .body(Map.of("message", "Logged out successfully"));
    }

    /**
     * Extracts the JWT token from cookies in the HTTP request.
     * 
     * @param request HTTP servlet request containing cookies
     * @return JWT token string if found, null otherwise
     */
    private static final java.util.regex.Pattern EMAIL_SHAPE =
            java.util.regex.Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private String extractJwtToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("jwt_token".equals(cookie.getName())) return cookie.getValue();
            }
        }
        return null;
    }
}
