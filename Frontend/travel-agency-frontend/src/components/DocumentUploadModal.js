import React, { useState, useCallback, useEffect, useRef } from 'react';
import './DocumentUploadModal.css';
import { uploadDocument, listBookingDocuments, deleteDocument } from '../api/documents';
import Toast from './Toast';

function CloseIcon() {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
      <line x1="18" y1="6" x2="6" y2="18" />
      <line x1="6" y1="6" x2="18" y2="18" />
    </svg>
  );
}

function CheckmarkIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="#00b894" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <circle cx="12" cy="12" r="10" />
      <polyline points="9 12 11 14 15 10" />
    </svg>
  );
}

function TrashIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="3 6 5 6 21 6" />
      <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" />
      <line x1="10" y1="11" x2="10" y2="17" />
      <line x1="14" y1="11" x2="14" y2="17" />
    </svg>
  );
}

function UploadIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
      <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
      <polyline points="17 8 12 3 7 8" />
      <line x1="12" y1="3" x2="12" y2="15" />
    </svg>
  );
}

function shortenFileName(name, maxBaseLength = 18) {
  if (!name) return '';

  const dotIndex = name.lastIndexOf('.');
  const hasExtension = dotIndex > 0 && dotIndex < name.length - 1;
  const base = hasExtension ? name.slice(0, dotIndex) : name;
  const extension = hasExtension ? name.slice(dotIndex) : '';

  if (base.length <= maxBaseLength) return name;
  return `${base.slice(0, maxBaseLength)}...${extension}`;
}

