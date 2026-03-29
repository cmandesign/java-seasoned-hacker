import React, { useState, useEffect } from 'react';
import { useAuth } from './AuthContext';
import { useApi } from './ApiContext';
import axios from 'axios';

const Dashboard = () => {
  const { user, logout, isAdmin } = useAuth();
  const { apiMode } = useApi();
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    if (user && (apiMode === 'secure' || user.userId)) {
      fetchProfile();
    }
  }, [apiMode, user]);

  const fetchProfile = async () => {
    try {
      let url;
      if (apiMode === 'vulnerable' && user?.userId) {
        // Use vulnerable endpoint with userId from JWT
        url = `/api/v1/vulnerable/accounts/${user.userId}`;
      } else {
        // Use secure /me endpoint
        url = `/api/v1/secure/auth/me`;
      }
      const response = await axios.get(url);
      setProfile(response.data);
    } catch (error) {
      setError('Failed to load profile');
      console.error('Profile fetch error:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="app-container">
        <div className="dashboard-container">
          <div className="text-center">Loading...</div>
        </div>
      </div>
    );
  }

  return (
    <div className="app-container">
      <div className="dashboard-container">
        <div className="user-info">
          <h1>Welcome back, {user?.username}!</h1>
          <p>Role: {user?.role}</p>
          <button onClick={logout} className="btn btn-danger">
            Sign Out
          </button>
        </div>

        {error && (
          <div className="error-message">
            {error}
          </div>
        )}

        <div className="grid">
          {/* Profile Card */}
          <div className="card">
            <h2>My Profile</h2>
            {profile ? (
              <div>
                <p><strong>Username:</strong> {profile.username}</p>
                <p><strong>Role:</strong> {profile.role}</p>
              </div>
            ) : (
              <p>Unable to load profile information</p>
            )}
          </div>

          {/* Admin Features */}
          {isAdmin() && (
            <div className="card">
              <h2>Admin Panel</h2>
              <div className="flex-column">
                <button
                  onClick={() => window.location.href = '/admin/users'}
                  className="btn btn-success"
                >
                  Search Users
                </button>
              </div>
            </div>
          )}

          {/* User Features */}
          <div className="card">
            <h2>Quick Actions</h2>
            <div className="flex-column">
              <button
                onClick={() => window.location.reload()}
                className="btn btn-info"
              >
                Refresh Profile
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;