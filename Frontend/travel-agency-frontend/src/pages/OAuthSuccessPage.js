import React, { useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuth } from '../auth/AuthContext';

/**
 * Handles the OAuth2 redirect from the backend after a successful social login.
 * URL: /oauth-success?token=<JWT>&role=<ROLE>
 *
 * Extracts the JWT from the query string, stores it via loginWithToken(),
 * fetches the current user profile, then redirects to the home page.
 * On any failure it sends the user back to the sign-in page with an error.
 */
export default function OAuthSuccessPage() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const { loginWithToken } = useAuth();
  const processed = useRef(false);

  useEffect(() => {
    if (processed.current) return;
    processed.current = true;

    const token = searchParams.get('token');

    if (!token) {
      navigate('/sign-in?error=Social+login+failed.+No+token+received.', { replace: true });
      return;
    }

    loginWithToken(token)
      .then(() => {
        navigate('/', { replace: true });
      })
      .catch(() => {
        navigate('/sign-in?error=Social+login+failed.+Please+try+again.', { replace: true });
      });
  }, []); // intentionally run once on mount

  return (
    <div
      style={{
        minHeight: '100dvh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        fontFamily: 'inherit',
        fontSize: 16,
        color: '#374151',
      }}
    >
      Signing you in&hellip;
    </div>
  );
}
