import React from 'react';
import { Link, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { 
  ShieldCheck, 
  Wallet, 
  ArrowUpRight, 
  ArrowDownLeft, 
  Send, 
  Clock, 
  User, 
  LogOut, 
  Sliders, 
  FileCheck2,
  Lock
} from 'lucide-react';

const Navbar = () => {
  const { user, isAuthenticated, role, logout } = useAuth();
  const location = useLocation();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isActive = (path) => location.pathname === path;

  return (
    <nav style={{
      backgroundColor: '#ffffff',
      borderBottom: '1px solid var(--border)',
      position: 'sticky',
      top: 0,
      zIndex: 40,
    }}>
      <div className="container" style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        height: '4.25rem'
      }}>
        {/* Brand Logo */}
        <Link to="/" style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', textDecoration: 'none' }}>
          <div style={{
            background: 'linear-gradient(135deg, #0f172a 0%, #1e3a8a 100%)',
            color: '#ffffff',
            padding: '0.5rem',
            borderRadius: '10px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            boxShadow: 'var(--shadow-sm)'
          }}>
            <ShieldCheck size={22} color="#10b981" />
          </div>
          <div>
            <div style={{ fontWeight: 800, fontSize: '1.25rem', color: '#0f172a', letterSpacing: '-0.02em', display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
              LedgerLine
              <span style={{ fontSize: '0.65rem', backgroundColor: '#e2e8f0', color: '#334155', padding: '0.15rem 0.4rem', borderRadius: '4px', fontWeight: 600 }}>PRO</span>
            </div>
            <div style={{ fontSize: '0.7rem', color: '#64748b', fontWeight: 500 }}>Verifiable Double-Entry Ledger</div>
          </div>
        </Link>

        {/* Dynamic Navigation Links based on Authentication & Role */}
        {isAuthenticated && (
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            {role === 'ROLE_ACCOUNT_HOLDER' && (
              <>
                <Link to="/dashboard" className={`btn btn-sm ${isActive('/dashboard') ? 'btn-primary' : 'btn-outline'}`}>
                  <Wallet size={16} /> Overview
                </Link>
                <Link to="/deposit" className={`btn btn-sm ${isActive('/deposit') ? 'btn-primary' : 'btn-outline'}`}>
                  <ArrowDownLeft size={16} /> Deposit
                </Link>
                <Link to="/withdraw" className={`btn btn-sm ${isActive('/withdraw') ? 'btn-primary' : 'btn-outline'}`}>
                  <ArrowUpRight size={16} /> Withdraw
                </Link>
                <Link to="/transfer" className={`btn btn-sm ${isActive('/transfer') ? 'btn-primary' : 'btn-outline'}`}>
                  <Send size={16} /> Transfer
                </Link>
                <Link to="/transactions" className={`btn btn-sm ${isActive('/transactions') ? 'btn-primary' : 'btn-outline'}`}>
                  <Clock size={16} /> Statements
                </Link>
              </>
            )}

            {role === 'ROLE_ADMIN' && (
              <>
                <Link to="/admin" className={`btn btn-sm ${isActive('/admin') ? 'btn-primary' : 'btn-outline'}`}>
                  <Sliders size={16} /> Admin Command Center
                </Link>
                <Link to="/transactions" className={`btn btn-sm ${isActive('/transactions') ? 'btn-primary' : 'btn-outline'}`}>
                  <Clock size={16} /> All Transactions
                </Link>
              </>
            )}

            {role === 'ROLE_AUDITOR' && (
              <>
                <Link to="/auditor" className={`btn btn-sm ${isActive('/auditor') ? 'btn-primary' : 'btn-outline'}`}>
                  <FileCheck2 size={16} /> Auditor Portal
                </Link>
                <Link to="/transactions" className={`btn btn-sm ${isActive('/transactions') ? 'btn-primary' : 'btn-outline'}`}>
                  <Clock size={16} /> Ledger Explorer
                </Link>
              </>
            )}
          </div>
        )}

        {/* User Actions / Auth State */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem' }}>
          {isAuthenticated ? (
            <div style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
              <div style={{ textAlign: 'right' }}>
                <div style={{ fontSize: '0.875rem', fontWeight: 600, color: 'var(--primary)' }}>
                  {user?.fullName || user?.username}
                </div>
                <div style={{ fontSize: '0.7rem', color: 'var(--text-muted)', display: 'flex', alignItems: 'center', gap: '0.25rem', justifyContent: 'flex-end' }}>
                  <span className={`badge ${role === 'ROLE_ADMIN' ? 'badge-danger' : role === 'ROLE_AUDITOR' ? 'badge-info' : 'badge-success'}`} style={{ fontSize: '0.65rem', padding: '0.1rem 0.4rem' }}>
                    {role === 'ROLE_ADMIN' ? 'Administrator' : role === 'ROLE_AUDITOR' ? 'Auditor' : 'Account Holder'}
                  </span>
                </div>
              </div>
              <button onClick={handleLogout} className="btn btn-outline btn-sm" title="Sign Out">
                <LogOut size={16} /> Sign Out
              </button>
            </div>
          ) : (
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Link to="/login" className="btn btn-outline btn-sm">
                Sign In
              </Link>
              <Link to="/register" className="btn btn-primary btn-sm">
                Get Started
              </Link>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
};

export default Navbar;

