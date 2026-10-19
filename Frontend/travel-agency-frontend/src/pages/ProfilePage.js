import React, { useState, useRef, useEffect, useMemo } from 'react';
import { useAuth } from '../auth/AuthContext';
import Header from '../components/Header';
import { updateUserName, updateUserImage, updatePassword, initiateEmailChange } from '../api/user';
import './ProfilePage.css';

// ─────────────────────────────────────────────
// Icons
// ─────────────────────────────────────────────
function EditIcon() {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#0c6d8f" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
      <path d="M12 20h9"/>
      <path d="M16.5 3.5a2.121 2.121 0 0 1 3 3L7 19l-4 1 1-4L16.5 3.5z"/>
    </svg>
  );
}

function CameraIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M23 19a2 2 0 0 1-2 2H3a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h4l2-3h6l2 3h4a2 2 0 0 1 2 2z"/>
      <circle cx="12" cy="13" r="4"/>
    </svg>
  );
}

function UserPlaceholderIcon() {
  return (
    <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
      <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
      <circle cx="12" cy="7" r="4"/>
    </svg>
  );
}

function EyeIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M2 12s3-8 10-8 10 8 10 8-3 8-10 8-10-8-10-8z"/>
      <circle cx="12" cy="12" r="3"/>
    </svg>
  );
}

function EyeOffIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <path d="M17.94 17.94A10.06 10.06 0 0 1 12 20c-7 0-10-8-10-8a18.54 18.54 0 0 1 5.06-5.94"/>
      <path d="M9.9 4.24A10 10 0 0 1 12 4c7 0 10 8 10 8a18.67 18.67 0 0 1-2.16 3.19"/>
      <path d="M14.12 14.12a3 3 0 1 1-4.24-4.24"/>
      <line x1="2" y1="2" x2="22" y2="22"/>
    </svg>
  );
}

function CheckIcon() {
  return (
    <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round">
      <polyline points="20 6 9 17 4 12"/>
    </svg>
  );
}

function CloseIcon() {
  return (
    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      <line x1="18" y1="6" x2="6" y2="18"/>
      <line x1="6" y1="6" x2="18" y2="18"/>
    </svg>
  );
}

// ─────────────────────────────────────────────
// Toast Component
// ─────────────────────────────────────────────
function Toast({ message, onClose }) {
  useEffect(() => {
    const timer = setTimeout(() => {
      onClose();
    }, 5000);
    return () => clearTimeout(timer);
  }, [onClose]);

  return (
    <div className="profile-toast">
      <div className="profile-toast-icon">
        <CheckIcon />
      </div>
      <div className="profile-toast-content">
        <div className="profile-toast-title">Success</div>
        <div className="profile-toast-message">{message}</div>
      </div>
      <button className="profile-toast-close" onClick={onClose} aria-label="Close">
        <CloseIcon />
      </button>
    </div>
  );
}

// ─────────────────────────────────────────────
// Password Rules Evaluation (Profile-specific)
// ─────────────────────────────────────────────
function evaluateProfilePasswordRules(password, confirmPassword) {
  const pw = password || '';
  return {
    uppercase: /[A-Z]/.test(pw),
    lowercase: /[a-z]/.test(pw),
    number: /[0-9]/.test(pw),
    special: /[^A-Za-z0-9]/.test(pw),
    length: pw.length >= 8 && pw.length <= 16,
    confirmMatch: pw.length > 0 && pw === confirmPassword,
  };
}

function allProfileRulesPass(rules) {
  return rules.uppercase && rules.lowercase && rules.number && rules.special && rules.length && rules.confirmMatch;
}

