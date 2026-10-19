import React, { useState } from 'react';
import { Link, useLocation, useNavigate, useSearchParams } from 'react-router-dom';
import SplitLayout from '../components/SplitLayout';
import TextField from '../components/TextField';
import PasswordField from '../components/PasswordField';
import Toast from '../components/Toast';
import { signIn, loginWithGoogle, loginWithFacebook } from '../api/auth';
import { useAuth } from '../auth/AuthContext';
import '../styles/components.css';

export default function SignInPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { loginWith } = useAuth();
  const state = location.state || {};
  const redirectTo = typeof state.from === 'string' && state.from.trim() ? state.from : '/';

  // Error message forwarded from backend OAuth2 failure redirect (?error=...)
  const oauthError = searchParams.get('error');

  const [showToast, setShowToast] = useState(Boolean(state.justRegistered));
  const [showEmailChangedToast, setShowEmailChangedToast] = useState(Boolean(state.emailChanged));
  const [showInfoToast, setShowInfoToast] = useState(Boolean(state.message));
  const [infoMessage, setInfoMessage] = useState(state.message || '');
  const [email, setEmail] = useState(state.email || '');
  const [password, setPassword] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [lockedBanner, setLockedBanner] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  function clearFieldError(field) {
    if (fieldErrors[field]) {
      setFieldErrors((prev) => {
        const next = { ...prev };
        delete next[field];
        return next;
      });
    }
  }

  function validateClient() {
    const errs = {};
    if (!email.trim()) errs.email = 'Email address is required. Please enter your email to continue';
    else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email))
      errs.email = 'Enter a valid email address';
    if (!password) errs.password = 'Password is required. Please enter your password to continue.';
    return errs;
  }

  async function onSubmit(e) {
    e.preventDefault();
    setLockedBanner(null);
    const errs = validateClient();
    if (Object.keys(errs).length) {
      setFieldErrors(errs);
      return;
    }
    setSubmitting(true);
    try {
      const data = await signIn({ email: email.trim(), password });
      await loginWith(data);
      navigate(redirectTo, { replace: true });
    } catch (err) {
      if (err.status === 423) {
        setLockedBanner(
          err.message ||
            'Your account is temporarily locked due to multiple failed login attempts. Please try again later.'
        );
        setFieldErrors({});
      } else if (err.fieldErrors && Object.keys(err.fieldErrors).length) {
        setFieldErrors(err.fieldErrors);
      } else {
        setFieldErrors({
          email: 'Incorrect email or password. Try again or create an account.',
          password: 'Incorrect email or password. Try again or create an account.',
        });
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <SplitLayout>
      {showToast && (
        <Toast
          title="Congratulations"
          message="Your account has been created successfully. Please sign in with the details."
          onClose={() => setShowToast(false)}
        />
      )}

      {showEmailChangedToast && (
        <Toast
          title="Success"
          message="Your email has been changed successfully."
          onClose={() => setShowEmailChangedToast(false)}
        />
      )}

      {showInfoToast && (
        <Toast
          title="Info"
          message={infoMessage}
          onClose={() => setShowInfoToast(false)}
        />
      )}

      <div className="eyebrow">WELCOME BACK</div>
      <h1 className="page-title">Sign in to your account</h1>

      {(lockedBanner || oauthError) && (
        <div className="banner-error" role="alert">
          {lockedBanner || oauthError}
        </div>
      )}

      {/* When a social login fails or is cancelled, show quick-action links inline
          so the user doesn't need to scroll to find the buttons or the switch-hint. */}
      {oauthError && (
        <div className="oauth-retry-bar">
          <span className="oauth-retry-bar__label">Try again:</span>
          <button type="button" className="oauth-retry-bar__btn" onClick={loginWithGoogle}>
            Google
          </button>
          <span className="oauth-retry-bar__sep" aria-hidden="true">·</span>
          <button type="button" className="oauth-retry-bar__btn" onClick={loginWithFacebook}>
            Facebook
          </button>
        </div>
      )}

      <form onSubmit={onSubmit} noValidate>
        <TextField
          label="Email"
          type="email"
          placeholder="Enter your email"
          hint="e.g. username@domain.com"
          autoComplete="email"
          value={email}
          onChange={(e) => {
            setEmail(e.target.value);
            clearFieldError('email');
          }}
          error={fieldErrors.email}
        />
        <PasswordField
          label="Password"
          placeholder="Enter your password"
          autoComplete="current-password"
          value={password}
          onChange={(e) => {
            setPassword(e.target.value);
            clearFieldError('password');
          }}
          error={fieldErrors.password}
        />
        <div className="forgot-row">
          <Link to="/forgot-password">Forgot password?</Link>
        </div>
        <button className="btn-primary" type="submit" disabled={submitting || !email.trim() || !password}>
          {submitting ? 'Signing in\u2026' : 'Sign in'}
        </button>
      </form>

      <div className="form-footer">
        Don&rsquo;t have an account? <Link to="/register" state={{ from: redirectTo }}>Create an account</Link>
      </div>

      {/* Social login divider */}
      <div className="social-divider">
        <span className="social-divider__line" />
        <span className="social-divider__text">or continue with</span>
        <span className="social-divider__line" />
      </div>

      {/* Social login buttons */}
      <div className="social-buttons">
        <button
          type="button"
          className="btn-social btn-social--google"
          onClick={loginWithGoogle}
          aria-label="Sign in with Google"
        >
          {/* Google G icon */}
          <svg className="social-icon" viewBox="0 0 48 48" aria-hidden="true">
            <path fill="#EA4335" d="M24 9.5c3.54 0 6.71 1.22 9.21 3.6l6.85-6.85C35.9 2.38 30.47 0 24 0 14.62 0 6.51 5.38 2.56 13.22l7.98 6.19C12.43 13.72 17.74 9.5 24 9.5z"/>
            <path fill="#4285F4" d="M46.98 24.55c0-1.57-.15-3.09-.38-4.55H24v9.02h12.94c-.58 2.96-2.26 5.48-4.78 7.18l7.73 6c4.51-4.18 7.09-10.36 7.09-17.65z"/>
            <path fill="#FBBC05" d="M10.53 28.59c-.48-1.45-.76-2.99-.76-4.59s.27-3.14.76-4.59l-7.98-6.19C.92 16.46 0 20.12 0 24c0 3.88.92 7.54 2.56 10.78l7.97-6.19z"/>
            <path fill="#34A853" d="M24 48c6.48 0 11.93-2.13 15.89-5.81l-7.73-6c-2.15 1.45-4.92 2.3-8.16 2.3-6.26 0-11.57-4.22-13.47-9.91l-7.98 6.19C6.51 42.62 14.62 48 24 48z"/>
          </svg>
          Continue with Google
        </button>

        <button
          type="button"
          className="btn-social btn-social--facebook"
          onClick={loginWithFacebook}
          aria-label="Sign in with Facebook"
        >
          {/* Facebook icon */}
          <svg className="social-icon" viewBox="0 0 24 24" aria-hidden="true">
            <path fill="#1877F2" d="M24 12.073C24 5.446 18.627 0 12 0S0 5.446 0 12.073C0 18.1 4.388 23.094 10.125 24v-8.437H7.078v-3.49h3.047v-2.66c0-3.025 1.792-4.697 4.533-4.697 1.312 0 2.686.236 2.686.236v2.971h-1.514c-1.491 0-1.956.932-1.956 1.887v2.263h3.328l-.532 3.49h-2.796V24C19.612 23.094 24 18.1 24 12.073z"/>
          </svg>
          Continue with Facebook
        </button>
      </div>

      <p className="social-consent">
        By continuing with Google or Facebook, you allow us to access your name and email for account authentication.
      </p>
      <p className="social-switch-hint">
        Need to use a different Facebook account?{' '}
        <a href="https://www.facebook.com" target="_blank" rel="noreferrer">Open Facebook</a>{' '}and switch accounts first.
      </p>
    </SplitLayout>
  );
}
