import React, { useState, useEffect, useMemo } from 'react';
import { useLocation, useNavigate, Link } from 'react-router-dom';
import SplitLayout from '../components/SplitLayout';
import TextField from '../components/TextField';
import Toast from '../components/Toast';
import PasswordRuleList from '../components/PasswordRuleList';
import { resetPassword } from '../api/passwordReset';
import { evaluatePasswordRules, allRulesPass } from '../hooks/usePasswordRules';
import '../styles/components.css';
import './ResetPasswordPage.css';

export default function ResetPasswordPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const email = location.state?.email || '';
  const code = location.state?.code || '';
  
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [fieldErrors, setFieldErrors] = useState({});
  const [submitting, setSubmitting] = useState(false);
  const [showSuccessToast, setShowSuccessToast] = useState(false);

  const passwordRules = useMemo(
    () => evaluatePasswordRules(newPassword, '', email),
    [newPassword, email]
  );

  useEffect(() => {
    if (!email || !code) {
      navigate('/forgot-password');
      return;
    }
  }, [email, code, navigate]);


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
    
    if (!newPassword) {
      errs.newPassword = 'New password is required';
    } else if (newPassword.length < 8) {
      errs.newPassword = 'Password must be at least 8 characters long';
    } else if (!allRulesPass(passwordRules)) {
      errs.newPassword = 'Password does not meet all requirements';
    }
    
    if (!confirmPassword) {
      errs.confirmPassword = 'Please confirm your password';
    } else if (newPassword !== confirmPassword) {
      errs.confirmPassword = 'Passwords do not match';
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
      await resetPassword(email, code, newPassword);
      
      setShowSuccessToast(true);
      
      // Navigate to sign in page after a short delay
      setTimeout(() => {
        navigate('/sign-in', { 
          state: { message: 'Password reset successful. Please sign in with your new password.' } 
        });
      }, 2000);
    } catch (err) {
      const status = err.response?.status;
      const message = err.response?.data?.message || 'Failed to reset password. Please try again.';
      
      if (status === 400 && err.response?.data?.fieldErrors) {
        setFieldErrors(err.response.data.fieldErrors);
      } else {
        setFieldErrors({
          newPassword: message,
        });
      }
    } finally {
      setSubmitting(false);
    }
  }

  const allRulesValid = allRulesPass(passwordRules);

  return (
    <SplitLayout>
      <div className="reset-password-container">
        {showSuccessToast && (
          <Toast
            title="Success!"
            message="Your password has been reset successfully."
            onClose={() => setShowSuccessToast(false)}
          />
        )}

        <h1 className="page-title">Create new password</h1>
        
        <p className="reset-message">
          Please enter your new password for <strong>{email}</strong>.
        </p>

        <form onSubmit={onSubmit} noValidate>
          <div className="password-wrap">
            <TextField
              label="New password"
              type={showNewPassword ? 'text' : 'password'}
              placeholder="Enter new password"
              autoComplete="new-password"
              value={newPassword}
              onChange={(e) => {
                setNewPassword(e.target.value);
                clearFieldError('newPassword');
              }}
              error={fieldErrors.newPassword}
            />
            <button
              type="button"
              className="password-toggle"
              onClick={() => setShowNewPassword(!showNewPassword)}
              aria-label={showNewPassword ? 'Hide password' : 'Show password'}
            >
              {showNewPassword ? '👁️' : '👁️‍🗨️'}
            </button>
          </div>

          <PasswordRuleList rules={passwordRules} active={newPassword.length > 0} />

          <div className="password-wrap">
            <TextField
              label="Confirm new password"
              type={showConfirmPassword ? 'text' : 'password'}
              placeholder="Re-enter new password"
              autoComplete="new-password"
              value={confirmPassword}
              onChange={(e) => {
                setConfirmPassword(e.target.value);
                clearFieldError('confirmPassword');
              }}
              error={fieldErrors.confirmPassword}
            />
            <button
              type="button"
              className="password-toggle"
              onClick={() => setShowConfirmPassword(!showConfirmPassword)}
              aria-label={showConfirmPassword ? 'Hide password' : 'Show password'}
            >
              {showConfirmPassword ? '👁️' : '👁️‍🗨️'}
            </button>
          </div>

          <button 
            className="btn-primary" 
            type="submit" 
            disabled={submitting || !allRulesValid || newPassword !== confirmPassword}
          >
            {submitting ? 'Resetting\u2026' : 'Reset password'}
          </button>
        </form>

        <div className="form-footer">
          Remembered your password? <Link to="/sign-in">Sign in</Link>
        </div>
      </div>
    </SplitLayout>
  );
}