// ─────────────────────────────────────────────
// Section: General Information
// ─────────────────────────────────────────────
function GeneralInformationSection({ user, refreshUser, showToast }) {
  const [isEditing, setIsEditing] = useState(false);
  const [firstName, setFirstName] = useState(user?.firstName || '');
  const [lastName, setLastName] = useState(user?.lastName || '');
  const [saving, setSaving] = useState(false);
  const [errors, setErrors] = useState({});
  const fileInputRef = useRef(null);

  useEffect(() => {
    setFirstName(user?.firstName || '');
    setLastName(user?.lastName || '');
  }, [user]);

  const handleAvatarClick = () => {
    fileInputRef.current?.click();
  };

  const handleAvatarChange = async (e) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Reset file input so the same file can be selected again
    e.target.value = '';

    // Allowed image formats
    const allowedTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp'];
    const allowedExtensions = ['.jpg', '.jpeg', '.png', '.gif', '.webp'];

    // Get file extension
    const fileName = file.name.toLowerCase();
    const fileExtension = fileName.substring(fileName.lastIndexOf('.'));

    // Validate file type - must be an image (not PDF or other formats)
    if (!allowedTypes.includes(file.type) || !allowedExtensions.includes(fileExtension)) {
      setErrors({ image: 'Please select a valid image file (JPG, PNG, GIF, or WebP only). PDF and other formats are not allowed.' });
      return;
    }

    // Validate file size (max 5MB)
    if (file.size > 5 * 1024 * 1024) {
      setErrors({ image: 'Image size must be less than 5MB.' });
      return;
    }

    // Convert to base64 and upload
    const reader = new FileReader();

    reader.onerror = () => {
      setErrors({ image: 'Failed to read image file. Please try again.' });
    };

    reader.onload = async () => {
      const base64 = reader.result;

      // Verify the base64 string starts with image data
      if (!base64.startsWith('data:image/')) {
        setErrors({ image: 'Invalid image format. Please select a valid image file.' });
        return;
      }

      setSaving(true);
      setErrors({});

      try {
        await updateUserImage(user.id, base64);
        await refreshUser();
        showToast('Your profile image has been updated successfully.');
      } catch (err) {
        setErrors({ image: err.message || 'Failed to update profile image. Please try again.' });
      } finally {
        setSaving(false);
      }
    };

    reader.readAsDataURL(file);
  };

  const validateName = (name, label) => {
    if (!name.trim()) return `${label} is required`;
    if (!/^[A-Za-z\u00C0-\u024F][A-Za-z\u00C0-\u024F'\- ]{0,49}$/.test(name.trim())) {
      return `${label} must be up to 50 characters. Only Latin letters, hyphens, and apostrophes are allowed.`;
    }
    return null;
  };

  const handleSave = async () => {
    const errs = {};
    const firstNameErr = validateName(firstName, 'First name');
    const lastNameErr = validateName(lastName, 'Last name');
    if (firstNameErr) errs.firstName = firstNameErr;
    if (lastNameErr) errs.lastName = lastNameErr;

    if (Object.keys(errs).length) {
      setErrors(errs);
      return;
    }

    setSaving(true);
    setErrors({});

    try {
      await updateUserName(user.id, firstName.trim(), lastName.trim());
      await refreshUser();
      showToast('Your account has been updated successfully.');
      setIsEditing(false);
    } catch (err) {
      if (err.fieldErrors) {
        setErrors(err.fieldErrors);
      } else {
        setErrors({ general: err.message || 'Failed to update name.' });
      }
    } finally {
      setSaving(false);
    }
  };

  const handleCancel = () => {
    setFirstName(user?.firstName || '');
    setLastName(user?.lastName || '');
    setErrors({});
    setIsEditing(false);
  };

  return (
    <div className="profile-card">
      <div className="profile-card-header">
        <h2 className="profile-card-title">General information</h2>
        {!isEditing && (
          <button className="profile-edit-btn" onClick={() => setIsEditing(true)} aria-label="Edit profile">
            <EditIcon />
          </button>
        )}
      </div>

      {errors.general && (
        <div className="error-message">{errors.general}</div>
      )}

      <div className="general-info-content">
        <div className="avatar-section">
          <div className="avatar-container">
            {user?.imageUrl ? (
              <img src={user.imageUrl} alt="Profile" className="avatar-image" />
            ) : (
              <span className="avatar-placeholder">
                <UserPlaceholderIcon />
              </span>
            )}
          </div>
          <button className="avatar-upload-btn" onClick={handleAvatarClick} disabled={saving} aria-label="Upload photo">
            <CameraIcon />
          </button>
          <input
            ref={fileInputRef}
            type="file"
            accept=".jpg,.jpeg,.png,.gif,.webp,image/jpeg,image/png,image/gif,image/webp"
            className="avatar-upload-input"
            onChange={handleAvatarChange}
          />
          {errors.image && <span className="avatar-error">{errors.image}</span>}
        </div>

        {isEditing ? (
          <div className="edit-form">
            <div className="field">
              <label className="field__label">First name</label>
              <input
                type="text"
                className={`field__input ${errors.firstName ? 'field__input--error' : ''}`}
                value={firstName}
                onChange={(e) => setFirstName(e.target.value)}
                placeholder="Enter your first name"
              />
              {errors.firstName && <span className="field__error">{errors.firstName}</span>}
            </div>
            <div className="field">
              <label className="field__label">Last name</label>
              <input
                type="text"
                className={`field__input ${errors.lastName ? 'field__input--error' : ''}`}
                value={lastName}
                onChange={(e) => setLastName(e.target.value)}
                placeholder="Enter your last name"
              />
              {errors.lastName && <span className="field__error">{errors.lastName}</span>}
            </div>
            <div className="edit-actions">
              <button className="btn-cancel" onClick={handleCancel} disabled={saving}>
                Cancel
              </button>
              <button className="btn-save" onClick={handleSave} disabled={saving}>
                {saving ? 'Saving...' : 'Save changes'}
              </button>
            </div>
          </div>
        ) : (
          <div className="info-fields">
            <div className="info-row">
              <span className="info-label">First name</span>
              <span className="info-value">{user?.firstName || '-'}</span>
            </div>
            <div className="info-row">
              <span className="info-label">Last name</span>
              <span className="info-value">{user?.lastName || '-'}</span>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────
// Section: Change Password
// ─────────────────────────────────────────────
function ChangePasswordSection({ user, showToast }) {
  const [oldPassword, setOldPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showOld, setShowOld] = useState(false);
  const [showNew, setShowNew] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [saving, setSaving] = useState(false);
  const [errors, setErrors] = useState({});

  const rules = useMemo(
    () => evaluateProfilePasswordRules(newPassword, confirmPassword),
    [newPassword, confirmPassword]
  );

  const handleSave = async () => {
    const errs = {};

    if (!oldPassword) errs.oldPassword = 'Old password is required';
    if (!newPassword) {
      errs.newPassword = 'New password is required';
    } else if (!allProfileRulesPass(rules)) {
      errs.newPassword = 'Password does not meet all requirements';
    }
    if (!confirmPassword) {
      errs.confirmPassword = 'Please confirm your new password';
    } else if (confirmPassword !== newPassword) {
      errs.confirmPassword = "Passwords don't match";
    }

    if (Object.keys(errs).length) {
      setErrors(errs);
      return;
    }

    setSaving(true);
    setErrors({});

    try {
      await updatePassword(user.id, oldPassword, newPassword);
      showToast('Your password has been updated successfully.');
      setOldPassword('');
      setNewPassword('');
      setConfirmPassword('');
    } catch (err) {
      if (err.fieldErrors) {
        setErrors(err.fieldErrors);
      } else {
        setErrors({ general: err.message || 'Failed to update password.' });
      }
    } finally {
      setSaving(false);
    }
  };

  // Password rule items for display
  const ruleItems = [
    { key: 'uppercase', label: 'At least one uppercase letter required', passed: rules.uppercase },
    { key: 'lowercase', label: 'At least one lowercase letter required', passed: rules.lowercase },
    { key: 'number', label: 'At least one number required', passed: rules.number },
    { key: 'special', label: 'At least one special character required', passed: rules.special },
    { key: 'length', label: 'Password must be 8-16 characters long', passed: rules.length },
    { key: 'confirmMatch', label: 'Confirm password must match new password', passed: rules.confirmMatch },
  ];

  const isActive = newPassword.length > 0;

  return (
    <div className="profile-card">
      <div className="profile-card-header">
        <h2 className="profile-card-title">Change password</h2>
      </div>

      {errors.general && (
        <div className="error-message">{errors.general}</div>
      )}

      <div className="password-section">
        <div className="field">
          <label className="field__label">Old password</label>
          <div className="password-wrap">
            <input
              type={showOld ? 'text' : 'password'}
              className={`field__input ${errors.oldPassword ? 'field__input--error' : ''}`}
              value={oldPassword}
              onChange={(e) => setOldPassword(e.target.value)}
              placeholder="Enter your password"
            />
            <button type="button" className="password-toggle" onClick={() => setShowOld(!showOld)}>
              {showOld ? <EyeOffIcon /> : <EyeIcon />}
            </button>
          </div>
          {errors.oldPassword && <span className="field__error">{errors.oldPassword}</span>}
        </div>

        <div className="field">
          <label className="field__label">New password</label>
          <div className="password-wrap">
            <input
              type={showNew ? 'text' : 'password'}
              className={`field__input ${errors.newPassword ? 'field__input--error' : ''}`}
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              placeholder="Enter your new password"
            />
            <button type="button" className="password-toggle" onClick={() => setShowNew(!showNew)}>
              {showNew ? <EyeOffIcon /> : <EyeIcon />}
            </button>
          </div>
          {errors.newPassword && <span className="field__error">{errors.newPassword}</span>}

          {/* Password Rules */}
          <ul className="profile-password-rules">
            {ruleItems.map(({ key, label, passed }) => (
              <li
                key={key}
                className={isActive ? (passed ? 'rule-ok' : 'rule-fail') : ''}
              >
                <span className="rule-dot"></span>
                {label}
              </li>
            ))}
          </ul>
        </div>

        <div className="field">
          <label className="field__label">Confirm password</label>
          <div className="password-wrap">
            <input
              type={showConfirm ? 'text' : 'password'}
              className={`field__input ${errors.confirmPassword ? 'field__input--error' : ''}`}
              value={confirmPassword}
              onChange={(e) => setConfirmPassword(e.target.value)}
              placeholder="Confirm your new password"
            />
            <button type="button" className="password-toggle" onClick={() => setShowConfirm(!showConfirm)}>
              {showConfirm ? <EyeOffIcon /> : <EyeIcon />}
            </button>
          </div>
          {errors.confirmPassword && <span className="field__error">{errors.confirmPassword}</span>}

          {/* Confirm password hint */}
          <div className={`confirm-password-hint ${confirmPassword.length > 0 ? (rules.confirmMatch ? 'hint-ok' : 'hint-fail') : ''}`}>
            <span className="hint-dot"></span>
            Confirm password must match new password
          </div>
        </div>

        <div className="password-actions">
          <button className="btn-save" onClick={handleSave} disabled={saving || !oldPassword || !newPassword || !confirmPassword}>
            {saving ? 'Saving...' : 'Save changes'}
          </button>
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────
// Section: Change Email
// ─────────────────────────────────────────────
function ChangeEmailSection({ user, showToast }) {
  const [newEmail, setNewEmail] = useState('');
  const [confirmEmail, setConfirmEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [saving, setSaving] = useState(false);
  const [errors, setErrors] = useState({});
  const [sentToEmail, setSentToEmail] = useState(null); // Track the email we sent confirmation to

  const validateEmail = (email) => {
    if (!email.trim()) return 'New email is required';
    if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
      return 'Invalid email address. Please ensure it follows the format: username@domain.com';
    }
    if (email.toLowerCase() === user?.email?.toLowerCase()) {
      return 'New email must be different from current email';
    }
    return null;
  };

  const handleSave = async () => {
    const errs = {};

    const emailErr = validateEmail(newEmail);
    if (emailErr) errs.newEmail = emailErr;

    if (!confirmEmail.trim()) {
      errs.confirmEmail = 'Please confirm your new email';
    } else if (confirmEmail.toLowerCase() !== newEmail.toLowerCase()) {
      errs.confirmEmail = 'Email addresses do not match';
    }

    if (!password) errs.password = 'Password is required to confirm email change';

    if (Object.keys(errs).length) {
      setErrors(errs);
      return;
    }

    setSaving(true);
    setErrors({});

    try {
      const emailToSend = newEmail.trim();
      await initiateEmailChange(user.id, emailToSend, password);
      setSentToEmail(emailToSend); // Show the confirmation message
      setNewEmail('');
      setConfirmEmail('');
      setPassword('');
    } catch (err) {
      if (err.fieldErrors) {
        // Map backend field names to frontend field names
        const mapped = {};
        if (err.fieldErrors.email) mapped.newEmail = err.fieldErrors.email;
        if (err.fieldErrors.newEmail) mapped.newEmail = err.fieldErrors.newEmail;
        if (err.fieldErrors.password) mapped.password = err.fieldErrors.password;
        if (Object.keys(mapped).length > 0) {
          setErrors(mapped);
        } else {
          setErrors({ general: err.message || 'Failed to initiate email change.' });
        }
      } else {
        setErrors({ general: err.message || 'Failed to initiate email change.' });
      }
    } finally {
      setSaving(false);
    }
  };

  // If we've sent a confirmation email, show the confirmation message
  if (sentToEmail) {
    return (
      <div className="profile-card">
        <div className="profile-card-header">
          <h2 className="profile-card-title">Change email</h2>
        </div>

        <div className="email-section">
          <div className="email-sent-message">
            <p className="email-sent-text">
              We sent an email to <strong>{sentToEmail}</strong> with a confirmation link.
            </p>
            <p className="email-sent-text">
              Please follow the instructions in the email.
            </p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="profile-card">
      <div className="profile-card-header">
        <h2 className="profile-card-title">Change email</h2>
      </div>

      {errors.general && (
        <div className="error-message">{errors.general}</div>
      )}

      <div className="email-section">
        <div className="current-email-info">
          <span className="current-email-label">Current email:</span>
          <span className="current-email-value">{user?.email || '-'}</span>
        </div>

        <div className="field">
          <label className="field__label">New email</label>
          <input
            type="email"
            className={`field__input ${errors.newEmail ? 'field__input--error' : ''}`}
            value={newEmail}
            onChange={(e) => setNewEmail(e.target.value)}
            placeholder="Enter your new email"
          />
          {errors.newEmail ? (
            <span className="field__error">{errors.newEmail}</span>
          ) : (
            <span className="field__hint">e.g. username@domain.com</span>
          )}
        </div>

        <div className="field">
          <label className="field__label">Confirm new email</label>
          <input
            type="email"
            className={`field__input ${errors.confirmEmail ? 'field__input--error' : ''}`}
            value={confirmEmail}
            onChange={(e) => setConfirmEmail(e.target.value)}
            placeholder="Confirm your new email"
          />
          {errors.confirmEmail ? (
            <span className="field__error">{errors.confirmEmail}</span>
          ) : (
            <span className="field__hint">e.g. username@domain.com</span>
          )}
        </div>

        <div className="field">
          <label className="field__label">Current password</label>
          <div className="password-wrap">
            <input
              type={showPassword ? 'text' : 'password'}
              className={`field__input ${errors.password ? 'field__input--error' : ''}`}
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Enter your password"
            />
            <button type="button" className="password-toggle" onClick={() => setShowPassword(!showPassword)}>
              {showPassword ? <EyeOffIcon /> : <EyeIcon />}
            </button>
          </div>
          {errors.password && <span className="field__error">{errors.password}</span>}
        </div>

        <div className="email-actions">
          <button className="btn-save" onClick={handleSave} disabled={saving || !newEmail || !confirmEmail || !password}>
            {saving ? 'Confirming...' : 'Confirm'}
          </button>
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────
// Main Profile Page
// ─────────────────────────────────────────────
export default function ProfilePage() {
  const { user, refreshUser } = useAuth();
  const [activeTab, setActiveTab] = useState('general');
  const [toast, setToast] = useState(null);

  const showToast = (message) => {
    setToast(message);
  };

  const hideToast = () => {
    setToast(null);
  };

  const tabs = [
    { id: 'general', label: 'General information' },
    { id: 'password', label: 'Change password' },
    { id: 'email', label: 'Change email' },
  ];

  return (
    <div className="profile-page">
      <Header />

      {/* Toast Notification */}
      {toast && <Toast message={toast} onClose={hideToast} />}

      <div className="profile-content">
        <h1 className="profile-page-title">My profile</h1>

        <div className="profile-layout">
          <aside className="profile-sidebar">
            <nav className="profile-nav">
              {tabs.map((tab) => (
                <button
                  key={tab.id}
                  className={`profile-nav-item ${activeTab === tab.id ? 'active' : ''}`}
                  onClick={() => setActiveTab(tab.id)}
                >
                  {tab.label}
                </button>
              ))}
            </nav>
          </aside>

          <main className="profile-main">
            {activeTab === 'general' && (
              <GeneralInformationSection user={user} refreshUser={refreshUser} showToast={showToast} />
            )}
            {activeTab === 'password' && (
              <ChangePasswordSection user={user} showToast={showToast} />
            )}
            {activeTab === 'email' && (
              <ChangeEmailSection user={user} showToast={showToast} />
            )}
          </main>
        </div>
      </div>
    </div>
  );
}













