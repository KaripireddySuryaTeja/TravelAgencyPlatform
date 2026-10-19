import React, { useState, useCallback, useEffect } from 'react';
import { cancelBookingByAgent } from '../api/bookings';
import './AgentCancelModal.css';

const CANCEL_REASONS = [
  "Customer's Emergency",
  'Hotel Emergency',
  'Safety concerns',
  'Insufficient bookings',
];

function CloseIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
      <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
    </svg>
  );
}

export default function AgentCancelModal({ booking, onSuccess, onClose }) {
  const [reason, setReason] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const handleKey = useCallback((e) => {
    if (e.key === 'Escape') onClose();
  }, [onClose]);

  useEffect(() => {
    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, [handleKey]);

  const handleCancel = async () => {
    if (!reason) return;
    setSubmitting(true);
    setError('');
    try {
      await cancelBookingByAgent(booking.id, reason);
      onSuccess();
    } catch (err) {
      setError(err.response?.data?.message || 'Failed to cancel booking. Please try again.');
    } finally {
      setSubmitting(false);
    }
  };

  const tourDetails = booking.tourDetails || {};
  const customerDetails = booking.customerDetails || {};

  return (
    <div className="acm-overlay" onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="acm-modal" role="dialog" aria-modal="true">

        {/* Header */}
        <div className="acm-header">
          <h2 className="acm-title">Cancel booking</h2>
          <button className="acm-close" onClick={onClose} aria-label="Close">
            <CloseIcon />
          </button>
        </div>

        {/* Booking details */}
        <div className="acm-details">
          <div className="acm-detail-row">
            <span className="acm-detail-label">Customer</span>
            <span className="acm-detail-value">{customerDetails.name || '—'}</span>
          </div>
          <div className="acm-detail-row">
            <span className="acm-detail-label">Contact email</span>
            <span className="acm-detail-value">{customerDetails.email || '—'}</span>
          </div>
          {customerDetails.phone && (
            <div className="acm-detail-row">
              <span className="acm-detail-label">Phone</span>
              <span className="acm-detail-value">{customerDetails.phone}</span>
            </div>
          )}
          <div className="acm-detail-row">
            <span className="acm-detail-label">Tour name</span>
            <span className="acm-detail-value">{booking.name || '—'}</span>
          </div>
          <div className="acm-detail-row">
            <span className="acm-detail-label">Location</span>
            <span className="acm-detail-value">{booking.destination || '—'}</span>
          </div>
          <div className="acm-detail-row">
            <span className="acm-detail-label">Start date</span>
            <span className="acm-detail-value">{tourDetails.date || '—'}</span>
          </div>
          <div className="acm-detail-row">
            <span className="acm-detail-label">Duration</span>
            <span className="acm-detail-value">{booking.rawDuration || '—'}</span>
          </div>
          <div className="acm-detail-row">
            <span className="acm-detail-label">Meal plan</span>
            <span className="acm-detail-value">{tourDetails.mealPlan || '—'}</span>
          </div>
        </div>

        {/* Cancellation reason */}
        <div className="acm-reason-section">
          <label className="acm-reason-label" htmlFor="cancel-reason">Cancellation reason</label>
          <select
            id="cancel-reason"
            className="acm-reason-select"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
          >
            <option value="" disabled>Select a reason</option>
            {CANCEL_REASONS.map(r => (
              <option key={r} value={r}>{r}</option>
            ))}
          </select>
        </div>

        {/* Error */}
        {error && <p className="acm-error">{error}</p>}

        {/* Actions */}
        <div className="acm-actions">
          <button
            className="acm-btn-cancel"
            onClick={handleCancel}
            disabled={!reason || submitting}
          >
            {submitting ? 'Cancelling…' : 'Cancel the booking'}
          </button>
          <button className="acm-btn-keep" onClick={onClose}>
            Keep the booking
          </button>
        </div>
      </div>
    </div>
  );
}
