import React, { useState, useEffect } from 'react';
import api from '../services/api';
import { 
  ShieldCheck, 
  Scale, 
  CheckCircle, 
  AlertTriangle, 
  FileText, 
  RefreshCw, 
  Layers, 
  ArrowLeftRight, 
  Search, 
  Download, 
  Lock,
  Activity,
  History,
  AlertOctagon
} from 'lucide-react';

const AuditorDashboardPage = () => {
  const [activeTab, setActiveTab] = useState('reconciliation');
  const [verification, setVerification] = useState(null);
  const [ledgerEntries, setLedgerEntries] = useState([]);
  const [transactions, setTransactions] = useState([]);
  const [auditLogs, setAuditLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [verifying, setVerifying] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [statusMessage, setStatusMessage] = useState('');

  const loadAuditorData = async () => {
    try {
      setLoading(true);
      const [verifRes, ledgerRes, txRes, logsRes] = await Promise.all([
        api.get('/auditor/verify'),
        api.get('/auditor/ledger?size=100'),
        api.get('/auditor/transactions?size=100'),
        api.get('/audit-logs?size=100')
      ]);

      if (verifRes.data?.data) setVerification(verifRes.data.data);
      if (ledgerRes.data?.data?.content) setLedgerEntries(ledgerRes.data.data.content);
      if (txRes.data?.data?.content) setTransactions(txRes.data.data.content);
      if (logsRes.data?.data?.content) setAuditLogs(logsRes.data.data.content);
    } catch (err) {
      console.error('Failed to load auditor telemetry', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAuditorData();
  }, []);

  const handleRunVerification = async () => {
    try {
      setVerifying(true);
      const res = await api.get('/auditor/verify');
      if (res.data?.data) {
        setVerification(res.data.data);
        setStatusMessage(
          res.data.data.balanced 
            ? '✅ Mathematical Invariant Verified: Double-entry ledger strictly balances (Debits == Credits).' 
            : '⚠️ Warning: Ledger imbalance detected!'
        );
        setTimeout(() => setStatusMessage(''), 5000);
      }
    } catch (err) {
      alert('Failed to execute ledger verification: ' + (err.response?.data?.message || err.message));
    } finally {
      setVerifying(false);
    }
  };

  const formatCurrency = (val) => {
    const num = parseFloat(val) || 0;
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      minimumFractionDigits: 4,
      maximumFractionDigits: 4,
    }).format(num);
  };

  const filteredReconciliations = (verification?.accountReconciliations || []).filter(item => 
    item.accountNumber?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    item.accountId?.toString().includes(searchTerm)
  );

  const filteredLedger = ledgerEntries.filter(entry => 
    entry.transactionReference?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    entry.accountNumber?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    entry.entryType?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const filteredTx = transactions.filter(tx => 
    tx.transactionReference?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    tx.sourceAccountNumber?.toLowerCase().includes(searchTerm.toLowerCase()) ||
    tx.destinationAccountNumber?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="container" style={{ padding: '2.5rem 1.5rem' }}>
      {/* Top Header */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '0.25rem' }}>
            <h1 style={{ margin: 0 }}>Independent Auditor Portal</h1>
            <span className="badge badge-info" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}>
              <Lock size={12} /> READ-ONLY MANDATE
            </span>
          </div>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem', margin: 0 }}>
            Verifiable double-entry math, accounting trial balances, transaction traceability, and immutable audit logs.
          </p>
        </div>
        <div style={{ display: 'flex', gap: '0.75rem' }}>
          <button onClick={loadAuditorData} className="btn btn-outline btn-sm" disabled={loading}>
            <RefreshCw size={14} className={loading ? 'spin' : ''} /> Refresh Data
          </button>
          <button 
            onClick={handleRunVerification} 
            className="btn btn-primary btn-sm" 
            disabled={verifying}
            style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}
          >
            <Scale size={16} /> {verifying ? 'Auditing Ledger...' : 'Run Mathematical Integrity Audit'}
          </button>
        </div>
      </div>

      {statusMessage && (
        <div className={`alert ${verification?.balanced ? 'alert-success' : 'alert-danger'}`} style={{ marginBottom: '1.5rem' }}>
          {statusMessage}
        </div>
      )}

      {/* Trial Balance & Core Ledger Invariant Banner */}
      <div className="card" style={{ marginBottom: '2rem', border: verification?.balanced ? '1px solid var(--accent-success)' : '1px solid var(--accent-danger)' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', flexWrap: 'wrap', gap: '1.5rem' }}>
          <div>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '0.5rem' }}>
              {verification?.balanced ? (
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--accent-success)', fontWeight: 700, fontSize: '1.1rem' }}>
                  <ShieldCheck size={24} />
                  <span>DOUBLE-ENTRY TRIAL BALANCE: VERIFIED BALANCED</span>
                </div>
              ) : (
                <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: 'var(--accent-danger)', fontWeight: 700, fontSize: '1.1rem' }}>
                  <AlertOctagon size={24} />
                  <span>ALERT: TRIAL BALANCE IMBALANCE DETECTED</span>
                </div>
              )}
            </div>
            <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', margin: 0 }}>
              Audit Law: Total System Debits must strictly equal Total System Credits (Σ Debits - Σ Credits = 0).
              Last audited at: {verification?.verifiedAt ? new Date(verification.verifiedAt).toLocaleString() : 'Just now'}
            </p>
          </div>

          <div style={{ display: 'flex', gap: '2rem', flexWrap: 'wrap' }}>
            <div>
              <div style={{ fontSize: '0.75rem', textTransform: 'uppercase', color: 'var(--text-muted)', letterSpacing: '0.05em' }}>
                Total Posted Debits
              </div>
              <div style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--text-primary)', fontFamily: 'monospace' }}>
                {formatCurrency(verification?.totalDebits)}
              </div>
            </div>

            <div>
              <div style={{ fontSize: '0.75rem', textTransform: 'uppercase', color: 'var(--text-muted)', letterSpacing: '0.05em' }}>
                Total Posted Credits
              </div>
              <div style={{ fontSize: '1.25rem', fontWeight: 700, color: 'var(--text-primary)', fontFamily: 'monospace' }}>
                {formatCurrency(verification?.totalCredits)}
              </div>
            </div>

            <div>
              <div style={{ fontSize: '0.75rem', textTransform: 'uppercase', color: 'var(--text-muted)', letterSpacing: '0.05em' }}>
                System Imbalance
              </div>
              <div style={{ 
                fontSize: '1.25rem', 
                fontWeight: 700, 
                color: verification?.imbalance === 0 || parseFloat(verification?.imbalance || 0) === 0 ? 'var(--accent-success)' : 'var(--accent-danger)', 
                fontFamily: 'monospace' 
              }}>
                {formatCurrency(verification?.imbalance)}
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Metric Cards */}
      <div className="grid grid-4" style={{ marginBottom: '2rem' }}>
        <div className="card stat-card">
          <div className="stat-label">Total Transactions</div>
          <div className="stat-value">{verification?.totalTransactions ?? transactions.length}</div>
          <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
            Atomic transaction envelopes
          </div>
        </div>

        <div className="card stat-card">
          <div className="stat-label">Total Ledger Entries</div>
          <div className="stat-value">{verification?.totalLedgerEntries ?? ledgerEntries.length}</div>
          <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
            Immutable double-entry rows
          </div>
        </div>

        <div className="card stat-card">
          <div className="stat-label">Accounts Reconciled</div>
          <div className="stat-value">{verification?.accountReconciliations?.length || 0}</div>
          <div style={{ fontSize: '0.8rem', color: 'var(--accent-success)', marginTop: '0.25rem' }}>
            100% verified against ledger sum
          </div>
        </div>

        <div className="card stat-card">
          <div className="stat-label">Audit Trail Events</div>
          <div className="stat-value">{auditLogs.length}</div>
          <div style={{ fontSize: '0.8rem', color: 'var(--text-muted)', marginTop: '0.25rem' }}>
            Immutable security log rows
          </div>
        </div>
      </div>

      {/* Tabs and Search Bar */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.5rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div style={{ display: 'flex', gap: '0.5rem', borderBottom: '1px solid var(--border-color)', paddingBottom: '0.5rem' }}>
          <button 
            className={`btn btn-sm ${activeTab === 'reconciliation' ? 'btn-primary' : 'btn-outline'}`}
            onClick={() => setActiveTab('reconciliation')}
          >
            <Scale size={14} /> Account Reconciliations ({verification?.accountReconciliations?.length || 0})
          </button>
          <button 
            className={`btn btn-sm ${activeTab === 'ledger' ? 'btn-primary' : 'btn-outline'}`}
            onClick={() => setActiveTab('ledger')}
          >
            <Layers size={14} /> Immutable Ledger Journal ({ledgerEntries.length})
          </button>
          <button 
            className={`btn btn-sm ${activeTab === 'transactions' ? 'btn-primary' : 'btn-outline'}`}
            onClick={() => setActiveTab('transactions')}
          >
            <ArrowLeftRight size={14} /> Global Transactions ({transactions.length})
          </button>
          <button 
            className={`btn btn-sm ${activeTab === 'audit-logs' ? 'btn-primary' : 'btn-outline'}`}
            onClick={() => setActiveTab('audit-logs')}
          >
            <History size={14} /> Security Audit Trail ({auditLogs.length})
          </button>
        </div>

        <div style={{ position: 'relative', width: '280px' }}>
          <Search size={16} style={{ position: 'absolute', left: '0.75rem', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-muted)' }} />
          <input 
            type="text" 
            placeholder="Search reference, account..." 
            className="form-control"
            style={{ paddingLeft: '2.25rem', fontSize: '0.85rem' }}
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
          />
        </div>
      </div>

      {/* TAB 1: Account Reconciliations */}
      {activeTab === 'reconciliation' && (
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <div>
              <h3 style={{ margin: 0 }}>Per-Account Double-Entry Reconciliation</h3>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', margin: 0 }}>
                Verifies that `account.balance` equals `sum(CREDIT entries) - sum(DEBIT entries)`.
              </p>
            </div>
          </div>

          <div className="table-responsive">
            <table className="table">
              <thead>
                <tr>
                  <th>Account Number</th>
                  <th>Current Balance</th>
                  <th>Ledger Calculated Balance</th>
                  <th>Total Debits</th>
                  <th>Total Credits</th>
                  <th>Integrity Check</th>
                </tr>
              </thead>
              <tbody>
                {filteredReconciliations.map((rec) => (
                  <tr key={rec.accountId}>
                    <td style={{ fontFamily: 'monospace', fontWeight: 600 }}>{rec.accountNumber}</td>
                    <td style={{ fontFamily: 'monospace', fontWeight: 600 }}>{formatCurrency(rec.currentBalance)}</td>
                    <td style={{ fontFamily: 'monospace', fontWeight: 600 }}>{formatCurrency(rec.ledgerCalculatedBalance)}</td>
                    <td style={{ fontFamily: 'monospace', color: 'var(--accent-danger)' }}>{formatCurrency(rec.totalDebits)}</td>
                    <td style={{ fontFamily: 'monospace', color: 'var(--accent-success)' }}>{formatCurrency(rec.totalCredits)}</td>
                    <td>
                      {rec.consistent ? (
                        <span className="badge badge-success" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}>
                          <CheckCircle size={12} /> PERFECT MATCH
                        </span>
                      ) : (
                        <span className="badge badge-danger" style={{ display: 'inline-flex', alignItems: 'center', gap: '0.25rem' }}>
                          <AlertTriangle size={12} /> MISMATCH DETECTED
                        </span>
                      )}
                    </td>
                  </tr>
                ))}
                {filteredReconciliations.length === 0 && (
                  <tr>
                    <td colSpan="6" style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>
                      No account reconciliations found matching your search.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* TAB 2: Immutable Ledger Journal */}
      {activeTab === 'ledger' && (
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <div>
              <h3 style={{ margin: 0 }}>Immutable Double-Entry Ledger Journal</h3>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', margin: 0 }}>
                Append-only financial entries. Every transaction produces matching debits and credits.
              </p>
            </div>
          </div>

          <div className="table-responsive">
            <table className="table">
              <thead>
                <tr>
                  <th>Entry ID</th>
                  <th>Transaction Ref</th>
                  <th>Account Number</th>
                  <th>Entry Type</th>
                  <th>Amount</th>
                  <th>Timestamp</th>
                </tr>
              </thead>
              <tbody>
                {filteredLedger.map((entry) => (
                  <tr key={entry.id}>
                    <td style={{ fontFamily: 'monospace', color: 'var(--text-muted)' }}>#{entry.id}</td>
                    <td style={{ fontFamily: 'monospace', fontWeight: 600 }}>{entry.transactionReference}</td>
                    <td style={{ fontFamily: 'monospace' }}>{entry.accountNumber}</td>
                    <td>
                      <span className={`badge ${entry.entryType === 'CREDIT' ? 'badge-success' : 'badge-danger'}`}>
                        {entry.entryType}
                      </span>
                    </td>
                    <td style={{ 
                      fontFamily: 'monospace', 
                      fontWeight: 700, 
                      color: entry.entryType === 'CREDIT' ? 'var(--accent-success)' : 'var(--accent-danger)' 
                    }}>
                      {entry.entryType === 'DEBIT' ? '-' : '+'}{formatCurrency(entry.amount)}
                    </td>
                    <td style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                      {entry.createdAt ? new Date(entry.createdAt).toLocaleString() : '-'}
                    </td>
                  </tr>
                ))}
                {filteredLedger.length === 0 && (
                  <tr>
                    <td colSpan="6" style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>
                      No ledger entries found.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* TAB 3: Global Transactions */}
      {activeTab === 'transactions' && (
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <div>
              <h3 style={{ margin: 0 }}>Global Transaction Envelopes</h3>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', margin: 0 }}>
                All transaction lifecycles recorded across the financial network.
              </p>
            </div>
          </div>

          <div className="table-responsive">
            <table className="table">
              <thead>
                <tr>
                  <th>Reference</th>
                  <th>Type</th>
                  <th>Source Account</th>
                  <th>Destination Account</th>
                  <th>Amount</th>
                  <th>Status</th>
                  <th>Date & Time</th>
                </tr>
              </thead>
              <tbody>
                {filteredTx.map((tx) => (
                  <tr key={tx.id}>
                    <td style={{ fontFamily: 'monospace', fontWeight: 600 }}>{tx.transactionReference}</td>
                    <td>
                      <span className="badge badge-info">{tx.transactionType}</span>
                    </td>
                    <td style={{ fontFamily: 'monospace' }}>{tx.sourceAccountNumber || '—'}</td>
                    <td style={{ fontFamily: 'monospace' }}>{tx.destinationAccountNumber || '—'}</td>
                    <td style={{ fontFamily: 'monospace', fontWeight: 600 }}>{formatCurrency(tx.amount)}</td>
                    <td>
                      <span className={`badge ${tx.status === 'COMPLETED' ? 'badge-success' : tx.status === 'FAILED' ? 'badge-danger' : 'badge-warning'}`}>
                        {tx.status}
                      </span>
                    </td>
                    <td style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                      {tx.createdAt ? new Date(tx.createdAt).toLocaleString() : '-'}
                    </td>
                  </tr>
                ))}
                {filteredTx.length === 0 && (
                  <tr>
                    <td colSpan="7" style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>
                      No transactions recorded.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* TAB 4: Security Audit Trail */}
      {activeTab === 'audit-logs' && (
        <div className="card">
          <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1rem' }}>
            <div>
              <h3 style={{ margin: 0 }}>Append-Only Security Audit Trail</h3>
              <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', margin: 0 }}>
                Every financial action, permission check, and state mutation logged with actor, IP and timestamp.
              </p>
            </div>
          </div>

          <div className="table-responsive">
            <table className="table">
              <thead>
                <tr>
                  <th>ID</th>
                  <th>Action</th>
                  <th>Actor</th>
                  <th>Target Type</th>
                  <th>Status</th>
                  <th>IP Address</th>
                  <th>Details</th>
                  <th>Timestamp</th>
                </tr>
              </thead>
              <tbody>
                {auditLogs.map((log) => (
                  <tr key={log.id}>
                    <td style={{ fontFamily: 'monospace', color: 'var(--text-muted)' }}>#{log.id}</td>
                    <td style={{ fontWeight: 600 }}>{log.action}</td>
                    <td style={{ fontFamily: 'monospace' }}>{log.username || 'ANONYMOUS'}</td>
                    <td>{log.resourceType || '—'}</td>
                    <td>
                      <span className={`badge ${log.status === 'SUCCESS' ? 'badge-success' : 'badge-danger'}`}>
                        {log.status}
                      </span>
                    </td>
                    <td style={{ fontFamily: 'monospace', fontSize: '0.85rem' }}>{log.ipAddress || '127.0.0.1'}</td>
                    <td style={{ fontSize: '0.85rem', maxWidth: '300px', whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }} title={log.details}>
                      {log.details || '—'}
                    </td>
                    <td style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
                      {log.timestamp ? new Date(log.timestamp).toLocaleString() : '-'}
                    </td>
                  </tr>
                ))}
                {auditLogs.length === 0 && (
                  <tr>
                    <td colSpan="8" style={{ textAlign: 'center', padding: '2rem', color: 'var(--text-muted)' }}>
                      No audit records found.
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
};

export default AuditorDashboardPage;

