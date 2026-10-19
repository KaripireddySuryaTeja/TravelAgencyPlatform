import React, { useState, useRef, useEffect, useCallback } from 'react';
import { buildApiUrl } from '../api/client';
import './SearchBar.css';

const DURATION_OPTIONS = [
  { value: '1-3', label: '1-3 days' },
  { value: '4-7', label: '4-7 days' },
  { value: '8-12', label: '8-12 days' },
  { value: '13+', label: '13+ days' },
];

const MEAL_OPTIONS = [
  { value: 'BB', label: 'Breakfast (BB)' },
  { value: 'HB', label: 'Half-board (HB)' },
  { value: 'FB', label: 'Full-board (FB)' },
  { value: 'AI', label: 'All inclusive (AI)' },
];

const TOUR_TYPE_OPTIONS = [
  { value: 'Resorts', label: 'Resorts' },
  { value: 'Cruises', label: 'Cruises' },
  { value: 'Hikes', label: 'Hikes' },
];

const MONTHS = [
  'January','February','March','April','May','June',
  'July','August','September','October','November','December',
];
const WEEKDAYS = ['M','T','W','T','F','S','S'];

function getCalendarDays(year, month) {
  const firstDay = new Date(year, month, 1);
  const numDays = new Date(year, month + 1, 0).getDate();
  const startOffset = (firstDay.getDay() + 6) % 7;
  const days = [];
  for (let i = 0; i < startOffset; i++) days.push(null);
  for (let d = 1; d <= numDays; d++) days.push(new Date(year, month, d));
  return days;
}

function isSameDay(a, b) {
  return a && b && a.toDateString() === b.toDateString();
}

function isBetween(day, start, end) {
  if (!start || !end) return false;
  return day >= start && day <= end;
}

function fmtShort(date) {
  if (!date) return '';
  return date.toLocaleDateString('en-US', { month: 'short', day: 'numeric' });
}

function ChevronDown() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
      <path d="M6 9l6 6 6-6"/>
    </svg>
  );
}
function ChevronUp() {
  return (
    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
      <path d="M6 15l6-6 6 6"/>
    </svg>
  );
}

function PinIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M21 10c0 7-9 13-9 13S3 17 3 10a9 9 0 0 1 18 0z"/>
      <circle cx="12" cy="10" r="3"/>
    </svg>
  );
}
function CalIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <rect x="3" y="4" width="18" height="18" rx="2"/>
      <line x1="16" y1="2" x2="16" y2="6"/>
      <line x1="8" y1="2" x2="8" y2="6"/>
      <line x1="3" y1="10" x2="21" y2="10"/>
    </svg>
  );
}
function PersonIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
      <circle cx="9" cy="7" r="4"/>
      <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
      <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
    </svg>
  );
}
function ForkIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M3 2v7c0 1.1.9 2 2 2h4a2 2 0 0 0 2-2V2"/>
      <path d="M7 2v20"/>
      <path d="M21 15V2a5 5 0 0 0-5 5v6c0 1.1.9 2 2 2h3zm0 0v7"/>
    </svg>
  );
}
function PalmIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M13 8c0-2.76-2.46-5-5.5-5S2 5.24 2 8h2l1-1 1 1h4"/>
      <path d="M13 7.14A5.82 5.82 0 0 1 16.5 6c3.04 0 5.5 2.24 5.5 5h-3l-1-1-1 1h-3"/>
      <path d="M5.89 9.71c-2.15 2.15-2.3 5.47-.35 7.43l4.24-4.25.7-.7.71-.71 2.12-2.12c-1.95-1.96-5.27-1.8-7.42.35z"/>
      <path d="M11 15.5c.5 2.5-.17 4.5-1 6.5h4c2-5.5-.5-11-3-14l-1 2 1 5.5z"/>
    </svg>
  );
}

function CheckboxOption({ checked, onChange, label }) {
  return (
    <label className="checkbox-option">
      <input type="checkbox" checked={checked} onChange={onChange} />
      <span className="custom-checkbox" />
      <span className="checkbox-label">{label}</span>
    </label>
  );
}

