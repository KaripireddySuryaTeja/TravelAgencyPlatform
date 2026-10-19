import React, { useState, useEffect, useCallback } from 'react';
import './EditBookingModal.css';
import { buildApiUrl } from '../api/client';

/* ── icon helpers ────────────────────────────────────────────── */
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

/* ── helpers ──────────────────────────────────────────────────── */
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

function buildPendingChanges(booking, payload) {
  const changes = [];
  const oldAdults = booking.rawAdults || 0;
  const oldChildren = booking.rawChildren || 0;
  const newAdults = payload.guests.adult;
  const newChildren = payload.guests.children;

  if (oldAdults !== newAdults || oldChildren !== newChildren) {
    const oldGuests = `${oldAdults} adult${oldAdults !== 1 ? 's' : ''}${oldChildren > 0 ? `, ${oldChildren} child${oldChildren !== 1 ? 'ren' : ''}` : ''}`;
    const newGuests = `${newAdults} adult${newAdults !== 1 ? 's' : ''}${newChildren > 0 ? `, ${newChildren} child${newChildren !== 1 ? 'ren' : ''}` : ''}`;
    changes.push(`Number of tourists: ${oldGuests} → ${newGuests}`);
  }

  if (booking.rawMealPlan !== payload.mealPlan) {
    changes.push(`Meal plan: ${booking.rawMealPlan} → ${payload.mealPlan}`);
  }

  if (booking.rawDate !== payload.date) {
    changes.push('Start date updated');
  }

  if (booking.rawDuration !== payload.duration) {
    changes.push(`Duration: ${booking.rawDuration} → ${payload.duration}`);
  }

  const originalDetails = booking.personalDetails || [];
  const detailsChanged = payload.personalDetails.length !== originalDetails.length
    || payload.personalDetails.some((person, idx) => {
      const original = originalDetails[idx] || {};
      return person.firstName !== (original.firstName || '')
        || person.lastName !== (original.lastName || '');
    });

  if (detailsChanged) {
    changes.push('Guest details updated');
  }

  return changes;
}

