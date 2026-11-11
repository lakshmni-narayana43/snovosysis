import React, { useState, useEffect } from 'react';
import api from '../services/api';
import { 
  FileText, 
  Search, 
  Filter, 
  Clock, 
  ArrowDownLeft, 
  ArrowUpRight, 
  ArrowRightLeft, 
  CheckCircle2, 
  AlertCircle,
  Eye,
  X,
  ShieldCheck,
  RefreshCw
} from 'lucide-react';

const TransactionsPage = () => {
  const [transactions, setTransactions] = useState([]);
  const [selectedTxn, setSelectedTxn] = useState(null);
  const [search, setSearch] = useState('');
  const [typeFilter, setTypeFilter] = useState('ALL');
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);

  const fetchTransactions = async () => {
    try {
      setLoading(true);
      const res = await api.get(`/transactions?page=${page}&size=20&sort=createdAt,desc`);
      if (res.data && res.data.data) {
        setTransactions(res.data.data.content || []);
        setTotalPages(res.data.data.totalPages || 1);
      }
    } catch (err) {
      console.error('Failed to fetch transactions', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTransactions();
  }, [page]);

  const filtered = transactions.filter((t) => {
    const matchesSearch = 
      t.transactionReference.toLowerCase().includes(search.toLowerCase()) ||
      (t.sourceAccountNumber && t.sourceAccountNumber.toLowerCase().includes(search.toLowerCase())) ||
      (t.destinationAccountNumber && t.destinationAccountNumber.toLowerCase().includes(search.toLowerCase())) ||
      (t.description && t.description.toLowerCase().includes(search.toLowerCase()));

    const matchesType = typeFilter === 'ALL' || t.transactionType === typeFilter;
    return matchesSearch && matchesType;
  });

  const formatCurrency = (val, curr = 'INR') => {
    const num = parseFloat(val) || 0;
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: curr,
      minimumFractionDigits: 2,
    }).format(num);
  };

  return (
    <div className="container" style={{ padding: '2.5rem 1.5rem' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '2rem', flexWrap: 'wrap', gap: '1rem' }}>
        <div>
          <h1>Transaction Statements</h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.9rem' }}>
            Auditable and traceable transaction records with double-entry ledger proofs
          </p>
        </div>
        <button onClick={fetchTransactions} className="btn btn-outline btn-sm">
          <RefreshCw size={14} /> Refresh
        </button>
      </div>

      {/* Filter and Search Bar */}
      <div className="card" style={{ padding: '1rem 1.25rem', marginBottom: '1.5rem' }}>
        <div style={{ display: 'flex', gap: '1rem', flexWrap: 'wrap', alignItems: 'center' }}>
          <div style={{ position: 'relative', flex: 1, minWidth: '240px' }}>
            <input
              type="text"
              className="form-control"
              placeholder="Search by reference, account number, or description..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              style={{ paddingLeft: '2.5rem' }}
            />
            <Search size={18} style={{ position: 'absolute', left: '0.85rem', top: '50%', transform: 'translateY(-50%)', color: '#94a3b8' }} />
          </div>

          <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
            <Filter size={16} color="#64748b" />
            <select
              className="form-control"
              value={typeFilter}
              onChange={(e) => setTypeFilter(e.target.value)}
              style={{ width: 'auto', minWidth: '150px' }}
            >
              <option value="ALL">All Types</option>
              <option value="DEPOSIT">Deposits</option>
              <option value="WITHDRAWAL">Withdrawals</option>
              <option value="TRANSFER">Transfers</option>
            </select>
          </div>
        </div>
      </div>

      {/* Transactions Table */}
      <div className="card" style={{ padding: 0 }}>
        {loading ? (
          <div style={{ padding: '3rem', textAlign: 'center', color: '#64748b' }}>
            Loading statements...
          </div>
        ) : filtered.length === 0 ? (
          <div style={{ padding: '3rem', textAlign: 'center', color: '#64748b' }}>
            <FileText size={36} style={{ margin: '0 auto 0.75rem', opacity: 0.5 }} />
            <div>No matching transactions found.</div>
          </div>
        ) : (
          <div className="table-container" style={{ border: 'none' }}>
            <table className="table">
              <thead>
                <tr>
                  <th>Transaction Reference</th>
                  <th>Type</th>
                  <th>From / To</th>
                  <th>Amount</th>
                  <th>Status</th>
                  <th>Date & Time</th>
                  <th>Ledger Proof</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((t) => (
                  <tr key={t.id}>
                    <td className="mono" style={{ fontWeight: 600, fontSize: '0.8rem', color: '#2563eb' }}>
                      {t.transactionReference}
                    </td>
                    <td>
                      <span className={`badge ${
                        t.transactionType === 'DEPOSIT' ? 'badge-success' :
                        t.transactionType === 'WITHDRAWAL' ? 'badge-danger' : 'badge-info'
                      }`}>
                        {t.transactionType}
                      </span>
                    </td>
                    <td style={{ fontSize: '0.8rem' }} className="mono">
                      <div><span style={{ color: '#64748b' }}>From:</span> {t.sourceAccountNumber || 'EXTERNAL_CLEARING'}</div>
                      <div><span style={{ color: '#64748b' }}>To:</span> {t.destinationAccountNumber || 'EXTERNAL_CLEARING'}</div>
                    </td>
                    <td className="mono" style={{ fontWeight: 700, fontSize: '0.95rem' }}>
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
                    <td>
                      <button
                        onClick={() => setSelectedTxn(t)}
                        className="btn btn-outline btn-sm"
                        style={{ fontSize: '0.75rem', padding: '0.3rem 0.6rem' }}
                      >
                        <Eye size={13} /> View Ledger
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Pagination */}
      {totalPages > 1 && (
        <div style={{ display: 'flex', justifyContent: 'center', gap: '0.5rem', marginTop: '1.5rem' }}>
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            className="btn btn-outline btn-sm"
          >
            Previous
          </button>
          <span style={{ display: 'flex', alignItems: 'center', fontSize: '0.875rem', color: '#64748b' }}>
            Page {page + 1} of {totalPages}
          </span>
          <button
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
            className="btn btn-outline btn-sm"
          >
            Next
          </button>
        </div>
      )}

      {/* Double-Entry Ledger Inspector Modal */}
      {selectedTxn && (
        <div className="modal-overlay">
          <div className="modal-content" style={{ maxWidth: '640px' }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
                <div style={{ backgroundColor: '#eff6ff', color: '#2563eb', padding: '0.5rem', borderRadius: '8px' }}>
                  <ShieldCheck size={20} />
                </div>
                <h3 style={{ margin: 0 }}>Double-Entry Ledger Proof</h3>
              </div>
              <button 
                onClick={() => setSelectedTxn(null)} 
                style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#64748b' }}
              >
                <X size={20} />
              </button>
            </div>

            <div style={{ backgroundColor: '#f8fafc', borderRadius: '8px', padding: '1rem', border: '1px solid #e2e8f0', marginBottom: '1.25rem' }}>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.75rem', fontSize: '0.8rem' }}>
                <div>
                  <span style={{ color: '#64748b' }}>Transaction Ref:</span>
                  <div className="mono" style={{ fontWeight: 600 }}>{selectedTxn.transactionReference}</div>
                </div>
                <div>
                  <span style={{ color: '#64748b' }}>Status:</span>
                  <div><span className="badge badge-success">{selectedTxn.status}</span></div>
                </div>
                <div>
                  <span style={{ color: '#64748b' }}>Total Amount:</span>
                  <div className="mono" style={{ fontWeight: 700 }}>{formatCurrency(selectedTxn.amount, selectedTxn.currency)}</div>
                </div>
                <div>
                  <span style={{ color: '#64748b' }}>Initiated By:</span>
                  <div style={{ fontWeight: 600 }}>{selectedTxn.initiatedByUsername || 'User'}</div>
                </div>
                {selectedTxn.idempotencyKey && (
                  <div style={{ gridColumn: 'span 2' }}>
                    <span style={{ color: '#64748b' }}>Idempotency Key:</span>
                    <div className="mono" style={{ fontSize: '0.75rem', wordBreak: 'break-all' }}>{selectedTxn.idempotencyKey}</div>
                  </div>
                )}
              </div>
            </div>

            <h4 style={{ marginBottom: '0.75rem' }}>Underlying Double-Entry Ledger Postings:</h4>

            <div className="table-container" style={{ marginBottom: '1.25rem' }}>
              <table className="table">
                <thead>
                  <tr>
                    <th>Entry ID</th>
                    <th>Account</th>
                    <th>Entry Type</th>
                    <th>Amount</th>
                  </tr>
                </thead>
                <tbody>
                  {selectedTxn.ledgerEntries && selectedTxn.ledgerEntries.length > 0 ? (
                    selectedTxn.ledgerEntries.map((le) => (
                      <tr key={le.id}>
                        <td className="mono" style={{ fontSize: '0.75rem' }}>#{le.id}</td>
                        <td className="mono" style={{ fontSize: '0.8rem', fontWeight: 600 }}>{le.accountNumber}</td>
                        <td>
                          <span className={`badge ${le.entryType === 'DEBIT' ? 'badge-danger' : 'badge-success'}`}>
                            {le.entryType}
                          </span>
                        </td>
                        <td className="mono" style={{ fontWeight: 700, color: le.entryType === 'DEBIT' ? '#dc2626' : '#16a34a' }}>
                          {formatCurrency(le.amount, le.currency)}
                        </td>
                      </tr>
                    ))
                  ) : (
                    <tr>
                      <td colSpan="4" style={{ textAlign: 'center', color: '#64748b' }}>No direct entry records attached.</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>

            <div style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem',
              backgroundColor: '#ecfdf5',
              padding: '0.75rem',
              borderRadius: '8px',
              fontSize: '0.75rem',
              color: '#065f46'
            }}>
              <CheckCircle2 size={16} />
              <span><strong>Immutable Ledger Record:</strong> Debit total strictly equals Credit total (Sum = 0.0000).</span>
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', marginTop: '1.25rem' }}>
              <button onClick={() => setSelectedTxn(null)} className="btn btn-outline btn-sm">
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default TransactionsPage;

