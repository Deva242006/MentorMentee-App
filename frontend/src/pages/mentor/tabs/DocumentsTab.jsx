import { useEffect, useState, useCallback, useRef } from 'react';
import { api, downloadFile } from '../../../api';
import {
  Spinner,
  ErrorAlert,
  EmptyState,
  formatDate,
  formatBytes,
} from '../../../components/ui.jsx';

export default function DocumentsTab({ menteeId }) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [category, setCategory] = useState('');
  const [uploading, setUploading] = useState(false);
  const fileRef = useRef(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await api.get(`/api/mentor/mentees/${menteeId}/documents`));
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, [menteeId]);

  useEffect(() => {
    load();
  }, [load]);

  async function upload(e) {
    e.preventDefault();
    const file = fileRef.current?.files?.[0];
    if (!file) return;
    setUploading(true);
    setError(null);
    try {
      const fd = new FormData();
      fd.append('file', file);
      if (category) fd.append('category', category);
      await api.postForm(`/api/mentor/mentees/${menteeId}/documents`, fd);
      fileRef.current.value = '';
      setCategory('');
      await load();
    } catch (err) {
      setError(err);
    } finally {
      setUploading(false);
    }
  }

  async function download(d) {
    try {
      await downloadFile(`/api/mentor/documents/${d.id}/download`, d.originalName);
    } catch (err) {
      setError(err);
    }
  }

  async function remove(d) {
    if (!confirm(`Delete "${d.originalName}"?`)) return;
    try {
      await api.del(`/api/mentor/documents/${d.id}`);
      await load();
    } catch (err) {
      setError(err);
    }
  }

  return (
    <div>
      <h2 className="h5 mb-3">Documents</h2>
      <ErrorAlert error={error} onClose={() => setError(null)} />

      <form className="card border-0 shadow-sm mb-4" onSubmit={upload}>
        <div className="card-body">
          <div className="row g-2 align-items-end">
            <div className="col-md-5">
              <label className="form-label small">File</label>
              <input ref={fileRef} type="file" className="form-control" required />
            </div>
            <div className="col-md-4">
              <label className="form-label small">Category (optional)</label>
              <input
                className="form-control"
                placeholder="e.g. Report, Certificate, ID"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
              />
            </div>
            <div className="col-md-3">
              <button className="btn btn-primary w-100" disabled={uploading}>
                {uploading ? (
                  <span className="spinner-border spinner-border-sm me-2"></span>
                ) : (
                  <i className="bi bi-upload me-2"></i>
                )}
                Upload
              </button>
            </div>
          </div>
        </div>
      </form>

      {loading ? (
        <Spinner />
      ) : items.length === 0 ? (
        <EmptyState icon="folder">No documents uploaded yet.</EmptyState>
      ) : (
        <div className="card border-0 shadow-sm">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th>Name</th>
                  <th>Category</th>
                  <th>Size</th>
                  <th>Uploaded by</th>
                  <th>Date</th>
                  <th className="text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {items.map((d) => (
                  <tr key={d.id}>
                    <td className="fw-medium">
                      <i className="bi bi-file-earmark me-2 text-muted"></i>
                      {d.originalName}
                    </td>
                    <td>{d.category ? <span className="badge bg-info-subtle text-info-emphasis">{d.category}</span> : '—'}</td>
                    <td className="text-muted small">{formatBytes(d.size)}</td>
                    <td className="text-muted small">{d.uploadedByName || '—'}</td>
                    <td className="text-muted small">{formatDate(d.uploadedAt)}</td>
                    <td className="text-end text-nowrap">
                      <button className="btn btn-sm btn-outline-primary me-1" onClick={() => download(d)}>
                        <i className="bi bi-download"></i>
                      </button>
                      <button className="btn btn-sm btn-outline-danger" onClick={() => remove(d)}>
                        <i className="bi bi-trash"></i>
                      </button>
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
}