export default function SearchBar({
  destination, onDestinationChange,
  startDate, onStartDateChange,
  endDate, onEndDateChange,
  selectedDurations, onDurationsChange,
  adults, onAdultsChange,
  childCount, onChildCountChange,
  selectedMealPlans, onMealPlansChange,
  selectedTourTypes, onTourTypesChange,
  onSearch,
}) {
  const [openDropdown, setOpenDropdown] = useState(null);
  const [calYear, setCalYear] = useState(new Date().getFullYear());
  const [calMonth, setCalMonth] = useState(new Date().getMonth());
  const [suggestions, setSuggestions] = useState([]);
  const barRef = useRef(null);

  useEffect(() => {
    const handler = (e) => {
      if (barRef.current && !barRef.current.contains(e.target)) {
        setOpenDropdown(null);
      }
    };
    document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, []);

  useEffect(() => {
    if (destination.length < 2) { setSuggestions([]); return; }
    const t = setTimeout(async () => {
      try {
        const res = await fetch(buildApiUrl(`/tours/destinations?destination=${encodeURIComponent(destination)}`));
        if (res.ok) {
          const data = await res.json();
          setSuggestions(data.destinations || []);
        }
      } catch { setSuggestions([]); }
    }, 300);
    return () => clearTimeout(t);
  }, [destination]);

  const toggle = useCallback((name) => {
    setOpenDropdown(prev => prev === name ? null : name);
  }, []);

  const dateLabel = () => {
    if (!startDate && selectedDurations.length === 0) return 'Any start date, any duration';
    const parts = [];
    if (startDate) {
      parts.push(endDate ? `${fmtShort(startDate)} - ${fmtShort(endDate)}` : fmtShort(startDate));
    }
    if (selectedDurations.length > 0) {
      parts.push(selectedDurations.map(d => `${d} days`).join(', '));
    }
    return parts.join(', ');
  };

  const travelersLabel = () => {
    if (childCount === 0) return `${adults} adult${adults !== 1 ? 's' : ''}`;
    return `${adults} adult${adults !== 1 ? 's' : ''}, ${childCount} child${childCount !== 1 ? 'ren' : ''}`;
  };

  const mealLabel = () => {
    if (selectedMealPlans.length === 0) return 'Meal';
    const labels = MEAL_OPTIONS.filter(o => selectedMealPlans.includes(o.value)).map(o => o.label);
    return labels.length <= 2 ? labels.join(', ') : `${labels[0]},...`;
  };

  const tourTypeLabel = () =>
    selectedTourTypes.length === 0 ? 'Tour type' : selectedTourTypes.join(', ');

  const toggleDuration = (val) =>
    onDurationsChange(prev => prev.includes(val) ? prev.filter(d => d !== val) : [...prev, val]);

  const toggleMealPlan = (val) =>
    onMealPlansChange(prev => prev.includes(val) ? prev.filter(m => m !== val) : [...prev, val]);

  const toggleTourType = (val) =>
    onTourTypesChange(prev => prev.includes(val) ? prev.filter(t => t !== val) : [...prev, val]);

  const handleDayClick = (day) => {
    if (!startDate || (startDate && endDate)) {
      onStartDateChange(day);
      onEndDateChange(null);
    } else {
      if (day > startDate) {
        onEndDateChange(day);
      } else {
        onStartDateChange(day);
        onEndDateChange(null);
      }
    }
  };

  const calDays = getCalendarDays(calYear, calMonth);

  const prevMonth = () => {
    if (calMonth === 0) { setCalYear(y => y - 1); setCalMonth(11); }
    else setCalMonth(m => m - 1);
  };
  const nextMonth = () => {
    if (calMonth === 11) { setCalYear(y => y + 1); setCalMonth(0); }
    else setCalMonth(m => m + 1);
  };

  return (
    <div className="sb-wrap" ref={barRef}>
      <div className="search-bar-container">
      <div className="search-bar">
        {/* Location */}
        <div className={`filter-item location ${openDropdown === 'location' ? 'active' : ''}`} onClick={() => toggle('location')}>
          <span className="filter-icon"><PinIcon /></span>
          {openDropdown === 'location' ? (
            <input className="filter-inline-input" type="text" placeholder="Destination" value={destination} onChange={e => onDestinationChange(e.target.value)} onClick={e => e.stopPropagation()} autoFocus />
          ) : (
            <span className="filter-text">{destination || 'Destination'}</span>
          )}
          <span className="filter-caret">{openDropdown === 'location' ? <ChevronUp /> : <ChevronDown />}</span>
          {openDropdown === 'location' && suggestions.length > 0 && (
            <div className="dropdown loc-dd" onClick={e => e.stopPropagation()}>
              {suggestions.map((s, i) => (
                <div key={i} className="suggestion-item" onClick={() => { onDestinationChange(s); setOpenDropdown(null); }}>{s}</div>
              ))}
            </div>
          )}
        </div>

        <div className="filter-divider" />

        {/* Date & Duration */}
        <div className={`filter-item ${openDropdown === 'date' ? 'active' : ''}`} onClick={() => toggle('date')}>
          <span className="filter-icon"><CalIcon /></span>
          <span className="filter-text">{dateLabel()}</span>
          <span className="filter-caret">{openDropdown === 'date' ? <ChevronUp /> : <ChevronDown />}</span>
          {openDropdown === 'date' && (
            <div className="dropdown date-dd" onClick={e => e.stopPropagation()}>
              <div className="date-dd-inner">
                <div className="cal-section">
                  <p className="dd-section-title">Start date</p>
                  <div className="cal-nav">
                    <button className="cal-nav-btn" onClick={prevMonth}>‹</button>
                    <span className="cal-month-label">{MONTHS[calMonth]} {calYear}</span>
                    <button className="cal-nav-btn" onClick={nextMonth}>›</button>
                  </div>
                  <div className="cal-weekdays">
                    {WEEKDAYS.map((d, i) => <span key={i}>{d}</span>)}
                  </div>
                  <div className="cal-grid">
                    {calDays.map((day, i) => {
                      if (!day) return <span key={i} className="cal-day empty" />;
                      const isStart = isSameDay(day, startDate);
                      const isEnd = isSameDay(day, endDate);
                      const inRange = isBetween(day, startDate, endDate);
                      return (
                        <button key={i} className={`cal-day ${isStart || isEnd ? 'selected' : ''} ${inRange ? 'in-range' : ''}`} onClick={() => handleDayClick(day)}>
                          {day.getDate()}
                        </button>
                      );
                    })}
                  </div>
                </div>
                <div className="dur-section">
                  <p className="dd-section-title">Duration</p>
                  <div className="dur-options">
                    {DURATION_OPTIONS.map(opt => (
                      <CheckboxOption key={opt.value} checked={selectedDurations.includes(opt.value)} onChange={() => toggleDuration(opt.value)} label={opt.label} />
                    ))}
                  </div>
                </div>
              </div>
            </div>
          )}
        </div>

        <div className="filter-divider" />

        {/* Travelers */}
        <div className={`filter-item ${openDropdown === 'travelers' ? 'active' : ''}`} onClick={() => toggle('travelers')}>
          <span className="filter-icon"><PersonIcon /></span>
          <span className="filter-text">{travelersLabel()}</span>
          <span className="filter-caret">{openDropdown === 'travelers' ? <ChevronUp /> : <ChevronDown />}</span>
          {openDropdown === 'travelers' && (
            <div className="dropdown travelers-dd" onClick={e => e.stopPropagation()}>
              <div className="traveler-row">
                <span className="traveler-label">Adults</span>
                <div className="counter">
                  <button className="counter-btn" onClick={() => onAdultsChange(Math.max(1, adults - 1))}>−</button>
                  <span className="counter-val">{adults}</span>
                  <button className="counter-btn active" onClick={() => onAdultsChange(adults + 1)}>+</button>
                </div>
              </div>
              <div className="traveler-row">
                <span className="traveler-label">Children</span>
                <div className="counter">
                  <button className="counter-btn" onClick={() => onChildCountChange(Math.max(0, childCount - 1))}>−</button>
                  <span className="counter-val">{childCount}</span>
                  <button className="counter-btn active" onClick={() => onChildCountChange(childCount + 1)}>+</button>
                </div>
              </div>
            </div>
          )}
        </div>

        <div className="filter-divider" />

        {/* Meal plan */}
        <div className={`filter-item ${openDropdown === 'meal' ? 'active' : ''}`} onClick={() => toggle('meal')}>
          <span className="filter-icon"><ForkIcon /></span>
          <span className="filter-text">{mealLabel()}</span>
          <span className="filter-caret">{openDropdown === 'meal' ? <ChevronUp /> : <ChevronDown />}</span>
          {openDropdown === 'meal' && (
            <div className="dropdown meal-dd" onClick={e => e.stopPropagation()}>
              {MEAL_OPTIONS.map(opt => (
                <CheckboxOption key={opt.value} checked={selectedMealPlans.includes(opt.value)} onChange={() => toggleMealPlan(opt.value)} label={opt.label} />
              ))}
            </div>
          )}
        </div>

        <div className="filter-divider" />

        {/* Tour type */}
        <div className={`filter-item ${openDropdown === 'tourtype' ? 'active' : ''}`} onClick={() => toggle('tourtype')}>
          <span className="filter-icon"><PalmIcon /></span>
          <span className="filter-text">{tourTypeLabel()}</span>
          <span className="filter-caret">{openDropdown === 'tourtype' ? <ChevronUp /> : <ChevronDown />}</span>
          {openDropdown === 'tourtype' && (
            <div className="dropdown tourtype-dd" onClick={e => e.stopPropagation()}>
              {TOUR_TYPE_OPTIONS.map(opt => (
                <CheckboxOption key={opt.value} checked={selectedTourTypes.includes(opt.value)} onChange={() => toggleTourType(opt.value)} label={opt.label} />
              ))}
            </div>
          )}
        </div>

        <button className="search-btn" onClick={onSearch}>Search</button>
      </div>
      </div>
    </div>
  );
}
