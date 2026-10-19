import React, { useState, useEffect, useCallback, useRef } from 'react';
import { client } from '../api/client';
import { useAuth } from '../auth/AuthContext';
import Header from '../components/Header';
import EditBookingModal from '../components/EditBookingModal';
import ConfirmChangesModal from '../components/ConfirmChangesModal';
import CancelBookingModal from '../components/CancelBookingModal';
import FeedbackModal from '../components/FeedbackModal';
import { getFeedback, createFeedback, updateFeedback } from '../api/feedback';
import DocumentUploadModal from '../components/DocumentUploadModal';
import './MyToursPage.css';

const STATUS_TABS = ['All tours', 'Booked', 'Confirmed', 'Started', 'Finished', 'Cancelled'];
const STEPS = ['Booked', 'Confirmed', 'Started', 'Finished'];
const CANCELLED_STEPS = ['Booked', 'Confirmed', 'Cancelled'];

function PinIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#6a9ab5" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M21 10c0 7-9 13-9 13S3 17 3 10a9 9 0 0 1 18 0z"/>
      <circle cx="12" cy="10" r="3"/>
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
function AgentIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#4a7d96" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="2" y="3" width="20" height="18" rx="2"/><path d="M8 7h.01"/><path d="M12 7h.01"/><path d="M16 7h.01"/>
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
function PhoneIcon() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#4a7d96" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M22 16.92v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6A19.79 19.79 0 0 1 2.12 4.18 2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72c.127.96.361 1.903.7 2.81a2 2 0 0 1-.45 2.11L8.09 9.91a16 16 0 0 0 6 6l1.27-1.27a2 2 0 0 1 2.11-.45c.907.339 1.85.573 2.81.7A2 2 0 0 1 22 16.92z"/>
    </svg>
  );
}
function WhatsAppIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="#25D366">
      <path d="M17.472 14.382c-.297-.149-1.758-.867-2.03-.967-.273-.099-.471-.148-.67.15-.197.297-.767.966-.94 1.164-.173.199-.347.223-.644.075-.297-.15-1.255-.463-2.39-1.475-.883-.788-1.48-1.761-1.653-2.059-.173-.297-.018-.458.13-.606.134-.133.298-.347.446-.52.149-.174.198-.298.298-.497.099-.198.05-.371-.025-.52-.075-.149-.669-1.612-.916-2.207-.242-.579-.487-.5-.669-.51-.173-.008-.371-.01-.57-.01-.198 0-.52.074-.792.372-.272.297-1.04 1.016-1.04 2.479 0 1.462 1.065 2.875 1.213 3.074.149.198 2.096 3.2 5.077 4.487.709.306 1.262.489 1.694.625.712.227 1.36.195 1.871.118.571-.085 1.758-.719 2.006-1.413.248-.694.248-1.289.173-1.413-.074-.124-.272-.198-.57-.347m-5.421 7.403h-.004a9.87 9.87 0 01-5.031-1.378l-.361-.214-3.741.982.998-3.648-.235-.374a9.86 9.86 0 01-1.51-5.26c.001-5.45 4.436-9.884 9.888-9.884 2.64 0 5.122 1.03 6.988 2.898a9.825 9.825 0 012.893 6.994c-.003 5.45-4.437 9.884-9.885 9.884m8.413-18.297A11.815 11.815 0 0012.05 0C5.495 0 .16 5.335.157 11.892c0 2.096.547 4.142 1.588 5.945L.057 24l6.305-1.654a11.882 11.882 0 005.683 1.448h.005c6.554 0 11.89-5.335 11.893-11.893a11.821 11.821 0 00-3.48-8.413z"/>
    </svg>
  );
}
function MessengerIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="#0084FF">
      <path d="M12 0C5.373 0 0 4.974 0 11.111c0 3.498 1.744 6.614 4.469 8.654V24l4.088-2.242c1.092.3 2.246.464 3.443.464 6.627 0 12-4.974 12-11.111S18.627 0 12 0zm1.191 14.963l-3.055-3.26-5.963 3.26L10.732 8l3.131 3.259L19.752 8l-6.561 6.963z"/>
    </svg>
  );
}
function TelegramIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="#0088CC">
      <path d="M11.944 0A12 12 0 0 0 0 12a12 12 0 0 0 12 12 12 12 0 0 0 12-12A12 12 0 0 0 12 0a12 12 0 0 0-.056 0zm4.962 7.224c.1-.002.321.023.465.14a.506.506 0 0 1 .171.325c.016.093.036.306.02.472-.18 1.898-.962 6.502-1.36 8.627-.168.9-.499 1.201-.82 1.23-.696.065-1.225-.46-1.9-.902-1.056-.693-1.653-1.124-2.678-1.8-1.185-.78-.417-1.21.258-1.91.177-.184 3.247-2.977 3.307-3.23.007-.032.014-.15-.056-.212s-.174-.041-.249-.024c-.106.024-1.793 1.14-5.061 3.345-.48.33-.913.49-1.302.48-.428-.008-1.252-.241-1.865-.44-.752-.245-1.349-.374-1.297-.789.027-.216.325-.437.893-.663 3.498-1.524 5.83-2.529 6.998-3.014 3.332-1.386 4.025-1.627 4.476-1.635z"/>
    </svg>
  );
}
function getMessengerIcon(messenger) {
  if (!messenger) return null;
  const type = messenger.toLowerCase();
  if (type.includes('whatsapp')) return <WhatsAppIcon />;
  if (type.includes('messenger')) return <MessengerIcon />;
  if (type.includes('telegram')) return <TelegramIcon />;
  return <MessengerIcon />; // default
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
      <line x1="18" y1="6" x2="6" y2="18"/>
      <line x1="6" y1="6" x2="18" y2="18"/>
    </svg>
  );
}

