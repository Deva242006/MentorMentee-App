// Small presentational helpers shared across pages. Bootstrap classes only —
// no Bootstrap JS bundle, so the modal is a controlled React component.

export function Spinner({ label = 'Loading…' }) {
  return (
    <div className="text-center py-5">
      <div className="spinner-border text-primary" role="status">
        <span className="visually-hidden">{label}</span>
      </div>
    </div>
  );
}

export function ErrorAlert({ error, onClose }) {
  if (!error) return null;
  const msg = typeof error === 'string' ? error : error.message || 'Something went wrong';
  return (
    <div className="alert alert-danger alert-dismissible d-flex align-items-center" role="alert">
      <i className="bi bi-exclamation-triangle-fill me-2"></i>
      <div className="flex-grow-1">{msg}</div>
      {onClose && <button type="button" className="btn-close" onClick={onClose}></button>}
    </div>
  );
}

export function EmptyState({ icon = 'inbox', children }) {
  return (
    <div className="text-center text-muted py-5">
      <i className={`bi bi-${icon} fs-1 d-block mb-2 opacity-50`}></i>
      {children}
    </div>
  );
}

export function Modal({ title, show, onClose, children, footer, size }) {
  if (!show) return null;
  return (
    <>
      <div className="modal fade show d-block" tabIndex="-1" role="dialog">
        <div className={`modal-dialog modal-dialog-centered ${size ? `modal-${size}` : ''}`}>
          <div className="modal-content">
            <div className="modal-header">
              <h5 className="modal-title">{title}</h5>
              <button type="button" className="btn-close" onClick={onClose}></button>
            </div>
            <div className="modal-body">{children}</div>
            {footer && <div className="modal-footer">{footer}</div>}
          </div>
        </div>
      </div>
      <div className="modal-backdrop fade show" onClick={onClose}></div>
    </>
  );
}

// Colored badge for the various status enums.
export function StatusBadge({ value }) {
  const map = {
    PENDING: 'bg-secondary',
    SUBMITTED: 'bg-info text-dark',
    GRADED: 'bg-success',
    TODO: 'bg-secondary',
    IN_PROGRESS: 'bg-warning text-dark',
    DONE: 'bg-success',
    LOW: 'bg-light text-dark border',
    MEDIUM: 'bg-primary',
    HIGH: 'bg-danger',
  };
  return <span className={`badge ${map[value] || 'bg-secondary'}`}>{label(value)}</span>;
}

function label(v) {
  return typeof v === 'string' ? v.replace(/_/g, ' ') : v;
}

export function formatDate(value) {
  if (!value) return '—';
  try {
    // LocalDate ("2026-08-26") or Instant ("...Z")
    const d = value.length <= 10 ? new Date(value + 'T00:00:00') : new Date(value);
    return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
  } catch {
    return value;
  }
}

export function formatBytes(bytes) {
  if (bytes == null) return '—';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}
