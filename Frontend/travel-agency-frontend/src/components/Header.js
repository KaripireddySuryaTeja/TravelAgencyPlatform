import React, { useState, useRef, useEffect } from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import './Header.css';

function LuggageIcon() {
  return (
    <svg width="30" height="30" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <rect x="6" y="6" width="12" height="15" rx="2" stroke="#0c6d8f" strokeWidth="1.8"/>
      <path d="M9 6V4.5A1.5 1.5 0 0 1 10.5 3h3A1.5 1.5 0 0 1 15 4.5V6" stroke="#0c6d8f" strokeWidth="1.8"/>
      <line x1="6" y1="11" x2="18" y2="11" stroke="#0c6d8f" strokeWidth="1.4"/>
      <line x1="9" y1="21" x2="9" y2="23" stroke="#0c6d8f" strokeWidth="1.8" strokeLinecap="round"/>
      <line x1="15" y1="21" x2="15" y2="23" stroke="#0c6d8f" strokeWidth="1.8" strokeLinecap="round"/>
    </svg>
  );
}

function ProfileIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <circle cx="12" cy="8" r="3" stroke="currentColor" strokeWidth="1.8"/>
      <path d="M5 19c0-3.314 3.134-6 7-6s7 2.686 7 6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"/>
    </svg>
  );
}

function SignOutIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <path d="M15 17l5-5-5-5" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
      <path d="M20 12H9" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"/>
      <path d="M12 20H6a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2h6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round"/>
    </svg>
  );
}

function UserCircleIcon() {
  return (
    <svg width="32" height="32" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
      <circle cx="12" cy="12" r="10" stroke="#1a3a4a" strokeWidth="1.5"/>
      <circle cx="12" cy="9.5" r="3" stroke="#1a3a4a" strokeWidth="1.5"/>
      <path d="M5.5 19.5c0-3.038 2.91-5.5 6.5-5.5s6.5 2.462 6.5 5.5" stroke="#1a3a4a" strokeWidth="1.5" strokeLinecap="round"/>
    </svg>
  );
}

export default function Header({ activeTab, onTabChange }) {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [profileDropdownOpen, setProfileDropdownOpen] = useState(false);
  const dropdownRef = useRef(null);
  const currentPath = `${location.pathname}${location.search}${location.hash}`;

  useEffect(() => {
    const handleClickOutside = (e) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target)) {
        setProfileDropdownOpen(false);
      }
    };
    if (profileDropdownOpen) {
      document.addEventListener('mousedown', handleClickOutside);
    }
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, [profileDropdownOpen]);

  const handleLogout = () => {
    setProfileDropdownOpen(false);
    logout();
  };

  const getRoleLabel = (role) => {
    switch (role) {
      case 'TRAVEL_AGENT': return 'Travel agent';
      case 'ADMIN': return 'Admin';
      default: return '';
    }
  };

  const getInitials = (name) => {
    if (!name) return 'U';
    return name
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0].toUpperCase())
      .join('');
  };

  const userName = user ? `${user.firstName} ${user.lastName}`.trim() : '';

  return (
    <header className="header">
      <div className="header-inner">
        <div className="logo">
          <LuggageIcon />
          <span className="logo-text">Travel Agency</span>
        </div>

        <nav className="nav">
          <button
            className={`nav-tab ${location.pathname === '/' || location.pathname.startsWith('/tours') ? 'active' : ''}`}
            onClick={() => { onTabChange && onTabChange('all'); navigate('/'); }}
          >
            All tours
          </button>
          {user?.role === 'TRAVEL_AGENT' ? (
            <button
              className={`nav-tab ${location.pathname === '/bookings' ? 'active' : ''}`}
              onClick={() => { onTabChange && onTabChange('bookings'); navigate('/bookings'); }}
            >
              Bookings
            </button>
          ) : (
            <button
              className={`nav-tab ${location.pathname === '/my-tours' ? 'active' : ''}`}
              onClick={() => {
                onTabChange && onTabChange('my');
                navigate(user ? '/my-tours' : '/sign-in', user ? undefined : { state: { from: '/my-tours' } });
              }}
            >
              My tours
            </button>
          )}
          {user && user.role === 'ADMIN' && (
            <button
              className={`nav-tab ${location.pathname === '/reports' ? 'active' : ''}`}
              onClick={() => navigate('/reports')}
            >
              Reports
            </button>
          )}
          {user && user.role === 'ADMIN' && (
            <button
              className={`nav-tab ${location.pathname === '/admin/feedback' ? 'active' : ''}`}
              onClick={() => navigate('/admin/feedback')}
            >
              Feedback
            </button>
          )}
          {user && user.role === 'ADMIN' && (
            <button
              className={`nav-tab ${location.pathname === '/admin/report-analysis' ? 'active' : ''}`}
              onClick={() => navigate('/admin/report-analysis')}
            >
              AI Analysis
            </button>
          )}
        </nav>

        <div className="header-right">
          {user ? (
            <div className="profile-section" ref={dropdownRef}>
              <button
                className="profile-button"
                onClick={() => setProfileDropdownOpen(!profileDropdownOpen)}
                aria-label="User profile"
              >
                <div className="profile-avatar" aria-hidden="true">
                  {user.imageUrl ? (
                    <img src={user.imageUrl} alt="" className="profile-avatar-image" />
                  ) : (
                    <span className="profile-avatar-initials">{getInitials(userName)}</span>
                  )}
                </div>
                {getRoleLabel(user.role) && (
                  <div className="profile-info">
                    <div className="profile-role">{getRoleLabel(user.role)}</div>
                  </div>
                )}
              </button>

              {profileDropdownOpen && (
                <div className="profile-dropdown">
                  <div className="dropdown-content">
                    <div className="dropdown-header">
                      <div className="user-detail">
                        <div className="user-name-large">{userName}</div>
                        <div className="user-email">{user.email}</div>
                      </div>
                    </div>
                    <div className="dropdown-divider"></div>
                    <button className="dropdown-item" onClick={() => { setProfileDropdownOpen(false); navigate('/profile'); }}>
                      <ProfileIcon />
                      <span>My Profile</span>
                    </button>
                    <button className="dropdown-item logout" onClick={handleLogout}>
                      <SignOutIcon />
                      <span>Sign Out</span>
                    </button>
                  </div>
                </div>
              )}
            </div>
          ) : (
            <button
              className="user-btn"
              aria-label="User account"
              onClick={() => navigate('/sign-in', { state: { from: currentPath } })}
            >
              <UserCircleIcon />
            </button>
          )}
        </div>
      </div>
    </header>
  );
}
