import { client } from './client';

export async function uploadDocument(bookingId, documentType, guestId, guestName, file) {
  const formData = new FormData();
  formData.append('documentType', documentType);
  if (guestId) {
    formData.append('guestId', guestId);
  }
  if (guestName) {
    formData.append('guestName', guestName);
  }
  formData.append('file', file);

  try {
    const response = await client.post(
      `/bookings/${bookingId}/documents/upload`,
      formData
      // Don't set Content-Type header - let axios/browser handle it with proper boundary
    );
    return response.data;
  } catch (error) {
    console.error('Upload error:', {
      status: error.response?.status,
      statusText: error.response?.statusText,
      data: error.response?.data,
      message: error.message,
    });
    throw error;
  }
}

export async function listBookingDocuments(bookingId) {
  try {
    const response = await client.get(`/bookings/${bookingId}/documents`);
    return response.data;
  } catch (error) {
    console.error('List documents error:', {
      status: error.response?.status,
      data: error.response?.data,
      message: error.message,
    });
    return [];
  }
}

export async function deleteDocument(bookingId, documentId) {
  await client.delete(`/bookings/${bookingId}/documents/${documentId}`);
}

export async function downloadDocument(bookingId, documentId) {
  const response = await client.get(
    `/bookings/${bookingId}/documents/${documentId}/download`,
    {
      responseType: 'blob',
    }
  );
  return response.data;
}
