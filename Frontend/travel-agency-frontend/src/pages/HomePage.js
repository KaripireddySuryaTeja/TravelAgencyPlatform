import React, { useState, useCallback, useEffect } from 'react';
import Header from '../components/Header';
import SearchBar from '../components/SearchBar';
import TourList from '../components/TourList';
import { buildApiUrl } from '../api/client';
import './HomePage.css';

export default function HomePage() {
  const [activeTab, setActiveTab] = useState('all');

  /* filter state */
  const [destination, setDestination] = useState('');
  const [startDate, setStartDate] = useState(null);
  const [endDate, setEndDate] = useState(null);
  const [selectedDurations, setSelectedDurations] = useState([]);
  const [adults, setAdults] = useState(1);
  const [childCount, setChildCount] = useState(0);
  const [selectedMealPlans, setSelectedMealPlans] = useState([]);
  const [selectedTourTypes, setSelectedTourTypes] = useState([]);
  const [sortBy, setSortBy] = useState('RATING_DESC');

  /* applied filters */
  const [appliedFilters, setAppliedFilters] = useState({ sortBy: 'RATING_DESC' });

  /* result state */
  const [tours, setTours] = useState([]);
  const [totalItems, setTotalItems] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [page, setPage] = useState(1);
  const [loading, setLoading] = useState(true);

  /* fetch */
  const fetchTours = useCallback(async (filters, pg) => {
    setLoading(true);
    try {
      const params = new URLSearchParams();
      if (filters.destination) params.set('destination', filters.destination);
      if (filters.startDate) params.set('startDate', filters.startDate);
      if (filters.endDate) params.set('endDate', filters.endDate);
      if (filters.duration) params.set('duration', filters.duration);
      if (filters.adults != null) params.set('adults', filters.adults);
      if (filters.children != null) params.set('children', filters.children);
      if (filters.mealPlan) params.set('mealPlan', filters.mealPlan);
      if (filters.tourType) params.set('tourType', filters.tourType);
      params.set('sortBy', filters.sortBy || 'RATING_DESC');
      params.set('page', pg ?? 1);
      params.set('pageSize', 6);

      const res = await fetch(buildApiUrl(`/tours/available?${params}`));
      if (res.ok) {
        const data = await res.json();
        setTours(data.tours ?? []);
        setTotalItems(data.totalItems ?? 0);
        setTotalPages(data.totalPages ?? 1);
      }
    } catch (err) {
      console.error('Failed to fetch tours:', err);
      setTours([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchTours(appliedFilters, page);
  }, [appliedFilters, page, fetchTours]);

  /* handlers */
  const handleSearch = () => {
    const filters = {
      destination,
      startDate: startDate ? `${startDate.getFullYear()}-${String(startDate.getMonth()+1).padStart(2,'0')}-${String(startDate.getDate()).padStart(2,'0')}` : undefined,
      endDate: endDate ? `${endDate.getFullYear()}-${String(endDate.getMonth()+1).padStart(2,'0')}-${String(endDate.getDate()).padStart(2,'0')}` : undefined,
      duration: selectedDurations.length > 0 ? selectedDurations[0] : undefined,
      adults,
      children: childCount,
      mealPlan: selectedMealPlans.length > 0 ? selectedMealPlans[0] : undefined,
      tourType: selectedTourTypes.length > 0 ? selectedTourTypes[0] : undefined,
      sortBy,
    };
    setAppliedFilters(filters);
    setPage(1);
  };

  const handleClearFilters = () => {
    setDestination('');
    setStartDate(null);
    setEndDate(null);
    setSelectedDurations([]);
    setAdults(1);
    setChildCount(0);
    setSelectedMealPlans([]);
    setSelectedTourTypes([]);
    setSortBy('RATING_DESC');
    setAppliedFilters({ sortBy: 'RATING_DESC' });
    setPage(1);
  };

  const handleSortChange = (val) => {
    setSortBy(val);
    setAppliedFilters(prev => ({ ...prev, sortBy: val }));
  };

  const hasFilters = !!(
    destination || startDate || selectedDurations.length > 0 ||
    adults !== 1 || childCount !== 0 ||
    selectedMealPlans.length > 0 || selectedTourTypes.length > 0
  );

  return (
    <div className="app">
      <Header activeTab={activeTab} onTabChange={setActiveTab} />

      <div className="main-content">
        <h1 className="dashboard-title">Search for your next tour</h1>

        <SearchBar
          destination={destination}
          onDestinationChange={setDestination}
          startDate={startDate}
          onStartDateChange={setStartDate}
          endDate={endDate}
          onEndDateChange={setEndDate}
          selectedDurations={selectedDurations}
          onDurationsChange={setSelectedDurations}
          adults={adults}
          onAdultsChange={setAdults}
          childCount={childCount}
          onChildCountChange={setChildCount}
          selectedMealPlans={selectedMealPlans}
          onMealPlansChange={setSelectedMealPlans}
          selectedTourTypes={selectedTourTypes}
          onTourTypesChange={setSelectedTourTypes}
          onSearch={handleSearch}
        />

        <TourList
          tours={tours}
          totalItems={totalItems}
          totalPages={totalPages}
          loading={loading}
          hasFilters={hasFilters}
          onClearFilters={handleClearFilters}
          sortBy={sortBy}
          onSortChange={handleSortChange}
          page={page}
          onPageChange={setPage}
        />
      </div>
    </div>
  );
}
