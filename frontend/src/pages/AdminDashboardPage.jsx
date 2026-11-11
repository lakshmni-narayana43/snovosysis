import React, { useState, useEffect } from 'react';
import api from '../services/api';
import { 
  Users, 
  Wallet, 
  ArrowLeftRight, 
  ShieldAlert, 
  CheckCircle, 
  AlertTriangle, 
  Lock, 
  Unlock, 
  FileText,
  Clock,
  RefreshCw,
  Search,
  Filter
} from 'lucide-react';

const AdminDashboardPage = () => {
  const [activeTab, setActiveTab] = useState('accounts');
  const [stats, setStats] = useState({ totalUsers: 0, totalAccounts: 0, totalTransactions: 0 });
  const [accounts, setAccounts] = useState([]);
  const [usersList, setUsersList] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState(false);
  const [notification, setNotification] = useState('');

  const loadAdminData = async () => {
    try {
      setLoading(true);
      const [statsRes, accRes, usrRes, auditRes] = await Promise.all([
        api.get('/admin/stats'),
        api.get('/admin/accounts'),
        api.get('/admin/users'),
        api.get('/audit-logs?size=50')
      ]);

      if (statsRes.data?.data) setStats(statsRes.data.data);
      if (accRes.data?.data) setAccounts(accRes.data.data);
      if (usrRes.data?.data) setUsersList(usrRes.data.data);
      if (auditRes.data?.data?.content) setAuditLogs(auditRes.data.data.content);
    } catch (err) {
      console.error('Failed to load admin telemetry', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAdminData();
  }, []);

  const handleToggleFreeze = async (account) => {
    const isCurrentlyActive = account.status === 'ACTIVE';
    const newStatus = isCurrentlyActive ? 'FROZEN' : 'ACTIVE';
    const confirmPrompt = window.confirm(
      `Are you sure you want to ${isCurrentlyActive ? 'FREEZE' : 'UNFREEZE'} account ${account.accountNumber}?`
    );

    if (!confirmPrompt) return;

    try {
      setActionLoading(true);
      await api.patch(`/admin/accounts/${account.id}/status`, {
        status: newStatus,
        reason: `Admin requested status change to ${newStatus}`,
      });

      setNotification(`Account ${account.accountNumber} successfully set to ${newStatus}.`);
      setTimeout(() => setNotification(''), 4000);
      loadAdminData();
    } catch (err) {
      alert(err.response?.data?.message || 'Failed to update account status.');
    } finally {
      setActionLoading(false);
    }
  };

  const formatCurrency = (val) => {
    const num = parseFloat(val) || 0;
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 2,
    }).format(num);
  };

  return (
    <div className="container" style={{ padding: '2.5rem 1.5rem' }}>
      {/* Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1>Administrator Command Center</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
            System activity monitoring, account freeze controls, and tamper-resistant audit logs
          </p>
        </div>
        <button onClick={loadAdminData} className="btn btn-outline btn-sm">
          <RefreshCw size={14} /> Refresh Telemetry
        </button>
      </div>

      {notification && (
        <div style={{
          backgroundColor: '#ecfdf5',
          border: '1px solid #a7f3d0',
          color: '#065f46',
          padding: '0.75rem 1rem',
          borderRadius: '8px',
          marginBottom: '1.5rem',
          fontSize: '0.875rem',
          display: 'flex',
          alignItems: 'center',
          gap: '0.5rem'
        }}>
          <CheckCircle size={18} />
          <span>{notification}</span>
        </div>
      )}

      {/* Metrics Row */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(220px, 1fr))', gap: '1.25rem', marginBottom: '2rem' }}>
        <div className="card" style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{ backgroundColor: '#eff6ff', color: '#2563eb', padding: '0.75rem', borderRadius: '10px' }}>
            <Users size={24} />
          </div>
          <div>
            <div style={{ fontSize: '0.8rem', color: '#64748b', fontWeight: 600 }}>REGISTERED USERS</div>
            <div style={{ fontSize: '1.75rem', fontWeight: 800 }}>{stats.totalUsers}</div>
          </div>
        </div>

        <div className="card" style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{ backgroundColor: '#ecfdf5', color: '#059669', padding: '0.75rem', borderRadius: '10px' }}>
            <Wallet size={24} />
          </div>
          <div>
            <div style={{ fontSize: '0.8rem', color: '#64748b', fontWeight: 600 }}>FINANCIAL ACCOUNTS</div>
            <div style={{ fontSize: '1.75rem', fontWeight: 800 }}>{stats.totalAccounts}</div>
          </div>
        </div>

        <div className="card" style={{ display: 'flex', alignItems: 'center', gap: '1rem' }}>
          <div style={{ backgroundColor: '#fef3c7', color: '#d97706', padding: '0.75rem', borderRadius: '10px' }}>
            <ArrowLeftRight size={24} />
          </div>
          <div>
            <div style={{ fontSize: '0.8rem', color: '#64748b', fontWeight: 600 }}>SYSTEM TRANSACTIONS</div>
            <div style={{ fontSize: '1.75rem', fontWeight: 800 }}>{stats.totalTransactions}</div>
          </div>
        </div>
      </div>

      {/* Navigation Tabs */}
      <div style={{ display: 'flex', gap: '0.5rem', borderBottom: '1px solid var(--border)', marginBottom: '1.5rem' }}>
        <button
          onClick={() => setActiveTab('accounts')}
          style={{
            padding: '0.6rem 1.2rem',
            background: 'none',
            border: 'none',
            borderBottom: activeTab === 'accounts' ? '2px solid #2563eb' : '2px solid transparent',
            color: activeTab === 'accounts' ? '#2563eb' : '#64748b',
            fontWeight: 600,
            cursor: 'pointer',
            fontSize: '0.875rem'
          }}
        >
          Account Controls ({accounts.length})
        </button>
        <button
          onClick={() => setActiveTab('users')}
          style={{
            padding: '0.6rem 1.2rem',
            background: 'none',
            border: 'none',
            borderBottom: activeTab === 'users' ? '2px solid #2563eb' : '2px solid transparent',
            color: activeTab === 'users' ? '#2563eb' : '#64748b',
            fontWeight: 600,
            cursor: 'pointer',
            fontSize: '0.875rem'
          }}
        >
          User Directory ({usersList.length})
        </button>
        <button
          onClick={() => setActiveTab('audit')}
          style={{
            padding: '0.6rem 1.2rem',
            background: 'none',
            border: 'none',
            borderBottom: activeTab === 'audit' ? '2px solid #2563eb' : '2px solid transparent',
            color: activeTab === 'audit' ? '#2563eb' : '#64748b',
            fontWeight: 600,
            cursor: 'pointer',
            fontSize: '0.875rem'
          }}
        >
          Immutable Audit Logs ({auditLogs.length})
        </button>
      </div>

      {/* TAB 1: ACCOUNTS & FREEZE/UNFREEZE */}
      {activeTab === 'accounts' && (
        <div className="card" style={{ padding: 0 }}>
          <div className="table-container" style={{ border: 'none' }}>
            <table className="table">
              <thead>
                <tr>
                  <th>Account Number</th>
                  <th>Owner</th>
                  <th>Type</th>
                  <th>Balance</th>
                  <th>Status</th>
                  <th>Security Action</th>
                </tr>
              </thead>
              <tbody>
                {accounts.map((acc) => (
                  <tr key={acc.id}>
                    <td className="mono" style={{ fontWeight: 600, color: '#2563eb' }}>
                      {acc.accountNumber}
                    </td>
                    <td>{acc.username || 'System Counterparty'}</td>
                    <td><span className="badge badge-neutral">{acc.accountType}</span></td>
                    <td className="mono" style={{ fontWeight: 700 }}>{formatCurrency(acc.balance)}</td>
                    <td>
                      <span className={`badge ${
                        acc.status === 'ACTIVE' ? 'badge-success' :
                        acc.status === 'FROZEN' ? 'badge-warning' : 'badge-danger'
                      }`}>
                        {acc.status}
                      </span>
                    </td>
                    <td>
                      {acc.accountType !== 'SYSTEM_CLEARING' ? (
                        <button
                          onClick={() => handleToggleFreeze(acc)}
                          disabled={actionLoading}
                          className={`btn btn-sm ${acc.status === 'ACTIVE' ? 'btn-danger' : 'btn-success'}`}
                          style={{ padding: '0.3rem 0.6rem', fontSize: '0.75rem' }}
                        >
                          {acc.status === 'ACTIVE' ? (
                            <><Lock size={12} /> Freeze Account</>
                          ) : (
                            <><Unlock size={12} /> Unfreeze Account</>
                          )}
                        </button>
                      ) : (
                        <span style={{ fontSize: '0.75rem', color: '#94a3b8' }}>System Protected</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* TAB 2: USERS DIRECTORY */}
      {activeTab === 'users' && (
        <div className="card" style={{ padding: 0 }}>
          <div className="table-container" style={{ border: 'none' }}>
            <table className="table">
              <thead>
                <tr>
                  <th>User ID</th>
                  <th>Full Name</th>
                  <th>Username</th>
                  <th>Email</th>
                  <th>Assigned Role</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {usersList.map((u) => (
                  <tr key={u.id}>
                    <td className="mono">#{u.id}</td>
                    <td style={{ fontWeight: 600 }}>{u.fullName}</td>
                    <td className="mono">{u.username}</td>
                    <td>{u.email}</td>
                    <td>
                      <span className={`badge ${
                        u.role === 'ROLE_ADMIN' ? 'badge-danger' :
                        u.role === 'ROLE_AUDITOR' ? 'badge-info' : 'badge-success'
                      }`}>
                        {u.role}
                      </span>
                    </td>
                    <td>
                      <span className="badge badge-success">{u.status}</span>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* TAB 3: AUDIT LOGS */}
      {activeTab === 'audit' && (
        <div className="card" style={{ padding: 0 }}>
          <div className="table-container" style={{ border: 'none' }}>
            <table className="table">
              <thead>
                <tr>
                  <th>Log ID</th>
                  <th>Timestamp</th>
                  <th>Actor</th>
                  <th>Action</th>
                  <th>Entity Type / ID</th>
                  <th>Status</th>
                  <th>Details / Reason</th>
                </tr>
              </thead>
              <tbody>
                {auditLogs.map((log) => (
                  <tr key={log.id}>
                    <td className="mono" style={{ fontSize: '0.75rem' }}>#{log.id}</td>
                    <td style={{ fontSize: '0.8rem', color: '#64748b' }}>
                      {log.timestamp ? new Date(log.timestamp).toLocaleString('en-IN') : '-'}
                    </td>
                    <td style={{ fontWeight: 600 }}>{log.actorUsername || 'System'}</td>
                    <td>
                      <span className={`badge ${
                        log.status === 'SUCCESS' ? 'badge-info' : 'badge-danger'
                      }`} style={{ fontSize: '0.7rem' }}>
                        {log.action}
                      </span>
                    </td>
                    <td className="mono" style={{ fontSize: '0.8rem' }}>
                      {log.entityType}: {log.entityId || 'N/A'}
                    </td>
                    <td>
                      <span className={`badge ${log.status === 'SUCCESS' ? 'badge-success' : 'badge-danger'}`}>
                        {log.status}
                      </span>
                    </td>
                    <td style={{ fontSize: '0.8rem', color: '#475569', maxWidth: '300px' }}>
                      {log.metadata || log.errorMessage || '-'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminDashboardPage;

