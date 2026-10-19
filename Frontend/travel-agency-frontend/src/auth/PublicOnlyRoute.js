import React from 'react';
import { Navigate } from 'react-router-dom';
import { useAuth } from './AuthContext';

export default function PublicOnlyRoute({ children }) {
  const { user, token, loading } = useAuth();

  if (loading) return null;
  if (token && user) return <Navigate to="/" replace />;
  return <>{children}</>;
}
