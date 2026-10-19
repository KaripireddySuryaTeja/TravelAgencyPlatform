import React, { useState, useCallback, useEffect } from 'react';
import { useDropzone } from 'react-dropzone';
import {
  BarChart, Bar,
  LineChart, Line,
  PieChart, Pie, Cell,
  XAxis, YAxis, CartesianGrid,
  Tooltip, Legend, ResponsiveContainer,
} from 'recharts';
import {
  Upload,
  UploadCloud,
  FolderOpen,
  FileText,
  FileSpreadsheet,
  BarChart2,
  File,
  AlertTriangle,
  XCircle,
  RefreshCw,
  X,
  Users,
  TrendingUp,
  AlertCircle,
  CheckCircle2,
  Zap,
  Info,
  BrainCircuit,
  Check,
  FileSearch,
} from 'lucide-react';
import Header from '../components/Header';
import { analyzeReport } from '../api/reportAnalysis';
import './ReportAnalysisPage.css';

// ─── Chart colours ────────────────────────────────────────────────────────────
const CHART_COLORS = ['#0284c7', '#059669', '#d97706', '#dc2626', '#7c3aed', '#0891b2'];

// ─── Helpers ──────────────────────────────────────────────────────────────────
function toRechartsData({ labels = [], values = [] }) {
  return labels.map((name, i) => ({ name, value: values[i] ?? 0 }));
}

function formatBytes(bytes) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function fileIcon(name) {
  if (!name) return <File size={22} strokeWidth={1.5} color="#94a3b8" />;
  const ext = name.split('.').pop().toLowerCase();
  if (ext === 'pdf')  return <FileText size={22} strokeWidth={1.5} color="#dc2626" />;
  if (ext === 'xlsx') return <FileSpreadsheet size={22} strokeWidth={1.5} color="#059669" />;
  if (ext === 'csv')  return <BarChart2 size={22} strokeWidth={1.5} color="#0284c7" />;
  return <File size={22} strokeWidth={1.5} color="#94a3b8" />;
}

function humanErrorTitle(code) {
  switch (code) {
    case 'SERVICE_UNAVAILABLE': return 'AI Service Unavailable';
    case 'TIMEOUT':             return 'Request Timed Out';
    case 'NETWORK_ERROR':       return 'Cannot Connect to Server';
    case 'PARSE_ERROR':         return 'File Could Not Be Parsed';
    case 'AI_FORMAT_ERROR':     return 'Unexpected AI Response';
    case 'EMPTY_FILE':          return 'Empty File Uploaded';
    default:                    return 'Analysis Failed';
  }
}

// ══════════════════════════════════════════════════════════════════════════════
// ErrorBanner — persistent until the user dismisses it
// ══════════════════════════════════════════════════════════════════════════════
function ErrorBanner({ error, onRetry, onDismiss }) {
  const isWarning = ['SERVICE_UNAVAILABLE', 'TIMEOUT', 'NETWORK_ERROR'].includes(error?.code);
  const variant = isWarning ? 'warning' : 'danger';
  const iconColor = isWarning ? '#d97706' : '#dc2626';

  const title   = humanErrorTitle(error?.code);
  const message = error?.error || error?.message || 'An unexpected error occurred. Please try again.';
  const code    = error?.code;

  return (
    <div className={`ra-error-banner ra-error-banner--${variant}`} role="alert">
      <span className="ra-error-banner-icon">
        {isWarning
          ? <AlertTriangle size={20} strokeWidth={2} color={iconColor} />
          : <XCircle size={20} strokeWidth={2} color={iconColor} />}
      </span>
      <div className="ra-error-banner-body">
        <div className="ra-error-banner-title">{title}</div>
        <div className="ra-error-banner-msg">{message}</div>
        {code && <div className="ra-error-banner-code">Error code: {code}</div>}
        {(isWarning && onRetry) && (
          <div className="ra-error-banner-actions">
            <button className="ra-error-banner-retry" onClick={onRetry}>
              <RefreshCw size={13} strokeWidth={2.5} style={{ marginRight: 5 }} />
              Try Again
            </button>
          </div>
        )}
      </div>
      <button className="ra-error-dismiss" onClick={onDismiss} aria-label="Dismiss error">
        <X size={16} strokeWidth={2} />
      </button>
    </div>
  );
}

// ══════════════════════════════════════════════════════════════════════════════
// LoadingSkeleton — step-by-step progress indicator + shimmer
// ══════════════════════════════════════════════════════════════════════════════
const STEPS = [
  { label: 'Uploading file to server',    duration: 1500 },
  { label: 'Parsing file contents',       duration: 2000 },
  { label: 'Sending to AI for analysis',  duration: 3000 },
  { label: 'Processing AI insights',      duration: null  },
];

