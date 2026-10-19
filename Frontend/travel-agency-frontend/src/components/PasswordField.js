import React, { useState } from 'react';

export default function PasswordField({ label, error, hint, id, ...rest }) {
  const [visible, setVisible] = useState(false);
  const inputId = id || `f-${label.toLowerCase().replace(/\s+/g, '-')}`;
  const errorId = `${inputId}-error`;
  return (
    <div className="field">
      <label className="field__label" htmlFor={inputId}>{label}</label>
      <div className="password-wrap">
        <input
          id={inputId}
          type={visible ? 'text' : 'password'}
          className={`field__input ${error ? 'field__input--error' : ''}`}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? errorId : undefined}
          {...rest}
        />
        <button
          type="button"
          className="password-toggle"
          onClick={() => setVisible((v) => !v)}
          aria-label={visible ? 'Hide password' : 'Show password'}
        >
          {visible ? (
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M17.94 17.94A10.06 10.06 0 0 1 12 20c-7 0-10-8-10-8a18.54 18.54 0 0 1 5.06-5.94" />
              <path d="M9.9 4.24A10 10 0 0 1 12 4c7 0 10 8 10 8a18.67 18.67 0 0 1-2.16 3.19" />
              <path d="M14.12 14.12a3 3 0 1 1-4.24-4.24" />
              <line x1="2" y1="2" x2="22" y2="22" />
            </svg>
          ) : (
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
              <path d="M2 12s3-8 10-8 10 8 10 8-3 8-10 8-10-8-10-8z" />
              <circle cx="12" cy="12" r="3" />
            </svg>
          )}
        </button>
      </div>
      {error
        ? <span id={errorId} className="field__error">{error}</span>
        : hint
          ? <span className="field__hint">{hint}</span>
          : null}
    </div>
  );
}
