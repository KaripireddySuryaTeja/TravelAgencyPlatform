import React, { useState, useEffect, useRef, useCallback } from 'react';
import Header from '../components/Header';
import { getReportLocations, generateStaffPerformanceReport, generateSalesReport } from '../api/reports';
import jsPDF from 'jspdf';
import autoTable from 'jspdf-autotable';
import * as XLSX from 'xlsx';
import './ReportsPage.css';

const REPORT_TYPES = [
  { value: 'STAFF_PERFORMANCE', label: 'Staff performance' },
  { value: 'SALES', label: 'Sales' },
];

const MONTHS = ['January', 'February', 'March', 'April', 'May', 'June',
  'July', 'August', 'September', 'October', 'November', 'December'];

function parseLocalDate(str) {
  if (!str) return null;
  const [y, m, d] = str.split('-').map(Number);
  return new Date(y, m - 1, d);
}

function formatDisplay(str) {
  if (!str) return '';
  const d = parseLocalDate(str);
  return `${MONTHS[d.getMonth()].slice(0, 3)} ${d.getDate()}, ${d.getFullYear()}`;
}

function toISODate(date) {
  const y = date.getFullYear();
  const m = String(date.getMonth() + 1).padStart(2, '0');
  const d = String(date.getDate()).padStart(2, '0');
  return `${y}-${m}-${d}`;
}