/* ══════════════════════════════════════════════════════════════ */
export default function EditBookingModal({ booking, onClose, onSaved }) {
  const initialGuestCount = booking.personalDetails?.length || ((booking.rawAdults || 1) + (booking.rawChildren || 0));

  /* ── fetch full tour details for dropdowns ──────────────── */
  const [tourDetail, setTourDetail] = useState(null);
  const [fetchLoading, setFetchLoading] = useState(true);

  useEffect(() => {
    if (!booking?.tourId) { setFetchLoading(false); return; }
    let cancelled = false;
    async function load() {
      try {
        const res = await fetch(buildApiUrl(`/tours/${booking.tourId}`));
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

  /* ── combo options ─────────────────────────────────────── */
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

  /* ── state (pre-fill from booking) ─────────────────────── */
  const [personalDetails, setPersonalDetails] = useState(
    booking.personalDetails?.length
      ? booking.personalDetails.map(p => ({ firstName: p.firstName || '', lastName: p.lastName || '' }))
      : [{ firstName: '', lastName: '' }]
  );
  const [comboIdx, setComboIdx] = useState(0);
  const [adults, setAdults] = useState(booking.rawAdults || 1);
  const [children, setChildren] = useState(booking.rawChildren || 0);
  const [guestsOpen, setGuestsOpen] = useState(false);
  const [mealPlan, setMealPlan] = useState(booking.rawMealPlan || '');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  /* Set default combo index based on booking's date + duration */
  useEffect(() => {
    if (comboOptions.length && booking.rawDate) {
      const idx = comboOptions.findIndex(o => {
        const oDate = new Date(o.date).toISOString().split('T')[0];
        return oDate === booking.rawDate && o.duration === booking.rawDuration;
      });
      if (idx >= 0) setComboIdx(idx);
    }
  }, [comboOptions, booking.rawDate, booking.rawDuration]);

  /* Set default meal plan */
  useEffect(() => {
    if (tourDetail?.mealPlans?.length && !mealPlan) {
      const fallback = tourDetail.mealPlans[0];
      const fallbackCode = fallback.match(/\(([^)]+)\)$/)?.[1] || fallback;
      setMealPlan(booking.rawMealPlan || fallbackCode);
    }
  }, [tourDetail, mealPlan, booking.rawMealPlan]);

  /* Sync personal details array with total guests */
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

  /* ── derived ───────────────────────────────────────────── */
  const selected = comboOptions[comboIdx] || {};
  const total = calcTotal(tourDetail, selected.duration || booking.rawDuration, mealPlan, adults);
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

  /* ── submit ────────────────────────────────────────────── */
  async function handleSubmit(e) {
    e.preventDefault();
    setError('');

    const incomplete = personalDetails.some(p => !p.firstName.trim() || !p.lastName.trim());
    if (incomplete) {
      setError('Please enter first and last name for all customers.');
      return;
    }

    const dateStr = selected.date
      ? new Date(selected.date).toISOString().split('T')[0]
      : booking.rawDate;

    const payload = {
      date: dateStr,
      duration: selected.duration || booking.rawDuration,
      mealPlan: mealPlan,
      guests: { adult: adults, children: children },
      personalDetails: personalDetails.map(p => ({ firstName: p.firstName.trim(), lastName: p.lastName.trim() })),
    };

    const totalGuests = adults + children;
    const removedGuestIds = totalGuests < initialGuestCount
      ? Array.from({ length: initialGuestCount - totalGuests }, (_, idx) => `GUEST_${totalGuests + idx + 1}`)
      : [];

    const previewBooking = {
      ...booking,
      rawDate: payload.date,
      rawDuration: payload.duration,
      rawMealPlan: payload.mealPlan,
      rawAdults: payload.guests.adult,
      rawChildren: payload.guests.children,
      personalDetails: payload.personalDetails,
      tourDetails: {
        ...booking.tourDetails,
        totalPrice: total != null ? `$${total.toLocaleString()}` : booking.tourDetails?.totalPrice,
      },
    };

    const changes = buildPendingChanges(booking, payload);

    try {
      setLoading(true);
      onSaved({
        bookingId: booking.id,
        payload,
        changes,
        removedGuestIds,
        previewBooking,
        newTotalPrice: previewBooking.tourDetails?.totalPrice,
      });
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
      <div className="ebm-modal" role="dialog" aria-modal="true">

        {/* close */}
        <button className="ebm-close" onClick={onClose} aria-label="Close">
          <CloseIcon />
        </button>

        {/* header */}
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

        {fetchLoading ? (
          <div className="ebm-loading">Loading tour details…</div>
        ) : (
          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '28px' }}>
            {/* ── Personal details ────────────────────────── */}
            {personalDetails.map((person, idx) => {
              const isChild = idx >= adults;
              const guestLabel = isChild
                ? ` (Child ${idx - adults + 1})`
                : (adults > 1 ? ` (Adult ${idx + 1})` : '');

              return (
              <div key={idx}>
                <p className="ebm-section-title">
                  Personal details{guestLabel}
                </p>
                <div className="ebm-personal-row">
                  <div className="ebm-field">
                    <label>First name</label>
                    <input
                      type="text"
                      placeholder="e.g. Johnson"
                      value={person.firstName}
                      onChange={(e) => {
                        const updated = [...personalDetails];
                        updated[idx] = { ...updated[idx], firstName: e.target.value };
                        setPersonalDetails(updated);
                      }}
                      required
                    />
                    <span className="ebm-hint">e.g. Johnson</span>
                  </div>
                  <div className="ebm-field">
                    <label>Last name</label>
                    <input
                      type="text"
                      placeholder="e.g. Doe"
                      value={person.lastName}
                      onChange={(e) => {
                        const updated = [...personalDetails];
                        updated[idx] = { ...updated[idx], lastName: e.target.value };
                        setPersonalDetails(updated);
                      }}
                      required
                    />
                    <span className="ebm-hint">e.g. Doe</span>
                  </div>
                </div>
              </div>
            )})}

            {/* ── Tour details ─────────────────────────────── */}
            <div>
              <p className="ebm-section-title">Tour details</p>
              <div className="ebm-tour-details">
                {/* Date + Duration */}
                <div className="ebm-select-row">
                  <CalIcon />
                  <select value={comboIdx} onChange={(e) => setComboIdx(Number(e.target.value))}>
                    {comboOptions.length > 0
                      ? comboOptions.map((opt, i) => (
                          <option key={i} value={i}>{opt.label}</option>
                        ))
                      : <option>{booking.rawDate ? `${fmtDate(booking.rawDate)}, ${booking.rawDuration}` : 'No dates available'}</option>
                    }
                  </select>
                  <ChevronIcon />
                </div>

                {/* Guests */}
                <div className="ebm-guests-dropdown-wrap">
                  <div className="ebm-select-row" onClick={() => setGuestsOpen(o => !o)} style={{ cursor: 'pointer' }}>
                    <PersonIcon />
                    <span className="ebm-guests-summary">
                      {adults} adult{adults > 1 ? 's' : ''}{children > 0 ? `, ${children} child${children > 1 ? 'ren' : ''}` : ''}
                    </span>
                    <ChevronIcon />
                  </div>
                  {guestsOpen && (
                    <div className="ebm-guests-panel">
                      <div className="ebm-stepper-row">
                        <span className="ebm-stepper-label">Adults</span>
                        <button type="button" className="ebm-stepper-btn" disabled={adults <= 1} onClick={() => setAdults(a => a - 1)}>−</button>
                        <span className="ebm-stepper-value">{adults}</span>
                        <button type="button" className="ebm-stepper-btn ebm-stepper-btn-plus" disabled={adults >= maxAdults} onClick={() => setAdults(a => a + 1)}>+</button>
                      </div>
                      <div className="ebm-stepper-row">
                        <span className="ebm-stepper-label">Children</span>
                        <button type="button" className="ebm-stepper-btn" disabled={children <= 0} onClick={() => setChildren(c => c - 1)}>−</button>
                        <span className="ebm-stepper-value">{children}</span>
                        <button type="button" className="ebm-stepper-btn ebm-stepper-btn-plus" disabled={children >= maxChildren} onClick={() => setChildren(c => c + 1)}>+</button>
                      </div>
                    </div>
                  )}
                </div>

                {/* Meal plan */}
                <div className="ebm-select-row">
                  <ForkIcon />
                  <select value={mealPlan} onChange={(e) => setMealPlan(e.target.value)}>
                    {(tourDetail?.mealPlans || [booking.rawMealPlan]).filter(Boolean).map((mp) => {
                      // Backend stores codes (BB, HB...) but TourService formats them
                      // to display names e.g. "Half-board (HB)". Extract the raw code
                      // from inside the parentheses so the payload sends the correct value.
                      const code = mp.match(/\(([^)]+)\)$/)?.[1] || mp;
                      return <option key={mp} value={code}>{mp}</option>;
                    })}
                  </select>
                  <ChevronIcon />
                </div>
              </div>
            </div>

            {/* Total price + submit */}
            <div>
              <div className="ebm-total-row">
                <span className="ebm-total-label">Total price:</span>
                <span className="ebm-total-price">
                  {total != null ? `$${total.toLocaleString()}` : (booking.tourDetails?.totalPrice || '—')}
                </span>
              </div>

              {error && <div className="ebm-error">{error}</div>}

              <button className="ebm-submit" type="submit" disabled={loading}>
                {loading ? 'Saving…' : 'Save changes'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  );
}

