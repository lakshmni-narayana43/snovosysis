import React from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { 
  ShieldCheck, 
  Lock, 
  ArrowRightLeft, 
  FileText, 
  CheckCircle2, 
  Cpu, 
  Database, 
  Users, 
  Sparkles,
  ArrowRight
} from 'lucide-react';

const LandingPage = () => {
  const { login, isAuthenticated } = useAuth();
  const navigate = useNavigate();

  const handleQuickDemo = async (username, password, redirectPath) => {
    try {
      await login(username, password);
      navigate(redirectPath);
    } catch (err) {
      console.error('Quick demo login error', err);
    }
  };

  return (
    <div>
      {/* Hero Section */}
      <section style={{
        background: 'linear-gradient(180deg, #ffffff 0%, #f1f5f9 100%)',
        padding: '5rem 0 4rem',
        borderBottom: '1px solid var(--border)',
        textAlign: 'center'
      }}>
        <div className="container" style={{ maxWidth: '900px' }}>
          <div style={{
            display: 'inline-flex',
            alignItems: 'center',
            gap: '0.5rem',
            backgroundColor: '#eff6ff',
            border: '1px solid #bfdbfe',
            color: '#1d4ed8',
            padding: '0.35rem 0.85rem',
            borderRadius: '9999px',
            fontSize: '0.8rem',
            fontWeight: 600,
            marginBottom: '1.5rem'
          }}>
            <Sparkles size={14} /> Production-Grade Financial Transaction Architecture
          </div>

          <h1 style={{ fontSize: '3rem', fontWeight: 800, lineHeight: 1.15, marginBottom: '1.25rem', color: '#0f172a' }}>
            Money Movement With a Ledger <br />
            <span style={{
              background: 'linear-gradient(90deg, #1e3a8a 0%, #059669 100%)',
              WebkitBackgroundClip: 'text',
              WebkitTextFillColor: 'transparent'
            }}>You Can Prove.</span>
          </h1>

          <p style={{ fontSize: '1.15rem', color: '#475569', marginBottom: '2.5rem', lineHeight: 1.6 }}>
            Every movement of money is authorized, accurate, protected against duplicate requests,
            immune to simultaneous withdrawal race conditions, and recorded in an immutable double-entry ledger.
          </p>

          <div style={{ display: 'flex', gap: '1rem', justifyContent: 'center', flexWrap: 'wrap', marginBottom: '3rem' }}>
            {isAuthenticated ? (
              <Link to="/dashboard" className="btn btn-primary" style={{ padding: '0.75rem 1.75rem', fontSize: '1rem' }}>
                Go to Dashboard <ArrowRight size={18} />
              </Link>
            ) : (
              <>
                <Link to="/register" className="btn btn-primary" style={{ padding: '0.75rem 1.75rem', fontSize: '1rem' }}>
                  Open Account <ArrowRight size={18} />
                </Link>
                <Link to="/login" className="btn btn-outline" style={{ padding: '0.75rem 1.75rem', fontSize: '1rem' }}>
                  Sign In to Portal
                </Link>
              </>
            )}
          </div>

          {/* Quick Demo Evaluation Panel */}
          <div style={{
            backgroundColor: '#ffffff',
            borderRadius: '14px',
            border: '1px solid #e2e8f0',
            padding: '1.5rem',
            boxShadow: 'var(--shadow-md)',
            textAlign: 'left'
          }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '1rem' }}>
              <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <span className="badge badge-info" style={{ textTransform: 'none', fontSize: '0.75rem' }}>Reviewer Evaluation</span>
                <span style={{ fontWeight: 600, fontSize: '0.9rem', color: '#0f172a' }}>1-Click Instant Demo Logins:</span>
              </div>
              <span style={{ fontSize: '0.75rem', color: '#64748b' }}>Pre-seeded with verifiable double-entry balances</span>
            </div>

            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(190px, 1fr))', gap: '0.75rem' }}>
              <button 
                onClick={() => handleQuickDemo('alice', 'User@123', '/dashboard')}
                className="btn btn-outline btn-sm"
                style={{ justifyContent: 'flex-start', padding: '0.6rem 0.8rem' }}
              >
                <Users size={16} color="#2563eb" />
                <div style={{ textAlign: 'left' }}>
                  <div style={{ fontWeight: 600, fontSize: '0.8rem' }}>Alice (User)</div>
                  <div style={{ fontSize: '0.7rem', color: '#64748b' }}>₹10,000 Balance</div>
                </div>
              </button>

              <button 
                onClick={() => handleQuickDemo('bob', 'User@123', '/dashboard')}
                className="btn btn-outline btn-sm"
                style={{ justifyContent: 'flex-start', padding: '0.6rem 0.8rem' }}
              >
                <Users size={16} color="#059669" />
                <div style={{ textAlign: 'left' }}>
                  <div style={{ fontWeight: 600, fontSize: '0.8rem' }}>Bob (User)</div>
                  <div style={{ fontSize: '0.7rem', color: '#64748b' }}>₹5,000 Balance</div>
                </div>
              </button>

              <button 
                onClick={() => handleQuickDemo('admin', 'Admin@123', '/admin')}
                className="btn btn-outline btn-sm"
                style={{ justifyContent: 'flex-start', padding: '0.6rem 0.8rem' }}
              >
                <ShieldCheck size={16} color="#dc2626" />
                <div style={{ textAlign: 'left' }}>
                  <div style={{ fontWeight: 600, fontSize: '0.8rem' }}>Administrator</div>
                  <div style={{ fontSize: '0.7rem', color: '#64748b' }}>System & Users</div>
                </div>
              </button>

              <button 
                onClick={() => handleQuickDemo('auditor', 'Auditor@123', '/auditor')}
                className="btn btn-outline btn-sm"
                style={{ justifyContent: 'flex-start', padding: '0.6rem 0.8rem' }}
              >
                <FileText size={16} color="#d97706" />
                <div style={{ textAlign: 'left' }}>
                  <div style={{ fontWeight: 600, fontSize: '0.8rem' }}>Auditor</div>
                  <div style={{ fontSize: '0.7rem', color: '#64748b' }}>Trial Balance Verifier</div>
                </div>
              </button>
            </div>
          </div>
        </div>
      </section>

      {/* Core Architectural Pillars */}
      <section style={{ padding: '4rem 0' }}>
        <div className="container">
          <div style={{ textAlign: 'center', marginBottom: '3rem' }}>
            <h2>Engineered For Financial Correctness</h2>
            <p style={{ color: 'var(--text-muted)', marginTop: '0.5rem' }}>
              Solving the fundamental hazards of electronic money movement at the database level.
            </p>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(260px, 1fr))', gap: '1.5rem' }}>
            <div className="card">
              <div style={{ backgroundColor: '#eff6ff', color: '#2563eb', padding: '0.75rem', borderRadius: '10px', display: 'inline-block', marginBottom: '1rem' }}>
                <ArrowRightLeft size={24} />
              </div>
              <h3 style={{ marginBottom: '0.5rem' }}>Zero Balance Leak</h3>
              <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', lineHeight: 1.6 }}>
                Every transaction requires at least 2 balanced ledger entries. Internal transfers preserve total currency: sender debits match receiver credits to the fourth decimal place.
              </p>
            </div>

            <div className="card">
              <div style={{ backgroundColor: '#ecfdf5', color: '#059669', padding: '0.75rem', borderRadius: '10px', display: 'inline-block', marginBottom: '1rem' }}>
                <Lock size={24} />
              </div>
              <h3 style={{ marginBottom: '0.5rem' }}>Idempotency Locks</h3>
              <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', lineHeight: 1.6 }}>
                Database-enforced idempotency keys guarantee that double-clicks, network timeouts, and client retries never execute the same financial movement twice.
              </p>
            </div>

            <div className="card">
              <div style={{ backgroundColor: '#fff7ed', color: '#ea580c', padding: '0.75rem', borderRadius: '10px', display: 'inline-block', marginBottom: '1rem' }}>
                <Cpu size={24} />
              </div>
              <h3 style={{ marginBottom: '0.5rem' }}>Concurrency Shield</h3>
              <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', lineHeight: 1.6 }}>
                Pessimistic row-level locking with deterministic account ID sorting guarantees deadlock-free execution. Two simultaneous withdrawals from ₹1,000 never allow overdrawing.
              </p>
            </div>

            <div className="card">
              <div style={{ backgroundColor: '#f5f3ff', color: '#7c3aed', padding: '0.75rem', borderRadius: '10px', display: 'inline-block', marginBottom: '1rem' }}>
                <Database size={24} />
              </div>
              <h3 style={{ marginBottom: '0.5rem' }}>Immutable Audit Trail</h3>
              <p style={{ fontSize: '0.875rem', color: 'var(--text-muted)', lineHeight: 1.6 }}>
                Posted ledger records cannot be edited or deleted. An append-only audit log records actor, action, timestamp, and metadata for every state change.
              </p>
            </div>
          </div>
        </div>
      </section>
    </div>
  );
};

export default LandingPage;