function getCanceledByLabel(canceledBy, currentUserId) {
  if (!canceledBy) return 'Tourist';
  if (currentUserId && canceledBy === currentUserId) return 'Tourist';

  const normalized = String(canceledBy).toLowerCase();
  if (normalized.includes('tourist') || normalized.includes('user')) return 'Tourist';
  if (normalized.includes('agent')) return 'Travel agent';

  return 'Travel agent';
}

function getStepIndex(state) {
  const map = { BOOKED: 0, DOCUMENTS_VERIFIED: 0, CONFIRMED: 1, STARTED: 2, FINISHED: 3, CANCELLED: -1 };
  return map[state] ?? 0;
}

function ProgressStepper({ state }) {
  const current = getStepIndex(state);
  const isCancelled = state === 'CANCELLED';

  if (isCancelled) {
    return (
      <div className="mt-stepper">
        {CANCELLED_STEPS.map((step, i) => {
          const done = i < CANCELLED_STEPS.length - 1;
          const cancelled = i === CANCELLED_STEPS.length - 1;
          return (
            <div key={step} className={`mt-step ${done ? 'done' : ''} ${cancelled ? 'cancelled' : ''} ${i === 0 ? 'first' : ''} ${i === CANCELLED_STEPS.length - 1 ? 'last' : ''}`}>
              <span className="mt-step-content">
                {done && <CheckIcon />}
                {cancelled && <CrossIcon />}
                <span className="mt-step-label">{step}</span>
              </span>
            </div>
          );
        })}
      </div>
    );
  }

  return (
    <div className="mt-stepper">
      {STEPS.map((step, i) => {
        const done = i <= current;
        return (
          <div key={step} className={`mt-step ${done ? 'done' : ''} ${i === 0 ? 'first' : ''} ${i === STEPS.length - 1 ? 'last' : ''}`}>
            <span className="mt-step-content">
              {done && <CheckIcon />}
              <span className="mt-step-label">{step}</span>
            </span>
          </div>
        );
      })}
    </div>
  );
}