// ─── DateRangePicker ──────────────────────────────────────────────────────────
function DateRangePicker({ value, onChange }) {
  const { from, to } = value;
  const [open, setOpen] = useState(false);
  const [selecting, setSelecting] = useState('from'); // 'from' | 'to'
  const [hovered, setHovered] = useState(null);
  const ref = useRef(null);

  const today = new Date();
  const initMonth = from ? parseLocalDate(from) : today;
  const [viewYear, setViewYear] = useState(initMonth.getFullYear());
  const [viewMonth, setViewMonth] = useState(initMonth.getMonth());

  useEffect(() => {
    const handler = (e) => {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false);
    };
    if (open) document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, [open]);

  const daysInMonth = new Date(viewYear, viewMonth + 1, 0).getDate();
  const firstDayOfWeek = new Date(viewYear, viewMonth, 1).getDay(); // 0=Sun
  // Start on Monday
  const offset = (firstDayOfWeek + 6) % 7;

  const cells = [];
  for (let i = 0; i < offset; i++) cells.push(null);
  for (let d = 1; d <= daysInMonth; d++) cells.push(d);

  const prevMonth = () => {
    if (viewMonth === 0) { setViewMonth(11); setViewYear(y => y - 1); }
    else setViewMonth(m => m - 1);
  };
  const nextMonth = () => {
    if (viewMonth === 11) { setViewMonth(0); setViewYear(y => y + 1); }
    else setViewMonth(m => m + 1);
  };

  const handleDayClick = (day) => {
    if (!day) return;
    const clicked = toISODate(new Date(viewYear, viewMonth, day));
    if (selecting === 'from' || !from) {
      onChange({ from: clicked, to: null });
      setSelecting('to');
    } else {
      if (clicked < from) {
        onChange({ from: clicked, to: from });
      } else {
        onChange({ from, to: clicked });
      }
      setSelecting('from');
      setOpen(false);
    }
  };

  const isDayStart = (day) => {
    if (!day || !from) return false;
    return toISODate(new Date(viewYear, viewMonth, day)) === from;
  };
  const isDayEnd = (day) => {
    if (!day || !to) return false;
    return toISODate(new Date(viewYear, viewMonth, day)) === to;
  };
  const isDayInRange = (day) => {
    if (!day || !from) return false;
    const d = toISODate(new Date(viewYear, viewMonth, day));
    const end = to || hovered;
    if (!end) return false;
    const lo = from < end ? from : end;
    const hi = from < end ? end : from;
    return d > lo && d < hi;
  };

  const displayLabel = from && to
    ? `${formatDisplay(from)} - ${formatDisplay(to)}`
    : from
      ? `${formatDisplay(from)} - Select end`
      : 'Select period';

  return (
    <div className="rp-dropdown-wrap" ref={ref}>
      <button
        className={`rp-dropdown-btn${open ? ' open' : ''}${from ? ' has-value' : ''}`}
        onClick={() => { setOpen(o => !o); setSelecting('from'); }}
        type="button"
      >
        <span className="rp-dropdown-label">{displayLabel}</span>
        <ChevronIcon open={open} />
      </button>
      {open && (
        <div className="rp-calendar-panel">
          <div className="rp-cal-header">
            <button className="rp-cal-nav" onClick={prevMonth} type="button">&#8249;</button>
            <span className="rp-cal-month-label">{MONTHS[viewMonth]} {viewYear}</span>
            <button className="rp-cal-nav" onClick={nextMonth} type="button">&#8250;</button>
          </div>
          <div className="rp-cal-grid">
            {['M', 'T', 'W', 'T', 'F', 'S', 'S'].map((d, i) => (
              <div key={i} className="rp-cal-weekday">{d}</div>
            ))}
            {cells.map((day, i) => (
              <div
                key={i}
                className={[
                  'rp-cal-day',
                  !day ? 'empty' : '',
                  isDayStart(day) ? 'start' : '',
                  isDayEnd(day) ? 'end' : '',
                  isDayInRange(day) ? 'in-range' : '',
                ].filter(Boolean).join(' ')}
                onClick={() => handleDayClick(day)}
                onMouseEnter={() => day && setHovered(toISODate(new Date(viewYear, viewMonth, day)))}
                onMouseLeave={() => setHovered(null)}
              >
                {day || ''}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

// ─── CustomDropdown ───────────────────────────────────────────────────────────
function CustomDropdown({ options, value, onChange, placeholder }) {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);

  useEffect(() => {
    const handler = (e) => {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false);
    };
    if (open) document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, [open]);

  const selected = options.find(o => o.value === value);

  return (
    <div className="rp-dropdown-wrap" ref={ref}>
      <button
        className={`rp-dropdown-btn${open ? ' open' : ''}${value ? ' has-value' : ''}`}
        onClick={() => setOpen(o => !o)}
        type="button"
      >
        <span className="rp-dropdown-label">{selected ? selected.label : placeholder}</span>
        <ChevronIcon open={open} />
      </button>
      {open && (
        <div className="rp-dropdown-menu">
          {options.map(opt => (
            <div
              key={opt.value}
              className={`rp-dropdown-item${opt.value === value ? ' selected' : ''}`}
              onClick={() => { onChange(opt.value); setOpen(false); }}
            >
              {opt.label}
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function ChevronIcon({ open }) {
  return (
    <svg
      className={`rp-chevron${open ? ' rotated' : ''}`}
      width="16" height="16" viewBox="0 0 24 24"
      fill="none" xmlns="http://www.w3.org/2000/svg"
    >
      <path d="M6 9l6 6 6-6" stroke="currentColor" strokeWidth="1.8"
        strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

// ─── Staff Performance Table ──────────────────────────────────────────────────
function StaffPerformanceTable({ rows }) {
  return (
    <div className="rp-table-wrap">
      <table className="rp-table">
        <thead>
          <tr>
            <th>Travel Agent</th>
            <th>Travel Agent e-mail</th>
            <th title="Reporting period start">Reporting period start</th>
            <th title="Reporting period end">Reporting period end</th>
            <th>Tours sold</th>
            <th title="Delta of tours sold vs previous period">Delta of tours sold</th>
            <th title="Average Feedback for Travel experience (1 to 5)">Avg. Feedback</th>
            <th title="Minimum Feedback for Travel experience (1 to 5)">Min. Feedback</th>
            <th title="Delta of Average Feedback vs previous period">Delta of Avg. Feedback</th>
            <th>Reviews</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row, i) => (
            <tr key={i}>
              <td>{row.agentName}</td>
              <td><a href={`mailto:${row.agentEmail}`} className="rp-email-link">{row.agentEmail}</a></td>
              <td>{formatDisplay(row.periodStart)}</td>
              <td>{formatDisplay(row.periodEnd)}</td>
              <td>{row.toursSold}</td>
              <td className={deltaClass(row.deltaOfToursSold)}>{row.deltaOfToursSold}</td>
              <td>{row.avgFeedback ?? '—'}</td>
              <td>{row.minFeedback ?? '—'}</td>
              <td className={deltaClass(row.deltaOfAvgFeedback)}>{row.deltaOfAvgFeedback}</td>
              <td>{row.reviewCount}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

// ─── Sales Table ──────────────────────────────────────────────────────────────
function SalesTable({ rows }) {
  return (
    <div className="rp-table-wrap">
      <table className="rp-table">
        <thead>
          <tr>
            <th>Tour Name</th>
            <th>Destination</th>
            <th title="Reporting period start">Reporting period start</th>
            <th title="Reporting period end">Reporting period end</th>
            <th>Bookings</th>
            <th title="Delta of bookings vs previous period">Delta of bookings</th>
            <th>Total Guests</th>
            <th title="Delta of guests vs previous period">Delta of guests</th>
          </tr>
        </thead>
        <tbody>
          {rows.map((row, i) => (
            <tr key={i}>
              <td>{row.tourName}</td>
              <td>{row.destination}</td>
              <td>{formatDisplay(row.periodStart)}</td>
              <td>{formatDisplay(row.periodEnd)}</td>
              <td>{row.bookingsCount}</td>
              <td className={deltaClass(row.deltaBookings)}>{row.deltaBookings}</td>
              <td>{row.totalGuests}</td>
              <td className={deltaClass(row.deltaGuests)}>{row.deltaGuests}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function deltaClass(val) {
  if (!val || val === '0%' || val === 'N/A') return '';
  return val.startsWith('+') ? 'rp-delta-pos' : 'rp-delta-neg';
}

// ─── Column definitions for export ──────────────────────────────────────────
const STAFF_COLS = [
  { header: 'Travel Agent',            key: 'agentName' },
  { header: 'Travel Agent e-mail',     key: 'agentEmail' },
  { header: 'Reporting period start',  key: 'periodStart' },
  { header: 'Reporting period end',    key: 'periodEnd' },
  { header: 'Tours sold',              key: 'toursSold' },
  { header: 'Delta of tours sold',     key: 'deltaOfToursSold' },
  { header: 'Avg. Feedback (1-5)',     key: 'avgFeedback' },
  { header: 'Min. Feedback (1-5)',     key: 'minFeedback' },
  { header: 'Delta of Avg. Feedback',  key: 'deltaOfAvgFeedback' },
  { header: 'Reviews',                 key: 'reviewCount' },
];

const SALES_COLS = [
  { header: 'Tour Name',               key: 'tourName' },
  { header: 'Destination',             key: 'destination' },
  { header: 'Reporting period start',  key: 'periodStart' },
  { header: 'Reporting period end',    key: 'periodEnd' },
  { header: 'Bookings',                key: 'bookingsCount' },
  { header: 'Delta of bookings',       key: 'deltaBookings' },
  { header: 'Total Guests',            key: 'totalGuests' },
  { header: 'Delta of guests',         key: 'deltaGuests' },
];

// ─── Download Button ──────────────────────────────────────────────────────────
function DownloadButton({ reportType, rows }) {
  const [open, setOpen] = useState(false);
  const ref = useRef(null);

  useEffect(() => {
    const handler = (e) => {
      if (ref.current && !ref.current.contains(e.target)) setOpen(false);
    };
    if (open) document.addEventListener('mousedown', handler);
    return () => document.removeEventListener('mousedown', handler);
  }, [open]);

  const cols = reportType === 'STAFF_PERFORMANCE' ? STAFF_COLS : SALES_COLS;
  const filename = reportType === 'STAFF_PERFORMANCE' ? 'staff_performance_report' : 'sales_report';

  const getTableData = () => ({
    head: [cols.map(c => c.header)],
    body: rows.map(row => cols.map(c => row[c.key] ?? '')),
  });

  const downloadPDF = () => {
    if (!rows || rows.length === 0) return;
    const doc = new jsPDF({ orientation: 'landscape' });
    doc.setFontSize(13);
    doc.text(filename.replace(/_/g, ' ').replace(/\b\w/g, l => l.toUpperCase()), 14, 14);
    const { head, body } = getTableData();
    autoTable(doc, {
      head,
      body,
      startY: 20,
      styles: { fontSize: 8 },
      headStyles: { fillColor: [12, 109, 143] },
    });
    doc.save(`${filename}.pdf`);
    setOpen(false);
  };

  const downloadExcel = () => {
    if (!rows || rows.length === 0) return;
    const { head, body } = getTableData();
    const wsData = [head[0], ...body];
    const ws = XLSX.utils.aoa_to_sheet(wsData);
    const wb = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(wb, ws, 'Report');
    XLSX.writeFile(wb, `${filename}.xlsx`);
    setOpen(false);
  };

  const downloadCSV = () => {
    if (!rows || rows.length === 0) return;
    const { head, body } = getTableData();
    const csvRows = [
      head[0].map(h => `"${h}"`).join(','),
      ...body.map(row => row.map(v => `"${v}"`).join(',')),
    ];
    const blob = new Blob([csvRows.join('\n')], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${filename}.csv`;
    a.click();
    URL.revokeObjectURL(url);
    setOpen(false);
  };

  return (
    <div className="rp-download-wrap" ref={ref}>
      <button
        className={`rp-download-btn${open ? ' open' : ''}`}
        onClick={() => setOpen(o => !o)}
        type="button"
      >
        Download
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none"
          xmlns="http://www.w3.org/2000/svg" style={{ marginLeft: 6 }}>
          <path d={open ? 'M18 15l-6-6-6 6' : 'M6 9l6 6 6-6'} stroke="currentColor" strokeWidth="2"
            strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </button>
      {open && (
        <div className="rp-download-menu">
          <button className="rp-download-item" onClick={downloadPDF}>Download PDF</button>
          <button className="rp-download-item" onClick={downloadExcel}>Download Excel</button>
          <button className="rp-download-item" onClick={downloadCSV}>Download CSV</button>
        </div>
      )}
    </div>
  );
}

// ─── Main Page ────────────────────────────────────────────────────────────────
export default function ReportsPage() {
  const [reportType, setReportType] = useState('');
  const [dateRange, setDateRange] = useState({ from: '', to: '' });
  const [location, setLocation] = useState('');
  const [locations, setLocations] = useState([]);
  const [reportData, setReportData] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    getReportLocations()
      .then(locs => setLocations(locs.map(l => ({ value: l, label: l }))))
      .catch(() => {});
  }, []);

  const locationOptions = [{ value: '', label: 'All locations' }, ...locations];

  const canGenerate = reportType && dateRange.from && dateRange.to;

  const handleGenerate = useCallback(async () => {
    if (!canGenerate) return;
    setLoading(true);
    setError('');
    setReportData(null);
    try {
      const params = { from: dateRange.from, to: dateRange.to, location: location || undefined };
      const data = reportType === 'STAFF_PERFORMANCE'
        ? await generateStaffPerformanceReport(params)
        : await generateSalesReport(params);
      setReportData(data);
    } catch (e) {
      setError(e?.response?.data?.message || 'Failed to generate report. Please try again.');
    } finally {
      setLoading(false);
    }
  }, [reportType, dateRange, location, canGenerate]);

  return (
    <div className="rp-page">
      <Header />
      <main className="rp-main">
        <h1 className="rp-title">Generate a report</h1>

        <div className="rp-filter-card">
          <CustomDropdown
            options={REPORT_TYPES}
            value={reportType}
            onChange={setReportType}
            placeholder="Select report type"
          />
          <DateRangePicker
            value={dateRange}
            onChange={setDateRange}
          />
          <CustomDropdown
            options={locationOptions}
            value={location}
            onChange={setLocation}
            placeholder="Select location"
          />
          <button
            className="rp-generate-btn"
            onClick={handleGenerate}
            disabled={!canGenerate || loading}
            type="button"
          >
            {loading ? 'Generating…' : 'Generate report'}
          </button>
        </div>

        {error && <p className="rp-error">{error}</p>}

        {reportData && (
          <section className="rp-results">
            <div className="rp-results-header">
              <h2 className="rp-results-title">Report</h2>
              <DownloadButton
                reportType={reportType}
                rows={reportData}
              />
            </div>
            {reportData.length === 0 ? (
              <p className="rp-empty">No data found for the selected filters.</p>
            ) : reportType === 'STAFF_PERFORMANCE' ? (
              <StaffPerformanceTable rows={reportData} />
            ) : (
              <SalesTable rows={reportData} />
            )}
          </section>
        )}
      </main>
    </div>
  );
}
