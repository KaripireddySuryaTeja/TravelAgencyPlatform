import React, { useMemo, useRef, useState, useEffect } from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import ReCAPTCHA from 'react-google-recaptcha';
import SplitLayout from '../components/SplitLayout';
import TextField from '../components/TextField';
import PasswordField from '../components/PasswordField';
import PasswordRuleList from '../components/PasswordRuleList';
import Toast from '../components/Toast';
import { allRulesPass, evaluatePasswordRules } from '../hooks/usePasswordRules';
import { signUp, loginWithGoogle, loginWithFacebook, checkEmailExists, requestEmailVerification, verifyEmailCode } from '../api/auth';
import '../styles/components.css';
import './VerifyCodePage.css';

const RECAPTCHA_SITE_KEY = process.env.REACT_APP_RECAPTCHA_SITE_KEY;

const empty = {
  firstName: '', lastName: '', email: '', password: '', confirmPassword: '',
};

export default function RegisterPage() {
  const navigate = useNavigate();
  const location = useLocation();
  const state = location.state || {};
  const redirectTo = typeof state.from === 'string' && state.from.trim() ? state.from : '/';
  const [step, setStep] = useState(1);
  const [form, setForm] = useState(empty);
  const [fieldErrors, setFieldErrors] = useState({});
  const [touched, setTouched] = useState({});
  const [banner, setBanner] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [captchaToken, setCaptchaToken] = useState(null);
  const [captchaError, setCaptchaError] = useState(null);
  const recaptchaRef = useRef(null);
  
  // Verification step state
  const [verificationCode, setVerificationCode] = useState('');
  const [countdown, setCountdown] = useState(59);
  const [canResend, setCanResend] = useState(false);
  const [showToast, setShowToast] = useState(false);

  const rules = useMemo(
    () => evaluatePasswordRules(form.password, form.firstName, form.email),
    [form.password, form.firstName, form.email]
  );

  // Countdown timer for step 2
  useEffect(() => {
    if (step === 2 && countdown > 0) {
      const timer = setTimeout(() => setCountdown(countdown - 1), 1000);
      return () => clearTimeout(timer);
    } else if (step === 2 && countdown === 0) {
      setCanResend(true);
    }
  }, [countdown, step]);

  function validateField(name, state) {
    const v = state[name];
    if (name === 'firstName') {
      if (!v.trim()) return 'First name is required';
      if (!/^[A-Za-z\u00C0-\u024F][A-Za-z\u00C0-\u024F'\- ]{0,49}$/.test(v.trim()))
        return 'First name must be up to 50 characters. Only Latin letters, hyphens, and apostrophes are allowed.';
      return null;
    }
    if (name === 'lastName') {
      if (!v.trim()) return 'Last name is required';
      if (!/^[A-Za-z\u00C0-\u024F][A-Za-z\u00C0-\u024F'\- ]{0,49}$/.test(v.trim()))
        return 'Last name must be up to 50 characters. Only Latin letters, hyphens, and apostrophes are allowed.';
      return null;
    }
    if (name === 'email') {
      if (!v.trim()) return 'Email is required';
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(v))
        return 'Invalid email address. Please ensure it follows the format: username@domain.com';
      return null;
    }
    if (name === 'password') {
      if (!v) return 'Password is required';
      if (!allRulesPass(evaluatePasswordRules(v, state.firstName, state.email)))
        return 'Password does not meet all requirements';
      return null;
    }
    if (name === 'confirmPassword') {
      if (!v) return 'Please confirm your password';
      if (v !== state.password) return "Passwords don't match.";
      return null;
    }
    return null;
  }

  function applyErrors(state, fields) {
    setFieldErrors((prev) => {
      const next = { ...prev };
      fields.forEach((k) => {
        const msg = validateField(k, state);
        if (msg) next[k] = msg;
        else delete next[k];
      });
      return next;
    });
  }

  const update = (field) => (e) => {
    const value = e.target.value;
    setForm((prev) => {
      const nextState = { ...prev, [field]: value };
      const fieldsToCheck = Object.keys(nextState).filter((k) => touched[k]);
      if (fieldsToCheck.length) applyErrors(nextState, fieldsToCheck);
      return nextState;
    });
  };

  const onFieldBlur = (field) => () => {
    setTouched((t) => (t[field] ? t : { ...t, [field]: true }));
    applyErrors(form, [field]);
  };

  function validateStep1() {
    const fields = ['firstName', 'lastName', 'email'];
    const errs = {};
    fields.forEach((k) => {
      const msg = validateField(k, form);
      if (msg) errs[k] = msg;
    });
    return errs;
  }

  function validateStep2() {
    const errs = {};
    if (!verificationCode.trim()) {
      errs.code = 'Verification code is required';
    } else if (verificationCode.trim().length < 6) {
      errs.code = 'Please enter a valid verification code';
    }
    return errs;
  }

  function validateStep3() {
    const fields = ['password', 'confirmPassword'];
    const errs = {};
    fields.forEach((k) => {
      const msg = validateField(k, form);
      if (msg) errs[k] = msg;
    });
    return errs;
  }

  async function handleNext(e) {
    e.preventDefault();
    setBanner(null);
    setCaptchaError(null);
    const errs = validateStep1();
    if (Object.keys(errs).length) {
      setFieldErrors(errs);
      setTouched((t) => ({ ...t, firstName: true, lastName: true, email: true }));
      return;
    }
    if (!captchaToken) {
      setCaptchaError('Please complete the CAPTCHA verification.');
      return;
    }
    // Check if email already exists before proceeding to step 2
    setSubmitting(true);
    try {
      const exists = await checkEmailExists(form.email.trim());
      if (exists) {
        setFieldErrors({ email: 'An account with this email already exists.' });
        setSubmitting(false);
        return;
      }
      // Send verification code
      await requestEmailVerification(form.email.trim());
      setFieldErrors({});
      setStep(2);
      setCountdown(59);
      setCanResend(false);
      setVerificationCode('');
    } catch (err) {
      setBanner(err.message || 'Failed to send verification code. Please try again.');
    } finally {
      setSubmitting(false);
    }
  }

  function handleBackToStep1() {
    setStep(1);
    setBanner(null);
    setFieldErrors({});
  }

  async function handleVerify(e) {
    e.preventDefault();
    setBanner(null);
    const errs = validateStep2();
    if (Object.keys(errs).length) {
      setFieldErrors(errs);
      return;
    }

    setSubmitting(true);
    try {
      await verifyEmailCode(form.email.trim(), verificationCode.trim());
      setFieldErrors({});
      setBanner(null);
      setStep(3);
    } catch (err) {
      const message = err.response?.data?.message || 'Invalid verification code. Please try again.';
      if (err.response?.data?.fieldErrors) {
        setFieldErrors(err.response.data.fieldErrors);
      } else {
        setFieldErrors({ code: message });
      }
    } finally {
      setSubmitting(false);
    }
  }

  async function handleResend() {
    if (!canResend) return;
    
    setCanResend(false);
    setCountdown(59);
    
    try {
      await requestEmailVerification(form.email.trim());
      setShowToast(true);
    } catch (err) {
      console.error('Failed to resend code:', err);
      setShowToast(true);
    }
  }

  function handleBackToStep2() {
    setStep(2);
    setBanner(null);
    setFieldErrors({});
  }

  async function onSubmit(e) {
    e.preventDefault();
    setBanner(null);
    setCaptchaError(null);
    const errs = validateStep3();
    if (Object.keys(errs).length) {
      setFieldErrors(errs);
      setTouched((t) => ({ ...t, password: true, confirmPassword: true }));
      return;
    }
    setSubmitting(true);
    try {
      await signUp({
        firstName: form.firstName.trim(),
        lastName: form.lastName.trim(),
        email: form.email.trim(),
        password: form.password,
        captchaToken: captchaToken,
        verificationCode: verificationCode.trim(),
      });
      navigate('/sign-in', {
        state: { justRegistered: true, email: form.email.trim(), from: redirectTo },
        replace: true,
      });
    } catch (err) {
      // Reset CAPTCHA on failure so user must re-verify
      if (recaptchaRef.current) {
        recaptchaRef.current.reset();
        setCaptchaToken(null);
      }
      if (err.fieldErrors && Object.keys(err.fieldErrors).length) {
        // If server returns errors for step 1 fields, go back to step 1
        const step1Fields = ['firstName', 'lastName', 'email'];
        const hasStep1Errors = step1Fields.some((f) => err.fieldErrors[f]);
        if (hasStep1Errors) setStep(1);
        setFieldErrors(err.fieldErrors);
      } else {
        setBanner(err.message || err.error || 'Something went wrong. Please try again.');
      }
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <SplitLayout>
      {showToast && (
        <Toast
          title="Code resent"
          message="A new verification code has been sent to your email."
          onClose={() => setShowToast(false)}
        />
      )}

      <div className="eyebrow">LET&rsquo;S GET YOU STARTED</div>
      <h1 className="page-title">Create an account</h1>

      {/* Step indicator */}
      <div className="step-indicator" style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '24px' }}>
        <span className={`step-dot ${step >= 1 ? 'step-dot--active' : ''}`} style={{ width: '10px', height: '10px', borderRadius: '50%', background: step >= 1 ? '#1a73e8' : '#ccc' }} />
        <span style={{ width: '40px', height: '2px', background: step >= 2 ? '#1a73e8' : '#ccc' }} />
        <span className={`step-dot ${step >= 2 ? 'step-dot--active' : ''}`} style={{ width: '10px', height: '10px', borderRadius: '50%', background: step >= 2 ? '#1a73e8' : '#ccc' }} />
        <span style={{ width: '40px', height: '2px', background: step >= 3 ? '#1a73e8' : '#ccc' }} />
        <span className={`step-dot ${step >= 3 ? 'step-dot--active' : ''}`} style={{ width: '10px', height: '10px', borderRadius: '50%', background: step >= 3 ? '#1a73e8' : '#ccc' }} />
        <span style={{ marginLeft: '12px', fontSize: '0.85rem', color: '#666' }}>Step {step} of 3</span>
      </div>

      {banner && <div className="banner-error" role="alert">{banner}</div>}

      {/* ─── STEP 1: Personal info ─── */}
      {step === 1 && (
        <form onSubmit={handleNext} noValidate>
          <div className="form-grid">
            <TextField
              label="First name"
              placeholder="Enter your first name"
              hint="e.g. Johnson"
              autoComplete="given-name"
              value={form.firstName}
              onChange={update('firstName')}
              onBlur={onFieldBlur('firstName')}
              error={fieldErrors.firstName}
            />
            <TextField
              label="Last name"
              placeholder="Enter your last name"
              hint="e.g. Doe"
              autoComplete="family-name"
              value={form.lastName}
              onChange={update('lastName')}
              onBlur={onFieldBlur('lastName')}
              error={fieldErrors.lastName}
            />
            <div className="form-row">
              <TextField
                label="Email"
                type="email"
                placeholder="Enter your email"
                hint="e.g. username@domain.com"
                autoComplete="email"
                value={form.email}
                onChange={update('email')}
                onBlur={onFieldBlur('email')}
                error={fieldErrors.email}
              />
            </div>

            {/* CAPTCHA widget */}
            {RECAPTCHA_SITE_KEY && (
              <div className="form-row" style={{ marginBottom: '8px' }}>
                <ReCAPTCHA
                  ref={recaptchaRef}
                  sitekey={RECAPTCHA_SITE_KEY}
                  onChange={(token) => { setCaptchaToken(token); setCaptchaError(null); }}
                  onExpired={() => { setCaptchaToken(null); setCaptchaError('CAPTCHA expired. Please verify again.'); }}
                  onErrored={() => { setCaptchaToken(null); setCaptchaError('CAPTCHA error. Please try again.'); }}
                />
                {captchaError && <div className="field-error" role="alert">{captchaError}</div>}
              </div>
            )}
          </div>

          <button
            className="btn-primary"
            type="submit"
            disabled={submitting || !form.firstName.trim() || !form.lastName.trim() || !form.email.trim() || !captchaToken}
          >
            {submitting ? 'Checking\u2026' : 'Continue'}
          </button>

          <div className="form-footer">
            Already have an account? <Link to="/sign-in" state={{ from: redirectTo }}>Login</Link> instead
          </div>

          {/* Social login divider */}
          <div className="social-divider">
            <span className="social-divider__line" />
            <span className="social-divider__text">or sign up with</span>
            <span className="social-divider__line" />
          </div>

          {/* Social login buttons */}
          <div className="social-buttons">
            <button
              type="button"
              className="btn-social btn-social--google"
              onClick={loginWithGoogle}
              aria-label="Sign up with Google"
            >
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
              aria-label="Sign up with Facebook"
            >
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
        </form>
      )}

      {/* ─── STEP 2: Email Verification ─── */}
      {step === 2 && (
        <div className="verify-code-container">
          <p className="verify-message">
            The verification code has been sent to your email to <strong>{form.email}</strong>.
          </p>

          <form onSubmit={handleVerify} noValidate>
            <TextField
              label="Verification code"
              type="text"
              placeholder="Enter verification code"
              autoComplete="off"
              value={verificationCode}
              onChange={(e) => {
                setVerificationCode(e.target.value);
                if (fieldErrors.code) {
                  setFieldErrors((prev) => {
                    const next = { ...prev };
                    delete next.code;
                    return next;
                  });
                }
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

            <div className="form-actions">
              <button 
                className="btn-primary" 
                type="submit" 
                disabled={submitting || !verificationCode.trim()}
              >
                {submitting ? 'Verifying\u2026' : 'Continue'}
              </button>
              <button
                className="btn-secondary"
                type="button"
                onClick={handleBackToStep1}
              >
                Back
              </button>
            </div>

            <div className="form-footer">
              Already have an account? <Link to="/sign-in" state={{ from: redirectTo }}>Login</Link> instead
            </div>
          </form>
        </div>
      )}

      {/* ─── STEP 3: Password ─── */}
      {step === 3 && (
        <form onSubmit={onSubmit} noValidate>
          <div className="form-grid">
            <div className="form-row">
              <PasswordField
                label="Password"
                placeholder="Enter your password"
                autoComplete="new-password"
                value={form.password}
                onChange={update('password')}
                onBlur={onFieldBlur('password')}
                error={fieldErrors.password}
              />
              <PasswordRuleList rules={rules} active={form.password.length > 0 || Boolean(touched.password)} />
            </div>
            <div className="form-row">
              <PasswordField
                label="Confirm password"
                placeholder="Confirm your password"
                hint="Confirm password must match your password"
                autoComplete="new-password"
                value={form.confirmPassword}
                onChange={update('confirmPassword')}
                onBlur={onFieldBlur('confirmPassword')}
                error={fieldErrors.confirmPassword}
              />
            </div>
          </div>

          <div className="form-actions">
            <button
              className="btn-primary"
              type="submit"
              disabled={submitting || !form.password || !form.confirmPassword}
            >
              {submitting ? 'Creating account\u2026' : 'Create an account'}
            </button>
            <button
              className="btn-secondary"
              type="button"
              onClick={handleBackToStep2}
            >
              Back
            </button>
          </div>

          <div className="form-footer">
            Already have an account? <Link to="/sign-in" state={{ from: redirectTo }}>Login</Link> instead
          </div>
        </form>
      )}
    </SplitLayout>
  );
}
