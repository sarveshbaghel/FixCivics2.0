import React, { useState, useEffect, createContext, useContext } from 'react';
import { BrowserRouter, Routes, Route, Navigate, Link, useLocation } from 'react-router-dom';
import LoginPage from './pages/Login.jsx';
import ReportsList from './pages/ReportsList.jsx';
import ReportDetail from './pages/ReportDetail.jsx';
import MapView from './pages/MapView.jsx';
import Settings from './pages/Settings.jsx';

// Auth Context
export const AuthContext = createContext(null);

export function useAuth() {
    return useContext(AuthContext);
}

function AuthProvider({ children }) {
    const [user, setUser] = useState(null);
    const [token, setToken] = useState(null);

    useEffect(() => {
        const savedToken = localStorage.getItem('civicfix_token');
        const savedUser = localStorage.getItem('civicfix_user');
        if (savedToken && savedUser) {
            setToken(savedToken);
            setUser(JSON.parse(savedUser));
        }
    }, []);

    const login = (userData, accessToken) => {
        setUser(userData);
        setToken(accessToken);
        localStorage.setItem('civicfix_token', accessToken);
        localStorage.setItem('civicfix_user', JSON.stringify(userData));
    };

    const logout = () => {
        setUser(null);
        setToken(null);
        localStorage.removeItem('civicfix_token');
        localStorage.removeItem('civicfix_user');
    };

    return (
        <AuthContext.Provider value={{ user, token, login, logout, isAuthenticated: !!token }}>
            {children}
        </AuthContext.Provider>
    );
}

function ProtectedRoute({ children }) {
    const { isAuthenticated } = useAuth();
    if (!isAuthenticated) return <Navigate to="/login" replace />;
    return children;
}

function Sidebar() {
    const location = useLocation();
    const { user, logout } = useAuth();

    const navItems = [
        { path: '/', icon: 'dashboard', label: 'Dashboard' },
        { path: '/reports', icon: 'list_alt', label: 'Reports' },
        { path: '/map', icon: 'map', label: 'Map View' },
        { path: '/settings', icon: 'settings', label: 'Settings' },
    ];

    return (
        <aside className="sidebar">
            <div className="sidebar-brand">
                <h1>
                    <span className="material-icons-outlined">report_problem</span>
                    CivicFix
                </h1>
                <p>Admin Dashboard</p>
            </div>
            <nav className="sidebar-nav">
                {navItems.map((item) => (
                    <Link
                        key={item.path}
                        to={item.path}
                        className={`nav-item ${location.pathname === item.path ? 'active' : ''}`}
                    >
                        <span className="material-icons-outlined">{item.icon}</span>
                        {item.label}
                    </Link>
                ))}
            </nav>
            <div className="sidebar-footer">
                <div className="sidebar-user">
                    <div className="avatar">
                        {user?.display_name?.[0]?.toUpperCase() || 'A'}
                    </div>
                    <div className="user-info">
                        <div className="user-name">{user?.display_name || 'Admin'}</div>
                        <div className="user-role">Administrator</div>
                    </div>
                    <button className="logout-btn" onClick={logout} title="Logout">
                        <span className="material-icons-outlined">logout</span>
                    </button>
                </div>
            </div>
        </aside>
    );
}

function AppLayout() {
    return (
        <div className="app-layout">
            <Sidebar />
            <main className="main-content">
                <Routes>
                    <Route path="/" element={<ReportsList />} />
                    <Route path="/reports" element={<ReportsList />} />
                    <Route path="/reports/:id" element={<ReportDetail />} />
                    <Route path="/map" element={<MapView />} />
                    <Route path="/settings" element={<Settings />} />
                </Routes>
            </main>
        </div>
    );
}

export default function App() {
    return (
        <AuthProvider>
            <BrowserRouter>
                <Routes>
                    <Route path="/login" element={<LoginPage />} />
                    <Route
                        path="/*"
                        element={
                            <ProtectedRoute>
                                <AppLayout />
                            </ProtectedRoute>
                        }
                    />
                </Routes>
            </BrowserRouter>
        </AuthProvider>
    );
}
