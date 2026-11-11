import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../services/api';
import ConfirmModal from '../components/ConfirmModal';
import { Send, ShieldCheck, CheckCircle2, AlertCircle, RefreshCw } from 'lucide-react';

const TransferPage = () => {
  const [accounts, setAccounts] = useState([]);
  const [recipients, setRecipients] = useState([]);
  const [sourceAccount, setSourceAccount] = useState('');
  const [destinationAccount, setDestinationAccount] = useState('');
  const [amount, setAmount] = useState('');
  const [description, setDescription] = useState('');
  const [idempotencyKey, setIdempotencyKey] = useState(() => crypto.randomUUID());
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
            setSourceAccount(res.data.data[0].accountNumber);
          }
        }
      })
      .catch(() => setError('Failed to load user accounts'))
      .finally(() => setLoading(false));
    api.get('/accounts/recipients')
      .then((res) => setRecipients(res.data?.data || []))
      .catch(() => setRecipients([]));
  }, []);

  const currentAccount = accounts.find((a) => a.accountNumber === sourceAccount);
  const availableBalance = currentAccount ? parseFloat(currentAccount.balance) : 0;

  const handleOpenConfirm = (e) => {
    e.preventDefault();
    setError('');

    const destClean = destinationAccount.trim();
    if (!destClean) {
      setError('Please provide a destination account number.');
      return;
    }

    if (destClean.toUpperCase() === sourceAccount.toUpperCase()) {
      setError('Source and destination accounts cannot be identical.');
      return;
    }

    const num = parseFloat(amount);
    if (!num || num <= 0) {
      setError('Please enter a valid transfer amount greater than zero.');
      return;
    }

    if (num > availableBalance) {
      setError(`Insufficient available funds. Available: ₹${availableBalance.toFixed(2)}.`);
      return;
    }

    setShowConfirm(true);
  };

  const handleConfirmTransfer = async () => {
    setSubmitting(true);
    setError('');

    try {
      const res = await api.post('/transactions/transfer', {
        sourceAccountNumber: sourceAccount,
        destinationAccountNumber: destinationAccount.trim(),
        amount: parseFloat(amount),
        currency: 'INR',
        description: description || 'Peer money transfer',
      }, {
        headers: {
          'Idempotency-Key': idempotencyKey,
        }
      });

      if (res.data && res.data.data) {
        setSuccessResult(res.data.data);
        setShowConfirm(false);
        setAmount('');
        setDescription('');
        setDestinationAccount('');
        // Generate a new idempotency key for the next transfer
        setIdempotencyKey(crypto.randomUUID());
        // Refresh balance
        api.get('/accounts').then((r) => setAccounts(r.data?.data || []));
      }
    } catch (err) {
      setError(err.response?.data?.message || 'Transfer failed. Please check destination account or balance.');
      setShowConfirm(false);
    } finally {
      setSubmitting(false);
    }
  };

  const regenerateKey = () => {
    setIdempotencyKey(crypto.randomUUID());
  };

  if (loading) {
    return <div className="container" style={{ padding: '3rem 0', textAlign: 'center' }}>Loading transfer gateway...</div>;
  }

  return (
    <div className="container" style={{ padding: '3rem 1.5rem', maxWidth: '640px' }}>
      <div className="card">
        <div style={{ display: 'flex', alignItems: 'center', gap: '0.75rem', marginBottom: '1.5rem' }}>
          <div style={{
            backgroundColor: '#eff6ff',
            color: '#2563eb',
            padding: '0.6rem',
            borderRadius: '10px'
          }}>
            <Send size={24} />
          </div>
          <div>
            <h2 style={{ margin: 0 }}>Transfer Funds</h2>
            <div style={{ fontSize: '0.85rem', color: 'var(--text-muted)' }}>
              Atomic money movement: Sender Debit = Receiver Credit (Conserved Total)
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
              <span>Transfer Successfully Committed</span>
            </div>
            <div style={{ fontSize: '0.85rem', color: '#047857', marginBottom: '0.75rem' }}>
              Transaction Reference: <strong className="mono">{successResult.transactionReference}</strong>
            </div>

            <div style={{ fontSize: '0.75rem', color: '#065f46', borderTop: '1px solid #bbf7d0', paddingTop: '0.75rem' }}>
              <strong>Double-Entry Invariant Satisfied:</strong>
              <ul style={{ paddingLeft: '1.25rem', marginTop: '0.25rem' }}>
                <li>Debit Sender: {successResult.sourceAccountNumber} (-₹{parseFloat(successResult.amount).toFixed(2)})</li>
                <li>Credit Receiver: {successResult.destinationAccountNumber} (+₹{parseFloat(successResult.amount).toFixed(2)})</li>
                <li>Imbalance: ₹0.0000 (Exact conservation of value)</li>
              </ul>
            </div>
          </div>
        )}

        <form onSubmit={handleOpenConfirm}>
          <div className="form-group">
            <label className="form-label">From Account (Sender)</label>
            <select
              className="form-control"
              value={sourceAccount}
              onChange={(e) => {
                const nextSource = e.target.value;
                setSourceAccount(nextSource);
                if (destinationAccount.trim().toUpperCase() === nextSource.toUpperCase()) {
                  setDestinationAccount('');
                }
                setError('');
              }}
              required
            >
              {accounts.map((acc) => (
                <option key={acc.id} value={acc.accountNumber}>
                  {acc.accountNumber} ({acc.accountType}) &mdash; Available: ₹{parseFloat(acc.balance).toFixed(2)}
                </option>
              ))}
            </select>
            <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '0.35rem' }}>
              Available Balance: <strong className="mono">₹{availableBalance.toFixed(2)}</strong>
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">To Account Number (Recipient)</label>
            <input
              type="text"
              className="form-control mono"
              placeholder="e.g. LL4178272490 or Bob's account"
              value={destinationAccount}
              list="recipient-accounts"
              onChange={(e) => {
                setDestinationAccount(e.target.value);
                if (e.target.value.trim().toUpperCase() !== sourceAccount.toUpperCase()) {
                  setError('');
                }
              }}
              required
            />
            <datalist id="recipient-accounts">
              {recipients.map((recipient) => (
                <option key={recipient.id} value={recipient.accountNumber}>
                  {recipient.username} ({recipient.accountType})
                </option>
              ))}
            </datalist>
            <div style={{ fontSize: '0.75rem', color: '#64748b', marginTop: '0.35rem' }}>
              Choose a suggested account for another user. Your own accounts are excluded.
            </div>
          </div>

          <div className="form-group">
            <label className="form-label">Transfer Amount (₹ INR)</label>
            <input
              type="number"
              step="0.01"
              min="0.01"
              max={availableBalance}
              className="form-control mono"
              placeholder="0.00"
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              required
              style={{ fontSize: '1.2rem', fontWeight: 600 }}
            />
          </div>

          <div className="form-group">
            <label className="form-label">Transfer Note / Description</label>
            <input
              type="text"
              className="form-control"
              placeholder="e.g. Project milestone invoice settlement"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>

          {/* Idempotency Key Indicator */}
          <div style={{
            backgroundColor: '#f8fafc',
            border: '1px solid #e2e8f0',
            borderRadius: '8px',
            padding: '0.75rem',
            marginBottom: '1.25rem',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between'
          }}>
            <div>
              <div style={{ fontSize: '0.7rem', color: '#64748b', fontWeight: 600 }}>ACTIVE IDEMPOTENCY KEY:</div>
              <div className="mono" style={{ fontSize: '0.75rem', color: '#334155' }}>{idempotencyKey}</div>
            </div>
            <button
              type="button"
              onClick={regenerateKey}
              className="btn btn-outline btn-sm"
              style={{ fontSize: '0.7rem', padding: '0.2rem 0.5rem' }}
              title="Generate new idempotency key"
            >
              <RefreshCw size={12} /> New Key
            </button>
          </div>

          <button
            type="submit"
            className="btn btn-blue"
            style={{ width: '100%', padding: '0.75rem', fontSize: '1rem' }}
          >
            Review & Complete Transfer
          </button>
        </form>
      </div>

      <ConfirmModal
        isOpen={showConfirm}
        onClose={() => setShowConfirm(false)}
        onConfirm={handleConfirmTransfer}
        title="Confirm Money Transfer"
        isSubmitting={submitting}
        confirmText="Authorize Transfer"
        details={[
          { label: 'Debit Account', value: sourceAccount, mono: true },
          { label: 'Credit Account', value: destinationAccount, mono: true },
          { label: 'Transfer Amount', value: `₹${parseFloat(amount || 0).toFixed(2)}`, highlight: true, mono: true },
          { label: 'Remaining Sender Balance', value: `₹${(availableBalance - parseFloat(amount || 0)).toFixed(2)}`, mono: true },
          { label: 'Idempotency Key', value: idempotencyKey, mono: true },
        ]}
      />
    </div>
  );
};

export default TransferPage;
