import React from 'react';
import { AlertCircle, CheckCircle2, ShieldCheck, X } from 'lucide-react';

const ConfirmModal = ({ isOpen, onClose, onConfirm, title, details, confirmText = 'Confirm Transaction', isSubmitting = false }) => {
  if (!isOpen) return null;

  return (
    <div className="modal-overlay">
      <div className="modal-content">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.6rem' }}>
            <div style={{
              backgroundColor: '#eff6ff',
              color: '#2563eb',
              padding: '0.5rem',
              borderRadius: '8px'
            }}>
              <ShieldCheck size={20} />
            </div>
            <h3 style={{ margin: 0 }}>{title}</h3>
          </div>
          <button 
            onClick={onClose} 
            disabled={isSubmitting}
            style={{ background: 'none', border: 'none', cursor: 'pointer', color: '#64748b' }}
          >
            <X size={20} />
          </button>
        </div>

        <div style={{
          backgroundColor: '#f8fafc',
          borderRadius: '10px',
          border: '1px solid #e2e8f0',
          padding: '1.25rem',
          marginBottom: '1.5rem'
        }}>
          {details.map((item, idx) => (
            <div key={idx} style={{
              display: 'flex',
              justifyContent: 'space-between',
              alignItems: 'center',
              padding: '0.5rem 0',
              borderBottom: idx === details.length - 1 ? 'none' : '1px dashed #cbd5e1',
              fontSize: '0.875rem'
            }}>
              <span style={{ color: '#64748b' }}>{item.label}</span>
              <span style={{ fontWeight: item.highlight ? 700 : 600, color: item.highlight ? '#0f172a' : '#334155' }} className={item.mono ? 'mono' : ''}>
                {item.value}
              </span>
            </div>
          ))}
        </div>

        <div style={{
          display: 'flex',
          alignItems: 'flex-start',
          gap: '0.5rem',
          backgroundColor: '#ecfdf5',
          border: '1px solid #a7f3d0',
          borderRadius: '8px',
          padding: '0.75rem',
          marginBottom: '1.5rem',
          fontSize: '0.75rem',
          color: '#065f46'
        }}>
          <CheckCircle2 size={16} style={{ flexShrink: 0, marginTop: '2px' }} />
          <div>
            <strong>Protected by Idempotency & Double-Entry:</strong> This operation creates balanced debit & credit entries and cannot be processed twice.
          </div>
        </div>

        <div style={{ display: 'flex', gap: '0.75rem', justifyContent: 'flex-end' }}>
          <button 
            onClick={onClose} 
            className="btn btn-outline"
            disabled={isSubmitting}
          >
            Cancel
          </button>
          <button 
            onClick={onConfirm} 
            className="btn btn-primary"
            disabled={isSubmitting}
            style={{ minWidth: '150px' }}
          >
            {isSubmitting ? 'Securing Transaction...' : confirmText}
          </button>
        </div>
      </div>
    </div>
  );
};

export default ConfirmModal;

