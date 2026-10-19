import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import './TourCard.css';
import BookingModal from './BookingModal';

function StarIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="#0c6d8f" xmlns="http://www.w3.org/2000/svg">
      <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z"/>
    </svg>
  );
}
function PinIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#6a9ab5"
         strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M21 10c0 7-9 13-9 13S3 17 3 10a9 9 0 0 1 18 0z"/>
      <circle cx="12" cy="10" r="3"/>
    </svg>
  );
}
function CalIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#6a9ab5"
         strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="4" width="18" height="18" rx="2"/>
      <line x1="16" y1="2" x2="16" y2="6"/>
      <line x1="8" y1="2" x2="8" y2="6"/>
      <line x1="3" y1="10" x2="21" y2="10"/>
    </svg>
  );
}
function ForkIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#6a9ab5"
         strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 2v7c0 1.1.9 2 2 2h4a2 2 0 0 0 2-2V2"/>
      <path d="M7 2v20"/>
      <path d="M21 15V2a5 5 0 0 0-5 5v6c0 1.1.9 2 2 2h3zm0 0v7"/>
    </svg>
  );
}
function WalletIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#6a9ab5"
         strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="2" y="5" width="20" height="14" rx="2"/>
      <path d="M16 12h.01"/>
    </svg>
  );
}
function NoCancelIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#e53935"
         strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10"/>
      <line x1="4.93" y1="4.93" x2="19.07" y2="19.07"/>
    </svg>
  );
}

function fmtDate(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  if (Number.isNaN(d.getTime())) return '';
  return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}
function fmtCancellation(dateStr) {
  if (!dateStr) return '';
  const d = new Date(dateStr);
  if (Number.isNaN(d.getTime())) return '';
  return d.toLocaleDateString('en-US', { month: 'long', day: 'numeric' });
}

const LOCAL_IMAGES = [
  '/images/tour1.png',
  '/images/tour2.png',
  '/images/tour3.png',
  '/images/tour4.png',
  '/images/tour5.png',
  '/images/tour6.png',
];

export default function TourCard({ tour, index }) {
  const navigate = useNavigate();
  const idx = index ?? 0;
  const [showBooking, setShowBooking] = useState(false);

  const imageUrl = idx < LOCAL_IMAGES.length
    ? LOCAL_IMAGES[idx]
    : `https://picsum.photos/seed/${(idx % 30) + 1}/480/360`;

  const durationText = tour.durations
    ? tour.durations.map(d => (d.toLowerCase().includes('day') ? d : `${d} days`)).join(', ')
    : '';

  const mealText = tour.mealPlans ? tour.mealPlans.join(', ') : '';

  const handleSeeDetails = () => {
    if (tour?.id != null) navigate(`/tours/${tour.id}`);
  };

  return (
    <div className="tour-card">
      <div className="tour-card-img">
        <img src={imageUrl} alt={tour.name} loading="lazy" />
      </div>

      <div className="tour-card-body">
        <div className="card-header-row">
          <h3 className="tour-name">{tour.name}</h3>
          <div className="tour-rating">
            <span className="rating-val">
              <StarIcon /> {tour.rating != null ? tour.rating.toFixed(1) : '\u2014'}
            </span>
            <span className="review-count">{tour.reviews ?? 0} reviews</span>
          </div>
        </div>

        <div className="info-row">
          <PinIcon />
          <span>{tour.destination}</span>
        </div>

        <div className="info-row spacer" />

        <div className="info-row">
          <CalIcon />
          <span>
            {fmtDate(tour.startDate)}
            {durationText && <span className="duration-text"> ({durationText})</span>}
          </span>
        </div>

        {mealText && (
          <div className="info-row">
            <ForkIcon />
            <span>{mealText}</span>
          </div>
        )}

        <div className="info-row">
          <WalletIcon />
          <span>From <strong>${tour.price}</strong> for 1 person</span>
        </div>

        {tour.cancellationExpired && !tour.freeCancellation && (
          <div className="no-cancel">
            <NoCancelIcon />
            Free cancellation is no longer available
          </div>
        )}

        {tour.freeCancellation && (
          <div className="free-cancel">
            <span className="check-mark">✓</span>
            Free cancellation until {fmtCancellation(tour.freeCancellation)}
          </div>
        )}

        <div className="card-actions">
          <button type="button" className="btn-outline" onClick={handleSeeDetails}>
            See details
          </button>
          <button type="button" className="btn-solid" onClick={() => setShowBooking(true)}>
            Book the tour
          </button>
        </div>
      </div>

      {showBooking && (
        <BookingModal tour={tour} onClose={() => setShowBooking(false)} />
      )}
    </div>
  );
}
