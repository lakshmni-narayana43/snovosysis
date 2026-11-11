import axios from 'axios';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Attach Authorization Token to outgoing requests
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('ledgerline_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }

  // Automatically attach Idempotency-Key to mutating state calls if not manually provided
  if (['post', 'patch', 'put'].includes(config.method.toLowerCase())) {
    if (!config.headers['Idempotency-Key'] && !config.headers['idempotency-key']) {
      config.headers['Idempotency-Key'] = crypto.randomUUID();
    }
  }

  return config;
}, (error) => Promise.reject(error));

// Centralized error interceptor
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      // If unauthorized and not already on login page, remove token
      if (!window.location.pathname.includes('/login')) {
        localStorage.removeItem('ledgerline_token');
        localStorage.removeItem('ledgerline_user');
      }
    }
    return Promise.reject(error);
  }
);

export default api;

