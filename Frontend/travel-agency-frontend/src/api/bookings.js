import { client } from './client';

export function getAgentBookings(agentId) {
  return client.get(`/bookings?agentId=${agentId}`).then(res => res.data);
}

export function cancelBookingByAgent(bookingId, reason) {
  return client.patch(`/bookings/${bookingId}/cancel`, { cancelReason: reason }).then(res => res.data);
}

export function verifyDocuments(bookingId) {
  return client.patch(`/bookings/${bookingId}/verify-documents`).then(res => res.data);
}

export function confirmBooking(bookingId) {
  return client.patch(`/bookings/${bookingId}/confirm`).then(res => res.data);
}

export function editBooking(bookingId, payload) {
  return client.put(`/bookings/${bookingId}`, payload).then(res => res.data);
}
