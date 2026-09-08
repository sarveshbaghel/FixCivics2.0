import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { MapContainer, TileLayer, Marker, Popup } from 'react-leaflet';
import { getReports } from '../api/client.js';
import 'leaflet/dist/leaflet.css';

// Fix Leaflet default marker icon
import L from 'leaflet';
delete L.Icon.Default.prototype._getIconUrl;
L.Icon.Default.mergeOptions({
    iconRetinaUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon-2x.png',
    iconUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-icon.png',
    shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/1.9.4/images/marker-shadow.png',
});

const issueIcons = {
    'Pothole': '🕳️',
    'Garbage': '🗑️',
    'Broken streetlight': '💡',
    'Water leakage': '💧',
    'Other': '📋',
};

export default function MapView() {
    const [reports, setReports] = useState([]);
    const [loading, setLoading] = useState(true);
    const navigate = useNavigate();

    useEffect(() => {
        fetchAllReports();
    }, []);

    const fetchAllReports = async () => {
        try {
            const data = await getReports({ page: 1, page_size: 500 });
            setReports(data.reports || []);
        } catch (err) {
            console.error('Failed to fetch reports for map:', err);
        } finally {
            setLoading(false);
        }
    };

    // Default center (will be overridden by first report)
    const center = reports.length > 0
        ? [reports[0].latitude, reports[0].longitude]
        : [40.7128, -74.006]; // NYC default

    return (
        <>
            <div className="page-header">
                <h2>Map View</h2>
                <p>All reports plotted on the map — click a marker for details</p>
            </div>

            {loading ? (
                <div className="loading">
                    <div className="spinner"></div>
                    Loading map data...
                </div>
            ) : (
                <div className="card">
                    <div className="card-body" style={{ padding: 0 }}>
                        <div className="map-container">
                            <MapContainer
                                center={center}
                                zoom={12}
                                style={{ height: '100%', width: '100%' }}
                            >
                                <TileLayer
                                    attribution='&copy; <a href="https://osm.org/copyright">OpenStreetMap</a>'
                                    url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
                                />
                                {reports.map((report) => (
                                    <Marker key={report.id} position={[report.latitude, report.longitude]}>
                                        <Popup>
                                            <div style={{ minWidth: 200 }}>
                                                <strong>{issueIcons[report.issue_type] || '📋'} {report.issue_type}</strong>
                                                <p style={{ fontSize: 13, margin: '4px 0' }}>{report.description.slice(0, 100)}</p>
                                                <p style={{ fontSize: 12, color: '#64748b' }}>{report.address}</p>
                                                <span className={`badge ${report.status}`} style={{ marginBottom: 8, display: 'inline-block' }}>
                                                    {report.status}
                                                </span>
                                                <br />
                                                <button
                                                    className="btn btn-primary btn-sm"
                                                    style={{ marginTop: 8 }}
                                                    onClick={() => navigate(`/reports/${report.id}`)}
                                                >
                                                    View Details
                                                </button>
                                            </div>
                                        </Popup>
                                    </Marker>
                                ))}
                            </MapContainer>
                        </div>
                    </div>
                </div>
            )}
        </>
    );
}
