import React, { useState, useEffect, useCallback } from 'react';
import { listBookingDocuments, downloadDocument } from '../api/documents';
import './DocumentViewerModal.css';

export default function DocumentViewerModal({ bookingId, bookingName, onClose }) {
  const [documents, setDocuments] = useState([]);
  const [loading, setLoading] = useState(true);
  const [downloading, setDownloading] = useState(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      setLoading(true);
      const docs = await listBookingDocuments(bookingId);
      if (!cancelled) {
        setDocuments(docs);
        setLoading(false);
      }
    })();
    return () => { cancelled = true; };
  }, [bookingId]);

  const handleKey = useCallback((e) => {
    if (e.key === 'Escape') onClose();
  }, [onClose]);

  useEffect(() => {
    document.addEventListener('keydown', handleKey);
    return () => document.removeEventListener('keydown', handleKey);
  }, [handleKey]);

  const handleDownload = async (doc) => {
    setDownloading(doc.id || doc.documentId);
    try {
      const blob = await downloadDocument(bookingId, doc.id || doc.documentId);
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = doc.originalFileName || 'document';
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
    } catch (err) {
      console.error('Download failed:', err);
    } finally {
      setDownloading(null);
    }
  };

  const formatType = (type) => {
    if (!type) return 'Document';
    return type.replace(/_/g, ' ').replace(/\b\w/g, c => c.toUpperCase());
  };

  const formatDate = (ts) => {
    if (!ts) return '';
    const d = new Date(ts);
    return d.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
  };

  return (
    <div className="dvm-overlay" onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="dvm-modal" role="dialog" aria-modal="true">
        <div className="dvm-header">
          <h2 className="dvm-title">Documents{bookingName ? ` — ${bookingName}` : ''}</h2>
          <button className="dvm-close" onClick={onClose} aria-label="Close">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/>
            </svg>
          </button>
        </div>

        <div className="dvm-body">
          {loading ? (
            <p className="dvm-loading">Loading documents…</p>
          ) : documents.length === 0 ? (
            <p className="dvm-empty">No documents found for this booking.</p>
          ) : (
            <ul className="dvm-list">
              {documents.map((doc) => (
                <li key={doc.id || doc.documentId} className="dvm-item">
                  <div className="dvm-item-icon">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#0c6d8f" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                      <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/><polyline points="14 2 14 8 20 8"/>
                    </svg>
                  </div>
                  <div className="dvm-item-info">
                    <span className="dvm-item-name">{doc.originalFileName || 'Document'}</span>
                    <span className="dvm-item-meta">
                      {formatType(doc.documentType)}
                      {doc.guestName && ` • ${doc.guestName}`}
                      {doc.uploadTimestamp && ` • ${formatDate(doc.uploadTimestamp)}`}
                    </span>
                  </div>
                  <button
                    className="dvm-download-btn"
                    onClick={() => handleDownload(doc)}
                    disabled={downloading === (doc.id || doc.documentId)}
                    title="Download"
                  >
                    {downloading === (doc.id || doc.documentId) ? '…' : (
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/>
                      </svg>
                    )}
                  </button>
                </li>
              ))}
            </ul>
          )}
        </div>
      </div>
    </div>
  );
}
