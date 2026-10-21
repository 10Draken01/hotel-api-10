package com.equipouno.auth.services;

import com.equipouno.auth.dto.LoginRequest;
import com.equipouno.auth.dto.TokenResponse;

public interface AuthService {

    TokenResponse autenticar(LoginRequest request) throws Exception;
}