import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { getAgentBookings } from '../api/bookings';
import Header from '../components/Header';
import AgentEditBookingModal from '../components/AgentEditBookingModal';
import AgentCancelModal from '../components/AgentCancelModal';
import CheckAndConfirmModal from '../components/CheckAndConfirmModal';
import DocumentViewerModal from '../components/DocumentViewerModal';
import Toast from '../components/Toast';
import './AgentBookingsPage.css';

const STATUS_TABS = ['All tours', 'Booked', 'Confirmed', 'Started', 'Finished', 'Cancelled'];
const STEPS = ['Booked', 'Confirmed', 'Started', 'Finished'];
const CANCELLED_STEPS = ['Booked', 'Confirmed', 'Cancelled'];

const STATUS_CONFIG = {
  BOOKED:             { label: 'Booked',             className: 'ab-badge-pending' },
  DOCUMENTS_VERIFIED: { label: 'Booked',             className: 'ab-badge-pending' },
  CONFIRMED:          { label: 'Confirmed',          className: 'ab-badge-confirmed' },
  STARTED:            { label: 'Started',            className: 'ab-badge-started' },
  FINISHED:           { label: 'Finished',           className: 'ab-badge-finished' },
  CANCELLED:          { label: 'Cancelled',          className: 'ab-badge-cancelled' },
};

/* ── Icons ─────────────────────────────────────────────────────── */
function PinIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#6a9ab5" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M21 10c0 7-9 13-9 13S3 17 3 10a9 9 0 0 1 18 0z"/><circle cx="12" cy="10" r="3"/>
    </svg>
  );
}
function CalIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#4a7d96" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="4" width="18" height="18" rx="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/>
    </svg>
  );
}
function ForkIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#4a7d96" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 2v7c0 1.1.9 2 2 2h4a2 2 0 0 0 2-2V2"/><path d="M7 2v20"/><path d="M21 15V2a5 5 0 0 0-5 5v6c0 1.1.9 2 2 2h3zm0 0v7"/>
    </svg>
  );
}
function PersonIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#4a7d96" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/><circle cx="12" cy="7" r="4"/>
    </svg>
  );
}
function WalletIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#4a7d96" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="2" y="5" width="20" height="14" rx="2"/><path d="M16 12h.01"/>
    </svg>
  );
}
function DocIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#4a7d96" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/><line x1="16" y1="13" x2="8" y2="13"/><line x1="16" y1="17" x2="8" y2="17"/>
    </svg>
  );
}
function EmailIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#4a7d96" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="2" y="4" width="20" height="16" rx="2"/><polyline points="22,6 12,13 2,6"/>
    </svg>
  );
}
function CheckIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="20 6 9 17 4 12"/>
    </svg>
  );
}
function CrossIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
      <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
    </svg>
  );
}
function FreeCancelCheckIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#16a34a" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="20 6 9 17 4 12"/>
    </svg>
  );
}
function FreeCancelCrossIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#c61f1f" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
      <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
    </svg>
  );
}

/* ── Status Badge ──────────────────────────────────────────────── */
function StatusBadge({ state }) {
  const config = STATUS_CONFIG[state] || STATUS_CONFIG.BOOKED;
  return (
    <span className={`ab-badge ${config.className}`} role="status" aria-label={`Status: ${config.label}`}>
      {config.label}
    </span>
  );
}

/* ── Stepper ───────────────────────────────────────────────────── */
function getStepIndex(state) {
  const map = { BOOKED: 0, DOCUMENTS_VERIFIED: 0, CONFIRMED: 1, STARTED: 2, FINISHED: 3, CANCELLED: -1 };
  return map[state] ?? 0;
}

