export interface AuthResponse {
  token: string;
  expiresIn: number;
  userId: number;
  username: string;
  roles: string[];
}

export interface LoginRequest {
  username: string;
  password?: string;
}

export interface RegisterRequest {
  username: string;
  email: string;
  password?: string;
}
