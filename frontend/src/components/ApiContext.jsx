import React, { createContext, useContext, useState, useEffect } from 'react';
import axios from 'axios';

const ApiContext = createContext();

export const useApi = () => {
  const context = useContext(ApiContext);
  if (!context) {
    throw new Error('useApi must be used within an ApiProvider');
  }
  return context;
};

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8085';

export const ApiProvider = ({ children }) => {
  const [apiMode, setApiMode] = useState('vulnerable');

  const apiConfigs = {
    secure: {
      baseURL: API_BASE_URL,
      label: 'Secure APIs',
      description: 'Production-ready endpoints with security'
    },
    vulnerable: {
      baseURL: API_BASE_URL,
      label: 'Vulnerable APIs',
      description: 'Intentionally vulnerable endpoints for testing'
    }
  };

  useEffect(() => {
    axios.defaults.baseURL = apiConfigs[apiMode].baseURL;
  }, [apiMode]);

  const switchApiMode = (mode) => {
    setApiMode(mode);
    // Clear any existing auth headers when switching modes
    delete axios.defaults.headers.common['Authorization'];
    localStorage.removeItem('token');
    // Force page reload to reset authentication state
    window.location.reload();
  };

  const value = {
    apiMode,
    apiConfigs,
    switchApiMode,
    currentConfig: apiConfigs[apiMode]
  };

  return (
    <ApiContext.Provider value={value}>
      {children}
    </ApiContext.Provider>
  );
};