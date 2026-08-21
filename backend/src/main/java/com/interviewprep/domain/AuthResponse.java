package com.interviewprep.domain;

import lombok.Data;

@Data
public class AuthResponse {
    private Long userId;
    private String email;
    private String name;
}
