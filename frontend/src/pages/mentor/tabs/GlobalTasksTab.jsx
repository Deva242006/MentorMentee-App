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
const empty = { title: '', description: '', dueDate: '', priority: 'MEDIUM' };

export default function GlobalTasksTab() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  
  const [showCreate, setShowCreate] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(empty);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState(null);

  const [expandedTask, setExpandedTask] = useState(null);
  const [progressDetails, setProgressDetails] = useState(null);
  const [loadingProgress, setLoadingProgress] = useState(false);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await api.get('/api/mentor/tasks'));
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  function openCreate() {
    setEditing(null);
    setForm(empty);
    setFormError(null);
    setShowCreate(true);
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
    setShowCreate(true);
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
      else await api.post(`/api/mentor/tasks`, body);
      setShowCreate(false);
      await load();
    } catch (err) {
      setFormError(err);
    } finally {
      setSaving(false);
    }
  }

  async function remove(t) {
    if (!window.confirm(`Delete task "${t.title}" for all mentees?`)) return;
    try {
      await api.del(`/api/mentor/tasks/${t.id}`);
      await load();
    } catch (err) {
      setError(err);
    }
  }

  async function toggleExpand(t) {
    if (expandedTask === t.id) {
      setExpandedTask(null);
      setProgressDetails(null);
    } else {
      setExpandedTask(t.id);
      setLoadingProgress(true);
      try {
        const details = await api.get(`/api/mentor/tasks/${t.id}/progress`);
        setProgressDetails(details.rows);
      } catch (err) {
        setError(err);
      } finally {
        setLoadingProgress(false);
      }
    }
  }

  if (loading) return <Spinner />;

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h2 className="h5 mb-0">Cohort Tasks</h2>
        <button className="btn btn-sm btn-primary" onClick={openCreate}>
          <i className="bi bi-plus-lg me-1"></i>New task
        </button>
      </div>

      <ErrorAlert error={error} onClose={() => setError(null)} />

      {items.length === 0 ? (
        <EmptyState icon="check2-square">No tasks created yet.</EmptyState>
      ) : (
        <div className="card border-0 shadow-sm">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th>Task</th>
                  <th>Priority</th>
                  <th>Due</th>
                  <th>Progress</th>
                  <th className="text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {items.map((t) => (
                  <React.Fragment key={t.id}>
                    <tr>
                      <td>
                        <div className="fw-medium">
                          <button className="btn btn-link p-0 text-decoration-none text-dark fw-bold me-2" onClick={() => toggleExpand(t)}>
                            <i className={`bi bi-chevron-${expandedTask === t.id ? 'down' : 'right'}`}></i>
                          </button>
                          {t.title}
                        </div>
                        {t.description && <div className="text-muted small ms-4">{t.description}</div>}
                      </td>
                      <td>
                        <StatusBadge value={t.priority} />
                      </td>
                      <td className="text-muted small">{formatDate(t.dueDate)}</td>
                      <td>
                        <div className="small">
                          <span className="text-success fw-bold">{t.done}</span> done,{' '}
                          <span className="text-warning">{t.inProgress}</span> in progress,{' '}
                          <span className="text-secondary">{t.todo}</span> todo
                        </div>
                        <div className="progress mt-1" style={{ height: '5px' }}>
                          <div className="progress-bar bg-success" style={{ width: `${(t.done / Math.max(t.total, 1)) * 100}%` }}></div>
                          <div className="progress-bar bg-warning" style={{ width: `${(t.inProgress / Math.max(t.total, 1)) * 100}%` }}></div>
                        </div>
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
                    {expandedTask === t.id && (
                      <tr className="bg-light">
                        <td colSpan="5">
                          <div className="p-3">
                            <h6 className="mb-2">Mentee Progress</h6>
                            {loadingProgress ? (
                              <Spinner />
                            ) : progressDetails && progressDetails.length > 0 ? (
                              <div className="row g-2">
                                {progressDetails.map(p => (
                                  <div className="col-md-4 col-lg-3" key={p.menteeId}>
                                    <div className="card shadow-sm border-0 h-100">
                                      <div className="card-body p-2 d-flex flex-column">
                                        <div className="fw-medium small mb-1 text-truncate">{p.menteeName}</div>
                                        <div className="mt-auto d-flex justify-content-between align-items-center">
                                           <StatusBadge value={p.status} />
                                           {p.updatedAt && <span className="text-muted" style={{fontSize: '0.65rem'}}>{formatDate(p.updatedAt)}</span>}
                                        </div>
                                      </div>
                                    </div>
                                  </div>
                                ))}
                              </div>
                            ) : (
                              <div className="text-muted small">No mentees found.</div>
                            )}
                          </div>
                        </td>
                      </tr>
                    )}
                  </React.Fragment>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <Modal
        show={showCreate}
        title={editing ? 'Edit Task' : 'New Task'}
        onClose={() => setShowCreate(false)}
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setShowCreate(false)}>
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
