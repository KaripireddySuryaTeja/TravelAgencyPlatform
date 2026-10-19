import React, { useState, useEffect, useCallback } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import './BookingModal.css';
import { buildApiUrl, client } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import BookingConfirmation from './BookingConfirmation';

/* ── tiny icon helpers ───────────────────────────────────────── */
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
      <rect x="3" y="4" width="18" height="18" rx="2"/>
      <line x1="16" y1="2" x2="16" y2="6"/>
      <line x1="8" y1="2" x2="8" y2="6"/>
      <line x1="3" y1="10" x2="21" y2="10"/>
    </svg>
  );
}

function PersonIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#4a7d96" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
      <circle cx="12" cy="7" r="4"/>
    </svg>
  );
}

function ForkIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#4a7d96" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 2v7c0 1.1.9 2 2 2h4a2 2 0 0 0 2-2V2"/>
      <path d="M7 2v20"/>
      <path d="M21 15V2a5 5 0 0 0-5 5v6c0 1.1.9 2 2 2h3zm0 0v7"/>
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
      <line x1="18" y1="6" x2="6" y2="18"/>
      <line x1="6" y1="6" x2="18" y2="18"/>
    </svg>
  );
}

function LuggageIcon() {
  return (
    <svg width="34" height="34" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <rect x="6" y="6" width="12" height="15" rx="2" stroke="#0c6d8f" strokeWidth="1.8"/>
      <path d="M9 6V4.5A1.5 1.5 0 0 1 10.5 3h3A1.5 1.5 0 0 1 15 4.5V6" stroke="#0c6d8f" strokeWidth="1.8"/>
      <line x1="6" y1="11" x2="18" y2="11" stroke="#0c6d8f" strokeWidth="1.4"/>
      <line x1="9" y1="21" x2="9" y2="23" stroke="#0c6d8f" strokeWidth="1.8" strokeLinecap="round"/>
      <line x1="15" y1="21" x2="15" y2="23" stroke="#0c6d8f" strokeWidth="1.8" strokeLinecap="round"/>
    </svg>
  );
}

/* ── helpers ─────────────────────────────────────────────────── */
function fmtDate(dateVal) {
  if (!dateVal) return '';
  const d = new Date(dateVal);
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}

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
  const total = (base * (adults || 1)) + supplement * days * (adults || 1);
  return total;
}