function LoadingSkeleton() {
  const [activeStep, setActiveStep] = useState(0);

  useEffect(() => {
    let current = 0;
    function advance() {
      current += 1;
      if (current < STEPS.length - 1) {
        setActiveStep(current);
        setTimeout(advance, STEPS[current].duration);
      } else {
        setActiveStep(STEPS.length - 1);
      }
    }
    const t = setTimeout(advance, STEPS[0].duration);
    return () => clearTimeout(t);
  }, []);

  const statusMsg = STEPS[activeStep]?.label || 'Processing…';

  return (
    <div>
      <div className="ra-loading-panel">
        <div className="ra-loading-header">
          <div className="ra-loading-icon">
            <BrainCircuit size={24} strokeWidth={1.5} color="#0284c7" />
          </div>
          <div>
            <div className="ra-loading-title">AI Analysis in Progress</div>
            <div className="ra-loading-status">{statusMsg}…</div>
          </div>
        </div>

        <div className="ra-loading-steps">
          {STEPS.map((step, i) => {
            const state = i < activeStep ? 'done' : i === activeStep ? 'active' : 'pending';
            return (
              <div key={i} className={`ra-step ra-step--${state}`}>
                <div className="ra-step-indicator">
                  {state === 'done'
                    ? <Check size={13} strokeWidth={3} />
                    : i + 1}
                </div>
                <span className="ra-step-label">{step.label}</span>
              </div>
            );
          })}
        </div>

        <div className="ra-loading-progress">
          <div className="ra-loading-progress-bar" />
        </div>
      </div>

      {/* Shimmer placeholder cards */}
      <div className="ra-shimmer-cards">
        <div className="ra-shimmer-block">
          <div className="ra-shimmer-line ra-shimmer-line--h ra-shimmer-line--w50" />
          <div className="ra-shimmer-line ra-shimmer-line--w85" />
          <div className="ra-shimmer-line ra-shimmer-line--w70" />
        </div>
        <div className="ra-shimmer-block">
          <div className="ra-shimmer-line ra-shimmer-line--h ra-shimmer-line--w50" />
          <div className="ra-shimmer-row">
            {[0,1,2,3].map(i => <div key={i} className="ra-shimmer-col" />)}
          </div>
        </div>
      </div>
    </div>
  );
}

// ══════════════════════════════════════════════════════════════════════════════
// CircularGauge — SVG data-quality score
// ══════════════════════════════════════════════════════════════════════════════
function CircularGauge({ value }) {
  const R = 36;
  const C = 2 * Math.PI * R;
  const clamped = Math.min(100, Math.max(0, value));
  const offset = C - (clamped / 100) * C;
  const color = clamped >= 75 ? '#059669' : clamped >= 50 ? '#d97706' : '#dc2626';

  return (
    <div className="ra-gauge">
      <svg width="90" height="90" viewBox="0 0 90 90">
        <circle cx="45" cy="45" r={R} fill="none" stroke="#e2e8f0" strokeWidth="7" />
        <circle
          cx="45" cy="45" r={R} fill="none"
          stroke={color} strokeWidth="7"
          strokeDasharray={C}
          strokeDashoffset={offset}
          strokeLinecap="round"
          transform="rotate(-90 45 45)"
          style={{ transition: 'stroke-dashoffset 0.8s ease' }}
        />
        <text x="45" y="41" textAnchor="middle"
          style={{ fill: color, fontSize: '1rem', fontWeight: 700, fontFamily: 'IBM Plex Mono, monospace' }}>
          {clamped}
        </text>
        <text x="45" y="55" textAnchor="middle"
          style={{ fill: '#94a3b8', fontSize: '0.55rem', fontFamily: 'IBM Plex Mono, monospace' }}>
          / 100
        </text>
      </svg>
      <div className="ra-gauge-label">Data Quality</div>
    </div>
  );
}

// ══════════════════════════════════════════════════════════════════════════════
// InsightCard — scrollable list with coloured accent
// ══════════════════════════════════════════════════════════════════════════════
function InsightCard({ icon, title, items, colorClass }) {
  return (
    <div className={`ra-insight-card ra-insight-card--${colorClass}`}>
      <div className="ra-insight-card-header">
        <div className="ra-insight-card-icon">{icon}</div>
        <div className="ra-insight-card-title">{title}</div>
        <div className="ra-insight-card-count">{items?.length ?? 0}</div>
      </div>
      {items && items.length > 0 ? (
        <ul className="ra-insight-list">
          {items.map((item, i) => (
            <li key={i} className="ra-insight-item">
              <span className="ra-insight-item-bullet">●</span>
              {item}
            </li>
          ))}
        </ul>
      ) : (
        <p className="ra-insight-empty">None identified in this report.</p>
      )}
    </div>
  );
}

