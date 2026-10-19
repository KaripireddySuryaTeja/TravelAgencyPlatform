import React, { useState, useEffect, useMemo } from 'react';
import './TourDetail.css';
import BookingModal from './BookingModal';

/* ── icons ───────────────────────────────────────────────────── */
function PinIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor"
         strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M21 10c0 7-9 13-9 13S3 17 3 10a9 9 0 0 1 18 0z"/>
      <circle cx="12" cy="10" r="3"/>
    </svg>
  );
}
function CalIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
         strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="4" width="18" height="18" rx="2"/>
      <line x1="16" y1="2" x2="16" y2="6"/>
      <line x1="8" y1="2" x2="8" y2="6"/>
      <line x1="3" y1="10" x2="21" y2="10"/>
    </svg>
  );
}
function PersonIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
         strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
      <circle cx="9" cy="7" r="4"/>
      <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
      <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
    </svg>
  );
}
function ForkIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor"
         strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 2v7c0 1.1.9 2 2 2h4a2 2 0 0 0 2-2V2"/>
      <path d="M7 2v20"/>
      <path d="M21 15V2a5 5 0 0 0-5 5v6c0 1.1.9 2 2 2h3zm0 0v7"/>
    </svg>
  );
}
function ChevronDown() {
  return (
    <svg className="chevron" width="14" height="14" viewBox="0 0 24 24" fill="none"
         stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
      <path d="M6 9l6 6 6-6"/>
    </svg>
  );
}

/* ── stars ───────────────────────────────────────────────────── */
function StarFull({ size = 14 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="#0c6d8f" xmlns="http://www.w3.org/2000/svg">
      <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
    </svg>
  );
}
function StarEmpty({ size = 14 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="#c8dde8"
         strokeWidth="2" xmlns="http://www.w3.org/2000/svg">
      <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
    </svg>
  );
}
function StarHalf({ size = 14 }) {
  const id = `h${Math.random().toString(36).slice(2, 7)}`;
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id={id} x1="0" x2="1" y1="0" y2="0">
          <stop offset="50%" stopColor="#0c6d8f"/>
          <stop offset="50%" stopColor="transparent"/>
        </linearGradient>
      </defs>
      <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"
        fill={`url(#${id})`} stroke="#c8dde8" strokeWidth="1.5"/>
    </svg>
  );
}
function StarRating({ rating, size = 14 }) {
  return (
    <span className="star-row">
      {[1, 2, 3, 4, 5].map(i => {
        if (rating >= i) return <StarFull key={i} size={size} />;
        if (rating >= i - 0.5) return <StarHalf key={i} size={size} />;
        return <StarEmpty key={i} size={size} />;
      })}
    </span>
  );
}

/* ── helpers ─────────────────────────────────────────────────── */
function fmtShortDate(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  if (Number.isNaN(d.getTime())) return '';
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}

function fmtLongDate(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  if (Number.isNaN(d.getTime())) return '';
  return d.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
}

function priceToNumber(value) {
  if (value == null) return 0;
  if (typeof value === 'number') return value;
  const cleaned = String(value).replace(/[^\d.]/g, '');
  const n = parseFloat(cleaned);
  return Number.isFinite(n) ? n : 0;
}

/**
 * TourDetail – renders the detail view for a single tour.
 *
 * @param {object} props
 * @param {object} props.tour - TourDetailResponseDTO from the backend
 * @param {object} props.reviewsData - ReviewListResponseDTO from the backend
 * @param {string} props.reviewSort
 * @param {(s:string)=>void} props.onReviewSortChange
 * @param {number} props.reviewPage
 * @param {(p:number)=>void} props.onReviewPageChange
 * @param {boolean} props.reviewsLoading
 * @param {()=>void} props.onBack
 */
