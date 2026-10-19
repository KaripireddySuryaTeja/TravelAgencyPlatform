import React, { useState, useEffect, useCallback, useMemo } from 'react';
import './EditBookingModal.css';
import './AgentEditBookingModal.css';
import { buildApiUrl, TOKEN_KEY } from '../api/client';
import { editBooking } from '../api/bookings';

/* ── Icon helpers (reused from EditBookingModal) ─────────────── */
function StarIcon() {
  return (
    <svg width="15" height="15" viewBox="0 0 24 24" fill="#0c6d8f">
      <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
    </svg>
  );
}
function CalIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#4a7d96" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>
    </svg>
  );
}
function PersonIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#4a7d96" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
    </svg>
  );
}
function ForkIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#4a7d96" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 2v7c0 1.1.9 2 2 2h4a2 2 0 0 0 2-2V2"/><path d="M7 2v20"/><path d="M21 15V2a5 5 0 0 0-5 5v6c0 1.1.9 2 2 2h3zm0 0v7"/>
    </svg>
  );
}
function ChevronIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#4a7d96" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="6 9 12 15 18 9"/>
    </svg>
  );
}
function CloseIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
      <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
    </svg>
  );
}
function LockIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#7fa8be" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="11" width="18" height="11" rx="2"/><path d="M7 11V7a5 5 0 0 1 10 0v4"/>
    </svg>
  );
}

/* ── Helpers ──────────────────────────────────────────────────── */
function parseAmount(priceStr) {
  if (priceStr == null) return 0;
  if (typeof priceStr === 'number') return priceStr;
  return parseInt(String(priceStr).replace(/[^0-9]/g, ''), 10) || 0;
}

function getDays(duration) {
  const m = duration ? String(duration).match(/(\d+)/) : null;
  return m ? parseInt(m[1], 10) : 1;
}

function calcTotal(tourDetail, duration, mealPlan, adults) {
  if (!tourDetail || !duration) return null;
  const base = parseAmount(tourDetail.pricePerDuration?.[duration]);
  if (!base) return null;
  const supplement = parseAmount(tourDetail.mealSupplementsPerDay?.[mealPlan]);
  const days = getDays(duration);
  return (base * (adults || 1)) + supplement * days * (adults || 1);
}