function ProgressStepper({ state }) {
  const current = getStepIndex(state);
  const isCancelled = state === 'CANCELLED';

  if (isCancelled) {
    return (
      <div className="ab-stepper">
        {CANCELLED_STEPS.map((step, i) => {
          const done = i < CANCELLED_STEPS.length - 1;
          const cancelled = i === CANCELLED_STEPS.length - 1;
          return (
            <div key={step} className={`ab-step ${done ? 'done' : ''} ${cancelled ? 'cancelled' : ''} ${i === 0 ? 'first' : ''} ${i === CANCELLED_STEPS.length - 1 ? 'last' : ''}`}>
              <span className="ab-step-content">
                {done && <CheckIcon />}
                {cancelled && <CrossIcon />}
                <span className="ab-step-label">{step}</span>
              </span>
            </div>
          );
        })}
      </div>
    );
  }

  return (
    <div className="ab-stepper">
      {STEPS.map((step, i) => {
        const done = i <= current;
        return (
          <div key={step} className={`ab-step ${done ? 'done' : ''} ${i === 0 ? 'first' : ''} ${i === STEPS.length - 1 ? 'last' : ''}`}>
            <span className="ab-step-content">
              {done && <CheckIcon />}
              <span className="ab-step-label">{step}</span>
            </span>
          </div>
        );
      })}
    </div>
  );
}

/* ── Free Cancellation Badge ───────────────────────────────────── */
function FreeCancellationBadge({ booking }) {
  const freeCancelDate = booking.freeCancellationDate;
  if (!freeCancelDate) return null;

  const todayStr = new Date().toISOString().split('T')[0];
  const isActive = todayStr <= freeCancelDate;
  const formatted = new Date(freeCancelDate + 'T00:00:00').toLocaleDateString('en-US', { month: 'short', day: 'numeric' });

  if (isActive) {
    return (
      <div className="ab-free-cancel ab-free-cancel-active">
        <FreeCancelCheckIcon /> Free cancellation until {formatted}
      </div>
    );
  }
  return (
    <div className="ab-free-cancel ab-free-cancel-expired">
      <FreeCancelCrossIcon /> Free cancellation is no longer available
    </div>
  );
}

