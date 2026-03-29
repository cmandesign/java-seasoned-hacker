import { useState, useRef } from 'react'

export default function DemoPanel({ demo, mode }) {
  const [expanded, setExpanded] = useState(false)
  const [results, setResults] = useState({})
  const [loading, setLoading] = useState({})
  const savedValues = useRef({})

  const actions = demo.actions[mode] || []

  async function runAction(action, index) {
    setLoading((prev) => ({ ...prev, [index]: true }))

    try {
      let url = action.path
      if (action.pathTemplate && action.useSaved) {
        const saved = savedValues.current[action.useSaved]
        if (saved) {
          url = action.pathTemplate.replace(`{${action.useSaved}}`, encodeURIComponent(saved))
        } else {
          setResults((prev) => ({
            ...prev,
            [index]: { error: `Run the previous step first to get ${action.useSaved}` },
          }))
          setLoading((prev) => ({ ...prev, [index]: false }))
          return
        }
      }

      const headers = {}
      if (action.auth) {
        headers['Authorization'] = 'Basic ' + btoa(`${action.auth.username}:${action.auth.password}`)
      }

      const opts = { method: action.method, headers }

      if (action.body) {
        if (action.contentType === 'application/xml') {
          headers['Content-Type'] = 'application/xml'
          opts.body = action.body
        } else {
          headers['Content-Type'] = 'application/json'
          opts.body = JSON.stringify(action.body)
        }
      }

      const res = await fetch(url, opts)
      const contentType = res.headers.get('content-type') || ''
      let data
      if (contentType.includes('json')) {
        data = await res.json()
      } else {
        data = await res.text()
      }

      // Save value if requested
      if (action.saveAs && data && typeof data === 'object') {
        const value = action.saveField ? data[action.saveField] : data.token
        if (value) savedValues.current[action.saveAs] = value
      }

      setResults((prev) => ({
        ...prev,
        [index]: { status: res.status, data },
      }))
    } catch (err) {
      setResults((prev) => ({
        ...prev,
        [index]: { error: err.message },
      }))
    }

    setLoading((prev) => ({ ...prev, [index]: false }))
  }

  const modeColor = mode === 'vulnerable' ? '#da3633' : '#238636'

  return (
    <div style={styles.panel}>
      <div style={styles.panelHeader} onClick={() => setExpanded(!expanded)}>
        <span
          style={{
            ...styles.badge,
            background: modeColor,
          }}
        >
          {demo.id.toUpperCase()}
        </span>
        <div style={styles.headerText}>
          <h2 style={styles.panelTitle}>{demo.title}</h2>
          <p style={styles.panelDesc}>{demo.description}</p>
        </div>
        <span style={styles.chevron}>{expanded ? '▲' : '▼'}</span>
      </div>

      {expanded && (
        <div style={styles.panelBody}>
          {actions.map((action, i) => (
            <div key={i} style={styles.actionRow}>
              <div style={styles.actionInfo}>
                <span style={{
                  ...styles.methodBadge,
                  background: action.method === 'GET' ? '#1f6feb' : '#8957e5',
                }}>
                  {action.method}
                </span>
                <span style={styles.actionLabel}>{action.label}</span>
                {action.auth && (
                  <span style={styles.authBadge}>
                    {action.auth.username}
                  </span>
                )}
              </div>
              <button
                style={styles.runBtn}
                onClick={() => runAction(action, i)}
                disabled={loading[i]}
              >
                {loading[i] ? '...' : 'Run'}
              </button>
              {results[i] && (
                <div style={styles.result}>
                  {results[i].error ? (
                    <pre style={styles.errorText}>Error: {results[i].error}</pre>
                  ) : (
                    <>
                      <span style={{
                        ...styles.statusBadge,
                        background: results[i].status < 300 ? '#238636' : results[i].status < 500 ? '#d29922' : '#da3633',
                      }}>
                        {results[i].status}
                      </span>
                      <pre style={styles.resultPre}>
                        {typeof results[i].data === 'string'
                          ? results[i].data
                          : JSON.stringify(results[i].data, null, 2)}
                      </pre>
                    </>
                  )}
                </div>
              )}
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

const styles = {
  panel: {
    background: '#161b22',
    border: '1px solid #30363d',
    borderRadius: 12,
    overflow: 'hidden',
  },
  panelHeader: {
    display: 'flex',
    alignItems: 'center',
    gap: 16,
    padding: '16px 20px',
    cursor: 'pointer',
    userSelect: 'none',
  },
  badge: {
    padding: '4px 10px',
    borderRadius: 6,
    fontSize: 12,
    fontWeight: 700,
    color: '#fff',
    whiteSpace: 'nowrap',
  },
  headerText: {
    flex: 1,
    minWidth: 0,
  },
  panelTitle: {
    fontSize: 16,
    fontWeight: 600,
    margin: 0,
  },
  panelDesc: {
    fontSize: 13,
    color: '#8b949e',
    margin: '4px 0 0 0',
  },
  chevron: {
    color: '#8b949e',
    fontSize: 12,
  },
  panelBody: {
    padding: '0 20px 20px',
    display: 'flex',
    flexDirection: 'column',
    gap: 12,
  },
  actionRow: {
    background: '#0d1117',
    borderRadius: 8,
    padding: 12,
  },
  actionInfo: {
    display: 'flex',
    alignItems: 'center',
    gap: 8,
    marginBottom: 8,
    flexWrap: 'wrap',
  },
  methodBadge: {
    padding: '2px 8px',
    borderRadius: 4,
    fontSize: 11,
    fontWeight: 700,
    color: '#fff',
  },
  actionLabel: {
    fontSize: 14,
    fontWeight: 500,
  },
  authBadge: {
    fontSize: 11,
    padding: '2px 6px',
    borderRadius: 4,
    background: '#30363d',
    color: '#8b949e',
  },
  runBtn: {
    padding: '6px 16px',
    borderRadius: 6,
    border: '1px solid #30363d',
    background: '#21262d',
    color: '#e1e4e8',
    cursor: 'pointer',
    fontSize: 13,
    fontWeight: 600,
  },
  result: {
    marginTop: 8,
  },
  statusBadge: {
    display: 'inline-block',
    padding: '2px 8px',
    borderRadius: 4,
    fontSize: 12,
    fontWeight: 700,
    color: '#fff',
    marginBottom: 6,
  },
  resultPre: {
    fontSize: 12,
    lineHeight: 1.5,
    background: '#010409',
    padding: 12,
    borderRadius: 6,
    overflow: 'auto',
    maxHeight: 300,
    margin: '4px 0 0 0',
    whiteSpace: 'pre-wrap',
    wordBreak: 'break-all',
  },
  errorText: {
    color: '#f85149',
    fontSize: 12,
    margin: 0,
  },
}
