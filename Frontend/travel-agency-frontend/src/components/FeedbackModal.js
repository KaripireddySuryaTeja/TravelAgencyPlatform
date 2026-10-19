import React, { useState, useEffect, useRef, useCallback } from 'react';
import './FeedbackModal.css';
import StarRating from './StarRating';

const MAX_COMMENT = 500;

function CloseIcon() {
  return (
    <svg
      width="20"
      height="20"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2.5"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <line x1="18" y1="6" x2="6" y2="18" />
      <line x1="6" y1="6" x2="18" y2="18" />
    </svg>
  );
}

/**
 * FeedbackModal – create or update booking feedback.
 *
 * Props:
 *   booking         – the booking object (for display / id)
 *   existingFeedback – { rating, comment } | null (null → create mode)
 *   onClose          – () => void
 *   onSubmit         – ({ rating, comment }) => Promise<void>
 *   submitting       – boolean loading state driven by parent
 */
export default function FeedbackModal({
  booking,
  existingFeedback,
  onClose,
  onSubmit,
  submitting = false,
}) {
  const isUpdate = Boolean(existingFeedback);

  const [rating, setRating] = useState(existingFeedback?.rating ?? 0);
  const [comment, setComment] = useState(existingFeedback?.comment ?? '');
  const [touched, setTouched] = useState(false);

  const overlayRef = useRef(null);
  const firstInteractiveRef = useRef(null);

  /* ── validation ─────────────────────────────────────────────── */
  const needsComment = rating > 0 && rating <= 3;
  const commentMissing = needsComment && comment.trim() === '';
  const showError = touched && commentMissing;
  const isValid = rating >= 1 && rating <= 5 && !commentMissing;

  /* ── keyboard / focus ───────────────────────────────────────── */
  const handleKeyDown = useCallback(
    (e) => {
      if (e.key === 'Escape') onClose();
    },
    [onClose]
  );

  useEffect(() => {
    document.addEventListener('keydown', handleKeyDown);
    // Focus first interactive element on mount
    const t = setTimeout(() => firstInteractiveRef.current?.focus(), 60);
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      clearTimeout(t);
    };
  }, [handleKeyDown]);

  /* ── handlers ───────────────────────────────────────────────── */
  const handleOverlayClick = (e) => {
    if (e.target === overlayRef.current) onClose();
  };

  const handleRatingChange = (val) => {
    setRating(val);
    // Clear error as soon as user picks a rating that no longer needs comment
    if (val > 3) setTouched(false);
  };

  const handleCommentChange = (e) => {
    const val = e.target.value;
    if (val.length <= MAX_COMMENT) setComment(val);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setTouched(true);
    if (!isValid) return;
    await onSubmit({ rating, comment: comment.trim() });
  };

  /* ── render ─────────────────────────────────────────────────── */
  return (
    <div
      className="fbm-overlay"
      ref={overlayRef}
      onClick={handleOverlayClick}
      role="dialog"
      aria-modal="true"
      aria-labelledby="fbm-title"
    >
      <div className="fbm-modal">
        {/* ── Header ── */}
        <div className="fbm-header">
          <h2 className="fbm-title" id="fbm-title">
            Feedback
          </h2>
          <button
            className="fbm-close"
            onClick={onClose}
            aria-label="Close feedback modal"
            ref={firstInteractiveRef}
          >
            <CloseIcon />
          </button>
        </div>

        {/* ── Form ── */}
        <form className="fbm-form" onSubmit={handleSubmit} noValidate>
          {/* Star rating */}
          <div className="fbm-field">
            <label className="fbm-label">
              Please rate your experience
              <span className="fbm-required" aria-hidden="true">
                *
              </span>
            </label>
            <StarRating value={rating} onChange={handleRatingChange} />
          </div>

          {/* Comment */}
          <div className="fbm-field">
            <label className="fbm-label" htmlFor="fbm-comment">
              Comment
            </label>
            <div className="fbm-textarea-wrap">
              <textarea
                id="fbm-comment"
                className={`fbm-textarea${showError ? ' fbm-textarea--error' : ''}`}
                placeholder="Add your comments"
                value={comment}
                onChange={handleCommentChange}
                onBlur={() => setTouched(true)}
                rows={4}
                aria-describedby={showError ? 'fbm-comment-error' : undefined}
              />
              <div className="fbm-char-counter" aria-live="polite">
                {comment.length}/{MAX_COMMENT}
              </div>
            </div>
            {showError && (
              <p className="fbm-validation-msg" id="fbm-comment-error" role="alert">
                Comment is required for ratings of 3 stars or below.
              </p>
            )}
          </div>

          {/* Actions */}
          <div className="fbm-actions">
            <button
              type="button"
              className="fbm-btn fbm-btn--cancel"
              onClick={onClose}
              disabled={submitting}
            >
              Cancel
            </button>
            <button
              type="submit"
              className="fbm-btn fbm-btn--submit"
              disabled={submitting || rating === 0}
              aria-disabled={submitting || rating === 0}
            >
              {submitting ? 'Submitting…' : 'Submit'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
