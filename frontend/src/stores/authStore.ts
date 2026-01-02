import { create } from 'zustand';
import { persist } from 'zustand/middleware';
import authService from '../services/authService';
import type { LoginRequest, RegisterRequest } from '../services/authService';

interface User {
  id: number;
  email: string;
  name: string;
}

interface AuthState {
  user: User | null;
  token: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
  setAuth: (user: User, token: string, refreshToken: string) => void;
  setTokenFromOAuth: (token: string) => void;
  login: (data: LoginRequest) => Promise<void>;
  register: (data: RegisterRequest) => Promise<void>;
  logout: () => void;
  clearError: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set, get) => ({
      user: null,
      token: null,
      refreshToken: null,
      isAuthenticated: false,
      isLoading: false,
      error: null,

      setAuth: (user, token, refreshToken) => {
        localStorage.setItem('token', token);
        localStorage.setItem('refreshToken', refreshToken);
        set({ user, token, refreshToken, isAuthenticated: true, error: null });
      },

      setTokenFromOAuth: (token: string) => {
        try {
          // Decode JWT payload to extract user info
          const payload = JSON.parse(atob(token.split('.')[1]));
          const user: User = {
            id: payload.userId || payload.sub,
            email: payload.email || payload.sub,
            name: payload.name || payload.email || 'User',
          };
          localStorage.setItem('token', token);
          set({ user, token, refreshToken: null, isAuthenticated: true, error: null });
        } catch {
          set({ error: 'Failed to process OAuth token' });
        }
      },

      login: async (data: LoginRequest) => {
        set({ isLoading: true, error: null });
        try {
          const response = await authService.login(data);
          get().setAuth(response.user, response.accessToken, response.refreshToken);
        } catch (err: unknown) {
          const errorMessage =
            (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
            'Login failed. Please try again.';
          set({ error: errorMessage, isLoading: false });
          throw err;
        } finally {
          set({ isLoading: false });
        }
      },

      register: async (data: RegisterRequest) => {
        set({ isLoading: true, error: null });
        try {
          const response = await authService.register(data);
          get().setAuth(response.user, response.accessToken, response.refreshToken);
        } catch (err: unknown) {
          const errorMessage =
            (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
            'Registration failed. Please try again.';
          set({ error: errorMessage, isLoading: false });
          throw err;
        } finally {
          set({ isLoading: false });
        }
      },

      logout: () => {
        authService.logout();
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
        set({
          user: null,
          token: null,
          refreshToken: null,
          isAuthenticated: false,
          error: null,
        });
      },

      clearError: () => set({ error: null }),
    }),
    {
      name: 'auth-storage',
      partialize: (state) => ({
        user: state.user,
        token: state.token,
        refreshToken: state.refreshToken,
        isAuthenticated: state.isAuthenticated,
      }),
    }
  )
);
