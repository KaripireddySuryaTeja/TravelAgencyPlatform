import React from 'react';
import BeachPanel from './BeachPanel';

export default function SplitLayout({ children }) {
  return (
    <div className="split-page">
      <div className="split">
        <div className="split__form">
          <div className="split__form-inner">{children}</div>
        </div>
        <BeachPanel />
      </div>
    </div>
  );
}
