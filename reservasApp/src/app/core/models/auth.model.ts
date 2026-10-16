import { Rol } from "./usuario.model";

export interface AuthRequest {
  username: string;
  password?: string;
}

export interface AuthResponse {
  token: string;
}

export interface JwtPayload {
    // sujeto
  sub: string;
  // expiracion
  exp: number;
  
  roles: Rol[];
}