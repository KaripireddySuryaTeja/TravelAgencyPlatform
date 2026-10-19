import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import SplitLayout from '../components/SplitLayout';
import TextField from '../components/TextField';
import Toast from '../components/Toast';
import { requestPasswordReset } from '../api/passwordReset';
import '../styles/components.css';
import './ForgotPasswordPage.css';

export default function ForgotPasswordPage() {
  const navigate = useNavigate();
  const [email, setEmail] = useState('');
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [showSuccessToast, setShowSuccessToast] = useState(false);

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
    if (!email.trim()) {
      errs.email = 'Email address is required. Please enter your email to continue';
    } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      errs.email = 'Enter a valid email address';
    }
    return errs;
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
      await requestPasswordReset(email.trim());
      
      // Navigate to verification code page
      navigate('/verify-code', { state: { email: email.trim() } });
    } catch (err) {
      const status = err.response?.status;
      const message = err.response?.data?.message || 'Unable to process your request. Please try again.';
      
      if (status === 400 && err.response?.data?.fieldErrors) {
        setFieldErrors(err.response.data.fieldErrors);
      } else {
        setFieldErrors({
          email: message,
        });
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <SplitLayout>
      <div className="forgot-password-container">
        {showSuccessToast && (
          <Toast
            title="Reset link sent"
            message="If an account exists with this email, you will receive a password reset link shortly."
            onClose={() => setShowSuccessToast(false)}
          />
        )}

        <h1 className="page-title">Reset password</h1>

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
          <button 
            className="btn-primary" 
            type="submit" 
            disabled={submitting || !email.trim()}
          >
            {submitting ? 'Sending\u2026' : 'Request a reset link'}
          </button>
        </form>

        <div className="form-footer">
          Don&rsquo;t have an account? <Link to="/register">Create an account</Link>
        </div>
      </div>
    </SplitLayout>
  );
}
