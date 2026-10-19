import React, { useState, useEffect, useCallback } from 'react';
import Header from '../components/Header';
import { getReviewsForModeration, setReviewVisibility } from '../api/admin';
import './FeedbackModerationPage.css';

const STARS = [1, 2, 3, 4, 5];

function StarIcon({ filled }) {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill={filled ? '#f59e0b' : 'none'}
      xmlns="http://www.w3.org/2000/svg" style={{ flexShrink: 0 }}>
      <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"
        stroke="#f59e0b" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function RatingStars({ rate }) {
  const rounded = Math.round(rate || 0);
  return (
    <span style={{ display: 'flex', gap: 2 }}>
      {STARS.map(s => <StarIcon key={s} filled={s <= rounded} />)}
    </span>
  );
}

function StatusBadge({ hidden }) {
  return (
    <span className={`fm-badge ${hidden ? 'fm-badge-hidden' : 'fm-badge-visible'}`}>
      {hidden ? 'Hidden' : 'Visible'}
    </span>
  );
}

export default function FeedbackModerationPage() {
  const [reviews, setReviews] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [filter, setFilter] = useState('ALL'); // ALL | VISIBLE | HIDDEN
  const [toggling, setToggling] = useState(null);
  const [toast, setToast] = useState('');

  const load = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const data = await getReviewsForModeration();
      setReviews(data);
    } catch (e) {
      setError('Failed to load reviews.');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  useEffect(() => {
    if (!toast) return;
    const t = setTimeout(() => setToast(''), 3000);
    return () => clearTimeout(t);
  }, [toast]);

  const handleToggle = async (review) => {
    setToggling(review.id);
    try {
      await setReviewVisibility(review.id, !review.hidden);
      setReviews(prev => prev.map(r =>
        r.id === review.id ? { ...r, hidden: !r.hidden } : r
      ));
      setToast(review.hidden ? 'Review is now visible to customers.' : 'Review hidden from customers.');
    } catch (e) {
      setToast('Failed to update review visibility.');
    } finally {
      setToggling(null);
    }
  };

  const filtered = reviews.filter(r => {
    if (filter === 'VISIBLE') return !r.hidden;
    if (filter === 'HIDDEN') return r.hidden;
    return true;
  });

  const counts = {
    all: reviews.length,
    visible: reviews.filter(r => !r.hidden).length,
    hidden: reviews.filter(r => r.hidden).length,
  };

  return (
    <div className="fm-page">
      <Header />
      <main className="fm-main">
        <h1 className="fm-title">Feedback Moderation</h1>

        {/* Filter tabs */}
        <div className="fm-filter-row">
          {[
            { key: 'ALL', label: `All (${counts.all})` },
            { key: 'VISIBLE', label: `Visible (${counts.visible})` },
            { key: 'HIDDEN', label: `Hidden (${counts.hidden})` },
          ].map(tab => (
            <button
              key={tab.key}
              className={`fm-filter-tab${filter === tab.key ? ' active' : ''}`}
              onClick={() => setFilter(tab.key)}
            >
              {tab.label}
            </button>
          ))}
          <button className="fm-refresh-btn" onClick={load} title="Refresh">
            ↻ Refresh
          </button>
        </div>

        {error && <p className="fm-error">{error}</p>}

        {loading ? (
          <p className="fm-loading">Loading reviews…</p>
        ) : filtered.length === 0 ? (
          <p className="fm-empty">No reviews found.</p>
        ) : (
          <div className="fm-table-wrap">
            <table className="fm-table">
              <thead>
                <tr>
                  <th>Tour</th>
                  <th>Reviewer</th>
                  <th>Rating</th>
                  <th>Comment</th>
                  <th>Date</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map(review => (
                  <tr key={review.id} className={review.hidden ? 'fm-row-hidden' : ''}>
                    <td className="fm-tour-name">{review.tourName}</td>
                    <td>{review.userName}</td>
                    <td>
                      <div className="fm-rating">
                        <RatingStars rate={review.rate} />
                        <span className="fm-rating-num">{review.rate?.toFixed(1)}</span>
                      </div>
                    </td>
                    <td className="fm-comment">{review.comment || <em className="fm-no-comment">No comment</em>}</td>
                    <td className="fm-date">{review.reviewDate || '—'}</td>
                    <td><StatusBadge hidden={review.hidden} /></td>
                    <td>
                      <button
                        className={`fm-action-btn ${review.hidden ? 'fm-btn-show' : 'fm-btn-hide'}`}
                        onClick={() => handleToggle(review)}
                        disabled={toggling === review.id}
                      >
                        {toggling === review.id ? '…' : review.hidden ? 'Show' : 'Hide'}
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </main>

      {toast && <div className="fm-toast">{toast}</div>}
    </div>
  );
}
