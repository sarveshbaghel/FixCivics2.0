import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { getReports, getFullImageUrl } from '../api/client.js';

const ISSUE_TYPES = ['', 'Pothole', 'Garbage', 'Broken streetlight', 'Water leakage', 'Other'];
const STATUSES = ['', 'pending', 'approved', 'resolved', 'rejected'];

const issueIcons = {
    'Pothole': '🕳️',
    'Garbage': '🗑️',
    'Broken streetlight': '💡',
    'Water leakage': '💧',
    'Other': '📋',
};

export default function ReportsList() {
    const [reports, setReports] = useState([]);
    const [total, setTotal] = useState(0);
    const [page, setPage] = useState(1);
    const [pageSize] = useState(15);
    const [issueFilter, setIssueFilter] = useState('');
    const [statusFilter, setStatusFilter] = useState('');
    const [loading, setLoading] = useState(true);
    const [stats, setStats] = useState({ pending: 0, resolved: 0, rejected: 0, total: 0 });

    const fetchReports = async () => {
        setLoading(true);
        try {
            const params = { page, page_size: pageSize };
            if (issueFilter) params.issue_type = issueFilter;
            if (statusFilter) params.status = statusFilter;
            const data = await getReports(params);
            setReports(data.reports || []);
            setTotal(data.total || 0);

            // Calculate stats from all reports
            const allData = await getReports({ page: 1, page_size: 1000 });
            const all = allData.reports || [];
            setStats({
                total: all.length,
                pending: all.filter(r => r.status === 'pending').length,
                resolved: all.filter(r => r.status === 'resolved').length,
                rejected: all.filter(r => r.status === 'rejected').length,
            });
        } catch (err) {
            console.error('Failed to fetch reports:', err);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        fetchReports();
    }, [page, issueFilter, statusFilter]);

    const totalPages = Math.ceil(total / pageSize);

    return (
        <>
            <div className="page-header">
                <h2>Dashboard</h2>
                <p>Overview of all civic issue reports</p>
            </div>

            {/* Stats */}
            <div className="stats-grid">
                <div className="stat-card">
                    <div className="stat-icon total">
                        <span className="material-icons-outlined">description</span>
                    </div>
                    <div className="stat-value">{stats.total}</div>
                    <div className="stat-label">Total Reports</div>
                </div>
                <div className="stat-card">
                    <div className="stat-icon pending">
                        <span className="material-icons-outlined">pending_actions</span>
                    </div>
                    <div className="stat-value">{stats.pending}</div>
                    <div className="stat-label">Pending</div>
                </div>
                <div className="stat-card">
                    <div className="stat-icon resolved">
                        <span className="material-icons-outlined">check_circle</span>
                    </div>
                    <div className="stat-value">{stats.resolved}</div>
                    <div className="stat-label">Resolved</div>
                </div>
                <div className="stat-card">
                    <div className="stat-icon rejected">
                        <span className="material-icons-outlined">cancel</span>
                    </div>
                    <div className="stat-value">{stats.rejected}</div>
                    <div className="stat-label">Rejected</div>
                </div>
            </div>

            {/* Filters */}
            <div className="card">
                <div className="card-header">
                    <h3>All Reports</h3>
                    <span style={{ fontSize: 13, color: '#94a3b8' }}>{total} total</span>
                </div>
                <div className="card-body">
                    <div className="filter-bar">
                        <select
                            value={issueFilter}
                            onChange={(e) => { setIssueFilter(e.target.value); setPage(1); }}
                        >
                            <option value="">All Issue Types</option>
                            {ISSUE_TYPES.filter(Boolean).map(t => (
                                <option key={t} value={t}>{issueIcons[t]} {t}</option>
                            ))}
                        </select>
                        <select
                            value={statusFilter}
                            onChange={(e) => { setStatusFilter(e.target.value); setPage(1); }}
                        >
                            <option value="">All Statuses</option>
                            {STATUSES.filter(Boolean).map(s => (
                                <option key={s} value={s}>{s.charAt(0).toUpperCase() + s.slice(1)}</option>
                            ))}
                        </select>
                    </div>

                    {loading ? (
                        <div className="loading">
                            <div className="spinner"></div>
                            Loading reports...
                        </div>
                    ) : reports.length === 0 ? (
                        <div className="empty-state">
                            <span className="material-icons-outlined">inbox</span>
                            <p>No reports found</p>
                        </div>
                    ) : (
                        <>
                            <div className="table-container">
                                <table>
                                    <thead>
                                        <tr>
                                            <th>Image</th>
                                            <th>Issue Type</th>
                                            <th>Description</th>
                                            <th>Location</th>
                                            <th>Status</th>
                                            <th>Date</th>
                                            <th>Action</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        {reports.map((report) => (
                                            <tr key={report.id}>
                                                <td>
                                                    {report.thumbnail_url || report.image_url ? (
                                                        <img
                                                            src={getFullImageUrl(report.thumbnail_url || report.image_url)}
                                                            alt="Report"
                                                            className="report-thumb"
                                                            onError={(e) => { e.target.style.display = 'none'; }}
                                                        />
                                                    ) : (
                                                        <span className="material-icons-outlined" style={{ color: '#cbd5e1' }}>image</span>
                                                    )}
                                                </td>
                                                <td>
                                                    <span className="issue-type-label">
                                                        {issueIcons[report.issue_type] || '📋'} {report.issue_type}
                                                    </span>
                                                </td>
                                                <td style={{ maxWidth: 200, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                                    {report.description}
                                                </td>
                                                <td style={{ maxWidth: 180, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                                    {report.address || `${report.latitude.toFixed(4)}, ${report.longitude.toFixed(4)}`}
                                                </td>
                                                <td>
                                                    <span className={`badge ${report.status}`}>
                                                        {report.status}
                                                    </span>
                                                </td>
                                                <td style={{ whiteSpace: 'nowrap', fontSize: 13 }}>
                                                    {new Date(report.created_at).toLocaleDateString('en-US', {
                                                        month: 'short', day: 'numeric', year: 'numeric'
                                                    })}
                                                </td>
                                                <td>
                                                    <Link to={`/reports/${report.id}`} className="btn btn-outline btn-sm">
                                                        View
                                                    </Link>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>

                            {/* Pagination */}
                            {totalPages > 1 && (
                                <div className="pagination">
                                    <button
                                        onClick={() => setPage(p => Math.max(1, p - 1))}
                                        disabled={page === 1}
                                    >
                                        ‹
                                    </button>
                                    {Array.from({ length: Math.min(totalPages, 5) }, (_, i) => {
                                        const pageNum = i + 1;
                                        return (
                                            <button
                                                key={pageNum}
                                                className={page === pageNum ? 'active' : ''}
                                                onClick={() => setPage(pageNum)}
                                            >
                                                {pageNum}
                                            </button>
                                        );
                                    })}
                                    <button
                                        onClick={() => setPage(p => Math.min(totalPages, p + 1))}
                                        disabled={page === totalPages}
                                    >
                                        ›
                                    </button>
                                </div>
                            )}
                        </>
                    )}
                </div>
            </div>
        </>
    );
}