/* ── Booking Card ──────────────────────────────────────────────── */
function BookingCard({ booking, onCancel, onEdit, onCheckConfirm, onViewDocs, currentUserId }) {
  const { tourDetails, customerDetails } = booking;

  const isEditable = booking.state === 'BOOKED' || booking.state === 'DOCUMENTS_VERIFIED';
  const isCancellable = booking.state === 'BOOKED' || booking.state === 'DOCUMENTS_VERIFIED' || booking.state === 'CONFIRMED' || booking.state === 'STARTED';
  const canConfirm = booking.state === 'BOOKED' || booking.state === 'DOCUMENTS_VERIFIED';
  const isCancelled = booking.state === 'CANCELLED';
  const isTerminal = booking.state === 'FINISHED';

  const canceledByLabel = getCanceledByLabel(booking.canceledBy, currentUserId);
  const cancelReason = booking.cancelReason?.trim() || '\u2014';

  return (
    <div className={`ab-card ${isCancelled ? 'ab-card-cancelled' : ''}`}>
      <ProgressStepper state={booking.state} />

      <div className="ab-card-body">
        {/* Tour header */}
        <div className="ab-tour-header">
          {booking.tourImageUrl && (
            <img className="ab-tour-img" src={booking.tourImageUrl} alt={booking.name} />
          )}
          <div>
            <div className="ab-tour-name-row">
              <h3 className="ab-tour-name">{booking.name}</h3>
              <StatusBadge state={booking.state} />
            </div>
            <div className="ab-tour-dest"><PinIcon /> {booking.destination}</div>
          </div>
        </div>

        {/* Details grid */}
        <div className="ab-details-grid">
          {/* Left: Tour details */}
          <div className="ab-details-col">
            <h4 className="ab-col-title">Tour details</h4>
            {tourDetails?.date && (
              <div className="ab-detail-row"><CalIcon /><span>{tourDetails.date}</span></div>
            )}
            {tourDetails?.mealPlan && (
              <div className="ab-detail-row"><ForkIcon /><span>{tourDetails.mealPlan}</span></div>
            )}
            {tourDetails?.guests && (
              <div className="ab-detail-row"><PersonIcon /><span>{tourDetails.guests}</span></div>
            )}
            {tourDetails?.totalPrice && (
              <div className="ab-detail-row"><WalletIcon /><span>Total price <strong>{tourDetails.totalPrice}</strong></span></div>
            )}
          </div>

          {/* Right: Customer details */}
          <div className="ab-details-col">
            <h4 className="ab-col-title">Customer details</h4>
            {customerDetails?.name && (
              <div className="ab-detail-row"><PersonIcon /><span>{customerDetails.name}</span></div>
            )}
            {customerDetails?.email && (
              <div className="ab-detail-row"><EmailIcon /><span>{customerDetails.email}</span></div>
            )}
            <div className="ab-detail-row">
              <DocIcon />
              <span>
                Documents: {booking.documentCount > 0
                  ? <button className="ab-doc-link" onClick={() => onViewDocs(booking)}>{booking.documentCount} item(s)</button>
                  : <span className="ab-no-docs">Not uploaded</span>
                }
              </span>
            </div>
          </div>
        </div>

        {/* Free cancellation badge */}
        {!isCancelled && !isTerminal && <FreeCancellationBadge booking={booking} />}
      </div>

      {/* Actions */}
      {!isCancelled && !isTerminal && (
        <div className="ab-card-actions">
          {isCancellable && (
            <button className="ab-btn-outline" onClick={() => onCancel(booking)}>Cancel</button>
          )}
          {isEditable && (
            <button className="ab-btn-outline" onClick={() => onEdit(booking)}>Edit</button>
          )}
          {canConfirm && (
            <button className="ab-btn-solid" onClick={() => onCheckConfirm(booking)}>Check and confirm</button>
          )}
        </div>
      )}

      {/* Cancelled state info */}
      {isCancelled && (
        <div className="ab-cancelled-info">
          <div className="ab-cancel-row">
            <span className="ab-cancel-label">Cancelled by:</span>
            <span className="ab-cancel-value">{canceledByLabel}</span>
          </div>
          <div className="ab-cancel-row">
            <span className="ab-cancel-label">Reason:</span>
            <span className="ab-cancel-value">{cancelReason}</span>
          </div>
        </div>
      )}
    </div>
  );
}

function getCanceledByLabel(canceledBy, currentUserId) {
  if (!canceledBy) return 'Tourist';
  if (currentUserId && canceledBy === currentUserId) return 'Travel agent';
  const normalized = String(canceledBy).toLowerCase();
  if (normalized.includes('agent')) return 'Travel agent';
  return 'Tourist';
}

