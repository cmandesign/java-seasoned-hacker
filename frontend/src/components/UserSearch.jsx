import React, { useState } from 'react';
import { useAuth } from './AuthContext';
import { useApi } from './ApiContext';
import axios from 'axios';

const UserSearch = () => {
  const { user, isAdmin } = useAuth();
  const { apiMode } = useApi();
  const [userId, setUserId] = useState('');
  const [userData, setUserData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  // Redirect if not admin
  if (!isAdmin()) {
    return (
      <div className="app-container">
        <div className="dashboard-container">
          <div className="card">
            <h2>Access Denied</h2>
            <p>You need admin privileges to access this page.</p>
            <button onClick={() => window.location.href = '/dashboard'} className="btn btn-primary">
              Back to Dashboard
            </button>
          </div>
        </div>
      </div>
    );
  }

  const searchUser = async () => {
    if (!userId.trim()) {
      setError('Please enter a user ID');
      return;
    }

    setLoading(true);
    setError('');
    setUserData(null);

    try {
      const response = await axios.get(`/api/v1/${apiMode}/accounts/${userId}`);
      setUserData(response.data);
    } catch (error) {
      if (error.response?.status === 404) {
        setError('User not found');
      } else if (error.response?.status === 403) {
        setError('You do not have permission to view this user');
      } else {
        setError('Failed to fetch user data');
      }
      console.error('User search error:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter') {
      searchUser();
    }
  };

  return (
    <div className="app-container">
      <div className="dashboard-container">
        <div className="mb-3">
          <button
            onClick={() => window.location.href = '/dashboard'}
            className="btn btn-secondary mb-3"
          >
            ← Back to Dashboard
          </button>
          <h1>Admin - User Search</h1>
          <p className="text-muted">Search for user accounts by ID. Only admins can access this feature.</p>
        </div>

        <div className="card mb-3">
          <div className="flex mb-3">
            <input
              type="text"
              placeholder="Enter User ID (e.g., 1, 2, 3)"
              value={userId}
              onChange={(e) => setUserId(e.target.value)}
              onKeyPress={handleKeyPress}
              style={{ flex: 1 }}
            />
            <button
              onClick={searchUser}
              disabled={loading}
              className="btn btn-primary"
            >
              {loading ? 'Searching...' : 'Search'}
            </button>
          </div>

          {error && (
            <div className="error-message">
              {error}
            </div>
          )}
        </div>

        {userData && (
          <div className="card">
            <h2>User Details</h2>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '15px' }}>
              <div>
                <strong>ID:</strong> {userData.id}
              </div>
              <div>
                <strong>Username:</strong> {userData.username}
              </div>
              <div>
                <strong>Email:</strong> {userData.email}
              </div>
              <div>
                <strong>Role:</strong> {userData.role}
              </div>
              <div>
                <strong>Enabled:</strong> {userData.enabled ? 'Yes' : 'No'}
              </div>
              <div>
                <strong>Created:</strong> {new Date(userData.createdAt).toLocaleDateString()}
              </div>
            </div>
          </div>
        )}

        <div className="info-box">
          <h3>Demo User IDs</h3>
          <p>Try searching for: <strong>1</strong> (admin), <strong>2</strong> (alice), <strong>3</strong> (bob)</p>
        </div>
      </div>
    </div>
  );
};

export default UserSearch;