export default function TourDetail({
  tour,
  reviewsData,
  reviewSort,
  onReviewSortChange,
  reviewPage,
  onReviewPageChange,
  reviewsLoading,
  onBack,
}) {
  const durations = tour.durations ?? [];
  const startDates = tour.startDates ?? [];
  const mealPlans = tour.mealPlans ?? [];
  const maxAdults = tour.guestQuantity?.adultsMaxValue ?? 8;

  const [selectedDuration, setSelectedDuration] = useState(durations[0] ?? '');
  const [selectedStartDate, setSelectedStartDate] = useState(startDates[0] ?? '');
  const [selectedAdults, setSelectedAdults] = useState(1);
  const [selectedMeal, setSelectedMeal] = useState(mealPlans[0] ?? '');
  const [showBooking, setShowBooking] = useState(false);

  useEffect(() => { window.scrollTo(0, 0); }, [tour.id]);

  // Reset any out-of-range selections if tour changes
  useEffect(() => {
    if (durations.length && !durations.includes(selectedDuration)) {
      setSelectedDuration(durations[0]);
    }
    if (startDates.length && !startDates.includes(selectedStartDate)) {
      setSelectedStartDate(startDates[0]);
    }
    if (mealPlans.length && !mealPlans.includes(selectedMeal)) {
      setSelectedMeal(mealPlans[0]);
    }
  }, [tour.id, durations, startDates, mealPlans, selectedDuration, selectedStartDate, selectedMeal]);

  const basePrice = useMemo(() => {
    const map = tour.pricePerDuration;
    if (map && selectedDuration && map[selectedDuration] != null) {
      return priceToNumber(map[selectedDuration]);
    }
    if (map) {
      const first = Object.values(map)[0];
      if (first != null) return priceToNumber(first);
    }
    return 0;
  }, [tour.pricePerDuration, selectedDuration]);

  const mealSupplement = useMemo(() => {
    const map = tour.mealSupplementsPerDay;
    if (!map || !selectedMeal) return 0;
    return priceToNumber(map[selectedMeal]);
  }, [tour.mealSupplementsPerDay, selectedMeal]);

  // Derive a numeric per-day count from a string like "7 days" / "10 days"
  const durationDays = useMemo(() => {
    const m = String(selectedDuration).match(/(\d+)/);
    return m ? parseInt(m[1], 10) : 0;
  }, [selectedDuration]);

  const totalPrice = (basePrice + mealSupplement * durationDays) * selectedAdults;

  const displayRating = (tour.rating ?? 0).toFixed(2);
  const reviews = reviewsData?.reviews ?? [];
  const totalReviewPages = reviewsData?.totalPages ?? 1;

  const cancellationLabel = useMemo(() => {
    if (tour.freeCancellationDeadline) {
      return `Free cancellation until ${fmtLongDate(tour.freeCancellationDeadline)}`;
    }
    if (tour.freeCancellationDaysBefore != null) {
      return `Free cancellation up to ${tour.freeCancellationDaysBefore} days before start`;
    }
    return null;
  }, [tour.freeCancellationDeadline, tour.freeCancellationDaysBefore]);

  return (
    <div className="tour-detail-page">
      <div className="detail-container">

        {/* Breadcrumbs */}
        <nav className="breadcrumbs">
          <button type="button" className="breadcrumb-link" onClick={onBack}>Main page</button>
          <span className="breadcrumb-sep">›</span>
          <span className="breadcrumb-current">{tour.name}</span>
        </nav>

        {/* Title block */}
        <div className="detail-title-block">
          <div className="detail-title-left">
            <h1 className="detail-title">{tour.name}</h1>
            <div className="detail-location">
              <PinIcon /> {tour.destination}
            </div>
          </div>
          <div className="detail-rating-block">
            <StarFull size={20} />
            <span className="detail-rating-num">{displayRating}</span>
            {tour.reviewCount != null && (
              <span className="reviewer-date" style={{ marginLeft: 6 }}>
                ({tour.reviewCount} reviews)
              </span>
            )}
          </div>
        </div>

        {/* Gallery grid — fixed mosaic layout (a/b/c/d/e/f/g) */}
        {(tour.imageUrls?.length ?? 0) > 0 && (() => {
          const AREA_KEYS = ['a', 'b', 'c', 'd', 'e', 'f', 'g'];
          // Pick exactly 7 images, repeating from the start if fewer were provided.
          const imgs = AREA_KEYS.map((_, i) => tour.imageUrls[i % tour.imageUrls.length]);
          return (
            <div className="gallery-grid">
              {AREA_KEYS.map((key, i) => (
                <div key={key} className={`gallery-item gallery-${key}`}>
                  <img src={imgs[i]} alt={`${tour.name} photo ${i + 1}`} loading="lazy" />
                </div>
              ))}
            </div>
          );
        })()}

        {/* Body */}
        <div className="detail-body">
          {/* Left */}
          <div className="detail-left">
            {tour.summary && (
              <p className="detail-description">{tour.summary}</p>
            )}

            <div className="about-section">
              <h2 className="about-title">About the tour</h2>

              {cancellationLabel && (
                <div className="about-block">
                  <div className="about-label">Free cancellation policy</div>
                  <div className="about-value">{cancellationLabel}</div>
                </div>
              )}
              {durations.length > 0 && (
                <div className="about-block">
                  <div className="about-label">Duration - {durations.join(', ')}</div>
                </div>
              )}
              {tour.accommodation && (
                <div className="about-block">
                  <div className="about-label">Accommodation</div>
                  <div className="about-value">{tour.accommodation}</div>
                </div>
              )}
              {tour.hotelName && (
                <div className="about-block">
                  <div className="about-label">Hotel</div>
                  <div className="about-value">
                    <strong>{tour.hotelName}</strong>
                    {tour.hotelDescription ? ` — ${tour.hotelDescription}` : ''}
                  </div>
                </div>
              )}
              {mealPlans.length > 0 && (
                <div className="about-block">
                  <div className="about-label">Meal plans</div>
                  <div className="about-value">{mealPlans.join(', ')}</div>
                </div>
              )}
              {tour.tourType && (
                <div className="about-block">
                  <div className="about-label">Tour type</div>
                  <div className="about-value">{tour.tourType}</div>
                </div>
              )}
              {tour.customDetails && Object.entries(tour.customDetails).map(([k, v]) => (
                <div className="about-block" key={k}>
                  <div className="about-label">{k}</div>
                  <div className="about-value">{v}</div>
                </div>
              ))}
            </div>
          </div>

          {/* Right – booking widget */}
          <div className="detail-right">
            <div className="booking-widget">
              <div className="booking-fields">
                {/* Combined date + duration: "Jan 15, 7 days" */}
                {(durations.length > 0 || startDates.length > 0) && (
                  <div className="booking-field">
                    <CalIcon />
                    <select
                      className="booking-select"
                      value={`${selectedStartDate}|${selectedDuration}`}
                      onChange={e => {
                        const [d, dur] = e.target.value.split('|');
                        if (d) setSelectedStartDate(d);
                        if (dur) setSelectedDuration(dur);
                      }}
                    >
                      {(startDates.length ? startDates : ['']).flatMap(d =>
                        (durations.length ? durations : ['']).map(dur => (
                          <option key={`${d}|${dur}`} value={`${d}|${dur}`}>
                            {fmtShortDate(d)}{d && dur ? ', ' : ''}{dur}
                          </option>
                        ))
                      )}
                    </select>
                    <ChevronDown />
                  </div>
                )}

                {/* Adults */}
                <div className="booking-field">
                  <PersonIcon />
                  <select
                    className="booking-select"
                    value={selectedAdults}
                    onChange={e => setSelectedAdults(Number(e.target.value))}
                  >
                    {Array.from({ length: maxAdults }, (_, i) => i + 1).map(n => (
                      <option key={n} value={n}>{n} {n === 1 ? 'adult' : 'adults'}</option>
                    ))}
                  </select>
                  <ChevronDown />
                </div>

                {/* Meal plan */}
                {mealPlans.length > 0 && (
                  <div className="booking-field">
                    <ForkIcon />
                    <select
                      className="booking-select"
                      value={selectedMeal}
                      onChange={e => setSelectedMeal(e.target.value)}
                    >
                      {mealPlans.map(m => (
                        <option key={m} value={m}>{m}</option>
                      ))}
                    </select>
                    <ChevronDown />
                  </div>
                )}
              </div>

              <div className="booking-total">
                Total price: <strong>${totalPrice.toLocaleString()}</strong>
              </div>

              <button type="button" className="btn-book-tour" onClick={() => setShowBooking(true)}>Book the tour</button>
            </div>
          </div>
        </div>

        {/* Reviews */}
        <div className="reviews-section">
          <div className="reviews-header">
            <h2 className="reviews-title">Customer Reviews</h2>
            <div className="reviews-sort-wrap">
              <span className="reviews-sort-label">Sort by:</span>
              <div className="review-sort-select-wrap">
                <select
                  className="review-sort-select"
                  value={reviewSort}
                  onChange={e => onReviewSortChange(e.target.value)}
                >
                  <option value="TOP_RATED_FIRST">Top rated first</option>
                  <option value="LOW_RATED_FIRST">Low rated first</option>
                  <option value="NEWEST_FIRST">Newest first</option>
                  <option value="OLDEST_FIRST">Oldest first</option>
                </select>
                <ChevronDown />
              </div>
            </div>
          </div>

          {reviewsLoading ? (
            <div className="detail-status">Loading reviews…</div>
          ) : reviews.length === 0 ? (
            <div className="detail-status">No reviews yet.</div>
          ) : (
            <>
              <div className="reviews-grid">
                {reviews.map(r => (
                  <div key={r.id} className="review-card">
                    <div className="review-card-top">
                      <div className="reviewer-avatar">
                        {(r.userName || '?').charAt(0).toUpperCase()}
                      </div>
                      <div className="reviewer-info">
                        <span className="reviewer-name">{r.userName}</span>
                        <span className="reviewer-date">{fmtLongDate(r.reviewDate)}</span>
                      </div>
                      <StarRating rating={r.rate ?? 0} size={13} />
                    </div>
                    <p className="review-text">{r.comment}</p>
                  </div>
                ))}
              </div>

              {totalReviewPages > 1 && (
                <div className="review-pagination">
                  <button type="button"
                          className="review-page-btn nav-btn"
                          disabled={reviewPage <= 1}
                          onClick={() => onReviewPageChange(reviewPage - 1)}>«</button>
                  {Array.from({ length: totalReviewPages }, (_, i) => i + 1).map(p => (
                    <button
                      key={p}
                      type="button"
                      className={`review-page-btn ${p === reviewPage ? 'active' : ''}`}
                      onClick={() => onReviewPageChange(p)}
                    >{p}</button>
                  ))}
                  <button type="button"
                          className="review-page-btn nav-btn"
                          disabled={reviewPage >= totalReviewPages}
                          onClick={() => onReviewPageChange(reviewPage + 1)}>»</button>
                </div>
              )}
            </>
          )}
        </div>

      </div>

      {showBooking && (
        <BookingModal tour={tour} onClose={() => setShowBooking(false)} />
      )}
    </div>
  );
}