/* ── Main Page ─────────────────────────────────────────────────── */
export default function AgentBookingsPage() {
  const { user } = useAuth();
  const [statusFilter, setStatusFilter] = useState('All tours');
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const initialLoadDone = useRef(false);
  const [cancellingBooking, setCancellingBooking] = useState(null);
  const [editingBooking, setEditingBooking] = useState(null);
  const [confirmingBooking, setConfirmingBooking] = useState(null);
  const [viewingDocsBooking, setViewingDocsBooking] = useState(null);
  const [toast, setToast] = useState(null);

  const fetchBookings = useCallback(async () => {
    if (!user?.id) return;
    if (!initialLoadDone.current) setLoading(true);
    try {
      const data = await getAgentBookings(user.id);
      setBookings(data.bookings || []);
    } catch (err) {
      console.error('Failed to fetch agent bookings:', err);
      setBookings([]);
    } finally {
      setLoading(false);
      initialLoadDone.current = true;
    }
  }, [user?.id]);

  useEffect(() => {
    fetchBookings();
  }, [fetchBookings]);

  // Auto-refresh every 15 seconds to pick up new bookings
  useEffect(() => {
    const interval = setInterval(fetchBookings, 15000);
    return () => clearInterval(interval);
  }, [fetchBookings]);

  const handleCancelSuccess = () => {
    setCancellingBooking(null);
    setToast({ title: 'Booking cancelled', body: 'The booking has been cancelled successfully.' });
    fetchBookings();
  };

  const handleConfirmSuccess = () => {
    setConfirmingBooking(null);
    setToast({ title: 'Booking confirmed', body: 'The booking has been confirmed successfully.' });
    fetchBookings();
  };

  const handleEditSaved = () => {
    setEditingBooking(null);
    setToast({ title: 'Booking updated', body: 'The booking has been updated successfully.' });
    fetchBookings();
  };

  const filtered = statusFilter === 'All tours'
    ? bookings
    : bookings.filter(b => {
        const tab = statusFilter.toUpperCase().replace(' ', '_');
        if (tab === 'BOOKED') return b.state === 'BOOKED' || b.state === 'DOCUMENTS_VERIFIED';
        return b.state === tab;
      });

  // Role guard — placed after hooks to satisfy rules-of-hooks
  if (user && user.role !== 'TRAVEL_AGENT') {
    return <Navigate to="/" replace />;
  }

  return (
    <div className="app">
      <Header />

      <div className="main-content">
        {/* Status filter tabs */}
        <div className="ab-status-tabs" role="tablist" aria-label="Filter bookings by status">
          {STATUS_TABS.map(tab => (
            <button
              key={tab}
              role="tab"
              aria-selected={statusFilter === tab}
              className={`ab-status-tab ${statusFilter === tab ? 'active' : ''}`}
              onClick={() => setStatusFilter(tab)}
            >
              {tab}
            </button>
          ))}
        </div>

        {/* Bookings grid */}
        {loading ? (
          <div className="ab-loading" role="status" aria-live="polite">
            <span className="ab-spinner" aria-hidden="true"></span>
            Loading bookings…
          </div>
        ) : filtered.length === 0 ? (
          <div className="ab-empty" role="status">
            <p>No bookings found{statusFilter !== 'All tours' ? ` with status "${statusFilter}"` : ''}.</p>
          </div>
        ) : (
          <div className="ab-bookings-grid">
            {filtered.map(booking => (
              <BookingCard
                key={booking.id}
                booking={booking}
                onCancel={setCancellingBooking}
                onEdit={setEditingBooking}
                onCheckConfirm={setConfirmingBooking}
                onViewDocs={setViewingDocsBooking}
                currentUserId={user?.id}
              />
            ))}
          </div>
        )}
      </div>

      {/* Toast notification */}
      {toast && (
        <Toast
          title={toast.title}
          body={toast.body}
          onClose={() => setToast(null)}
        />
      )}

      {/* Cancel Modal */}
      {cancellingBooking && (
        <AgentCancelModal
          booking={cancellingBooking}
          onSuccess={handleCancelSuccess}
          onClose={() => setCancellingBooking(null)}
        />
      )}

      {/* Edit Modal */}
      {editingBooking && (
        <AgentEditBookingModal
          booking={editingBooking}
          onClose={() => setEditingBooking(null)}
          onSaved={handleEditSaved}
        />
      )}

      {/* Check and Confirm Modal */}
      {confirmingBooking && (
        <CheckAndConfirmModal
          booking={confirmingBooking}
          onSuccess={handleConfirmSuccess}
          onClose={() => setConfirmingBooking(null)}
        />
      )}

      {/* Document Viewer Modal */}
      {viewingDocsBooking && (
        <DocumentViewerModal
          bookingId={viewingDocsBooking.id}
          bookingName={viewingDocsBooking.name}
          onClose={() => setViewingDocsBooking(null)}
        />
      )}
    </div>
  );
}
