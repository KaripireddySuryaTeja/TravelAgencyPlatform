import React, { useState, useCallback, useEffect } from 'react';
import { verifyDocuments, confirmBooking } from '../api/bookings';
import DocumentViewerModal from './DocumentViewerModal';
import './CheckAndConfirmModal.css';

function CloseIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
      <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
    </svg>
  );
}

function DocLinkIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#0c6d8f" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/>
    </svg>
  );
}

export default function CheckAndConfirmModal({ booking, onSuccess, onClose }) {
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');
  const [showDocViewer, setShowDocViewer] = useState(false);

  const handleKey = useCallback((e) => {
    if (e.key === 'Escape') onClose();
  }, [onClose]);

  useEffect(() => {
    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, [handleKey]);

  const handleConfirm = async () => {
    if (documentCount < 1) {
      setError('At least one document must be uploaded before confirmation.');
      return;
    }
    setSubmitting(true);
    setError('');
    try {
      // If BOOKED, verify documents first, then confirm
      if (booking.state === 'BOOKED') {
        await verifyDocuments(booking.id);
      }
      await confirmBooking(booking.id);
      onSuccess();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to confirm booking. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  const tourDetails = booking.tourDetails || {};
  const customerDetails = booking.customerDetails || {};
  const documentCount = booking.documentCount || 0;

  return (
    <div className="ccm-overlay" onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="ccm-modal" role="dialog" aria-modal="true">

        {/* Header */}
        <div className="ccm-header">
          <div className="ccm-title-block">
            <h2 className="ccm-tour-name">{booking.name}</h2>
            <div className="ccm-tour-meta">
              {booking.rating && (
                <span className="ccm-rating">
                  <span className="ccm-star">★</span> {booking.rating}
                </span>
              )}
              {booking.destination && <span>{booking.destination}</span>}
            </div>
          </div>
          <button className="ccm-close" onClick={onClose} aria-label="Close">
            <CloseIcon />
          </button>
        </div>

        {/* Booking details */}
        <div className="ccm-details">
          <div className="ccm-detail-row">
            <span className="ccm-detail-label">Customer</span>
            <span className="ccm-detail-value">{customerDetails.name || '—'}</span>
          </div>
          <div className="ccm-detail-row">
            <span className="ccm-detail-label">Contact email</span>
            <span className="ccm-detail-value">{customerDetails.email || '—'}</span>
          </div>
          {customerDetails.phone && (
            <div className="ccm-detail-row">
              <span className="ccm-detail-label">Phone</span>
              <span className="ccm-detail-value">{customerDetails.phone}</span>
            </div>
          )}
          <div className="ccm-detail-row">
            <span className="ccm-detail-label">Start date</span>
            <span className="ccm-detail-value">{tourDetails.date || '—'}</span>
          </div>
          <div className="ccm-detail-row">
            <span className="ccm-detail-label">Duration</span>
            <span className="ccm-detail-value">{booking.rawDuration || '—'}</span>
          </div>
          <div className="ccm-detail-row">
            <span className="ccm-detail-label">Meal plan</span>
            <span className="ccm-detail-value">{tourDetails.mealPlan || '—'}</span>
          </div>
          <div className="ccm-detail-row">
            <span className="ccm-detail-label">Guests</span>
            <span className="ccm-detail-value">{tourDetails.guests || '—'}</span>
          </div>
        </div>

        {/* Documents */}
        <div className="ccm-docs-section">
          <span className="ccm-docs-title">Documents</span>
          {documentCount > 0 ? (
            <div className="ccm-docs-list">
              <button className="ccm-doc-link" onClick={() => setShowDocViewer(true)}>
                <DocLinkIcon /> {documentCount} document{documentCount !== 1 ? 's' : ''} uploaded
              </button>
            </div>
          ) : (
            <span className="ccm-no-docs">No documents uploaded yet</span>
          )}
        </div>

        {/* Total price */}
        {tourDetails.totalPrice && (
          <div className="ccm-total">
            <span className="ccm-total-label">Total price</span>
            <span className="ccm-total-value">{tourDetails.totalPrice}</span>
          </div>
        )}

        {/* Error */}
        {error && <p className="ccm-error">{error}</p>}

        {/* Confirm button */}
        <button
          className="ccm-btn-confirm"
          onClick={handleConfirm}
          disabled={submitting}
        >
          {submitting ? 'Confirming…' : 'Confirm'}
        </button>
      </div>

      {showDocViewer && (
        <DocumentViewerModal
          bookingId={booking.id}
          bookingName={booking.name}
          onClose={() => setShowDocViewer(false)}
        />
      )}
    </div>
  );
}
