package com.draken.auth.services;

import com.draken.auth.dto.LoginRequest;
import com.draken.auth.dto.TokenResponse;

public interface AuthService {

    TokenResponse autenticar(LoginRequest request) throws Exception;
}