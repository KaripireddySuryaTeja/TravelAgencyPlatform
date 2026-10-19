import React, { useCallback, useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import Header from '../components/Header';
import TourDetail from '../components/TourDetail';
import { getTourById, getTourReviews } from '../api/tours';
import './TourDetailPage.css';

const REVIEWS_PAGE_SIZE = 4;

export default function TourDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();

  const [tour, setTour] = useState(null);
  const [tourLoading, setTourLoading] = useState(true);
  const [tourError, setTourError] = useState(null);

  const [reviewsData, setReviewsData] = useState(null);
  const [reviewsLoading, setReviewsLoading] = useState(true);
  const [reviewSort, setReviewSort] = useState('TOP_RATED_FIRST');
  const [reviewPage, setReviewPage] = useState(1);

  // Fetch tour
  useEffect(() => {
    let cancelled = false;
    setTourLoading(true);
    setTourError(null);

    getTourById(id)
      .then(data => { if (!cancelled) setTour(data); })
      .catch(err => {
        if (!cancelled) {
          setTourError(err?.error || err?.message || 'Failed to load tour');
        }
      })
      .finally(() => { if (!cancelled) setTourLoading(false); });

    return () => { cancelled = true; };
  }, [id]);

  // Fetch reviews
  const loadReviews = useCallback(async (sortBy, page) => {
    setReviewsLoading(true);
    try {
      const data = await getTourReviews(id, { sortBy, page, pageSize: REVIEWS_PAGE_SIZE });
      setReviewsData(data);
    } catch (err) {
      // Reviews failure shouldn't block the page; show empty.
      setReviewsData({ reviews: [], page: 1, totalPages: 1, totalItems: 0 });
    } finally {
      setReviewsLoading(false);
    }
  }, [id]);

  useEffect(() => {
    loadReviews(reviewSort, reviewPage);
  }, [loadReviews, reviewSort, reviewPage]);

  const handleSortChange = useCallback((s) => {
    setReviewSort(s);
    setReviewPage(1);
  }, []);

  const handleBack = useCallback(() => {
    navigate('/');
  }, [navigate]);

  if (tourLoading) {
    return (
      <>
        <Header activeTab="all" onTabChange={() => {}} />
        <div className="tour-detail-loading">Loading tour…</div>
      </>
    );
  }

  if (tourError || !tour) {
    return (
      <>
        <Header activeTab="all" onTabChange={() => {}} />
        <div className="tour-detail-error">
          <div>{tourError || 'Tour not found'}</div>
          <button type="button" className="back-link" onClick={handleBack}>
            Back to tours
          </button>
        </div>
      </>
    );
  }

  return (
    <>
      <Header activeTab="all" onTabChange={() => {}} />
      <TourDetail
        tour={tour}
        reviewsData={reviewsData}
        reviewsLoading={reviewsLoading}
        reviewSort={reviewSort}
        onReviewSortChange={handleSortChange}
        reviewPage={reviewPage}
        onReviewPageChange={setReviewPage}
        onBack={handleBack}
      />
    </>
  );
}
