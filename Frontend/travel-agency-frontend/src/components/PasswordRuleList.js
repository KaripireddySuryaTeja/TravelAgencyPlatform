import React from 'react';

const ITEMS = [
  { key: 'capital', label: 'At least one uppercase letter required' },
  { key: 'special', label: 'At least one special character required' },
  { key: 'number', label: 'At least one number required' },
  { key: 'length', label: 'Password must be 8\u201364 characters long' },
  { key: 'notIdentity', label: 'Must not contain your name or email address' },
];

export default function PasswordRuleList({ rules, active = false }) {
  return (
    <ul className="rules" aria-label="Password requirements">
      {ITEMS.map(({ key, label }) => {
        const ok = rules[key];
        const fail = active && !ok;
        const cls = ok
          ? 'rules__item rules__item--ok'
          : fail
            ? 'rules__item rules__item--fail'
            : 'rules__item';
        return (
          <li key={key} className={cls}>
            <span className="rules__dot" aria-hidden="true">
              {ok ? (
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                  <polyline points="20 6 9 17 4 12" />
                </svg>
              ) : fail ? (
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                  <line x1="18" y1="6" x2="6" y2="18" />
                  <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
              ) : (
                <svg width="6" height="6" viewBox="0 0 6 6" aria-hidden="true">
                  <circle cx="3" cy="3" r="3" fill="currentColor" />
                </svg>
              )}
            </span>
            {label}
          </li>
        );
      })}
    </ul>
  );
}
