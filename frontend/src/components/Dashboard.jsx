import React, { useState, useEffect } from 'react';
import { useAuth } from './AuthContext';
import { useApi } from './ApiContext';
import axios from 'axios';

const Dashboard = () => {
  const { user, logout, isAdmin } = useAuth();
  const { apiMode } = useApi();
  const [profile, setProfile] = useState(null);
  const [tickets, setTickets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [newEmail, setNewEmail] = useState('');
  const [emailMsg, setEmailMsg] = useState('');
  const [emailError, setEmailError] = useState('');
  const [photoFile, setPhotoFile] = useState(null);
  const [photoMsg, setPhotoMsg] = useState('');
  const [photoError, setPhotoError] = useState('');

  useEffect(() => {
    if (user && (apiMode === 'secure' || user.userId)) {
      fetchProfile();
      fetchTickets();
    }
  }, [apiMode, user]);

  const fetchProfile = async () => {
    try {
      let url;
      if (apiMode === 'vulnerable' && user?.userId) {
        url = `/api/v1/vulnerable/accounts/${user.userId}`;
      } else {
        url = `/api/v1/secure/auth/me`;
      }
      const response = await axios.get(url);
      setProfile(response.data);
      setNewEmail(response.data.email || '');
    } catch (error) {
      setError('Failed to load profile');
      console.error('Profile fetch error:', error);
    } finally {
      setLoading(false);
    }
  };

  const fetchTickets = async () => {
    try {
      if (!user?.userId) return;
      const url = `/api/v1/${apiMode}/accounts/${user.userId}/tickets`;
      const response = await axios.get(url);
      setTickets(response.data);
    } catch (err) {
      console.error('Tickets fetch error:', err);
    }
  };

  const handleUpdateEmail = async (e) => {
    e.preventDefault();
    setEmailMsg('');
    setEmailError('');
    try {
      if (!user?.userId) return;
      const url = `/api/v1/${apiMode}/accounts/${user.userId}`;
      await axios.put(url, { email: newEmail });
      setEmailMsg('Email updated successfully');
      fetchProfile();
    } catch (err) {
      setEmailError(err.response?.data?.error || 'Failed to update email');
    }
  };

  const handleUploadPhoto = async (e) => {
    e.preventDefault();
    setPhotoMsg('');
    setPhotoError('');
    if (!photoFile || !user?.userId) return;
    try {
      const formData = new FormData();
      formData.append('file', photoFile);
      const url = `/api/v1/${apiMode}/accounts/${user.userId}/photo`;
      const res = await axios.post(url, formData, {
        headers: { 'Content-Type': 'multipart/form-data' }
      });
      setPhotoMsg(res.data.message || 'Photo uploaded');
      setPhotoFile(null);
    } catch (err) {
      setPhotoError(err.response?.data?.error || 'Failed to upload photo');
    }
  };

  const parseHolders = (holdersJson) => {
    try {
      return JSON.parse(holdersJson);
    } catch {
      return [];
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
                <p><strong>Email:</strong> {profile.email}</p>
                <p><strong>Role:</strong> {profile.role}</p>
              </div>
            ) : (
              <p>Unable to load profile information</p>
            )}
          </div>

          {/* Update Email Card */}
          <div className="card">
            <h2>Update Email</h2>
            <form onSubmit={handleUpdateEmail}>
              <div className="form-group">
                <label htmlFor="email">New Email</label>
                <input
                  id="email"
                  type="email"
                  value={newEmail}
                  onChange={(e) => setNewEmail(e.target.value)}
                  required
                />
              </div>
              {emailMsg && <div className="success-message">{emailMsg}</div>}
              {emailError && <div className="error-message">{emailError}</div>}
              <button type="submit" className="btn btn-primary">
                Update Email
              </button>
            </form>
          </div>

          {/* Upload Photo Card */}
          <div className="card">
            <h2>Upload Photo</h2>
            <form onSubmit={handleUploadPhoto}>
              <div className="form-group">
                <label htmlFor="photo">Profile Photo</label>
                <input
                  id="photo"
                  type="file"
                  accept="image/*"
                  onChange={(e) => setPhotoFile(e.target.files[0])}
                />
              </div>
              {photoMsg && <div className="success-message">{photoMsg}</div>}
              {photoError && <div className="error-message">{photoError}</div>}
              <button type="submit" className="btn btn-primary" disabled={!photoFile}>
                Upload
              </button>
            </form>
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

          {/* Quick Actions */}
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

        {/* My Tickets Section */}
        <div style={{ marginTop: '1.5rem' }}>
          <div className="card">
            <h2>My Tickets</h2>
            {tickets.length === 0 ? (
              <p className="text-muted">No tickets purchased yet.</p>
            ) : (
              <div className="flex-column">
                {tickets.map((ticket) => (
                  <div key={ticket.id} style={{
                    border: '1px solid #e9ecef',
                    borderRadius: '8px',
                    padding: '1rem',
                    background: '#f8f9fa'
                  }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                      <strong style={{ fontSize: '1.1rem' }}>{ticket.eventName}</strong>
                      <span style={{
                        background: '#667eea',
                        color: 'white',
                        padding: '0.2rem 0.6rem',
                        borderRadius: '12px',
                        fontSize: '0.85rem'
                      }}>
                        {ticket.quantity} {ticket.quantity === 1 ? 'ticket' : 'tickets'}
                      </span>
                    </div>
                    <p style={{ margin: '0.25rem 0', color: '#718096' }}>
                      Price: ${ticket.price}
                    </p>
                    <div style={{ marginTop: '0.5rem' }}>
                      <strong>Ticket Holders:</strong>
                      <ul style={{ margin: '0.25rem 0 0 1.25rem', padding: 0 }}>
                        {parseHolders(ticket.ticketHolders).map((name, i) => (
                          <li key={i}>{name}</li>
                        ))}
                      </ul>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default Dashboard;
