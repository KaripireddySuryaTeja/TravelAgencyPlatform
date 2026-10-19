import { client } from './client';

/**
 * Request a password reset code to be sent to the user's email
 * @param {string} email - User's email address
 */
export async function requestPasswordReset(email) {
  const response = await client.post('/auth/password-reset/request', { email });
  return response.data;
}

/**
 * Verify the password reset code
 * @param {string} email - User's email address
 * @param {string} code - Verification code
 */
export async function verifyResetCode(email, code) {
  const response = await client.post('/auth/password-reset/verify', { email, code });
  return response.data;
}

/**
 * Reset the user's password
 * @param {string} email - User's email address
 * @param {string} code - Verification code
 * @param {string} newPassword - New password
 */
export async function resetPassword(email, code, newPassword) {
  const response = await client.post('/auth/password-reset/reset', { 
    email, 
    code, 
    newPassword 
  });
  return response.data;
}
