import { useEffect, useState, useCallback } from 'react';
import { api } from '../../../api';
import {
  Spinner,
  ErrorAlert,
  EmptyState,
  Modal,
  StatusBadge,
  formatDate,
} from '../../../components/ui.jsx';

const PRIORITIES = ['LOW', 'MEDIUM', 'HIGH'];
const STATUSES = ['TODO', 'IN_PROGRESS', 'DONE'];
const empty = { title: '', description: '', dueDate: '', priority: 'MEDIUM' };

export default function TasksTab({ menteeId }) {
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
      setItems(await api.get(`/api/mentor/mentees/${menteeId}/tasks`));
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

  function openEdit(t) {
    setEditing(t);
    setForm({
      title: t.title || '',
      description: t.description || '',
      dueDate: t.dueDate || '',
      priority: t.priority || 'MEDIUM',
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
      description: form.description || null,
      dueDate: form.dueDate || null,
      priority: form.priority,
    };
    try {
      if (editing) await api.put(`/api/mentor/tasks/${editing.id}`, body);
      else await api.post(`/api/mentor/mentees/${menteeId}/tasks`, body);
      setShow(false);
      await load();
    } catch (err) {
      setFormError(err);
    } finally {
      setSaving(false);
    }
  }

  async function changeStatus(t, status) {
    try {
      await api.put(`/api/mentor/tasks/${t.id}/status`, { status });
      await load();
    } catch (err) {
      setError(err);
    }
  }

  async function remove(t) {
    if (!confirm(`Delete task "${t.title}"?`)) return;
    try {
      await api.del(`/api/mentor/tasks/${t.id}`);
      await load();
    } catch (err) {
      setError(err);
    }
  }

  if (loading) return <Spinner />;

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h2 className="h5 mb-0">Tasks</h2>
        <button className="btn btn-sm btn-primary" onClick={openCreate}>
          <i className="bi bi-plus-lg me-1"></i>New task
        </button>
      </div>

      <ErrorAlert error={error} onClose={() => setError(null)} />

      {items.length === 0 ? (
        <EmptyState icon="check2-square">No tasks yet.</EmptyState>
      ) : (
        <div className="card border-0 shadow-sm">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th>Task</th>
                  <th>Priority</th>
                  <th>Due</th>
                  <th style={{ minWidth: '10rem' }}>Status</th>
                  <th className="text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {items.map((t) => (
                  <tr key={t.id}>
                    <td>
                      <div className="fw-medium">{t.title}</div>
                      {t.description && <div className="text-muted small">{t.description}</div>}
                    </td>
                    <td>
                      <StatusBadge value={t.priority} />
                    </td>
                    <td className="text-muted small">{formatDate(t.dueDate)}</td>
                    <td>
                      <select
                        className="form-select form-select-sm"
                        value={t.status}
                        onChange={(e) => changeStatus(t, e.target.value)}
                      >
                        {STATUSES.map((s) => (
                          <option key={s} value={s}>
                            {s.replace(/_/g, ' ')}
                          </option>
                        ))}
                      </select>
                    </td>
                    <td className="text-end text-nowrap">
                      <button className="btn btn-sm btn-outline-secondary me-1" onClick={() => openEdit(t)}>
                        <i className="bi bi-pencil"></i>
                      </button>
                      <button className="btn btn-sm btn-outline-danger" onClick={() => remove(t)}>
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
        title={editing ? 'Edit task' : 'New task'}
        onClose={() => setShow(false)}
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setShow(false)}>
              Cancel
            </button>
            <button className="btn btn-primary" form="task-form" disabled={saving}>
              {saving && <span className="spinner-border spinner-border-sm me-2"></span>}
              Save
            </button>
          </>
        }
      >
        <form id="task-form" onSubmit={save}>
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
            <label className="form-label">Description</label>
            <textarea
              className="form-control"
              rows="2"
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
            />
          </div>
          <div className="row">
            <div className="col-6 mb-1">
              <label className="form-label">Priority</label>
              <select
                className="form-select"
                value={form.priority}
                onChange={(e) => setForm({ ...form, priority: e.target.value })}
              >
                {PRIORITIES.map((p) => (
                  <option key={p} value={p}>
                    {p}
                  </option>
                ))}
              </select>
            </div>
            <div className="col-6 mb-1">
              <label className="form-label">Due date</label>
              <input
                type="date"
                className="form-control"
                value={form.dueDate}
                onChange={(e) => setForm({ ...form, dueDate: e.target.value })}
              />
            </div>
          </div>
        </form>
      </Modal>
    </div>
  );
}
