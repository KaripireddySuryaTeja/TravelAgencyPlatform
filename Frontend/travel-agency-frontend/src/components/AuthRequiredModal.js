import React, { useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import './AuthRequiredModal.css';

function LuggageIcon() {
  return (
    <svg width="48" height="48" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <rect x="6" y="6" width="12" height="15" rx="2" stroke="#0c6d8f" strokeWidth="1.8" />
      <path d="M9 6V4.5A1.5 1.5 0 0 1 10.5 3h3A1.5 1.5 0 0 1 15 4.5V6" stroke="#0c6d8f" strokeWidth="1.8" />
      <line x1="6" y1="11" x2="18" y2="11" stroke="#0c6d8f" strokeWidth="1.4" />
      <line x1="9" y1="21" x2="9" y2="23" stroke="#0c6d8f" strokeWidth="1.8" strokeLinecap="round" />
      <line x1="15" y1="21" x2="15" y2="23" stroke="#0c6d8f" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  );
}

function CloseIcon() {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <path d="M18 6L6 18" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
      <path d="M6 6L18 18" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

export default function AuthRequiredModal({ onClose }) {
  const navigate = useNavigate();
  const location = useLocation();

  useEffect(() => {
    const body = document.body;
    const html = document.documentElement;
    const previousBodyOverflow = body.style.overflow;
    const previousBodyPaddingRight = body.style.paddingRight;
    const previousHtmlOverflow = html.style.overflow;

    // Lock background scroll while modal is open and avoid horizontal layout shift.
    const scrollbarWidth = window.innerWidth - html.clientWidth;
    body.style.overflow = 'hidden';
    html.style.overflow = 'hidden';
    if (scrollbarWidth > 0) {
      body.style.paddingRight = `${scrollbarWidth}px`;
    }

    return () => {
      body.style.overflow = previousBodyOverflow;
      body.style.paddingRight = previousBodyPaddingRight;
      html.style.overflow = previousHtmlOverflow;
    };
  }, []);

  useEffect(() => {
    const onKeyDown = (e) => {
      if (e.key === 'Escape') onClose();
    };
    document.addEventListener('keydown', onKeyDown);
    return () => document.removeEventListener('keydown', onKeyDown);
  }, [onClose]);

  return (
    <div className="auth-required-overlay" onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="auth-required-modal" role="dialog" aria-modal="true" aria-label="Authentication required">
        <div className="auth-required-header">
          <div className="auth-required-brand">
            <LuggageIcon />
            <span>Travel Agency</span>
          </div>
          <button type="button" className="auth-required-close" aria-label="Close" onClick={onClose}>
            <CloseIcon />
          </button>
        </div>

        <p className="auth-required-message">To book a tour please sign in or create an account</p>

        <div className="auth-required-actions">
          <button
            type="button"
            className="auth-required-btn auth-required-btn-primary"
            onClick={() => navigate('/sign-in', { state: { from: location.pathname } })}
          >
            Sign in
          </button>
          <button
            type="button"
            className="auth-required-btn"
            onClick={() => navigate('/register')}
          >
            Create an account
          </button>
        </div>
      </div>
    </div>
  );
}
