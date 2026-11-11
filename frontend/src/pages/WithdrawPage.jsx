import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import ConfirmModal from '../components/ConfirmModal';
import { ArrowUpRight, ShieldCheck, CheckCircle2, AlertCircle } from 'lucide-react';

const WithdrawPage = () => {
  const [accounts, setAccounts] = useState([]);
  const [selectedAccount, setSelectedAccount] = useState('');
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [successResult, setSuccessResult] = useState(null);
  const [error, setError] = useState('');

  const navigate = useNavigate();

  useEffect(() => {
    api.get('/accounts')
      .then((res) => {
        if (res.data && res.data.data) {
          setAccounts(res.data.data);
          if (res.data.data.length > 0) {
            setSelectedAccount(res.data.data[0].accountNumber);
          }
        }
      })
      .catch(() => setError('Failed to load accounts'))
      .finally(() => setLoading(false));
  }, []);

  const currentAccount = accounts.find((a) => a.accountNumber === selectedAccount);
  const currentBalance = currentAccount ? parseFloat(currentAccount.balance) : 0;

  const handleOpenConfirm = (e) => {
    e.preventDefault();
    setError('');
    const num = parseFloat(amount);
    if (!num || num <= 0) {
      setError('Please enter a valid withdrawal amount greater than zero.');
      return;
    }
    if (num > currentBalance) {
      setError(`Insufficient funds. Your available balance is ₹${currentBalance.toFixed(2)}.`);
      return;
    }
    setShowConfirm(true);
  };

  const handleConfirmWithdraw = async () => {
    setSubmitting(true);
    setError('');

    try {
      const res = await api.post('/transactions/withdraw', {
        accountNumber: selectedAccount,
        amount: parseFloat(amount),
        currency: 'INR',
        description: description || 'Account withdrawal',
      });

      if (res.data && res.data.data) {
        setSuccessResult(res.data.data);
        setShowConfirm(false);
        setAmount('');
        setDescription('');
        // Refresh account list to reflect updated balance
        api.get('/accounts').then((r) => setAccounts(r.data?.data || []));
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Withdrawal failed. Insufficient funds or account issue.');
      setShowConfirm(false);
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return <div className="container" style={{ padding: '3rem 0', textAlign: 'center' }}>Loading withdrawal module...</div>;
  }

  return (
    <div className="container" style={{ padding: '3rem 1.5rem', maxWidth: '640px' }}>
      <div className="card">
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem' }}>
          <div style={{
            backgroundColor: '#fee2e2',
            color: '#b91c1c',
            padding: '0.6rem',
            borderRadius: '10px'
          }}>
            <ArrowUpRight size={24} />
          </div>
          <div>
            <h2 style={{ margin: 0 }}>Withdraw Money</h2>
            <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
              Debit user account with offsetting credit to clearing cash reserve
            </div>
          </div>
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
            marginBottom: '1.5rem',
            fontSize: '0.875rem'
          }}>
            <AlertCircle size={18} style={{ flexShrink: 0 }} />
            <span>{error}</span>
          </div>
        )}

        {successResult && (
          <div style={{
            backgroundColor: '#ecfdf5',
            border: '1px solid #a7f3d0',
            borderRadius: '10px',
            padding: '1.25rem',
            marginBottom: '1.5rem'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', color: '#065f46', fontWeight: 600, marginBottom: '0.5rem' }}>
              <CheckCircle2 size={20} />
              <span>Withdrawal Completed Successfully</span>
            </div>
            <div style={{ fontSize: '0.85rem', color: '#047857', marginBottom: '0.75rem' }}>
              Reference: <strong className="mono">{successResult.transactionReference}</strong>
            </div>

            <div style={{ fontSize: '0.75rem', color: '#065f46', borderTop: '1px solid #bbf7d0', paddingTop: '0.75rem' }}>
              <strong>Double-Entry Ledger Verified:</strong>
              <ul style={{ paddingLeft: '1.25rem', marginTop: '0.25rem' }}>
                <li>Debit: {successResult.sourceAccountNumber} (₹{parseFloat(successResult.amount).toFixed(2)})</li>
                <li>Credit: ACC-SYSTEM-CLEARING (₹{parseFloat(successResult.amount).toFixed(2)})</li>
              </ul>
            </div>
          </div>
        )}

        <form onSubmit={handleOpenConfirm}>
          <div className="form-group">
            <label className="form-label">Source Account</label>
            <select
              className="form-control"
              value={selectedAccount}
              onChange={(e) => setSelectedAccount(e.target.value)}
              required
            >
              {accounts.map((acc) => (
                <option key={acc.id} value={acc.accountNumber}>
                  {acc.accountNumber} ({acc.accountType}) &mdash; Available: ₹{parseFloat(acc.balance).toFixed(2)}
                </option>
              ))}
            </select>
            <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '0.35rem' }}>
              Available Balance: <strong className="mono">₹{currentBalance.toFixed(2)}</strong>
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Withdrawal Amount (₹ INR)</label>
            <input
              type="number"
              step="0.01"
              min="0.01"
              max={currentBalance}
              className="form-control mono"
              placeholder="0.00"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              required
              style={{ fontSize: '1.2rem', fontWeight: 600 }}
            />
          </div>

          <div className="form-group">
            <label className="form-label">Description (Optional)</label>
            <input
              type="text"
              className="form-control"
              placeholder="e.g. ATM cash withdrawal, bill settlement"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>

          <button
            type="submit"
            className="btn btn-danger"
            style={{ width: '100%', padding: '0.75rem', fontSize: '1rem', marginTop: '0.5rem' }}
          >
            Review & Withdraw Funds
          </button>
        </form>
      </div>

      <ConfirmModal
        isOpen={showConfirm}
        onClose={() => setShowConfirm(false)}
        onConfirm={handleConfirmWithdraw}
        title="Confirm Withdrawal"
        isSubmitting={submitting}
        confirmText="Confirm Withdrawal"
        details={[
          { label: 'Debit Account', value: selectedAccount, mono: true },
          { label: 'Current Balance', value: `₹${currentBalance.toFixed(2)}`, mono: true },
          { label: 'Withdrawal Amount', value: `₹${parseFloat(amount || 0).toFixed(2)}`, highlight: true, mono: true },
          { label: 'Projected Balance', value: `₹${(currentBalance - parseFloat(amount || 0)).toFixed(2)}`, mono: true },
        ]}
      />
    </div>
  );
};

export default WithdrawPage;