// ══════════════════════════════════════════════════════════════════════════════
// ChartCard — renders one AI-returned chart
// ══════════════════════════════════════════════════════════════════════════════
function ChartCard({ chart }) {
  const data = toRechartsData(chart);
  const tooltipStyle = {
    backgroundColor: '#fff',
    border: '1px solid #e2e8f0',
    borderRadius: 8,
    fontSize: 12,
  };

  return (
    <div className="ra-chart-card">
      <div className="ra-chart-title">{chart.title}</div>
      <ResponsiveContainer width="100%" height={220}>
        {chart.type === 'pie' ? (
          <PieChart>
            <Pie data={data} dataKey="value" nameKey="name" cx="50%" cy="50%"
                 outerRadius={75} label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}>
              {data.map((_, i) => <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} />)}
            </Pie>
            <Tooltip contentStyle={tooltipStyle} />
            <Legend />
          </PieChart>
        ) : chart.type === 'line' ? (
          <LineChart data={data} margin={{ top: 4, right: 16, bottom: 4, left: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
            <XAxis dataKey="name" tick={{ fill: '#94a3b8', fontSize: 11 }} axisLine={false} tickLine={false} />
            <YAxis tick={{ fill: '#94a3b8', fontSize: 11 }} axisLine={false} tickLine={false} />
            <Tooltip contentStyle={tooltipStyle} />
            <Line type="monotone" dataKey="value" stroke="#0284c7" strokeWidth={2.5}
                  dot={{ fill: '#0284c7', r: 4 }} activeDot={{ r: 6 }} />
          </LineChart>
        ) : (
          <BarChart data={data} margin={{ top: 4, right: 16, bottom: 4, left: 0 }}>
            <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" vertical={false} />
            <XAxis dataKey="name" tick={{ fill: '#94a3b8', fontSize: 11 }} axisLine={false} tickLine={false} />
            <YAxis tick={{ fill: '#94a3b8', fontSize: 11 }} axisLine={false} tickLine={false} />
            <Tooltip contentStyle={tooltipStyle} cursor={{ fill: '#f8fafc' }} />
            <Bar dataKey="value" radius={[5, 5, 0, 0]}>
              {data.map((_, i) => <Cell key={i} fill={CHART_COLORS[i % CHART_COLORS.length]} />)}
            </Bar>
          </BarChart>
        )}
      </ResponsiveContainer>
    </div>
  );
}

// ══════════════════════════════════════════════════════════════════════════════
// AnalysisResults — renders full AI result
// ══════════════════════════════════════════════════════════════════════════════
function AnalysisResults({ result }) {
  const risk = (result.riskLevel || 'LOW').toUpperCase();

  return (
    <div className="ra-results">
      {/* Executive Summary */}
      <div className="ra-summary-card">
        <div>
          <div className="ra-summary-top">
            <span className="ra-summary-label">Executive Summary</span>
            <span className={`ra-risk ra-risk--${risk}`}>
              <span className="ra-risk-dot" /> {risk} Risk
            </span>
          </div>
          <p className="ra-summary-text">{result.summary || 'No summary available.'}</p>
        </div>
        <CircularGauge value={result.dataQualityScore} />
      </div>

      {/* Insights grid */}
      <div className="ra-insights-grid">
        <InsightCard
          icon={<TrendingUp size={15} strokeWidth={2.5} color="#2563eb" />}
          title="Key Trends"
          items={result.keyTrends}
          colorClass="blue"
        />
        <InsightCard
          icon={<AlertTriangle size={15} strokeWidth={2.5} color="#d97706" />}
          title="Anomalies"
          items={result.anomalies}
          colorClass="amber"
        />
        <InsightCard
          icon={<AlertCircle size={15} strokeWidth={2.5} color="#dc2626" />}
          title="Potential Issues"
          items={result.potentialIssues}
          colorClass="red"
        />
        <InsightCard
          icon={<CheckCircle2 size={15} strokeWidth={2.5} color="#059669" />}
          title="Recommendations"
          items={result.actionableRecommendations}
          colorClass="green"
        />
      </div>

      {/* Charts */}
      {result.charts && result.charts.length > 0 && (
        <div className="ra-charts-section">
          <div className="ra-section-title">
            <BarChart2 size={16} strokeWidth={2} color="#0284c7" />
            Visual Insights
          </div>
          <div className="ra-charts-grid">
            {result.charts.map((chart, i) => <ChartCard key={i} chart={chart} />)}
          </div>
        </div>
      )}
    </div>
  );
}

