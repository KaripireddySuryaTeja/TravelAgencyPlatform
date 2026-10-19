import React from 'react';
import beach from '../assets/beach.png';
import BrandHeader from './BrandHeader';

export default function BeachPanel() {
  return (
    <div className="split__panel" aria-hidden="true">
      <img className="split__panel-img" src={beach} alt="" />
      <div className="split__panel-overlay" />
      <BrandHeader />
      <h2 className="panel-heading">
        Let&rsquo;s plan<br />your next trip!
      </h2>
    </div>
  );
}
