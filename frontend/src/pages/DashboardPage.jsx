import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import api from '../services/api';
import { 
  Wallet, 
  ArrowDownLeft, 
  ArrowUpRight, 
  Send, 
  Copy, 
  Check, 
  ShieldCheck, 
  RefreshCw, 
  AlertCircle,
  FileText,
  Clock,
  ArrowRight
} from 'lucide-react';

const DashboardPage = () => {
  const { user } = useAuth();
  const [accounts, setAccounts] = useState([]);
  const [recentTransactions, setRecentTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [copiedAcc, setCopiedAcc] = useState('');
  const [error, setError] = useState('');

  const fetchData = async () => {
    try {
      setRefreshing(true);
      const [accRes, txnRes] = await Promise.all([
        api.get('/accounts'),
        api.get('/transactions?size=5&sort=createdAt,desc')
      ]);

      if (accRes.data && accRes.data.data) {
        setAccounts(accRes.data.data);
      }
      if (txnRes.data && txnRes.data.data && txnRes.data.data.content) {
        setRecentTransactions(txnRes.data.data.content);
      }
    } catch (err) {
      setError('Failed to fetch account overview. Please retry.');
      console.error(err);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const copyToClipboard = (text) => {
    navigator.clipboard.writeText(text);
    setCopiedAcc(text);
    setTimeout(() => setCopiedAcc(''), 2000);
  };

  const totalBalance = accounts.reduce((acc, curr) => acc + (parseFloat(curr.balance) || 0), 0);

  const formatCurrency = (val, curr = 'INR') => {
    const num = parseFloat(val) || 0;
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: curr,
      minimumFractionDigits: 2,
    }).format(num);
  };

  if (loading) {
    return (
      <div className="container" style={{ padding: '3rem 0', textAlign: 'center' }}>
        <div style={{ color: '#64748b' }}>Loading financial accounts...</div>
      </div>
    );
  }

  return (
    <div className="container" style={{ padding: '2.5rem 1.5rem' }}>
      {/* Top Welcome Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1>Welcome, {user?.fullName || user?.username}</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
            Account Holder Dashboard &bull; Real-time Double-Entry Financial Ledger
          </p>
        </div>
        <button 
          onClick={fetchData} 
          className="btn btn-outline btn-sm"
          disabled={refreshing}
        >
          <RefreshCw size={15} style={{ animation: refreshing ? 'spin 1s linear infinite' : 'none' }} />
          {refreshing ? 'Updating...' : 'Refresh Balances'}
        </button>
      </div>

      {error && (
        <div style={{
          display: 'flex',
          alignItems: 'center',
          gap: '0.5rem',
          backgroundColor: '#fef2f2',
          border: '1px solid #fecaca',
          color: '#b91c1c',
          padding: '0.75rem 1rem',
          borderRadius: '8px',
          marginBottom: '1.5rem'
        }}>
          <AlertCircle size={18} />
          <span>{error}</span>
        </div>
      )}

      {/* Main Stats Grid */}
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(280px, 1fr))', gap: '1.5rem', marginBottom: '2rem' }}>
        {/* Total Available Balance Card */}
        <div className="card" style={{
          background: 'linear-gradient(135deg, #0f172a 0%, #1e293b 100%)',
          color: '#ffffff',
          position: 'relative',
          overflow: 'hidden'
        }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <span style={{ fontSize: '0.8rem', fontWeight: 600, color: '#94a3b8', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
              Total Portfolio Balance
            </span>
            <span className="badge badge-success" style={{ fontSize: '0.65rem' }}>
              <ShieldCheck size={12} /> Double-Entry Verified
            </span>
          </div>

          <div style={{ fontSize: '2.25rem', fontWeight: 800, marginBottom: '1rem' }} className="mono">
            {formatCurrency(totalBalance)}
          </div>

          <div style={{ display: 'flex', gap: '0.75rem', marginTop: '1rem' }}>
            <Link to="/deposit" className="btn btn-success btn-sm" style={{ flex: 1 }}>
              <ArrowDownLeft size={16} /> Deposit
            </Link>
            <Link to="/withdraw" className="btn btn-outline btn-sm" style={{ flex: 1, color: '#ffffff', borderColor: '#475569' }}>
              <ArrowUpRight size={16} /> Withdraw
            </Link>
            <Link to="/transfer" className="btn btn-blue btn-sm" style={{ flex: 1 }}>
              <Send size={16} /> Transfer
            </Link>
          </div>
        </div>

        {/* Primary Account Info */}
        {accounts.map((acc) => (
          <div key={acc.id} className="card">
            <div className="card-header" style={{ marginBottom: '0.75rem', paddingBottom: '0.5rem' }}>
              <div>
                <span className="badge badge-neutral" style={{ marginRight: '0.5rem' }}>{acc.accountType}</span>
                <span className={`badge ${acc.status === 'ACTIVE' ? 'badge-success' : acc.status === 'FROZEN' ? 'badge-warning' : 'badge-danger'}`}>
                  {acc.status}
                </span>
              </div>
              <div style={{ fontSize: '0.75rem', color: '#64748b' }}>Account ID: #{acc.id}</div>
            </div>

            <div style={{ marginBottom: '0.75rem' }}>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Account Number</div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <span className="mono" style={{ fontSize: '1.05rem', fontWeight: 600, color: 'var(--primary)' }}>
                  {acc.accountNumber}
                </span>
                <button
                  onClick={() => copyToClipboard(acc.accountNumber)}
                  style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#64748b' }}
                  title="Copy Account Number"
                >
                  {copiedAcc === acc.accountNumber ? <Check size={16} color="#10b981" /> : <Copy size={16} />}
                </button>
              </div>
            </div>

            <div>
              <div style={{ fontSize: '0.75rem', color: 'var(--text-muted)' }}>Available Balance</div>
              <div className="mono" style={{ fontSize: '1.5rem', fontWeight: 700, color: '#0f172a' }}>
                {formatCurrency(acc.balance, acc.currency)}
              </div>
            </div>
          </div>
        ))}
      </div>

      {/* Recent Activity Table */}
      <div className="card">
        <div className="card-header">
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Clock size={18} color="#2563eb" />
            <h3 style={{ margin: 0 }}>Recent Activity</h3>
          </div>
          <Link to="/transactions" className="btn btn-outline btn-sm">
            View All Statements <ArrowRight size={14} />
          </Link>
        </div>

        {recentTransactions.length === 0 ? (
          <div style={{ padding: '2.5rem', textAlign: 'center', color: '#64748b' }}>
            <FileText size={36} style={{ margin: '0 auto 0.75rem', opacity: 0.5 }} />
            <div>No transactions recorded yet. Make a deposit or transfer to get started!</div>
          </div>
        ) : (
          <div className="table-container">
            <table className="table">
              <thead>
                <tr>
                  <th>Reference</th>
                  <th>Type</th>
                  <th>Source / Dest</th>
                  <th>Amount</th>
                  <th>Status</th>
                  <th>Timestamp</th>
                </tr>
              </thead>
              <tbody>
                {recentTransactions.map((t) => (
                  <tr key={t.id}>
                    <td className="mono" style={{ fontWeight: 600, fontSize: '0.8rem', color: '#2563eb' }}>
                      {t.transactionReference}
                    </td>
                    <td>
                      <span className={`badge ${t.transactionType === 'DEPOSIT' ? 'badge-success' : t.transactionType === 'WITHDRAWAL' ? 'badge-danger' : 'badge-info'}`}>
                        {t.transactionType}
                      </span>
                    </td>
                    <td style={{ fontSize: '0.8rem' }} className="mono">
                      {t.sourceAccountNumber ? t.sourceAccountNumber : 'EXTERNAL_CLEARING'} &rarr; {t.destinationAccountNumber ? t.destinationAccountNumber : 'EXTERNAL_CLEARING'}
                    </td>
                    <td className="mono" style={{ fontWeight: 700, color: t.transactionType === 'DEPOSIT' ? '#059669' : '#0f172a' }}>
                      {t.transactionType === 'DEPOSIT' ? '+' : t.transactionType === 'WITHDRAWAL' ? '-' : ''}
                      {formatCurrency(t.amount, t.currency)}
                    </td>
                    <td>
                      <span className={`badge ${t.status === 'COMPLETED' ? 'badge-success' : 'badge-danger'}`}>
                        {t.status}
                      </span>
                    </td>
                    <td style={{ fontSize: '0.8rem', color: '#64748b' }}>
                      {t.createdAt ? new Date(t.createdAt).toLocaleString('en-IN') : '-'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};

export default DashboardPage;

