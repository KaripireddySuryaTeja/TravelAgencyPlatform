import React, { useEffect, useCallback } from 'react';
import './BookingConfirmation.css';

function CloseIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
      <line x1="18" y1="6" x2="6" y2="18"/>
      <line x1="6" y1="6" x2="18" y2="18"/>
    </svg>
  );
}

function formatCancelDate(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr + 'T00:00:00');
  return d.toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' });
}

export default function BookingConfirmation({ data, onClose }) {
  const handleKey = useCallback((e) => {
    if (e.key === 'Escape') onClose();
  }, [onClose]);

  useEffect(() => {
    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, [handleKey]);

  if (!data) return null;

  return (
    <div className="bc-overlay" onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="bc-modal" role="dialog" aria-modal="true">

        <div className="bc-header">
          <h2 className="bc-title">Booking confirmation</h2>
          <button className="bc-close" onClick={onClose} aria-label="Close">
            <CloseIcon />
          </button>
        </div>

        {data.freeCancelation && (
          <div className="bc-cancel-banner">
            Free cancellation is possible until {formatCancelDate(data.freeCancelation)}.
          </div>
        )}

        <div className="bc-details" dangerouslySetInnerHTML={{ __html: formatDetails(data.details) }} />

        <p className="bc-upload-note">
          Please upload your travel documents to the booking on the "My Tours" page and wait for the Travel Agent to contact you.
        </p>
      </div>
    </div>
  );
}

function formatDetails(details) {
  if (!details) return '';
  // Bold the key parts: tour name, date+duration, meal plan, guest count
  return details
    .replace(/at (.+?),/, 'at <strong>$1</strong>,')
    .replace(/starting date (.+?)\./, 'starting date <strong>$1</strong>.')
    .replace(/(Breakfast \(BB\)|Half Board|Full Board|All Inclusive|Bed & Breakfast|Half-board \(HB\)|Full-board \(HB\)|Room Only)/g, '<strong>$1</strong>')
    .replace(/for (\d+ adult[s]?)/g, 'for <strong>$1</strong>');
}

