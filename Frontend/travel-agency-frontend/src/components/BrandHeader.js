import React from 'react';

export default function BrandHeader() {
  return (
    <div className="brand" aria-label="Travel Agency">
      <svg className="brand__mark" width="22" height="22" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
        <rect x="6" y="6" width="12" height="15" rx="2" stroke="currentColor" strokeWidth="1.8"/>
        <path d="M9 6V4.5A1.5 1.5 0 0 1 10.5 3h3A1.5 1.5 0 0 1 15 4.5V6" stroke="currentColor" strokeWidth="1.8"/>
        <line x1="6" y1="11" x2="18" y2="11" stroke="currentColor" strokeWidth="1.4"/>
      </svg>
      <span className="brand__name">Travel Agency</span>
    </div>
  );
}
