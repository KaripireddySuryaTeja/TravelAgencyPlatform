import axios from 'axios';
import { API_BASE_URL, TOKEN_KEY } from './client';

const reportsClient = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

reportsClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers = config.headers || {};
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  return config;
});

export async function getReportLocations() {
  const { data } = await reportsClient.get('/api/v1/reports/locations');
  return data;
}

export async function generateStaffPerformanceReport({ from, to, location }) {
  const params = new URLSearchParams({ from, to });
  if (location) params.append('location', location);
  const { data } = await reportsClient.get(`/api/v1/reports/staff-performance?${params}`);
  return data;
}

export async function generateSalesReport({ from, to, location }) {
  const params = new URLSearchParams({ from, to });
  if (location) params.append('location', location);
  const { data } = await reportsClient.get(`/api/v1/reports/sales?${params}`);
  return data;
}
