import React, { useState, useEffect } from 'react';
import { useLocation, useNavigate } from 'react-router-dom';
import SplitLayout from '../components/SplitLayout';
import TextField from '../components/TextField';
import Toast from '../components/Toast';
import { verifyResetCode, requestPasswordReset } from '../api/passwordReset';
import '../styles/components.css';
import './VerifyCodePage.css';

export default function VerifyCodePage() {
  const location = useLocation();
  const navigate = useNavigate();
  const email = location.state?.email || '';
  
  const [code, setCode] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [countdown, setCountdown] = useState(59);
  const [canResend, setCanResend] = useState(false);
  const [showToast, setShowToast] = useState(false);

  useEffect(() => {
    if (!email) {
      navigate('/forgot-password');
      return;
    }

    if (countdown > 0) {
      const timer = setTimeout(() => setCountdown(countdown - 1), 1000);
      return () => clearTimeout(timer);
    } else {
      setCanResend(true);
    }
  }, [countdown, email, navigate]);

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
    if (!code.trim()) {
      errs.code = 'Verification code is required';
    } else if (code.trim().length < 6) {
      errs.code = 'Please enter a valid verification code';
    }
    return errs;
  }

  async function handleResend() {
    if (!canResend) return;
    
    setCanResend(false);
    setCountdown(59);
    
    try {
      await requestPasswordReset(email);
      setShowToast(true);
    } catch (err) {
      console.error('Failed to resend code:', err);
      // Still show toast even if there's an error for security reasons
      setShowToast(true);
    }
  }

  async function onSubmit(e) {
    e.preventDefault();
    const errs = validateClient();
    if (Object.keys(errs).length) {
      setFieldErrors(errs);
      return;
    }
    
    setSubmitting(true);
    try {
      await verifyResetCode(email, code.trim());
      
      // Navigate to reset password page
      navigate('/reset-password', { state: { email, code: code.trim() } });
    } catch (err) {
      const status = err.response?.status;
      const message = err.response?.data?.message || 'Invalid verification code. Please try again.';
      
      if (status === 400 && err.response?.data?.fieldErrors) {
        setFieldErrors(err.response.data.fieldErrors);
      } else {
        setFieldErrors({
          code: message,
        });
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <SplitLayout>
      <div className="verify-code-container">
        {showToast && (
          <Toast
            title="Code resent"
            message="A new verification code has been sent to your email."
            onClose={() => setShowToast(false)}
          />
        )}

        <h1 className="page-title">Enter verification code</h1>
        
        <p className="verify-message">
          The verification code has been sent to your email to <strong>{email}</strong>.
        </p>

        <form onSubmit={onSubmit} noValidate>
          <TextField
            label="Verification code"
            type="text"
            placeholder="Enter verification code"
            autoComplete="off"
            value={code}
            onChange={(e) => {
              setCode(e.target.value);
              clearFieldError('code');
            }}
            error={fieldErrors.code}
          />
          
          <div className="resend-row">
            {canResend ? (
              <button type="button" className="resend-link" onClick={handleResend}>
                Not received yet? <span className="resend-action">Resend</span>
              </button>
            ) : (
              <span className="resend-text">
                Not received yet? Resend in {countdown} seconds
              </span>
            )}
          </div>

          <button 
            className="btn-primary" 
            type="submit" 
            disabled={submitting || !code.trim()}
          >
            {submitting ? 'Verifying\u2026' : 'Continue'}
          </button>
        </form>
      </div>
    </SplitLayout>
  );
}
