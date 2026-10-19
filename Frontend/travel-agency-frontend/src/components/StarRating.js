import React from 'react';
import './StarRating.css';

function StarIcon({ filled }) {
  return filled ? (
    <svg
      width="28"
      height="28"
      viewBox="0 0 24 24"
      fill="#1596c8"
      stroke="#1596c8"
      strokeWidth="1.2"
      aria-hidden="true"
    >
      <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
    </svg>
  ) : (
    <svg
      width="28"
      height="28"
      viewBox="0 0 24 24"
      fill="none"
      stroke="#b0c8d8"
      strokeWidth="1.5"
      aria-hidden="true"
    >
      <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
    </svg>
  );
}

/**
 * Interactive 5-star rating component.
 *
 * Props:
 *   value    – current rating (0–5)
 *   onChange – (newValue) => void
 *   readOnly – disable interaction
 */
export default function StarRating({ value = 0, onChange, readOnly = false }) {
  return (
    <div className="star-rating" role="group" aria-label={`Rating: ${value} out of 5 stars`}>
      {[1, 2, 3, 4, 5].map((star) => (
        <button
          key={star}
          type="button"
          className={`star-btn ${star <= value ? 'filled' : 'empty'}`}
          onClick={() => !readOnly && onChange && onChange(star)}
          onKeyDown={(e) => {
            if ((e.key === 'Enter' || e.key === ' ') && !readOnly && onChange) {
              e.preventDefault();
              onChange(star);
            }
          }}
          disabled={readOnly}
          aria-label={`${star} star${star !== 1 ? 's' : ''}`}
          aria-pressed={star <= value}
          tabIndex={readOnly ? -1 : 0}
        >
          <StarIcon filled={star <= value} />
        </button>
      ))}
      <span className="star-count" aria-live="polite">
        {value}/5 stars
      </span>
    </div>
  );
}
