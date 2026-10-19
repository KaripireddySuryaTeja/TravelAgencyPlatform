import React, { useCallback, useEffect } from 'react';
import './ConfirmChangesModal.css';

function CloseIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
      <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
    </svg>
  );
}

export default function ConfirmChangesModal({ booking, changes, onConfirm, onDecline }) {
  const handleKey = useCallback((e) => {
    if (e.key === 'Escape') onDecline();
  }, [onDecline]);

  useEffect(() => {
    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, [handleKey]);

  // Format the booking date for display
  const formattedDate = booking?.rawDate
    ? new Date(booking.rawDate + 'T00:00:00').toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' })
    : '';

  return (
    <div className="ccm-overlay" onClick={(e) => { if (e.target === e.currentTarget) onDecline(); }}>
      <div className="ccm-modal" role="dialog" aria-modal="true">

        {/* Header */}
        <div className="ccm-header">
          <h2 className="ccm-title">Confirm tour booking changes</h2>
          <button className="ccm-close" onClick={onDecline} aria-label="Close">
            <CloseIcon />
          </button>
        </div>

        {/* Description */}
        <p className="ccm-description">
          Your booking for <strong>{booking?.name}</strong> on <strong>{formattedDate}</strong> has been updated by the travel agent.
        </p>

        {/* Changes list */}
        {changes && changes.length > 0 && (
          <div className="ccm-changes">
            <p className="ccm-changes-title">Changes:</p>
            <ul className="ccm-changes-list">
              {changes.map((change, i) => (
                <li key={i}>{change}</li>
              ))}
            </ul>
          </div>
        )}

        {/* Footer text */}
        <p className="ccm-footer-text">
          All other details of this booking remain the same.<br/>
          Please review the changes and confirm your updated booking, or contact us if you need further assistance.
        </p>

        {/* Action buttons */}
        <div className="ccm-actions">
          <button className="ccm-btn-decline" onClick={onDecline}>Decline changes</button>
          <button className="ccm-btn-confirm" onClick={onConfirm}>Confirm changes</button>
        </div>
      </div>
    </div>
  );
}

