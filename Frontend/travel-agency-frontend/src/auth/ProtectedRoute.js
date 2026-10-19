import React from 'react';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuth } from './AuthContext';

export default function ProtectedRoute({ children }) {
  const { user, token, loading } = useAuth();
  const location = useLocation();

  if (loading) return null;

  if (!token || !user) {
    return <Navigate to="/sign-in" state={{ from: `${location.pathname}${location.search}${location.hash}` }} replace />;
  }
  return <>{children}</>;
}