/* ══════════════════════════════════════════════════════════════ */
export default function AgentEditBookingModal({ booking, onClose, onSaved }) {
  const NON_EDITABLE_STATES = ['CONFIRMED', 'STARTED', 'FINISHED', 'CANCELLED'];
  const isDisabled = NON_EDITABLE_STATES.includes(booking?.state);

  /* ── Fetch tour details for dropdown options ────────────── */
  const [tourDetail, setTourDetail] = useState(null);
  const [fetchLoading, setFetchLoading] = useState(true);

  useEffect(() => {
    if (!booking?.tourId) { setFetchLoading(false); return; }
    let cancelled = false;
    async function load() {
      try {
        const token = localStorage.getItem(TOKEN_KEY);
        const headers = token ? { Authorization: `Bearer ${token}` } : {};
        const res = await fetch(buildApiUrl(`/tours/${booking.tourId}`), { headers });
        if (res.ok) {
          const data = await res.json();
          if (!cancelled) setTourDetail(data);
        }
      } catch (e) {
        console.error('Failed to fetch tour details:', e);
      } finally {
        if (!cancelled) setFetchLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, [booking?.tourId]);

  /* ── State (pre-fill from booking) ─────────────────────── */
  const [duration, setDuration] = useState(booking.rawDuration || '');
  const [adults, setAdults] = useState(booking.rawAdults || 1);
  const [children, setChildren] = useState(booking.rawChildren || 0);
  const [guestsOpen, setGuestsOpen] = useState(false);
  const [mealPlan, setMealPlan] = useState(booking.rawMealPlan || '');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  /* Set default duration when tourDetail loads */
  useEffect(() => {
    if (tourDetail?.durations?.length && !duration) {
      setDuration(booking.rawDuration || tourDetail.durations[0]);
    }
  }, [tourDetail, duration, booking.rawDuration]);

  /* Set default meal plan when tourDetail loads */
  useEffect(() => {
    if (tourDetail?.mealPlans?.length && !mealPlan) {
      const firstPlan = tourDetail.mealPlans[0];
      const code = firstPlan.match(/\(([^)]+)\)$/)?.[1] || firstPlan;
      setMealPlan(booking.rawMealPlan || code);
    }
  }, [tourDetail, mealPlan, booking.rawMealPlan]);

  /* ── Derived values ────────────────────────────────────── */
  const maxAdults = tourDetail?.guestQuantity?.totalMaxValue || tourDetail?.guestQuantity?.adultsMaxValue || 10;
  const maxChildren = tourDetail?.guestQuantity?.childrenMaxValue || 6;
  const total = calcTotal(tourDetail, duration, mealPlan, adults);

  /* Dirty check — disable submit if nothing changed */
  const isDirty = useMemo(() => {
    if (isDisabled) return false;
    const durationChanged = duration !== (booking.rawDuration || '');
    const adultsChanged = adults !== (booking.rawAdults || 1);
    const childrenChanged = children !== (booking.rawChildren || 0);
    const mealChanged = mealPlan !== (booking.rawMealPlan || '');
    return durationChanged || adultsChanged || childrenChanged || mealChanged;
  }, [duration, adults, children, mealPlan, booking, isDisabled]);

  /* ── Close on Escape ───────────────────────────────────── */
  const handleKey = useCallback((e) => {
    if (e.key === 'Escape') onClose();
  }, [onClose]);

  useEffect(() => {
    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, [handleKey]);

  /* ── Submit ────────────────────────────────────────────── */
  async function handleSubmit(e) {
    e.preventDefault();
    if (isDisabled || !isDirty) return;
    setError('');

    /* Validation */
    if (adults < 1) {
      setError('At least 1 adult guest is required.');
      return;
    }
    if (!duration) {
      setError('Please select tour duration.');
      return;
    }
    if (!mealPlan) {
      setError('Please select a meal plan.');
      return;
    }

    const payload = {
      date: booking.rawDate,
      duration: duration,
      mealPlan: mealPlan,
      guests: { adult: adults, children: children },
      personalDetails: booking.personalDetails || [],
    };

    try {
      setLoading(true);
      await editBooking(booking.id, payload);
      onSaved();
    } catch (err) {
      const msg = err.response?.data?.message || err.response?.data || err.message;
      setError(typeof msg === 'string' ? msg : 'Update failed. Please try again.');
    } finally {
      setLoading(false);
    }
  }

  if (!booking) return null;

  return (
    <div className="ebm-overlay" onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="ebm-modal aebm-modal" role="dialog" aria-modal="true">

        {/* Close */}
        <button className="ebm-close" onClick={onClose} aria-label="Close">
          <CloseIcon />
        </button>

        {/* Header */}
        <div className="ebm-header">
          <div className="ebm-title-row">
            <span className="ebm-tour-name">{booking.name}</span>
            {booking.rating != null && booking.rating > 0 && (
              <span className="ebm-rating">
                <StarIcon /> {Number(booking.rating).toFixed(1)}
              </span>
            )}
          </div>
          <div className="ebm-destination">{booking.destination}</div>
        </div>

        {/* Disabled banner */}
        {isDisabled && (
          <div className="aebm-disabled-banner" role="alert">
            <LockIcon />
            <span>Editing is disabled — booking is {booking.state.toLowerCase().replace('_', ' ')}.</span>
          </div>
        )}

        {fetchLoading ? (
          <div className="ebm-loading" role="status" aria-live="polite">Loading tour details…</div>
        ) : (
          <form onSubmit={handleSubmit} className="aebm-form">

            {/* ── Tour days (duration) ─────────────────────── */}
            <div>
              <p className="ebm-section-title">Tour days</p>
              <div className={`ebm-select-row ${isDisabled ? 'aebm-disabled' : ''}`}>
                <CalIcon />
                <select
                  value={duration}
                  onChange={(e) => setDuration(e.target.value)}
                  disabled={isDisabled}
                >
                  {(tourDetail?.durations || [booking.rawDuration]).filter(Boolean).map((dur) => (
                    <option key={dur} value={dur}>{dur}</option>
                  ))}
                </select>
                <ChevronIcon />
              </div>
            </div>

            {/* ── Guest count ──────────────────────────────── */}
            <div>
              <p className="ebm-section-title">Guest count</p>
              <div className="ebm-guests-dropdown-wrap">
                <div
                  className={`ebm-select-row ${isDisabled ? 'aebm-disabled' : ''}`}
                  onClick={() => { if (!isDisabled) setGuestsOpen(o => !o); }}
                  style={{ cursor: isDisabled ? 'not-allowed' : 'pointer' }}
                >
                  <PersonIcon />
                  <span className="ebm-guests-summary">
                    {adults} adult{adults > 1 ? 's' : ''}{children > 0 ? `, ${children} child${children > 1 ? 'ren' : ''}` : ''}
                  </span>
                  <ChevronIcon />
                </div>
                {guestsOpen && !isDisabled && (
                  <div className="ebm-guests-panel">
                    <div className="ebm-stepper-row">
                      <span className="ebm-stepper-label">Adults</span>
                      <button type="button" className="ebm-stepper-btn" disabled={adults <= 1} onClick={() => setAdults(a => a - 1)} aria-label="Decrease adults">−</button>
                      <span className="ebm-stepper-value" aria-live="polite">{adults}</span>
                      <button type="button" className="ebm-stepper-btn ebm-stepper-btn-plus" disabled={adults >= maxAdults} onClick={() => setAdults(a => a + 1)} aria-label="Increase adults">+</button>
                    </div>
                    <div className="ebm-stepper-row">
                      <span className="ebm-stepper-label">Children</span>
                      <button type="button" className="ebm-stepper-btn" disabled={children <= 0} onClick={() => setChildren(c => c - 1)} aria-label="Decrease children">−</button>
                      <span className="ebm-stepper-value" aria-live="polite">{children}</span>
                      <button type="button" className="ebm-stepper-btn ebm-stepper-btn-plus" disabled={children >= maxChildren} onClick={() => setChildren(c => c + 1)} aria-label="Increase children">+</button>
                    </div>
                  </div>
                )}
              </div>
            </div>

            {/* ── Meal plan ────────────────────────────────── */}
            <div>
              <p className="ebm-section-title">Meal plan</p>
              <div className={`ebm-select-row ${isDisabled ? 'aebm-disabled' : ''}`}>
                <ForkIcon />
                <select
                  value={mealPlan}
                  onChange={(e) => setMealPlan(e.target.value)}
                  disabled={isDisabled}
                >
                  {(tourDetail?.mealPlans || [booking.rawMealPlan]).filter(Boolean).map((mp) => {
                    const code = mp.match(/\(([^)]+)\)$/)?.[1] || mp;
                    return <option key={mp} value={code}>{mp}</option>;
                  })}
                </select>
                <ChevronIcon />
              </div>
            </div>

            {/* ── Total price + submit ────────────────────── */}
            <div>
              <div className="ebm-total-row">
                <span className="ebm-total-label">Total price:</span>
                <span className="ebm-total-price">
                  {total != null ? `$${total.toLocaleString()}` : (booking.tourDetails?.totalPrice || '—')}
                </span>
              </div>

              {error && <div className="ebm-error">{error}</div>}

              {!isDisabled && (
                <button className="ebm-submit" type="submit" disabled={loading || !isDirty}>
                  {loading ? 'Saving…' : 'Save changes'}
                </button>
              )}
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
