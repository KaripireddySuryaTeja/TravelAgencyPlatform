import React, { useEffect } from 'react';

export default function Toast({ title, message, onClose, autoDismissMs = 5000 }) {
  useEffect(() => {
    if (!autoDismissMs) return;
    const id = window.setTimeout(onClose, autoDismissMs);
    return () => window.clearTimeout(id);
  }, [autoDismissMs, onClose]);

  return (
    <div role="status" className="toast">
      <svg className="toast__icon" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
        <circle cx="12" cy="12" r="10" />
        <polyline points="8 12 11 15 16 9" />
      </svg>
      <div>
        <div className="toast__title">{title}</div>
        <div className="toast__body">{message}</div>
      </div>
      <button className="toast__close" onClick={onClose} aria-label="Dismiss">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
          <line x1="18" y1="6" x2="6" y2="18" />
          <line x1="6" y1="6" x2="18" y2="18" />
        </svg>
      </button>
    </div>
  );
}
