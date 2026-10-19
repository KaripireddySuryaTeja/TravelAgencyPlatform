import axios from 'axios';
import { API_BASE_URL, TOKEN_KEY } from './client';

// Tour endpoints live at /tours/** (not /api/v1/**).
const toursClient = axios.create({
  baseURL: API_BASE_URL || '/',
  headers: { 'Content-Type': 'application/json' },
});

toursClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers = config.headers || {};
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  return config;
});

function normalize(err) {
  if (err.response?.data) {
    return { ...err.response.data, status: err.response.status };
  }
  return { status: 0, error: 'Network error' };
}

export async function getTourById(id) {
  try {
    const { data } = await toursClient.get(`/tours/${id}`);
    return data;
  } catch (err) {
    throw normalize(err);
  }
}

export async function getTourReviews(id, { sortBy = 'TOP_RATED_FIRST', page = 1, pageSize = 4 } = {}) {
  try {
    const params = new URLSearchParams({ sortBy, page: String(page), pageSize: String(pageSize) });
    const { data } = await toursClient.get(`/tours/${id}/reviews?${params}`);
    return data;
  } catch (err) {
    throw normalize(err);
  }
}
