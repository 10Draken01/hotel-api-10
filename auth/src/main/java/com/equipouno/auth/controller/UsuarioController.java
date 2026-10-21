package com.equipouno.auth.controller;

import com.equipouno.auth.services.UsuarioService;
import com.equipouno.commons.controller.CrudController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.equipouno.auth.dto.UsuarioRequest;
import com.equipouno.auth.dto.UsuarioResponse;

@RestController
@RequestMapping("/admin/usuarios")
public class UsuarioController extends CrudController<UsuarioRequest, UsuarioResponse, UsuarioService> {
    public UsuarioController(UsuarioService service) {
        super(service);
    }
}