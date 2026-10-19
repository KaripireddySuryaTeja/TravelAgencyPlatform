import React from 'react';

export default function TextField({ label, error, hint, id, className, ...rest }) {
  const inputId = id || `f-${label.toLowerCase().replace(/\s+/g, '-')}`;
  const errorId = `${inputId}-error`;
  return (
    <div className="field">
      <label className="field__label" htmlFor={inputId}>{label}</label>
      <input
        id={inputId}
        className={`field__input ${error ? 'field__input--error' : ''} ${className || ''}`}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        {...rest}
      />
      {error
        ? <span id={errorId} className="field__error">{error}</span>
        : hint
          ? <span className="field__hint">{hint}</span>
          : null}
    </div>
  );
}