// ══════════════════════════════════════════════════════════════════════════════
// ReportUploader — drag-and-drop sidebar panel
// ══════════════════════════════════════════════════════════════════════════════
function ReportUploader({ onAnalyze, loading }) {
  const [file, setFile]         = useState(null);
  const [reportType, setReportType] = useState('STAFF');
  const [uploadError, setUploadError] = useState('');

  const onDrop = useCallback((accepted, rejected) => {
    setUploadError('');
    if (rejected && rejected.length > 0) {
      const code = rejected[0].errors?.[0]?.code;
      if (code === 'file-too-large')    setUploadError('File exceeds the 10 MB limit. Please upload a smaller file.');
      else if (code === 'file-invalid-type') setUploadError('Unsupported file type. Please upload CSV, XLSX, PDF, or TXT.');
      else                              setUploadError('Invalid file. Please try again.');
      return;
    }
    if (accepted?.length > 0) setFile(accepted[0]);
  }, []);

  const { getRootProps, getInputProps, isDragActive, isDragReject } = useDropzone({
    onDrop,
    accept: {
      'text/csv':        ['.csv'],
      'application/csv': ['.csv'],
      'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet': ['.xlsx'],
      'application/pdf': ['.pdf'],
      'text/plain':      ['.txt'],
    },
    maxSize: 10 * 1024 * 1024,
    multiple: false,
    disabled: loading,
  });

  const dropzoneClass = [
    'ra-dropzone',
    isDragActive  ? 'ra-dropzone--active' : '',
    isDragReject  ? 'ra-dropzone--reject'  : '',
  ].filter(Boolean).join(' ');

  return (
    <>
      {/* Upload section */}
      <div className="ra-card">
        <div className="ra-card-header">
          <div className="ra-card-icon ra-card-icon--teal">
            <Upload size={18} strokeWidth={2} color="#0284c7" />
          </div>
          <div>
            <div className="ra-card-title">Upload Report</div>
            <div className="ra-card-subtitle">CSV, XLSX, PDF or TXT · max 10 MB</div>
          </div>
        </div>

        <div {...getRootProps({ className: dropzoneClass })}>
          <input {...getInputProps()} />
          <span className="ra-dropzone-icon">
            {isDragActive
              ? <FolderOpen size={36} strokeWidth={1.5} color="#0284c7" />
              : <UploadCloud size={36} strokeWidth={1.5} color="#94a3b8" />}
          </span>
          <div className="ra-dropzone-primary">
            {isDragActive
              ? 'Drop your report here'
              : <><span className="ra-dropzone-link">Click to browse</span> or drag & drop</>}
          </div>
          <div className="ra-dropzone-secondary">Supports CSV, XLSX, PDF, TXT</div>
        </div>

        {file && (
          <div className="ra-file-pill">
            <span className="ra-file-pill-icon">{fileIcon(file.name)}</span>
            <div className="ra-file-pill-info">
              <div className="ra-file-pill-name" title={file.name}>{file.name}</div>
              <div className="ra-file-pill-meta">{formatBytes(file.size)} · {file.type || 'unknown type'}</div>
            </div>
            <button
              className="ra-file-pill-remove"
              onClick={e => { e.stopPropagation(); setFile(null); setUploadError(''); }}
              aria-label="Remove file"
              disabled={loading}
            >
              <X size={14} strokeWidth={2.5} />
            </button>
          </div>
        )}

        {uploadError && (
          <div className="ra-upload-error">
            <AlertTriangle size={14} strokeWidth={2} style={{ flexShrink: 0, marginTop: 1 }} />
            {uploadError}
          </div>
        )}
      </div>

      {/* Report type */}
      <div className="ra-card">
        <div className="ra-report-type-label">Report Type</div>
        <div className="ra-toggle-group">
          {[
            { value: 'STAFF',             icon: <Users size={18} strokeWidth={1.8} />,      label: 'Staff Report'      },
            { value: 'BUSINESS_ACTIVITY', icon: <TrendingUp size={18} strokeWidth={1.8} />, label: 'Business Activity' },
          ].map(opt => (
            <label key={opt.value} className="ra-toggle-opt">
              <input
                type="radio"
                name="reportType"
                value={opt.value}
                checked={reportType === opt.value}
                onChange={() => setReportType(opt.value)}
                disabled={loading}
              />
              <div className="ra-toggle-label">
                <span className="ra-toggle-label-icon">{opt.icon}</span>
                <span className="ra-toggle-label-text">{opt.label}</span>
              </div>
            </label>
          ))}
        </div>

        <button
          className={`ra-analyze-btn${loading ? ' ra-analyze-btn--loading' : ''}`}
          onClick={() => file && onAnalyze(file, reportType)}
          disabled={!file || loading}
        >
          {loading
            ? <><div className="ra-btn-spinner" /> Analyzing Report…</>
            : <><Zap size={15} strokeWidth={2.5} /> Analyze Report</>}
        </button>
      </div>

      {/* Info box */}
      <div className="ra-info-box">
        <Info size={14} strokeWidth={2} style={{ flexShrink: 0, marginTop: 1 }} />
        <span>
          Analysis is powered by <strong>gpt-4.1-mini</strong> via EPAM DIAL.
          Large files may take up to 30 seconds.
        </span>
      </div>
    </>
  );
}

