import { client } from './client';

/**
 * Fetch existing feedback for a booking.
 * Returns null if no feedback exists (404).
 */
export async function getFeedback(bookingId) {
  try {
    const { data } = await client.get(`/bookings/${bookingId}/feedback`);
    return data;
  } catch (err) {
    if (err.response?.status === 404) return null;
    throw err;
  }
}

/**
 * Submit new feedback for a booking.
 */
export async function createFeedback(bookingId, { rating, comment }) {
  const { data } = await client.post(`/bookings/${bookingId}/feedback`, {
    rating,
    comment: comment || '',
  });
  return data;
}

/**
 * Update existing feedback for a booking.
 */
export async function updateFeedback(bookingId, { rating, comment }) {
  const { data } = await client.put(`/bookings/${bookingId}/feedback`, {
    rating,
    comment: comment || '',
  });
  return data;
}
