import React, { useState, useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';
import { confirmEmailChange } from '../api/user';
import SplitLayout from '../components/SplitLayout';
import '../styles/components.css';

export default function ConfirmEmailPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { user, token, logout } = useAuth();

  const [status, setStatus] = useState('loading'); // 'loading', 'success', 'error'
  const [message, setMessage] = useState('');

  const confirmationToken = searchParams.get('token');
  const userId = searchParams.get('userId');

  useEffect(() => {
    async function confirmEmail() {
      // Validate required params
      if (!confirmationToken || !userId) {
        setStatus('error');
        setMessage('Invalid confirmation link. Missing required parameters.');
        return;
      }

      // Check if user is logged in
      if (!token) {
        // Redirect to sign-in with return URL
        navigate('/sign-in', {
          state: {
            from: `/confirm-email?token=${confirmationToken}&userId=${userId}`,
            message: 'Please sign in to confirm your email change.'
          },
          replace: true
        });
        return;
      }

      try {
        // Call authenticated API endpoint
        await confirmEmailChange(userId, confirmationToken);

        // Log out the user (their email has changed, need to re-login with new email)
        if (logout) {
          logout();
        }

        // Redirect to sign-in page with success message
        navigate('/sign-in', {
          state: {
            emailChanged: true
          },
          replace: true
        });
      } catch (err) {
        const errorMessage = err.message || err.error || 'Failed to confirm email change. The link may have expired.';

        // Check if the error is authentication related
        if (err.status === 401 || errorMessage.toLowerCase().includes('authentication')) {
          // Redirect to sign-in
          navigate('/sign-in', {
            state: {
              from: `/confirm-email?token=${confirmationToken}&userId=${userId}`,
              message: 'Please sign in to confirm your email change.'
            },
            replace: true
          });
          return;
        }

        setStatus('error');
        setMessage(errorMessage);
      }
    }

    // Wait a moment to ensure auth context is loaded
    const timer = setTimeout(() => {
      confirmEmail();
    }, 500);

    return () => clearTimeout(timer);
  }, [confirmationToken, userId, token, logout, navigate]);

  const handleSignIn = () => {
    navigate('/sign-in', {
      state: {
        from: `/confirm-email?token=${confirmationToken}&userId=${userId}`,
        message: 'Please sign in to confirm your email change.'
      }
    });
  };

  const handleGoHome = () => {
    navigate('/');
  };

  return (
    <SplitLayout>
      <div className="confirm-email-container" style={{ textAlign: 'center', padding: '40px 20px' }}>
        {status === 'loading' && (
          <>
            <h1 className="page-title">Confirming Email Change</h1>
            <p style={{ color: '#5a8a9d', marginTop: '20px' }}>
              Please wait while we verify your email change...
            </p>
            <div style={{
              width: '40px',
              height: '40px',
              border: '3px solid #e5f1f7',
              borderTopColor: '#0c6d8f',
              borderRadius: '50%',
              margin: '30px auto',
              animation: 'spin 1s linear infinite'
            }} />
            <style>{`
              @keyframes spin {
                to { transform: rotate(360deg); }
              }
            `}</style>
          </>
        )}

        {status === 'error' && (
          <>
            <div style={{
              width: '80px',
              height: '80px',
              background: '#ef4444',
              borderRadius: '50%',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 20px'
            }}>
              <svg width="40" height="40" viewBox="0 0 24 24" fill="none" stroke="white" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </div>
            <h1 className="page-title" style={{ color: '#ef4444' }}>Confirmation Failed</h1>
            <p style={{ color: '#5a8a9d', marginTop: '20px', marginBottom: '30px' }}>
              {message}
            </p>
            <div style={{ display: 'flex', gap: '12px', justifyContent: 'center' }}>
              <button
                className="btn-primary"
                onClick={handleSignIn}
              >
                Sign In
              </button>
              <button
                className="btn-secondary"
                onClick={handleGoHome}
                style={{
                  background: 'transparent',
                  border: '1px solid #0c6d8f',
                  color: '#0c6d8f'
                }}
              >
                Go to Home
              </button>
            </div>
          </>
        )}
      </div>
    </SplitLayout>
  );
}