// ══════════════════════════════════════════════════════════════════════════════
// ReportAnalysisPage — root component
// ══════════════════════════════════════════════════════════════════════════════
export default function ReportAnalysisPage() {
  const [loading,  setLoading]  = useState(false);
  const [result,   setResult]   = useState(null);
  const [apiError, setApiError] = useState(null);
  const [lastFile, setLastFile] = useState(null);
  const [lastType, setLastType] = useState(null);

  const runAnalysis = useCallback(async (file, reportType) => {
    setLoading(true);
    setApiError(null);
    setResult(null);
    setLastFile(file);
    setLastType(reportType);

    try {
      const data = await analyzeReport(file, reportType);
      setResult(data);
    } catch (err) {
      console.error('[ReportAnalysis] API error:', err);
      if (err && typeof err === 'object') {
        setApiError(err);
      } else {
        setApiError({ error: String(err), code: 'UNKNOWN_ERROR' });
      }
    } finally {
      setLoading(false);
    }
  }, []);

  const handleRetry = useCallback(() => {
    if (lastFile && lastType) {
      setApiError(null);
      runAnalysis(lastFile, lastType);
    } else {
      setApiError(null);
    }
  }, [lastFile, lastType, runAnalysis]);

  return (
    <div className="ra-page">
      <Header />
      <main className="ra-main">

        {/* Page header */}
        <div className="ra-page-header">
          <div className="ra-page-title-group">
            <div className="ra-breadcrumb">
              Admin
              <span className="ra-breadcrumb-sep">›</span>
              <span className="ra-breadcrumb-current">AI Report Analysis</span>
            </div>
            <h1 className="ra-page-title">Report Analyzer</h1>
            <p className="ra-page-subtitle">
              Upload staff or business activity reports to receive AI-powered trends,
              anomalies, and actionable recommendations.
            </p>
          </div>
          <div className="ra-header-meta">
            <span className="ra-badge ra-badge--admin">
              <span className="ra-badge--dot" /> Admin
            </span>
            <span className="ra-badge ra-badge--online">
              <span className="ra-badge--dot" /> AI Online
            </span>
          </div>
        </div>

        {/* Error banner */}
        {apiError && (
          <ErrorBanner
            error={apiError}
            onRetry={handleRetry}
            onDismiss={() => setApiError(null)}
          />
        )}

        {/* Two-panel layout */}
        <div className="ra-layout">
          <div className="ra-sidebar">
            <ReportUploader onAnalyze={runAnalysis} loading={loading} />
          </div>

          <div>
            {loading && !result && <LoadingSkeleton />}

            {!loading && result && <AnalysisResults result={result} />}

            {!loading && !result && !apiError && (
              <div className="ra-empty">
                <div className="ra-empty-illustration">
                  <FileSearch size={36} strokeWidth={1.5} color="#94a3b8" />
                </div>
                <div className="ra-empty-title">No Analysis Yet</div>
                <div className="ra-empty-desc">
                  Upload a report file on the left panel and click
                  <strong> "Analyze Report"</strong> to get AI-powered insights.
                </div>
                <div className="ra-empty-steps">
                  {['Upload a file', 'Select report type', 'Click Analyze'].map((s, i) => (
                    <div key={i} className="ra-empty-step">
                      <div className="ra-empty-step-num">{i + 1}</div>
                      {s}
                    </div>
                  ))}
                </div>
              </div>
            )}

            {!loading && !result && apiError && (
              <div className="ra-empty">
                <div className="ra-empty-illustration">
                  <AlertCircle size={36} strokeWidth={1.5} color="#d97706" />
                </div>
                <div className="ra-empty-title">Analysis Failed</div>
                <div className="ra-empty-desc">
                  Please check the error details above and try again. Make sure the backend
                  server is running and the API key is configured correctly.
                </div>
              </div>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}

