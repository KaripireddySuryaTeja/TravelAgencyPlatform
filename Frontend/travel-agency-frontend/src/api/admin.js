import axios from 'axios';
import { API_BASE_URL, TOKEN_KEY } from './client';

const adminClient = axios.create({
  baseURL: API_BASE_URL,
  headers: { 'Content-Type': 'application/json' },
});

adminClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers = config.headers || {};
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  return config;
});

export async function getReviewsForModeration(tourId) {
  const params = tourId ? `?tourId=${encodeURIComponent(tourId)}` : '';
  const { data } = await adminClient.get(`/api/v1/admin/reviews${params}`);
  return data;
}

export async function setReviewVisibility(reviewId, hidden) {
  const { data } = await adminClient.patch(
    `/api/v1/admin/reviews/${reviewId}/visibility?hidden=${hidden}`
  );
  return data;
}
