import React from 'react';
import { Routes, Route, Navigate } from 'react-router-dom';
import Navbar from './components/Navbar';
import ProtectedRoute from './components/ProtectedRoute';

// Pages
import LandingPage from './pages/LandingPage';
import LoginPage from './pages/LoginPage';
import RegisterPage from './pages/RegisterPage';
import DashboardPage from './pages/DashboardPage';
import DepositPage from './pages/DepositPage';
import WithdrawPage from './pages/WithdrawPage';
import TransferPage from './pages/TransferPage';
import TransactionsPage from './pages/TransactionsPage';
import AdminDashboardPage from './pages/AdminDashboardPage';
import AuditorDashboardPage from './pages/AuditorDashboardPage';

function App() {
  return (
    <div className="app-container" style={{ minHeight: '100vh', display: 'flex', flexDirection: 'column' }}>
      <Navbar />
      <main style={{ flex: 1 }}>
        <Routes>
          {/* Public Routes */}
          <Route path="/" element={<LandingPage />} />
          <Route path="/login" element={<LoginPage />} />
          <Route path="/register" element={<RegisterPage />} />

          {/* Account Holder Routes */}
          <Route 
            path="/dashboard" 
            element={
              <ProtectedRoute allowedRoles={['ROLE_ACCOUNT_HOLDER']}>
                <DashboardPage />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/deposit" 
            element={
              <ProtectedRoute allowedRoles={['ROLE_ACCOUNT_HOLDER']}>
                <DepositPage />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/withdraw" 
            element={
              <ProtectedRoute allowedRoles={['ROLE_ACCOUNT_HOLDER']}>
                <WithdrawPage />
              </ProtectedRoute>
            } 
          />
          <Route 
            path="/transfer" 
            element={
              <ProtectedRoute allowedRoles={['ROLE_ACCOUNT_HOLDER']}>
                <TransferPage />
              </ProtectedRoute>
            } 
          />

          {/* Shared / Role-Adaptive Transaction History & Statements */}
          <Route 
            path="/transactions" 
            element={
              <ProtectedRoute allowedRoles={['ROLE_ACCOUNT_HOLDER', 'ROLE_ADMIN', 'ROLE_AUDITOR']}>
                <TransactionsPage />
              </ProtectedRoute>
            } 
          />

          {/* Administrator Command Center */}
          <Route 
            path="/admin" 
            element={
              <ProtectedRoute allowedRoles={['ROLE_ADMIN']}>
                <AdminDashboardPage />
              </ProtectedRoute>
            } 
          />

          {/* Auditor Portal */}
          <Route 
            path="/auditor" 
            element={
              <ProtectedRoute allowedRoles={['ROLE_AUDITOR', 'ROLE_ADMIN']}>
                <AuditorDashboardPage />
              </ProtectedRoute>
            } 
          />

          {/* Fallback */}
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>

      <footer style={{
        backgroundColor: '#ffffff',
        borderTop: '1px solid var(--border)',
        padding: '1.5rem 0',
        color: 'var(--text-muted)',
        fontSize: '0.8rem',
        textAlign: 'center',
        marginTop: 'auto'
      }}>
        <div className="container" style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1rem' }}>
          <div>
            <strong>LedgerLine Enterprise</strong> — Money Movement With a Ledger You Can Prove
          </div>
          <div>
            Zero-Leak Invariant: <code>&Sigma; Debits &minus; &Sigma; Credits = 0</code> &bull; Immutable Double-Entry System
          </div>
        </div>
      </footer>
    </div>
  );
}

export default App;

