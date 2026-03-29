import React from 'react';
import { useApi } from './ApiContext';

const ApiSwitcher = () => {
  const { apiMode, apiConfigs, switchApiMode, currentConfig } = useApi();

  const handleApiChange = (e) => {
    const newMode = e.target.value;
    if (newMode !== apiMode) {
      switchApiMode(newMode);
    }
  };

  return (
    <div style={{
      backgroundColor: '#f8f9fa',
      borderBottom: '1px solid #e9ecef',
      padding: '0.75rem 2rem',
      position: 'sticky',
      top: 0,
      zIndex: 1000
    }}>
      <div style={{
        maxWidth: '1200px',
        margin: '0 auto',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between'
      }}>
        <div>
          <h3 style={{ margin: 0, color: '#495057', fontSize: '1.1rem' }}>
            OWASP Demo Frontend
          </h3>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <label htmlFor="api-mode" style={{ fontSize: '0.9rem', color: '#495057', fontWeight: '500' }}>
              API Mode:
            </label>
            <select
              id="api-mode"
              value={apiMode}
              onChange={handleApiChange}
              style={{
                padding: '0.375rem 0.75rem',
                border: '1px solid #ced4da',
                borderRadius: '0.375rem',
                backgroundColor: 'white',
                fontSize: '0.9rem',
                minWidth: '140px'
              }}
            >
              <option value="secure">🔒 Secure APIs</option>
              <option value="vulnerable">⚠️ Vulnerable APIs</option>
            </select>
          </div>

          <div style={{
            fontSize: '0.8rem',
            color: '#6c757d',
            backgroundColor: apiMode === 'vulnerable' ? '#fff3cd' : '#d1ecf1',
            padding: '0.25rem 0.5rem',
            borderRadius: '0.25rem',
            border: `1px solid ${apiMode === 'vulnerable' ? '#ffeaa7' : '#bee5eb'}`
          }}>
            {currentConfig.description}
          </div>
        </div>
      </div>
    </div>
  );
};

export default ApiSwitcher;