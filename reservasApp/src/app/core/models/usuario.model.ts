export const ROLES = [
  'ROLE_ADMIN',
  'ROLE_USER',
  'ROLE_PANCHITO'
] as const;

export type Rol = typeof ROLES[number];

export const ROL_LABELS: Record<Rol, string> = {
  ROLE_ADMIN: 'Administrador',
  ROLE_USER: 'Usuario',
  ROLE_PANCHITO: 'Panchito'
};

export const ROLES_CATALOGO: { id: Rol; label: string }[] = [
  { id: ROLES[0], label: ROL_LABELS[ROLES[0]] },
  { id: ROLES[1], label: ROL_LABELS[ROLES[1]] },
  { id: ROLES[2], label: ROL_LABELS[ROLES[2]] }
];

export interface UsuarioRequest {
  username: string;
  password?: string;
  roles: Rol[];
}

export interface UsuarioResponse {
  username: string;
  roles: Rol[];
}