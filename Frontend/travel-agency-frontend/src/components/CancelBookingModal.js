import React, { useCallback, useEffect } from 'react';
import './CancelBookingModal.css';

function CloseIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
      <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
    </svg>
  );
}

export default function CancelBookingModal({ booking, onConfirmCancel, onKeep }) {
  const handleKey = useCallback((e) => {
    if (e.key === 'Escape') onKeep();
  }, [onKeep]);

  useEffect(() => {
    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, [handleKey]);

  // Determine free cancellation info
  // freeCancellationDate is the last day you can cancel for free (e.g. "2026-05-25")
  const freeCancelDate = booking?.freeCancellationDate;

  // Compare dates using string comparison (YYYY-MM-DD format is sortable)
  const todayStr = new Date().toISOString().split('T')[0]; // "YYYY-MM-DD"

  // Only two states: either free cancellation is still possible (yellow) or it's expired (red)
  // If freeCancelDate exists → compare with today
  // If freeCancelDate is null → no free cancellation policy, show expired banner
  const isFreeCancel = freeCancelDate ? todayStr <= freeCancelDate : false;

  // Format the free cancel date for display
  const formattedFreeCancelDate = freeCancelDate
    ? new Date(freeCancelDate + 'T00:00:00').toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' })
    : '';

  // Format booking date for display
  const formattedBookingDate = booking?.rawDate
    ? new Date(booking.rawDate + 'T00:00:00').toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' })
    : '';

  // Build guests string
  const adults = booking?.rawAdults || 1;
  const guestsStr = `${adults} adult${adults > 1 ? 's' : ''}`;

  // Meal plan
  const mealPlan = booking?.tourDetails?.mealPlan || booking?.rawMealPlan || '';

  // Duration
  const duration = booking?.rawDuration || '';

  return (
    <div className="cbm-overlay" onClick={(e) => { if (e.target === e.currentTarget) onKeep(); }}>
      <div className="cbm-modal" role="dialog" aria-modal="true">

        {/* Header */}
        <div className="cbm-header">
          <h2 className="cbm-title">Cancel</h2>
          <button className="cbm-close" onClick={onKeep} aria-label="Close">
            <CloseIcon />
          </button>
        </div>

        {/* Warning banner - always one of two: yellow (free) or red (expired) */}
        {isFreeCancel ? (
          <div className="cbm-banner cbm-banner-info">
            Free cancellation is possible until {formattedFreeCancelDate || 'the start date'}.
          </div>
        ) : (
          <div className="cbm-banner cbm-banner-warning">
            Please note, that the free cancelation period for this booking is over, the charges are non-refundable.
          </div>
        )}

        {/* Confirmation text */}
        <p className="cbm-description">
          Are you sure you want to cancel the tour at <strong>{booking?.name}</strong>, starting date{' '}
          <strong>{formattedBookingDate}{duration ? ` (${duration})` : ''}</strong>,{' '}
          <strong>{mealPlan}</strong> for <strong>{guestsStr}</strong>?
        </p>

        {/* Action buttons */}
        <div className="cbm-actions">
          <button className="cbm-btn-cancel" onClick={onConfirmCancel}>Cancel the booking</button>
          <button className="cbm-btn-keep" onClick={onKeep}>Keep the booking</button>
        </div>
      </div>
    </div>
  );
}

