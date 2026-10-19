import axios from 'axios';

export const TOKEN_KEY = 'ta.jwt';

const rawApiBaseUrl = process.env.REACT_APP_API_BASE_URL || '';
const DEFAULT_API_BASE_URL = 'http://localhost:8080';
export const API_BASE_URL = (rawApiBaseUrl || DEFAULT_API_BASE_URL).replace(/\/+$/, '');
export const API_V1_BASE_URL = API_BASE_URL ? `${API_BASE_URL}/api/v1` : '/api/v1';

export function buildApiUrl(path) {
  if (!API_BASE_URL) return path;
  if (!path.startsWith('/')) return `${API_BASE_URL}/${path}`;
  return `${API_BASE_URL}${path}`;
}

export const client = axios.create({
  baseURL: API_V1_BASE_URL,
});

client.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers = config.headers || {};
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  
  // Only set Content-Type for non-FormData requests
  if (!(config.data instanceof FormData)) {
    config.headers = config.headers || {};
    config.headers['Content-Type'] = 'application/json';
  }
  
  return config;
});

let onUnauthorized = null;

export function setUnauthorizedHandler(fn) {
  onUnauthorized = fn;
}

client.interceptors.response.use(
  (res) => res,
  (err) => {
    const sentAuth = Boolean(err.config?.headers?.Authorization);
    if (err.response?.status === 401 && sentAuth && onUnauthorized) {
      onUnauthorized();
    }
    return Promise.reject(err);
  }
);
