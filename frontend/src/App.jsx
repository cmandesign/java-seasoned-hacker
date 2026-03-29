import { useState } from 'react'
import DemoPanel from './components/DemoPanel'
import { demos } from './demos'

export default function App() {
  const [mode, setMode] = useState('vulnerable')

  return (
    <div style={styles.app}>
      <header style={styles.header}>
        <h1 style={styles.title}>OWASP Top 10 (2025) — Java Demo</h1>
        <div style={styles.toggleContainer}>
          <button
            style={{
              ...styles.toggleBtn,
              ...(mode === 'vulnerable' ? styles.vulnerableActive : {}),
            }}
            onClick={() => setMode('vulnerable')}
          >
            Vulnerable
          </button>
          <button
            style={{
              ...styles.toggleBtn,
              ...(mode === 'secure' ? styles.secureActive : {}),
            }}
            onClick={() => setMode('secure')}
          >
            Secure
          </button>
        </div>
        <a
          href="/swagger-ui/index.html"
          target="_blank"
          rel="noopener noreferrer"
          style={styles.swaggerLink}
        >
          Swagger UI
        </a>
      </header>

      <main style={styles.main}>
        {demos.map((demo) => (
          <DemoPanel key={demo.id} demo={demo} mode={mode} />
        ))}
      </main>
    </div>
  )
}

const styles = {
  app: {
    fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, sans-serif",
    background: '#0f1117',
    color: '#e1e4e8',
    minHeight: '100vh',
    margin: 0,
  },
  header: {
    display: 'flex',
    alignItems: 'center',
    gap: 24,
    padding: '16px 32px',
    background: '#161b22',
    borderBottom: '1px solid #30363d',
    position: 'sticky',
    top: 0,
    zIndex: 100,
  },
  title: {
    fontSize: 18,
    fontWeight: 600,
    margin: 0,
    whiteSpace: 'nowrap',
  },
  toggleContainer: {
    display: 'flex',
    borderRadius: 8,
    overflow: 'hidden',
    border: '1px solid #30363d',
  },
  toggleBtn: {
    padding: '8px 20px',
    border: 'none',
    cursor: 'pointer',
    fontSize: 14,
    fontWeight: 600,
    background: '#21262d',
    color: '#8b949e',
    transition: 'all 0.2s',
  },
  vulnerableActive: {
    background: '#da3633',
    color: '#fff',
  },
  secureActive: {
    background: '#238636',
    color: '#fff',
  },
  swaggerLink: {
    marginLeft: 'auto',
    color: '#58a6ff',
    textDecoration: 'none',
    fontSize: 14,
    fontWeight: 500,
  },
  main: {
    padding: '24px 32px',
    display: 'flex',
    flexDirection: 'column',
    gap: 16,
    maxWidth: 1200,
    margin: '0 auto',
  },
}
