import React, { useState, useEffect } from 'react';
import { getSettings, updateSettings } from '../api/client.js';

export default function Settings() {
    const [settings, setSettings] = useState(null);
    const [loading, setLoading] = useState(true);
    const [toggling, setToggling] = useState(false);
    const [message, setMessage] = useState(null);

    useEffect(() => {
        fetchSettings();
    }, []);

    const fetchSettings = async () => {
        try {
            const data = await getSettings();
            setSettings(data);
        } catch (err) {
            console.error('Failed to fetch settings:', err);
            setMessage({ type: 'error', text: 'Failed to load settings' });
        } finally {
            setLoading(false);
        }
    };

    const handleToggle = async () => {
        if (!settings) return;
        setToggling(true);
        setMessage(null);
        try {
            const newValue = !settings.x_auto_post_enabled;
            const data = await updateSettings({ x_auto_post_enabled: newValue });
            setSettings(data);
            setMessage({
                type: 'success',
                text: `Auto-post to X ${newValue ? 'enabled' : 'disabled'} successfully`,
            });
        } catch (err) {
            setMessage({ type: 'error', text: err.response?.data?.detail || 'Failed to update settings' });
        } finally {
            setToggling(false);
        }
    };

    if (loading) {
        return <div className="loading"><div className="spinner"></div>Loading settings...</div>;
    }

    return (
        <>
            <div className="page-header">
                <h2>Settings</h2>
                <p>Configure your CivicFix application</p>
            </div>

            {message && (
                <div className="error-msg" style={{
                    background: message.type === 'success' ? '#ecfdf5' : undefined,
                    color: message.type === 'success' ? '#047857' : undefined,
                    marginBottom: 20,
                }}>
                    {message.text}
                </div>
            )}

            {/* X Integration Card */}
            <div className="card settings-card" style={{ marginBottom: 24 }}>
                <div className="card-header">
                    <h3>
                        <span style={{ fontSize: 22 }}>𝕏</span>&nbsp; X (Twitter) Integration
                    </h3>
                    <span className={`badge ${settings?.x_api_connected ? 'resolved' : 'pending'}`}>
                        {settings?.x_api_connected ? '✅ Connected' : '⚠️ API Keys Missing'}
                    </span>
                </div>
                <div className="card-body">
                    <div className="settings-description">
                        <p style={{ color: '#64748b', lineHeight: 1.7, marginBottom: 0 }}>
                            When auto-post is <strong>enabled</strong>, every new report submitted by
                            citizens will automatically be posted to your X (Twitter) account, increasing
                            public visibility and accountability. When <strong>disabled</strong>, you can
                            still manually post individual reports using the "Post to X" button on each
                            report's detail page.
                        </p>
                    </div>

                    {/* Auto-post toggle */}
                    <div className="settings-toggle-row">
                        <div className="toggle-info">
                            <div className="toggle-label">Automatic Post to X</div>
                            <div className="toggle-sublabel">
                                {settings?.x_auto_post_enabled
                                    ? 'New reports will be automatically posted to X'
                                    : 'Reports must be posted to X manually'}
                            </div>
                        </div>
                        <button
                            className={`toggle-switch ${settings?.x_auto_post_enabled ? 'active' : ''} ${toggling ? 'toggling' : ''}`}
                            onClick={handleToggle}
                            disabled={toggling}
                            id="auto-post-toggle"
                            aria-label="Toggle automatic post to X"
                        >
                            <span className="toggle-knob"></span>
                        </button>
                    </div>
                </div>
            </div>

            {/* Connection Status Card */}
            <div className="card settings-card">
                <div className="card-header">
                    <h3>🔗 Connection Status</h3>
                </div>
                <div className="card-body">
                    <div className="status-grid">
                        <div className="status-item">
                            <div className="status-dot-container">
                                <span className={`status-dot ${settings?.x_api_connected ? 'connected' : 'disconnected'}`}></span>
                            </div>
                            <div className="status-info">
                                <div className="status-name">OAuth 1.0a (Post access)</div>
                                <div className="status-detail">
                                    {settings?.x_api_connected
                                        ? 'Consumer Key & Access Token configured'
                                        : 'Access Token & Secret needed for posting'}
                                </div>
                            </div>
                        </div>
                        <div className="status-item">
                            <div className="status-dot-container">
                                <span className={`status-dot ${settings?.x_bearer_configured ? 'connected' : 'disconnected'}`}></span>
                            </div>
                            <div className="status-info">
                                <div className="status-name">Bearer Token (Read access)</div>
                                <div className="status-detail">
                                    {settings?.x_bearer_configured
                                        ? 'Bearer token configured'
                                        : 'Bearer token not configured'}
                                </div>
                            </div>
                        </div>
                    </div>

                    {!settings?.x_api_connected && (
                        <div className="settings-hint">
                            <span className="material-icons-outlined" style={{ fontSize: 18, marginRight: 8, verticalAlign: 'middle' }}>info</span>
                            To enable real posting, add your <strong>Access Token</strong> and <strong>Access Token Secret</strong> from the
                            {' '}<a href="https://developer.x.com/en/portal/dashboard" target="_blank" rel="noopener noreferrer">X Developer Portal</a>.
                            Until then, posts will be simulated.
                        </div>
                    )}
                </div>
            </div>
        </>
    );
}
