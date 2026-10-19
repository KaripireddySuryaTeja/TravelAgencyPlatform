import axios from 'axios';
import { API_BASE_URL, TOKEN_KEY } from './client';

/**
 * Axios client for the Admin Report Analysis API.
 * Timeout is 35 s (> the 30-s backend AI timeout).
 */
const reportAnalysisClient = axios.create({
  baseURL: API_BASE_URL,
  timeout: 35000,
});

reportAnalysisClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY);
  if (token) {
    config.headers = config.headers || {};
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  return config;
});

/**
 * Normalises any Axios error into a plain { error, code } object
 * so the UI always has a consistent shape to render.
 */
function normaliseError(err) {
  // Axios timeout
  if (err.code === 'ECONNABORTED' || (err.message || '').toLowerCase().includes('timeout')) {
    return { error: 'The request timed out. The AI service may be slow — please try again.', code: 'TIMEOUT' };
  }
  // Network error (backend not running, CORS, etc.)
  if (!err.response) {
    return {
      error: `Cannot connect to the server at ${API_BASE_URL}. Make sure the backend is running.`,
      code: 'NETWORK_ERROR',
    };
  }
  // Server returned a structured JSON error (our backend error format)
  const data = err.response.data;
  if (data && typeof data === 'object' && (data.error || data.code)) {
    return data;
  }
  // HTTP error with plain / HTML body
  return {
    error: `Server returned HTTP ${err.response.status}. ${err.response.statusText || ''}`.trim(),
    code: `HTTP_${err.response.status}`,
  };
}

/**
 * Uploads a report file and triggers AI analysis.
 *
 * @param {File}   file        The report file (CSV / XLSX / PDF / TXT, max 10 MB)
 * @param {string} reportType  "STAFF" | "BUSINESS_ACTIVITY"
 * @returns {Promise<AnalysisResult>}
 */
export async function analyzeReport(file, reportType) {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('reportType', reportType);

  console.info(`[ReportAnalysis] POST ${API_BASE_URL}/api/reports/analyze`, { file: file.name, reportType });

  try {
    const { data } = await reportAnalysisClient.post('/api/reports/analyze', formData);
    console.info('[ReportAnalysis] Success:', data);
    return data;
  } catch (err) {
    const normalised = normaliseError(err);
    console.error('[ReportAnalysis] Error:', normalised, err);
    throw normalised;
  }
}

/**
 * Liveness check for the report analysis backend.
 */
export async function checkReportAnalysisHealth() {
  const { data } = await reportAnalysisClient.get('/api/reports/health');
  return data;
}
