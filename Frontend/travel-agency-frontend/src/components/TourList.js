import React from 'react';
import TourCard from './TourCard';
import './TourList.css';

const SORT_OPTIONS = [
  { value: 'RATING_DESC', label: 'Top rated first' },
  { value: 'PRICE_ASC', label: 'Price: low first' },
  { value: 'PRICE_DESC', label: 'Price: high first' },
  { value: 'DATE_ASC', label: 'Earliest start' },
];

export default function TourList({
  tours, totalItems, totalPages, loading, hasFilters,
  onClearFilters, sortBy, onSortChange, page, onPageChange,
}) {
  return (
    <div className="tour-list-section">
      <div className="results-meta">
        <p className="results-count">
          {loading ? (
            <span className="loading-text">Searching\u2026</span>
          ) : (
            <>
              <strong>{totalItems.toLocaleString()}</strong> tours match your filters.
              {hasFilters && (
                <button className="clear-filters-btn" onClick={onClearFilters}>
                  Clear all filters
                </button>
              )}
            </>
          )}
        </p>

        <div className="sort-wrapper">
          <span className="sort-label">Sort by:</span>
          <div className="sort-select-wrap">
            <select
              className="sort-select"
              value={sortBy}
              onChange={e => onSortChange(e.target.value)}
            >
              {SORT_OPTIONS.map(opt => (
                <option key={opt.value} value={opt.value}>{opt.label}</option>
              ))}
            </select>
            <span className="sort-caret">▾</span>
          </div>
        </div>
      </div>

      {loading && (
        <div className="tours-grid">
          {[1, 2, 3, 4].map(i => (
            <div key={i} className="skeleton-card" />
          ))}
        </div>
      )}

      {!loading && tours.length > 0 && (
        <div className="tours-grid">
          {tours.map((tour, i) => (
            <TourCard key={tour.id} tour={tour} index={i} />
          ))}
        </div>
      )}

      {!loading && tours.length === 0 && (
        <div className="empty-state">
          <p>No tours found matching your criteria.</p>
          {hasFilters && (
            <button className="clear-filters-btn prominent" onClick={onClearFilters}>
              Clear all filters
            </button>
          )}
        </div>
      )}

      {!loading && totalPages > 1 && (
        <div className="pagination">
          <button
            className="page-btn"
            disabled={page <= 1}
            onClick={() => onPageChange(page - 1)}
          >
            ‹ Prev
          </button>

          {Array.from({ length: totalPages }, (_, i) => i + 1)
            .filter(p => p === 1 || p === totalPages || Math.abs(p - page) <= 2)
            .reduce((acc, p, idx, arr) => {
              if (idx > 0 && p - arr[idx - 1] > 1) acc.push('\u2026');
              acc.push(p);
              return acc;
            }, [])
            .map((item, idx) =>
              item === '\u2026' ? (
                <span key={`ellipsis-${idx}`} className="page-ellipsis">\u2026</span>
              ) : (
                <button
                  key={item}
                  className={`page-btn ${item === page ? 'active' : ''}`}
                  onClick={() => onPageChange(item)}
                >
                  {item}
                </button>
              )
            )}

          <button
            className="page-btn"
            disabled={page >= totalPages}
            onClick={() => onPageChange(page + 1)}
          >
            Next ›
          </button>
        </div>
      )}
    </div>
  );
}
