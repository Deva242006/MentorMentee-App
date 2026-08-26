import { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { api, getToken, setToken } from './api';

// Auth context: holds the current user, exposes login/logout, and restores the
// session on refresh by calling /api/auth/me with the stored token.

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    let cancelled = false;
    async function restore() {
      if (!getToken()) {
        setLoading(false);
        return;
      }
      try {
        const me = await api.get('/api/auth/me');
        if (!cancelled) setUser(me);
      } catch {
        setToken(null);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }
    restore();
    return () => {
      cancelled = true;
    };
  }, []);

  const login = useCallback(async (email, password) => {
    const res = await api.post('/api/auth/login', { email, password });
    setToken(res.token);
    setUser(res.user);
    return res.user;
  }, []);

  const logout = useCallback(() => {
    setToken(null);
    setUser(null);
  }, []);

  // Let pages refresh the cached user (e.g. after connecting Google).
  const refresh = useCallback(async () => {
    const me = await api.get('/api/auth/me');
    setUser(me);
    return me;
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading, login, logout, refresh }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within AuthProvider');
  return ctx;
}

// Where each role lands after login.
export function homePathFor(user) {
  if (!user) return '/login';
  switch (user.role) {
    case 'ADMIN':
      return '/admin/users';
    case 'MENTOR':
      return '/mentor';
    case 'MENTEE':
      return '/mentee';
    default:
      return '/login';
  }
}