export default function DocumentUploadModal({ booking, onClose, onSuccess }) {
  const [activeTab, setActiveTab] = useState('');
  const [files, setFiles] = useState({});
  const [loading, setLoading] = useState(false);
  const [deleting, setDeleting] = useState(null); // Track which doc is being deleted
  const [error, setError] = useState('');
  const [documents, setDocuments] = useState([]);
  const [loadingDocs, setLoadingDocs] = useState(true);
  const [toast, setToast] = useState(null);
  const fileInputRef = useRef(null);

  const guestCount = (booking.personalDetails || []).length;

  // Build tabs from booking personal details
  const guestTabs = (booking.personalDetails || []).map((pd, idx) => ({
    id: `GUEST_${idx + 1}`,
    label: `Passport ${pd.firstName} ${pd.lastName}`,
    type: 'PASSPORT',
    guestName: `${pd.firstName} ${pd.lastName}`,
  }));

  const paymentTab = {
    id: 'payment',
    label: 'Payment confirmation',
    type: 'PAYMENT_CONFIRMATION',
    guestName: null,
  };

  const tabs = [...guestTabs, paymentTab];

  // Set first tab as active on mount
  useEffect(() => {
    if (tabs.length > 0 && !activeTab) {
      setActiveTab(tabs[0].id);
    }
  }, [tabs, activeTab]);

  // Fetch existing documents
  useEffect(() => {
    const fetchDocs = async () => {
      try {
        setLoadingDocs(true);
        const docs = await listBookingDocuments(booking.id);
        setDocuments(Array.isArray(docs) ? docs : []);
      } catch (err) {
        console.error('Failed to fetch documents:', err);
        setDocuments([]);
      } finally {
        setLoadingDocs(false);
      }
    };
    fetchDocs();
  }, [booking.id]);

  const currentTab = tabs.find(t => t.id === activeTab);

  const getNextPaymentGuest = useCallback(() => {
    const personalDetails = booking.personalDetails || [];
    const assignedPaymentGuestIds = new Set(
      documents
        .filter(doc => doc.documentType === 'PAYMENT_CONFIRMATION' && doc.guestId)
        .map(doc => doc.guestId)
    );

    return personalDetails
      .map((pd, idx) => ({
        guestId: `GUEST_${idx + 1}`,
        guestName: `${pd.firstName || ''} ${pd.lastName || ''}`.trim(),
      }))
      .find(guest => !assignedPaymentGuestIds.has(guest.guestId)) || null;
  }, [booking.personalDetails, documents]);

  // Helper function to get documents for current tab
  const getTabDocuments = () => {
    if (!currentTab) return [];
    return documents.filter(doc => {
      if (currentTab.type === 'PAYMENT_CONFIRMATION') {
        return doc.documentType === 'PAYMENT_CONFIRMATION';
      }
      return doc.guestId === currentTab.id && doc.documentType === 'PASSPORT';
    });
  };

  const tabDocuments = getTabDocuments();
  const hasDocument = tabDocuments.length > 0;
  const isPaymentTab = currentTab?.type === 'PAYMENT_CONFIRMATION';
  const isPassportTab = currentTab?.type === 'PASSPORT';
  const nextPaymentGuest = isPaymentTab ? getNextPaymentGuest() : null;
  const isPaymentLimitReached = isPaymentTab && tabDocuments.length >= guestCount;
  const isSaveDisabled =
    loading ||
    !files[activeTab] ||
    (isPassportTab && tabDocuments.length > 0) ||
    isPaymentLimitReached;

  const handleFileSelect = useCallback((e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Only allow single upload for passport documents
    if (currentTab.type === 'PASSPORT' && tabDocuments.length > 0) {
      setError('Document already uploaded. Delete it first to upload a new one.');
      setTimeout(() => setError(''), 3000);
      return;
    }

    // For payment confirmation, limit to number of guests
    if (currentTab.type === 'PAYMENT_CONFIRMATION' && tabDocuments.length >= guestCount) {
      setError(`Maximum ${guestCount} payment receipt(s) allowed (one per guest).`);
      setTimeout(() => setError(''), 3000);
      return;
    }

    if (file.size > 2 * 1024 * 1024) {
      setError('File size exceeds 2MB limit');
      setTimeout(() => setError(''), 3000);
      return;
    }

    if (!file.type.includes('pdf') && !file.name.toLowerCase().endsWith('.pdf')) {
      setError('Only PDF files are allowed');
      setTimeout(() => setError(''), 3000);
      return;
    }

    setFiles(prev => ({
      ...prev,
      [activeTab]: file,
    }));
    setError('');
  }, [activeTab, tabDocuments, currentTab, guestCount]);

  const handleClickUpload = useCallback(() => {
    // Only block if it's a passport and document already exists
    if (currentTab.type === 'PASSPORT' && tabDocuments.length > 0) return;
    fileInputRef.current?.click();
  }, [tabDocuments, currentTab]);

  const handleDragOver = (e) => {
    e.preventDefault();
    e.stopPropagation();
  };

  const handleDrop = useCallback((e) => {
    e.preventDefault();
    e.stopPropagation();
    
    // Only allow single upload for passport documents
    if (currentTab.type === 'PASSPORT' && tabDocuments.length > 0) {
      setError('Document already uploaded. Delete it first to upload a new one.');
      setTimeout(() => setError(''), 3000);
      return;
    }

    // For payment confirmation, limit to number of guests
    if (currentTab.type === 'PAYMENT_CONFIRMATION' && tabDocuments.length >= guestCount) {
      setError(`Maximum ${guestCount} payment receipt(s) allowed (one per guest).`);
      setTimeout(() => setError(''), 3000);
      return;
    }

    const file = e.dataTransfer.files?.[0];
    if (file) {
      if (file.size > 2 * 1024 * 1024) {
        setError('File size exceeds 2MB limit');
        setTimeout(() => setError(''), 3000);
        return;
      }
      if (!file.type.includes('pdf') && !file.name.toLowerCase().endsWith('.pdf')) {
        setError('Only PDF files are allowed');
        setTimeout(() => setError(''), 3000);
        return;
      }
      setFiles(prev => ({
        ...prev,
        [activeTab]: file,
      }));
      setError('');
    }
  }, [activeTab, tabDocuments, currentTab, guestCount]);

  const handleUpload = async () => {
    if (!currentTab) return;

    const file = files[activeTab];
    if (!file) {
      setError('Please select a file');
      return;
    }

    setLoading(true);
    setError('');

    try {
      if (currentTab.type === 'PAYMENT_CONFIRMATION' && !nextPaymentGuest) {
        setError('Unable to map payment receipt to a guest for this booking.');
        setLoading(false);
        return;
      }
      const uploadGuestId = currentTab.type === 'PAYMENT_CONFIRMATION'
        ? nextPaymentGuest.guestId
        : currentTab.id;
      const uploadGuestName = currentTab.type === 'PAYMENT_CONFIRMATION'
        ? nextPaymentGuest.guestName
        : currentTab.guestName;

      console.log('Uploading document:', {
        bookingId: booking.id,
        type: currentTab.type,
        guestId: uploadGuestId,
        guestName: uploadGuestName,
        fileName: file.name,
        fileSize: file.size,
      });
      
      await uploadDocument(booking.id, currentTab.type, uploadGuestId, uploadGuestName, file);
      setFiles(prev => {
        const updated = { ...prev };
        delete updated[activeTab];
        return updated;
      });

      // Refresh documents list
      const docs = await listBookingDocuments(booking.id);
      const updatedDocs = Array.isArray(docs) ? docs : [];
      setDocuments(updatedDocs);

      const passportCount = updatedDocs.filter(doc => doc.documentType === 'PASSPORT').length;
      const paymentCount = updatedDocs.filter(doc => doc.documentType === 'PAYMENT_CONFIRMATION').length;
      const allPassportsUploaded = passportCount >= guestCount;
      const allPaymentsUploaded = paymentCount >= guestCount;

      if (allPassportsUploaded && allPaymentsUploaded && guestCount > 0) {
        setToast({
          title: 'Success',
          message: 'All documents have been uploaded successfully.',
        });
      } else {
        setToast({
          title: 'Success',
          message: 'Your document has been uploaded successfully.',
        });
      }

      onSuccess?.({ bookingId: booking.id, documentCount: updatedDocs.length });
    } catch (err) {
      console.error('Upload error details:', err);
      let message = 'Upload failed';
      
      if (err.response?.status === 400) {
        message = err.response?.data?.message || 'Invalid file (failed validation)';
      } else if (err.response?.status === 409) {
        message = err.response?.data?.message || 'Document already uploaded for this guest';
      } else if (err.response?.status === 413) {
        message = 'File too large';
      } else if (err.response?.data?.message) {
        message = err.response.data.message;
      } else if (err.message) {
        message = err.message;
      }
      
      setError(message);
    } finally {
      setLoading(false);
    }
  };

  const handleDeleteDocument = async (docId) => {
    setDeleting(docId);
    try {
      await deleteDocument(booking.id, docId);
      const docs = await listBookingDocuments(booking.id);
      setDocuments(Array.isArray(docs) ? docs : []);
      setError('');
      setToast({
        title: 'Success',
        message: 'Document deleted successfully.',
      });
    } catch (err) {
      const message = err.response?.data?.message || 'Failed to delete document';
      setError(message);
    } finally {
      setDeleting(null);
    }
  };

  return (
    <div className="dum-overlay" onClick={onClose}>
      {toast && (
        <Toast
          title={toast.title}
          message={toast.message}
          onClose={() => setToast(null)}
        />
      )}
      <div className="dum-modal" onClick={e => e.stopPropagation()}>
        <button className="dum-close" onClick={onClose} aria-label="Close">
          <CloseIcon />
        </button>

        <h2 className="dum-title">Upload documents</h2>

        <div className="dum-content">
          {/* Left: Tabs */}
          <div className="dum-tabs-section">
            <div className="dum-tabs">
              {tabs.map(tab => {
                const tabHasDoc = documents.some(doc => {
                  if (tab.type === 'PAYMENT_CONFIRMATION') {
                    return doc.documentType === 'PAYMENT_CONFIRMATION';
                  }
                  return doc.guestId === tab.id && doc.documentType === 'PASSPORT';
                });
                return (
                  <button
                    key={tab.id}
                    type="button"
                    className={`dum-tab ${activeTab === tab.id ? 'active' : ''}`}
                    onClick={() => setActiveTab(tab.id)}
                  >
                    <span>{tab.label}</span>
                    {tabHasDoc && <CheckmarkIcon />}
                  </button>
                );
              })}
            </div>
          </div>

          {/* Right: Upload area */}
          <div className="dum-upload-section">
            <h3 className="dum-upload-title">Add attachments</h3>

            {tabDocuments.length === 0 || currentTab?.type === 'PAYMENT_CONFIRMATION' ? (
              <div
                className="dum-drop-zone"
                onDragOver={handleDragOver}
                onDrop={handleDrop}
                onClick={handleClickUpload}
                role="button"
                tabIndex={0}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' || e.key === ' ') {
                    handleClickUpload();
                  }
                }}
              >
                <UploadIcon />
                <p className="dum-drop-text">Click to upload or drag and drop</p>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept=".pdf,application/pdf"
                  onChange={handleFileSelect}
                  style={{ display: 'none' }}
                  id="file-input"
                  aria-hidden="true"
                />
              </div>
            ) : (
              <div className="dum-drop-zone">
                <UploadIcon />
                <p className="dum-drop-text">Click to upload or drag and drop</p>
              </div>
            )}

            {/* Files list */}
            {tabDocuments.length > 0 && (
              <div className="dum-files-list">
                {tabDocuments.map(doc => (
                  <div key={doc.documentId} className="dum-file-item">
                    <div className="dum-file-info">
                      <span className="dum-file-icon">📄</span>
                      <div className="dum-file-details">
                        <p className="dum-file-name" title={doc.originalFileName}>
                          {shortenFileName(doc.originalFileName)}
                        </p>
                        <p className="dum-file-size">{(doc.fileSize / 1024).toFixed(0)} KB</p>
                      </div>
                    </div>
                    <button
                      type="button"
                      className="dum-delete-btn"
                      onClick={() => handleDeleteDocument(doc.documentId)}
                      aria-label="Delete"
                      disabled={deleting === doc.documentId}
                      title={deleting === doc.documentId ? 'Deleting...' : 'Delete'}
                    >
                      {deleting === doc.documentId ? '⏳' : <TrashIcon />}
                    </button>
                  </div>
                ))}
              </div>
            )}

            {/* Error message */}
            {error && <div className="dum-error">{error}</div>}
          </div>
        </div>

        {/* Actions */}
        <div className="dum-actions">
          <button type="button" className="dum-btn-cancel" onClick={onClose}>
            Cancel
          </button>
          <button
            type="button"
            className="dum-btn-save"
            onClick={handleUpload}
            disabled={isSaveDisabled}
            title={
              isPaymentLimitReached ? `Maximum ${guestCount} payment receipt(s) reached` :
              isPassportTab && tabDocuments.length > 0 ? 'Delete document first to upload a new one' :
              !files[activeTab] ? 'Select a file first' : ''
            }
          >
            {loading ? 'Uploading...' : 'Save'}
          </button>
        </div>
      </div>
    </div>
  );
}
