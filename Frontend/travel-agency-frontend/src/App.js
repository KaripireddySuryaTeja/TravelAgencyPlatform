import React from 'react';
import { Route, Routes } from 'react-router-dom';
import RegisterPage from './pages/RegisterPage';
import SignInPage from './pages/SignInPage';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import VerifyCodePage from './pages/VerifyCodePage';
import ResetPasswordPage from './pages/ResetPasswordPage';
import HomePage from './pages/HomePage';
import TourDetailPage from './pages/TourDetailPage';
import MyToursPage from './pages/MyToursPage';
import ReportsPage from './pages/ReportsPage';
import ReportAnalysisPage from './pages/ReportAnalysisPage';
import FeedbackModerationPage from './pages/FeedbackModerationPage';
import ProfilePage from './pages/ProfilePage';
import ConfirmEmailPage from './pages/ConfirmEmailPage';
import AgentBookingsPage from './pages/AgentBookingsPage';
import OAuthSuccessPage from './pages/OAuthSuccessPage';
import ProtectedRoute from './auth/ProtectedRoute';
import PublicOnlyRoute from './auth/PublicOnlyRoute';
import AdminRoute from './auth/AdminRoute';

export default function App() {
  return (
    <Routes>
      <Route
        path="/register"
        element={
          <PublicOnlyRoute>
            <RegisterPage />
          </PublicOnlyRoute>
        }
      />
      <Route
        path="/sign-in"
        element={
          <PublicOnlyRoute>
            <SignInPage />
          </PublicOnlyRoute>
        }
      />
      <Route
        path="/forgot-password"
        element={
          <PublicOnlyRoute>
            <ForgotPasswordPage />
          </PublicOnlyRoute>
        }
      />
      <Route
        path="/verify-code"
        element={
          <PublicOnlyRoute>
            <VerifyCodePage />
          </PublicOnlyRoute>
        }
      />
      <Route
        path="/reset-password"
        element={
          <PublicOnlyRoute>
            <ResetPasswordPage />
          </PublicOnlyRoute>
        }
      />
      <Route
        path="/"
        element={<HomePage />}
      />
      <Route
        path="/tours/:id"
        element={<TourDetailPage />}
      />
      <Route
        path="/my-tours"
        element={
          <ProtectedRoute>
            <MyToursPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/reports"
        element={
          <AdminRoute>
            <ReportsPage />
          </AdminRoute>
        }
      />
      <Route
        path="/admin/feedback"
        element={
          <AdminRoute>
            <FeedbackModerationPage />
          </AdminRoute>
        }
      />
      <Route
        path="/admin/report-analysis"
        element={
          <AdminRoute>
            <ReportAnalysisPage />
          </AdminRoute>
        }
      />
      <Route
        path="/profile"
        element={
          <ProtectedRoute>
            <ProfilePage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/bookings"
        element={
          <ProtectedRoute>
            <AgentBookingsPage />
          </ProtectedRoute>
        }
      />
      <Route
        path="/confirm-email"
        element={<ConfirmEmailPage />}
      />
      {/* OAuth2 social login callback — must be public, not wrapped in PublicOnlyRoute */}
      <Route
        path="/oauth-success"
        element={<OAuthSuccessPage />}
      />
      <Route
        path="*"
        element={<HomePage />}
      />
    </Routes>
  );
}
