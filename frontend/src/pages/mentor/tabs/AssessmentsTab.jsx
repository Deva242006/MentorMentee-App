import { useEffect, useState, useCallback } from 'react';
import { api } from '../../../api';
import { Spinner, ErrorAlert, EmptyState, Modal, formatDate } from '../../../components/ui.jsx';

const empty = { title: '', type: '', score: '', maxScore: '', assessedOn: '', remarks: '' };

export default function AssessmentsTab({ menteeId }) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [show, setShow] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(empty);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await api.get(`/api/mentor/mentees/${menteeId}/assessments`));
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, [menteeId]);

  useEffect(() => {
    load();
  }, [load]);

  function openCreate() {
    setEditing(null);
    setForm(empty);
    setFormError(null);
    setShow(true);
  }

  function openEdit(a) {
    setEditing(a);
    setForm({
      title: a.title || '',
      type: a.type || '',
      score: a.score ?? '',
      maxScore: a.maxScore ?? '',
      assessedOn: a.assessedOn || '',
      remarks: a.remarks || '',
    });
    setFormError(null);
    setShow(true);
  }

  async function save(e) {
    e.preventDefault();
    setSaving(true);
    setFormError(null);
    const body = {
      title: form.title,
      type: form.type || null,
      score: form.score === '' ? null : Number(form.score),
      maxScore: form.maxScore === '' ? null : Number(form.maxScore),
      assessedOn: form.assessedOn || null,
      remarks: form.remarks || null,
    };
    try {
      if (editing) await api.put(`/api/mentor/assessments/${editing.id}`, body);
      else await api.post(`/api/mentor/mentees/${menteeId}/assessments`, body);
      setShow(false);
      await load();
    } catch (err) {
      setFormError(err);
    } finally {
      setSaving(false);
    }
  }

  async function remove(a) {
    if (!confirm(`Delete assessment "${a.title}"?`)) return;
    try {
      await api.del(`/api/mentor/assessments/${a.id}`);
      await load();
    } catch (err) {
      setError(err);
    }
  }

  if (loading) return <Spinner />;

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h2 className="h5 mb-0">Assessments</h2>
        <button className="btn btn-sm btn-primary" onClick={openCreate}>
          <i className="bi bi-plus-lg me-1"></i>Add assessment
        </button>
      </div>

      <ErrorAlert error={error} onClose={() => setError(null)} />

      {items.length === 0 ? (
        <EmptyState icon="clipboard-data">No assessments recorded yet.</EmptyState>
      ) : (
        <div className="card border-0 shadow-sm">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th>Title</th>
                  <th>Type</th>
                  <th>Score</th>
                  <th>Date</th>
                  <th>Remarks</th>
                  <th className="text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {items.map((a) => (
                  <tr key={a.id}>
                    <td className="fw-medium">{a.title}</td>
                    <td>{a.type || '—'}</td>
                    <td>
                      {a.score != null ? (
                        <span className="fw-semibold">
                          {a.score}
                          {a.maxScore != null && <span className="text-muted"> / {a.maxScore}</span>}
                        </span>
                      ) : (
                        '—'
                      )}
                    </td>
                    <td className="text-muted small">{formatDate(a.assessedOn)}</td>
                    <td className="text-muted small">{a.remarks || '—'}</td>
                    <td className="text-end text-nowrap">
                      <button className="btn btn-sm btn-outline-secondary me-1" onClick={() => openEdit(a)}>
                        <i className="bi bi-pencil"></i>
                      </button>
                      <button className="btn btn-sm btn-outline-danger" onClick={() => remove(a)}>
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

      <Modal
        show={show}
        title={editing ? 'Edit assessment' : 'Add assessment'}
        onClose={() => setShow(false)}
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setShow(false)}>
              Cancel
            </button>
            <button className="btn btn-primary" form="assess-form" disabled={saving}>
              {saving && <span className="spinner-border spinner-border-sm me-2"></span>}
              Save
            </button>
          </>
        }
      >
        <form id="assess-form" onSubmit={save}>
          <ErrorAlert error={formError} onClose={() => setFormError(null)} />
          <div className="mb-3">
            <label className="form-label">Title</label>
            <input
              className="form-control"
              value={form.title}
              onChange={(e) => setForm({ ...form, title: e.target.value })}
              required
            />
          </div>
          <div className="mb-3">
            <label className="form-label">Type</label>
            <input
              className="form-control"
              placeholder="e.g. Quiz, Viva, Project review"
              value={form.type}
              onChange={(e) => setForm({ ...form, type: e.target.value })}
            />
          </div>
          <div className="row">
            <div className="col-6 mb-3">
              <label className="form-label">Score</label>
              <input
                type="number"
                step="any"
                className="form-control"
                value={form.score}
                onChange={(e) => setForm({ ...form, score: e.target.value })}
              />
            </div>
            <div className="col-6 mb-3">
              <label className="form-label">Max score</label>
              <input
                type="number"
                step="any"
                className="form-control"
                value={form.maxScore}
                onChange={(e) => setForm({ ...form, maxScore: e.target.value })}
              />
            </div>
          </div>
          <div className="mb-3">
            <label className="form-label">Assessed on</label>
            <input
              type="date"
              className="form-control"
              value={form.assessedOn}
              onChange={(e) => setForm({ ...form, assessedOn: e.target.value })}
            />
          </div>
          <div className="mb-1">
            <label className="form-label">Remarks</label>
            <textarea
              className="form-control"
              rows="2"
              value={form.remarks}
              onChange={(e) => setForm({ ...form, remarks: e.target.value })}
            />
          </div>
        </form>
      </Modal>
    </div>
  );
}