function BookingCard({ booking, onCancel, onEdit, onUpload, currentUserId, feedbackData, onFeedback }) {
  const { tourDetails, travelAgent } = booking;
  const canceledByLabel = getCanceledByLabel(booking.canceledBy, currentUserId);
  const cancelReason = booking.cancelReason?.trim() || '-';
  const hasFeedback = Boolean(feedbackData?.[booking.id]);
  const isFinished = booking.state === 'FINISHED';
  const isStarted = booking.state === 'STARTED';
  const isCancelled = booking.state === 'CANCELLED';
  const isConfirmed = booking.state === 'CONFIRMED';
  const hasDocuments = tourDetails?.documents && tourDetails.documents !== '0 items';
  // Cancel is allowed for BOOKED, DOCUMENTS_VERIFIED, CONFIRMED
  const showCancelAction = !isFinished && !isCancelled && !isStarted;
  // Edit/Upload only for BOOKED and DOCUMENTS_VERIFIED (not CONFIRMED)
  const showEditActions = showCancelAction && !isConfirmed;
  const showGiveFeedback = isStarted && !hasFeedback;
  const showUpdateFeedback = isFinished && hasFeedback;
  const showActionsRow = showCancelAction || showEditActions || showGiveFeedback || showUpdateFeedback;

  return (
    <div className={`mt-card ${booking.state === 'CANCELLED' ? 'mt-card-cancelled' : ''}`}>
      <ProgressStepper state={booking.state} />

      <div className="mt-card-body">
        {/* Tour header */}
        <div className="mt-tour-header">
          {booking.tourImageUrl && (
            <img className="mt-tour-img" src={booking.tourImageUrl} alt={booking.name} />
          )}
          <div>
            <h3 className="mt-tour-name">{booking.name}</h3>
            <div className="mt-tour-dest"><PinIcon /> {booking.destination}</div>
          </div>
        </div>

        {/* Details grid */}
        <div className="mt-details-grid">
          {/* Left: Tour details */}
          <div className="mt-details-col">
            <h4 className="mt-col-title">Tour details</h4>
            {tourDetails?.date && (
              <div className="mt-detail-row"><CalIcon /><span>{tourDetails.date}</span></div>
            )}
            {tourDetails?.mealPlan && (
              <div className="mt-detail-row"><ForkIcon /><span>{tourDetails.mealPlan}</span></div>
            )}
            {tourDetails?.guests && (
              <div className="mt-detail-row"><PersonIcon /><span>{tourDetails.guests}</span></div>
            )}
            {tourDetails?.totalPrice && (
              <div className="mt-detail-row"><WalletIcon /><span>Total price <strong>{tourDetails.totalPrice}</strong></span></div>
            )}
            <div className="mt-detail-row"><DocIcon /><span>Documents uploaded: {tourDetails?.documents || '0 items'}</span></div>
          </div>

          {/* Right: Travel agent */}
          <div className="mt-details-col">
            <h4 className="mt-col-title">Travel agent</h4>
            {travelAgent ? (
              <>
                {travelAgent.name && <div className="mt-detail-row"><AgentIcon /><span>{travelAgent.name}</span></div>}
                {travelAgent.email && <div className="mt-detail-row"><EmailIcon /><span>{travelAgent.email}</span></div>}
                {travelAgent.phone && <div className="mt-detail-row"><PhoneIcon /><span>{travelAgent.phone}</span></div>}
                {travelAgent.messenger && <div className="mt-detail-row">{getMessengerIcon(travelAgent.messenger)}<span>{travelAgent.messenger}</span></div>}
              </>
            ) : (
              <div className="mt-detail-row"><span className="mt-no-agent">Not assigned yet</span></div>
            )}
          </div>
        </div>
      </div>

      {/* Actions */}
      {showActionsRow && (
        <div className="mt-card-actions">
          {showCancelAction && (
            <button className="mt-btn-outline" onClick={() => onCancel(booking.id)}>Cancel</button>
          )}
          {showEditActions && (
            <>
              <button className="mt-btn-outline" onClick={() => onEdit(booking)}>Edit</button>
              <button className="mt-btn-solid" onClick={() => onUpload(booking)}>
                {hasDocuments ? 'Update documents' : 'Upload documents'}
              </button>
            </>
          )}
          {showGiveFeedback && (
            <button className="mt-btn-solid mt-btn-feedback-only" onClick={() => onFeedback(booking)}>
              Give feedback
            </button>
          )}
          {showUpdateFeedback && (
            <button className="mt-btn-solid mt-btn-feedback-only" onClick={() => onFeedback(booking)}>
              Update feedback
            </button>
          )}
        </div>
      )}

      {booking.state === 'CANCELLED' && (
        <div className="mt-cancelled-info">
          <div className="mt-cancel-row">
            <span className="mt-cancel-label">Cancelled by:</span>
            <span className="mt-cancel-value">{canceledByLabel}</span>
          </div>
          <div className="mt-cancel-row">
            <span className="mt-cancel-label">Reason:</span>
            <span className="mt-cancel-value">{cancelReason}</span>
          </div>
        </div>
      )}
    </div>
  );
}

