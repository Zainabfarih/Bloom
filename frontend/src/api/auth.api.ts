import client from './client';
import type {
  LoginRequest,
  RegisterRequest,
  AuthResponse,
  PasswordResetRequest,
  PasswordUpdateRequest,
  RefreshTokenRequest,
  TokenValidationResponse,
  UserDTO,
  AdminStatsResponse,
} from '@/types';

export const authApi = {
  login: async (credentials: LoginRequest): Promise<AuthResponse> => {
    const response = await client.post('/auth/login', credentials);
    return response.data;
  },

  register: async (data: RegisterRequest): Promise<AuthResponse> => {
    const response = await client.post('/auth/register', data);
    return response.data;
  },

  refreshToken: async (req: RefreshTokenRequest): Promise<AuthResponse> => {
    const response = await client.post('/auth/refresh', req);
    return response.data;
  },

  /**
   * Logout — sends refreshToken to backend for revocation.
   * Backend endpoint: POST /api/auth/logout { refreshToken }
   */
  logout: async (refreshToken?: string | null): Promise<void> => {
    if (refreshToken) {
      try {
        await client.post('/auth/logout', { refreshToken });
      } catch {
        // Silently ignore — we always clear client state
      }
    }
  },

  /**
   * Initiate password reset — sends email.
   * Backend: POST /api/auth/password-reset/initiate
   */
  requestPasswordReset: async (email: string): Promise<void> => {
    await client.post('/auth/password-reset/initiate', { email } as PasswordResetRequest);
  },

  /**
   * Update password with reset token.
   * Backend: POST /api/auth/password-reset/update
   */
  updatePassword: async (data: PasswordUpdateRequest): Promise<void> => {
    await client.post('/auth/password-reset/update', data);
  },

  validateToken: async (): Promise<TokenValidationResponse> => {
    const response = await client.get('/tokens/validate');
    return response.data;
  },

  // User profile endpoints
  getUser: async (id: number): Promise<UserDTO> => {
    const response = await client.get(`/users/${id}`);
    return response.data;
  },

  updateUser: async (id: number, data: Partial<UserDTO>): Promise<UserDTO> => {
    const response = await client.put(`/users/${id}`, data);
    return response.data;
  },

  deleteUser: async (id: number): Promise<void> => {
    await client.delete(`/users/${id}`);
  },

  // Admin endpoints
  getAllUsers: async (): Promise<UserDTO[]> => {
    const response = await client.get('/users');
    return response.data;
  },

  /** Platform analytics for the admin dashboard. */
  getAdminStats: async (): Promise<AdminStatsResponse> => {
    const response = await client.get('/users/stats');
    return response.data;
  },

  recoverUser: async (id: number): Promise<void> => {
    await client.patch(`/users/${id}/recover`);
  },
};
