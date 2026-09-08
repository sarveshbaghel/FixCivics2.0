import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../App.jsx';
import { loginAdmin } from '../api/client.js';

export default function LoginPage() {
    const [email, setEmail] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const [loading, setLoading] = useState(false);
    const { login } = useAuth();
    const navigate = useNavigate();

    const handleSubmit = async (e) => {
        e.preventDefault();
        setError('');
        setLoading(true);

        try {
            const data = await loginAdmin(email, password);
            login(
                { id: data.user_id, display_name: data.display_name, role: data.role },
                data.access_token
            );
            navigate('/');
        } catch (err) {
            setError(err.response?.data?.detail || 'Login failed. Please try again.');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="login-page">
            <div className="login-card">
                <div className="logo">
                    <h1>🏛️ CivicFix</h1>
                    <p>Admin Dashboard</p>
                </div>

                {error && <div className="error-msg">{error}</div>}

                <form onSubmit={handleSubmit}>
                    <div className="form-group">
                        <label htmlFor="email">Email Address</label>
                        <input
                            id="email"
                            type="email"
                            value={email}
                            onChange={(e) => setEmail(e.target.value)}
                            placeholder="admin@civicfix.com"
                            required
                        />
                    </div>
                    <div className="form-group">
                        <label htmlFor="password">Password</label>
                        <input
                            id="password"
                            type="password"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            placeholder="Enter password"
                            required
                        />
                    </div>
                    <button type="submit" className="btn btn-primary" disabled={loading}>
                        {loading ? (
                            <>
                                <span className="spinner" style={{ width: 16, height: 16, borderWidth: 2 }}></span>
                                Signing in...
                            </>
                        ) : (
                            <>
                                <span className="material-icons-outlined">login</span>
                                Sign In
                            </>
                        )}
                    </button>
                </form>

                <p style={{ textAlign: 'center', marginTop: 24, fontSize: 13, color: '#94a3b8' }}>
                    Default: admin@civicfix.com / admin123
                </p>
            </div>
        </div>
    );
}