export default function MyToursPage() {
  const { user } = useAuth();
  const [activeTab, setActiveTab] = useState('my');
  const [statusFilter, setStatusFilter] = useState('All tours');
  const [bookings, setBookings] = useState([]);
  const [loading, setLoading] = useState(true);
  const initialLoadDone = useRef(false);
  const [editingBooking, setEditingBooking] = useState(null);
  const [confirmData, setConfirmData] = useState(null);
  const [lastEditedBooking, setLastEditedBooking] = useState(null);
  const [cancellingBooking, setCancellingBooking] = useState(null);
  const [uploadingBooking, setUploadingBooking] = useState(null);

  // ── Feedback state ─────────────────────────────────────────────
  // Map of bookingId → { rating, comment } | null (null = no feedback)
  const [feedbackData, setFeedbackData] = useState({});
  const [feedbackModalBooking, setFeedbackModalBooking] = useState(null);
  const [feedbackSubmitting, setFeedbackSubmitting] = useState(false);
  const [successAlert, setSuccessAlert] = useState(null); // string message

  const fetchBookings = useCallback(async () => {
    if (!user?.id) return;
    if (!initialLoadDone.current) setLoading(true);
    try {
      const res = await client.get(`/bookings?userId=${user.id}`);
      const loaded = res.data.bookings || [];
      setBookings(loaded);
      // Pre-load feedback for Started and Finished bookings
      const relevant = loaded.filter(
        (b) => b.state === 'STARTED' || b.state === 'FINISHED'
      );
      if (relevant.length > 0) {
        const results = await Promise.allSettled(
          relevant.map((b) => getFeedback(b.id).then((fb) => ({ id: b.id, fb })))
        );
        const map = {};
        results.forEach((r) => {
          if (r.status === 'fulfilled') {
            map[r.value.id] = r.value.fb; // null if no feedback
          }
        });
        setFeedbackData((prev) => ({ ...prev, ...map }));
      }
    } catch (err) {
      console.error('Failed to fetch bookings:', err);
      setBookings([]);
    } finally {
      setLoading(false);
      initialLoadDone.current = true;
    }
  }, [user?.id]);

  useEffect(() => {
    fetchBookings();
  }, [fetchBookings]);

  // Auto-refresh every 15 seconds to pick up state changes from agent
  useEffect(() => {
    const interval = setInterval(fetchBookings, 15000);
    return () => clearInterval(interval);
  }, [fetchBookings]);

  const handleCancel = (bookingId) => {
    const booking = bookings.find(b => b.id === bookingId);
    if (booking) {
      setCancellingBooking(booking);
    }
  };

  const handleConfirmCancelBooking = async () => {
    if (!cancellingBooking) return;
    try {
      await client.patch(`/bookings/${cancellingBooking.id}/cancel`);
      setCancellingBooking(null);
      fetchBookings();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to cancel booking');
      setCancellingBooking(null);
    }
  };

  const handleKeepBooking = () => {
    setCancellingBooking(null);
  };

  const handleEdit = (booking) => {
    setEditingBooking(booking);
  };

  const handleUploadDocuments = (booking) => {
    setUploadingBooking(booking);
  };

  const handleUploadSuccess = ({ bookingId, documentCount } = {}) => {
    if (!bookingId) {
      return;
    }

    setBookings(prevBookings => prevBookings.map(booking => (
      booking.id === bookingId
        ? { ...booking, documentCount }
        : booking
    )));

    setUploadingBooking(prevBooking => (
      prevBooking && prevBooking.id === bookingId
        ? { ...prevBooking, documentCount }
        : prevBooking
    ));
  };

  const handleEditSaved = (response) => {
    setLastEditedBooking(response.previewBooking || editingBooking);
    setEditingBooking(null);
    // Show confirmation modal with changes
    setConfirmData(response);
  };

  const handleConfirmChanges = async () => {
    try {
      if (confirmData?.bookingId && confirmData?.payload) {
        await client.put(`/bookings/${confirmData.bookingId}`, confirmData.payload);
      }
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to finalize booking changes');
    } finally {
      setConfirmData(null);
      fetchBookings();
    }
  };

  const handleDeclineChanges = () => {
    setConfirmData(null);
  };

  // ── Feedback handlers ──────────────────────────────────────────
  const handleFeedbackClick = (booking) => {
    setFeedbackModalBooking(booking);
  };

  const handleFeedbackClose = () => {
    setFeedbackModalBooking(null);
  };

  const handleFeedbackSubmit = async ({ rating, comment }) => {
    if (!feedbackModalBooking) return;
    const bookingId = feedbackModalBooking.id;
    const existing = feedbackData[bookingId];
    setFeedbackSubmitting(true);
    try {
      let saved;
      if (existing) {
        saved = await updateFeedback(bookingId, { rating, comment });
      } else {
        saved = await createFeedback(bookingId, { rating, comment });
      }
      // Store returned feedback (fall back to submitted values if API returns nothing)
      const stored = saved || { rating, comment };
      setFeedbackData((prev) => ({ ...prev, [bookingId]: stored }));
      setFeedbackModalBooking(null);
      setSuccessAlert('Your feedback has been submitted successfully.');
    } catch (err) {
      console.error('Failed to submit feedback:', err);
      // Surface error to user without crashing
      alert(err?.response?.data?.message || 'Failed to submit feedback. Please try again.');
    } finally {
      setFeedbackSubmitting(false);
    }
  };

  // Auto-dismiss success alert after 5 s
  useEffect(() => {
    if (!successAlert) return;
    const t = setTimeout(() => setSuccessAlert(null), 5000);
    return () => clearTimeout(t);
  }, [successAlert]);

  const filtered = statusFilter === 'All tours'
    ? bookings
    : bookings.filter(b => {
        const filterState = statusFilter.toUpperCase();
        if (filterState === 'BOOKED') return b.state === 'BOOKED' || b.state === 'DOCUMENTS_VERIFIED';
        return b.state === filterState;
      });

  return (
    <div className="app">
      <Header activeTab={activeTab} onTabChange={setActiveTab} />

      <div className="main-content">
        {/* Status filter tabs */}
        <div className="mt-status-tabs">
          {STATUS_TABS.map(tab => (
            <button
              key={tab}
              className={`mt-status-tab ${statusFilter === tab ? 'active' : ''}`}
              onClick={() => setStatusFilter(tab)}
            >
              {tab}
            </button>
          ))}
        </div>

        {/* Bookings grid */}
        {loading ? (
          <div className="mt-loading">Loading your bookings…</div>
        ) : filtered.length === 0 ? (
          <div className="mt-empty">
            <p>No bookings found{statusFilter !== 'All tours' ? ` with status "${statusFilter}"` : ''}.</p>
          </div>
        ) : (
          <div className="mt-bookings-grid">
            {filtered.map(booking => (
              <BookingCard
                key={booking.id}
                booking={booking}
                onCancel={handleCancel}
                onEdit={handleEdit}
                onUpload={handleUploadDocuments}
                currentUserId={user?.id}
                feedbackData={feedbackData}
                onFeedback={handleFeedbackClick}
              />
            ))}
          </div>
        )}
      </div>

      {/* Cancel Booking Modal */}
      {cancellingBooking && (
        <CancelBookingModal
          booking={cancellingBooking}
          onConfirmCancel={handleConfirmCancelBooking}
          onKeep={handleKeepBooking}
        />
      )}

      {/* Edit Booking Modal */}
      {editingBooking && (
        <EditBookingModal
          booking={editingBooking}
          onClose={() => setEditingBooking(null)}
          onSaved={handleEditSaved}
        />
      )}

      {/* Confirm Changes Modal */}
      {confirmData && (
        <ConfirmChangesModal
          booking={confirmData.previewBooking || lastEditedBooking || confirmData}
          changes={confirmData.changes || []}
          onConfirm={handleConfirmChanges}
          onDecline={handleDeclineChanges}
        />
      )}

      {/* Feedback Modal */}
      {feedbackModalBooking && (
        <FeedbackModal
          booking={feedbackModalBooking}
          existingFeedback={feedbackData[feedbackModalBooking.id] || null}
          onClose={handleFeedbackClose}
          onSubmit={handleFeedbackSubmit}
          submitting={feedbackSubmitting}
        />
      )}

      {/* Success Alert */}
      {successAlert && (
        <div className="mt-success-alert" role="status" aria-live="polite">
          <div className="mt-success-alert__icon">
            <SuccessCheckIcon />
          </div>
          <div className="mt-success-alert__text">
            <div className="mt-success-alert__title">Success</div>
            <div className="mt-success-alert__msg">{successAlert}</div>
          </div>
          <button
            className="mt-success-alert__close"
            onClick={() => setSuccessAlert(null)}
            aria-label="Dismiss"
          >
            <AlertCloseIcon />
          </button>
        </div>
      )}

      {/* Document Upload Modal */}
      {uploadingBooking && (
        <DocumentUploadModal
          booking={uploadingBooking}
          onClose={() => setUploadingBooking(null)}
          onSuccess={handleUploadSuccess}
        />
      )}
    </div>
  );
}

function SuccessCheckIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <circle cx="12" cy="12" r="10" fill="#118819" />
      <polyline
        points="8 12 11 15 16 9"
        stroke="#ffffff"
        strokeWidth="2.2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function AlertCloseIcon() {
  return (
    <svg
      width="16"
      height="16"
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="2.2"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      <line x1="18" y1="6" x2="6" y2="18" />
      <line x1="6" y1="6" x2="18" y2="18" />
    </svg>
  );
}
