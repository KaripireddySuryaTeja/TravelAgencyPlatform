import axios from 'axios';
import { client, API_V1_BASE_URL } from './client';

function normalize(err) {
  if (err.response?.data) {
    return { ...err.response.data, status: err.response.status };
  }
  return { status: 0, error: 'Network error', fieldErrors: {} };
}

/**
 * Get user profile by ID
 */
export async function getUserProfile(userId) {
  try {
    const { data } = await client.get(`/users/${userId}`);
    return data;
  } catch (err) {
    throw normalize(err);
  }
}

/**
 * Update user's first and last name
 */
export async function updateUserName(userId, firstName, lastName) {
  try {
    const { data } = await client.put(`/users/${userId}/name`, { firstName, lastName });
    return data;
  } catch (err) {
    throw normalize(err);
  }
}

/**
 * Update user's profile image (Base64)
 */
export async function updateUserImage(userId, imageBase64) {
  try {
    const { data } = await client.put(`/users/${userId}/image`, { imageBase64 });
    return data;
  } catch (err) {
    throw normalize(err);
  }
}

/**
 * Update user's password
 */
export async function updatePassword(userId, currentPassword, newPassword) {
  try {
    const { data } = await client.put(`/users/${userId}/password`, { currentPassword, newPassword });
    return data;
  } catch (err) {
    throw normalize(err);
  }
}

/**
 * Initiate email change (sends confirmation to new email)
 * Note: We use axios directly here to avoid the global 401 interceptor
 * that would log the user out when they enter a wrong password.
 */
export async function initiateEmailChange(userId, newEmail, password) {
  try {
    const token = localStorage.getItem('ta.jwt');
    const { data } = await axios.put(
      `${API_V1_BASE_URL}/users/${userId}/email`,
      { newEmail, password },
      {
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        }
      }
    );
    return data;
  } catch (err) {
    // Check if this is a wrong password error (401)
    if (err.response?.status === 401) {
      // Return a field error for the password field instead of triggering logout
      throw {
        status: 401,
        fieldErrors: { password: 'Current password is incorrect' },
        error: 'Invalid password'
      };
    }
    throw normalize(err);
  }
}

/**
 * Confirm email change with token (requires authentication)
 */
export async function confirmEmailChange(userId, confirmationToken) {
  try {
    const { data } = await client.post(`/users/${userId}/email/confirm`, { confirmationToken });
    return data;
  } catch (err) {
    throw normalize(err);
  }
}

/**
 * Confirm email change with token (public endpoint, no authentication required)
 */
export async function confirmEmailChangePublic(userId, confirmationToken) {
  try {
    // Use a separate axios instance without the auth interceptor
    const { data } = await axios.post(
      `${API_V1_BASE_URL}/users/${userId}/email/confirm`,
      { confirmationToken },
      { headers: { 'Content-Type': 'application/json' } }
    );
    return data;
  } catch (err) {
    throw normalize(err);
  }
}