/* ══════════════════════════════════════════════════════════════ */
export default function BookingModal({ tour, onClose }) {
  const { user } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();
  const currentPath = `${location.pathname}${location.search}${location.hash}`;

  /* ── fetch full tour details ─────────────────────────────── */
  const [tourDetail, setTourDetail] = useState(null);
  const [fetchLoading, setFetchLoading] = useState(true);

  useEffect(() => {
    if (!user || !tour?.id) {
      setFetchLoading(false);
      return;
    }
    let cancelled = false;
    async function load() {
      try {
        const res = await fetch(buildApiUrl(`/tours/${tour.id}`));
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
  }, [tour?.id, user]);

  /* ── build date+duration combo options ───────────────────── */
  const comboOptions = React.useMemo(() => {
    if (!tourDetail) return [];
    const dates = tourDetail.startDates || [];
    const durations = tourDetail.durations || [];
    const opts = [];
    dates.forEach((d) => {
      durations.forEach((dur) => {
        opts.push({ label: `${fmtDate(d)}, ${dur}`, date: d, duration: dur });
      });
    });
    return opts;
  }, [tourDetail]);

  /* ── state ───────────────────────────────────────────────── */
  const [personalDetails, setPersonalDetails] = useState([
    { firstName: user?.firstName || '', lastName: user?.lastName || '' }
  ]);
  const [comboIdx,  setComboIdx]    = useState(0);
  const [adults,    setAdults]      = useState(1);
  const [children,  setChildren]    = useState(0);
  const [guestsOpen, setGuestsOpen] = useState(false);
  const [mealPlan,  setMealPlan]    = useState('');
  const [loading,   setLoading]     = useState(false);
  const [error,     setError]       = useState('');
  const [confirmData, setConfirmData] = useState(null);

  /* Sync personal details array with total guests (adults + children) */
  useEffect(() => {
    const totalGuests = adults + children;
    setPersonalDetails(prev => {
      if (totalGuests > prev.length) {
        const extra = Array.from({ length: totalGuests - prev.length }, () => ({ firstName: '', lastName: '' }));
        return [...prev, ...extra];
      }
      return prev.slice(0, totalGuests);
    });
  }, [adults, children]);

  /* Set default mealPlan (raw code) when tourDetail loads */
  useEffect(() => {
    if (tourDetail?.mealPlans?.length && !mealPlan) {
      const firstPlan = tourDetail.mealPlans[0];
      const code = firstPlan.match(/\(([^)]+)\)$/)?.[1] || firstPlan;
      setMealPlan(code);
    }
  }, [tourDetail, mealPlan]);

  /* ── derived ─────────────────────────────────────────────── */
  const selected  = comboOptions[comboIdx] || {};
  const total     = calcTotal(tourDetail, selected.duration, mealPlan, adults);
  const maxAdults = tourDetail?.guestQuantity?.totalMaxValue || tourDetail?.guestQuantity?.adultsMaxValue || 10;
  const maxChildren = tourDetail?.guestQuantity?.childrenMaxValue || 6;

  /* ── close on Escape ─────────────────────────────────────── */
  const handleKey = useCallback((e) => {
    if (e.key === 'Escape') onClose();
  }, [onClose]);

  useEffect(() => {
    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, [handleKey]);

  /* ── submit ──────────────────────────────────────────────── */
  async function handleSubmit(e) {
    e.preventDefault();
    setError('');

    if (!user) {
      setError('You must be signed in to book a tour.');
      return;
    }
    const incomplete = personalDetails.some(p => !p.firstName.trim() || !p.lastName.trim());
    if (incomplete) {
      setError('Please enter first and last name for all customers.');
      return;
    }
    if (!selected.date || !selected.duration) {
      setError('Please select a date and duration.');
      return;
    }
    if (!mealPlan || mealPlan.trim() === '') {
      setError('Please select a meal plan.');
      return;
    }

    const dateStr = new Date(selected.date).toISOString().split('T')[0];

    const payload = {
      userId:   user.id,
      tourId:   tour.id,
      date:     dateStr,
      duration: selected.duration,
      mealPlan: mealPlan,
      guests:   { adult: adults, children: children },
      personalDetails: personalDetails.map(p => ({ firstName: p.firstName.trim(), lastName: p.lastName.trim() })),
    };

    try {
      setLoading(true);
      const res = await client.post('/bookings', payload);
      setConfirmData(res.data);
    } catch (err) {
      // Handle validation errors with field-specific messages
      if (err.response?.data?.fieldErrors) {
        const fieldErrors = err.response.data.fieldErrors;
        const errorMessages = Object.entries(fieldErrors)
          .map(([field, message]) => `${field}: ${message}`)
          .join('; ');
        setError(errorMessages || 'Invalid input provided');
      } else {
        const msg = err.response?.data?.message || err.response?.data || err.message;
        setError(typeof msg === 'string' ? msg : 'Booking failed. Please try again.');
      }
    } finally {
      setLoading(false);
    }
  }

  if (!tour) return null;

  /* If booking succeeded, show only the confirmation modal */
  if (confirmData) {
    return <BookingConfirmation data={confirmData} onClose={onClose} />;
  }

  if (!user) {
    return (
      <div className="bm-overlay bm-overlay-center" onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}>
        <div className="bm-modal bm-auth-modal" role="dialog" aria-modal="true">
          <button className="bm-close" onClick={onClose} aria-label="Close">
            <CloseIcon />
          </button>

          <div className="bm-auth-brand">
            <LuggageIcon />
            <span className="bm-auth-brand-text">Travel Agency</span>
          </div>

          <div className="bm-auth-copy">
            <p className="bm-auth-title">To book a tour please sign in or create an account</p>
            <p className="bm-auth-subtitle">Continue to book <strong>{tour.name}</strong>{tour.destination ? ` in ${tour.destination}` : ''}.</p>
          </div>

          <div className="bm-auth-actions">
            <button
              className="bm-submit"
              type="button"
              onClick={() => navigate('/sign-in', { state: { from: currentPath } })}
            >
              Sign in
            </button>
            <button
              className="bm-auth-secondary"
              type="button"
              onClick={() => navigate('/register', { state: { from: currentPath } })}
            >
              Create an account
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="bm-overlay" onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="bm-modal" role="dialog" aria-modal="true">

        {/* close */}
        <button className="bm-close" onClick={onClose} aria-label="Close">
          <CloseIcon />
        </button>

        {/* header */}
        <div className="bm-header">
          <div className="bm-title-row">
            <span className="bm-tour-name">{tour.name}</span>
            {tour.rating != null && (
              <span className="bm-rating">
                <StarIcon /> {tour.rating.toFixed(1)}
              </span>
            )}
          </div>
          <div className="bm-destination">{tour.destination}</div>
        </div>

        {fetchLoading ? (
          <div className="bm-loading">Loading tour details…</div>
        ) : (
          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '32px' }}>
            {/* ── Personal details ─────────────────────────────── */}
            {personalDetails.map((person, idx) => {
              const isChild = idx >= adults;
              const guestLabel = isChild 
                ? ` (Child ${idx - adults + 1})` 
                : (adults > 1 ? ` (Adult ${idx + 1})` : '');
              
              return (
              <div key={idx}>
                <p className="bm-section-title">
                  Personal details{guestLabel}
                </p>
                <div className="bm-personal-row">
                  <div className="bm-field">
                    <label>First name</label>
                    <input
                      type="text"
                      placeholder="Johnson"
                      value={person.firstName}
                      onChange={(e) => {
                        const updated = [...personalDetails];
                        updated[idx] = { ...updated[idx], firstName: e.target.value };
                        setPersonalDetails(updated);
                      }}
                      required
                    />
                    <span className="bm-hint">e.g. Johnson</span>
                  </div>
                  <div className="bm-field">
                    <label>Last name</label>
                    <input
                      type="text"
                      placeholder="Doe"
                      value={person.lastName}
                      onChange={(e) => {
                        const updated = [...personalDetails];
                        updated[idx] = { ...updated[idx], lastName: e.target.value };
                        setPersonalDetails(updated);
                      }}
                      required
                    />
                    <span className="bm-hint">e.g. Doe</span>
                  </div>
                </div>
              </div>
            );
            })}

            {/* ── Tour details ──────────────────────────────────── */}
            <div>
              <p className="bm-section-title">Tour details</p>

              <div className="bm-tour-details">
                {/* Date + Duration */}
                <div className="bm-select-row">
                  <CalIcon />
                  <select
                    value={comboIdx}
                    onChange={(e) => setComboIdx(Number(e.target.value))}
                  >
                    {comboOptions.length > 0
                      ? comboOptions.map((opt, i) => (
                          <option key={i} value={i}>{opt.label}</option>
                        ))
                      : <option>No dates available</option>
                    }
                  </select>
                  <ChevronIcon />
                </div>

                {/* Guests */}
                <div className="bm-guests-dropdown-wrap">
                  <div className="bm-select-row" onClick={() => setGuestsOpen(o => !o)} style={{ cursor: 'pointer' }}>
                    <PersonIcon />
                    <span className="bm-guests-summary">
                      {adults} adult{adults > 1 ? 's' : ''}{children > 0 ? `, ${children} child${children > 1 ? 'ren' : ''}` : ''}
                    </span>
                    <ChevronIcon />
                  </div>
                  {guestsOpen && (
                    <div className="bm-guests-panel">
                      <div className="bm-stepper-row">
                        <span className="bm-stepper-label">Adults</span>
                        <button type="button" className="bm-stepper-btn" disabled={adults <= 1} onClick={() => setAdults(a => a - 1)}>−</button>
                        <span className="bm-stepper-value">{adults}</span>
                        <button type="button" className="bm-stepper-btn bm-stepper-btn-plus" disabled={adults >= maxAdults} onClick={() => setAdults(a => a + 1)}>+</button>
                      </div>
                      <div className="bm-stepper-row">
                        <span className="bm-stepper-label">Children</span>
                        <button type="button" className="bm-stepper-btn" disabled={children <= 0} onClick={() => setChildren(c => c - 1)}>−</button>
                        <span className="bm-stepper-value">{children}</span>
                        <button type="button" className="bm-stepper-btn bm-stepper-btn-plus" disabled={children >= maxChildren} onClick={() => setChildren(c => c + 1)}>+</button>
                      </div>
                    </div>
                  )}
                </div>

                {/* Meal plan */}
                <div className="bm-select-row">
                  <ForkIcon />
                  <select
                    value={mealPlan}
                    onChange={(e) => setMealPlan(e.target.value)}
                    disabled={fetchLoading}
                  >
                    {fetchLoading ? (
                      <option value="">Loading...</option>
                    ) : (tourDetail?.mealPlans || []).length === 0 ? (
                      <option value="">No meal plans available</option>
                    ) : (
                      (tourDetail?.mealPlans || []).map((mp) => {
                        // Backend stores codes (BB, HB...) but TourService formats them
                        // to display names e.g. "Half-board (HB)". Extract the raw code
                        // from inside the parentheses so the payload sends the correct value.
                        const code = mp.match(/\(([^)]+)\)$/)?.[1] || mp;
                        return <option key={mp} value={code}>{mp}</option>;
                      })
                    )}
                  </select>
                  <ChevronIcon />
                </div>
              </div>
            </div>

            {/* Total price + submit */}
            <div>
              <div className="bm-total-row">
                <span className="bm-total-label">Total price:</span>
                <span className="bm-total-price">
                  {total != null ? `$${total.toLocaleString()}` : '—'}
                </span>
              </div>

              {/* Messages */}
              {error && <div className="bm-error">{error}</div>}

              {/* Submit */}
              <button className="bm-submit" type="submit" disabled={loading || fetchLoading || !mealPlan}>
                {loading ? 'Booking…' : 'Book the tour'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}
