package com.equipouno.auth.services;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

import com.equipouno.commons.exceptions.ConflictoException;
import com.equipouno.commons.exceptions.RecursoNoEncontradoException;
import lombok.extern.java.Log;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.equipouno.auth.dto.UsuarioRequest;
import com.equipouno.auth.dto.UsuarioResponse;
import com.equipouno.auth.entities.Rol;
import com.equipouno.auth.entities.Usuario;
import com.equipouno.auth.mapper.UsuarioMapper;
import com.equipouno.auth.repositories.RolRepository;
import com.equipouno.auth.repositories.UsuarioRepository;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@Transactional
@AllArgsConstructor
@Slf4j
public class UsuarioServiceImpl implements UsuarioService {

    private final UsuarioRepository usuarioRepository;

    private final RolRepository rolRepository;

    private final UsuarioMapper usuarioMapper;

    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        log.info("Listado de todos los usuarios solicitado");
        return usuarioRepository.findAll().stream()
                .map(usuarioMapper::entityToResponse).toList();
    }

    @Override
    public UsuarioResponse obtenerPorId(Long id) {
        return usuarioMapper.entityToResponse(obtenerUsuario(id));
    }

    @Override
    public UsuarioResponse registrar(UsuarioRequest request) {
        log.info("Buscando usuario {}", request.username());
        validarUsuarioUnico(request.username());

        Set<Rol> roles = obtenerRoles(request.roles());

        Usuario usuario = usuarioMapper.requestToEntity(request,
                passwordEncoder.encode(request.password()), roles);

        usuario = usuarioRepository.saveAndFlush(usuario);
        return usuarioMapper.entityToResponse(usuario);
    }

    @Override
    public UsuarioResponse actualizar(UsuarioRequest request, Long id) {
        log.info("Actualizando usuario con id: {}", id);
        Usuario usuario = obtenerUsuario(id);

        validarUsuarioCambioUnico(request.username(), usuario.getUsername());

        Set<Rol> roles = obtenerRoles(request.roles());

        usuario.actualizar(request.username(), passwordEncoder.encode(request.password()), roles);
        usuarioRepository.saveAndFlush(usuario);
        log.info("Usuario con id: {} actualizado correctamente", id);

        return usuarioMapper.entityToResponse(usuario);
    }

    @Override
    public void eliminar(Long id) {
        log.info("Eliminando usuario con id: {}", id);

        Usuario usuario = obtenerUsuario(id);

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String usuarioAutenticado = authentication.getName();

        if (usuario.getUsername().equals(usuarioAutenticado)) {
            throw new ConflictoException(
                    "No puedes cruzificarte a ti mismo \uD83D\uDC80"
            );
        }

        usuarioRepository.delete(usuario);

        log.info("Usuario con id: {} crucificado correctamente", id);
    }

    private Usuario obtenerUsuario(Long id){
        log.info("Obteniendo usuario con id: {}", id);
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No se encontró el usuario con id: " + id));
    }

    private Set<Rol> obtenerRoles(Set<String> roles){
        log.info("Obteniendo roles: {}", roles);
        return roles.stream().map(rol ->
                        rolRepository.findByNombre(rol).orElseThrow(() ->
                                new RecursoNoEncontradoException("Rol " + rol + " no encontrado"))
                ).collect(Collectors.toSet());
    }

    private void validarUsuarioUnico(String username) {
        if (usuarioRepository.existsByUsername(username)) {
            throw new ConflictoException("El nombre de usuario " + username + " ya está en uso");
        }
    }

    private void validarUsuarioCambioUnico(String newUsername, String oldUsername) {
        if (usuarioRepository.existsByUsernameAndUsernameNot(newUsername, oldUsername)) {
            throw new ConflictoException("El nombre de usuario " + newUsername + " ya está en uso");
        }
    }
}