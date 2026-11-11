import React, { createContext, useContext, useState, useEffect } from 'react';
import api from '../services/api';

const AuthContext = createContext(null);

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(() => {
    const saved = localStorage.getItem('ledgerline_user');
    return saved ? JSON.parse(saved) : null;
  });
  const [token, setToken] = useState(() => localStorage.getItem('ledgerline_token') || null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (token) {
      api.get('/auth/me')
        .then((res) => {
          if (res.data && res.data.data) {
            setUser(res.data.data);
            localStorage.setItem('ledgerline_user', JSON.stringify(res.data.data));
          }
        })
        .catch(() => {
          logout();
        })
        .finally(() => setLoading(false));
    } else {
      setLoading(false);
    }
  }, [token]);

  const login = async (username, password) => {
    const res = await api.post('/auth/login', { username, password });
    if (res.data && res.data.data) {
      const { token: jwt, user: userData } = res.data.data;
      setToken(jwt);
      setUser(userData);
      localStorage.setItem('ledgerline_token', jwt);
      localStorage.setItem('ledgerline_user', JSON.stringify(userData));
      return userData;
    }
    throw new Error('Invalid response structure');
  };

  const register = async (registerData) => {
    const res = await api.post('/auth/register', registerData);
    if (res.data && res.data.data) {
      const { token: jwt, user: userData } = res.data.data;
      setToken(jwt);
      setUser(userData);
      localStorage.setItem('ledgerline_token', jwt);
      localStorage.setItem('ledgerline_user', JSON.stringify(userData));
      return userData;
    }
    throw new Error('Registration failed');
  };

  const logout = () => {
    setToken(null);
    setUser(null);
    localStorage.removeItem('ledgerline_token');
    localStorage.removeItem('ledgerline_user');
  };

  return (
    <AuthContext.Provider value={{
      user,
      token,
      loading,
      isAuthenticated: !!token && !!user,
      role: user?.role || null,
      login,
      register,
      logout,
    }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => useContext(AuthContext);

