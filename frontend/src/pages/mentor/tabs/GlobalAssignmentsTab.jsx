import React, { useEffect, useState, useCallback } from 'react';
import { api, downloadFile } from '../../../api';
import {
  Spinner,
  ErrorAlert,
  EmptyState,
  Modal,
  StatusBadge,
  formatDate,
} from '../../../components/ui.jsx';

const empty = { title: '', description: '', dueDate: '' };

export default function GlobalAssignmentsTab() {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [showCreate, setShowCreate] = useState(false);
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(empty);
  
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState(null);

  const [expandedId, setExpandedId] = useState(null);
  const [progressDetails, setProgressDetails] = useState(null);
  const [loadingProgress, setLoadingProgress] = useState(false);

  const [grading, setGrading] = useState(null); // { assignmentId, menteeId, menteeName, grade, feedback }
  const [gradeForm, setGradeForm] = useState({ grade: '', feedback: '' });

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await api.get('/api/mentor/assignments'));
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

  function openEdit(a) {
    setEditing(a);
    setForm({
      title: a.title || '',
      description: a.description || '',
      dueDate: a.dueDate || '',
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
    };
    try {
      if (editing) await api.put(`/api/mentor/assignments/${editing.id}`, body);
      else await api.post(`/api/mentor/assignments`, body);
      setShowCreate(false);
      await load();
    } catch (err) {
      setFormError(err);
    } finally {
      setSaving(false);
    }
  }

  async function remove(a) {
    if (!window.confirm(`Delete assignment "${a.title}" for all mentees?`)) return;
    try {
      await api.del(`/api/mentor/assignments/${a.id}`);
      await load();
    } catch (err) {
      setError(err);
    }
  }

  async function toggleExpand(a) {
    if (expandedId === a.id) {
      setExpandedId(null);
      setProgressDetails(null);
    } else {
      setExpandedId(a.id);
      setLoadingProgress(true);
      try {
        const details = await api.get(`/api/mentor/assignments/${a.id}/progress`);
        setProgressDetails(details.rows);
      } catch (err) {
        setError(err);
      } finally {
        setLoadingProgress(false);
      }
    }
  }

  async function download(docId, name) {
    try {
      await downloadFile(`/api/mentor/documents/${docId}/download`, name);
    } catch (err) {
      setError(err);
    }
  }

  function openGrade(aId, row) {
    setGrading({ assignmentId: aId, menteeId: row.menteeId, menteeName: row.menteeName });
    setGradeForm({ grade: row.grade ?? '', feedback: row.feedback || '' });
    setFormError(null);
  }

  async function submitGrade(e) {
    e.preventDefault();
    setSaving(true);
    setFormError(null);
    try {
      await api.put(`/api/mentor/assignments/${grading.assignmentId}/mentees/${grading.menteeId}/grade`, {
        grade: gradeForm.grade === '' ? null : Number(gradeForm.grade),
        feedback: gradeForm.feedback || null,
      });
      // reload progress
      const details = await api.get(`/api/mentor/assignments/${grading.assignmentId}/progress`);
      setProgressDetails(details.rows);
      setGrading(null);
      // also reload main list to update aggregate counts
      await load();
    } catch (err) {
      setFormError(err);
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <Spinner />;

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h2 className="h5 mb-0">Cohort Assignments</h2>
        <button className="btn btn-sm btn-primary" onClick={openCreate}>
          <i className="bi bi-plus-lg me-1"></i>New assignment
        </button>
      </div>

      <ErrorAlert error={error} onClose={() => setError(null)} />

      {items.length === 0 ? (
        <EmptyState icon="journal-text">No assignments yet.</EmptyState>
      ) : (
        <div className="card border-0 shadow-sm">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th>Assignment</th>
                  <th>Due Date</th>
                  <th>Submissions</th>
                  <th className="text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {items.map((a) => (
                  <React.Fragment key={a.id}>
                    <tr>
                      <td>
                        <div className="fw-medium">
                          <button className="btn btn-link p-0 text-decoration-none text-dark fw-bold me-2" onClick={() => toggleExpand(a)}>
                            <i className={`bi bi-chevron-${expandedId === a.id ? 'down' : 'right'}`}></i>
                          </button>
                          {a.title}
                        </div>
                        {a.description && <div className="text-muted small ms-4 text-truncate" style={{maxWidth: '250px'}}>{a.description}</div>}
                      </td>
                      <td className="text-muted small">{formatDate(a.dueDate)}</td>
                      <td>
                        <div className="small">
                          <span className="text-success fw-bold">{a.submitted}</span> submitted,{' '}
                          <span className="text-secondary">{a.pending}</span> pending
                        </div>
                        <div className="progress mt-1" style={{ height: '5px' }}>
                          <div className="progress-bar bg-success" style={{ width: `${(a.submitted / Math.max(a.total, 1)) * 100}%` }}></div>
                        </div>
                      </td>
                      <td className="text-end text-nowrap">
                        <button className="btn btn-sm btn-outline-secondary me-1" onClick={() => openEdit(a)}>
                          <i className="bi bi-pencil"></i>
                        </button>
                        <button className="btn btn-sm btn-outline-danger" onClick={() => remove(a)}>
                          <i className="bi bi-trash"></i>
                        </button>
                      </td>
                    </tr>
                    {expandedId === a.id && (
                      <tr className="bg-light">
                        <td colSpan="4">
                          <div className="p-3">
                            <h6 className="mb-2">Mentee Submissions</h6>
                            {loadingProgress ? (
                              <Spinner />
                            ) : progressDetails && progressDetails.length > 0 ? (
                              <div className="row g-3">
                                {progressDetails.map(p => (
                                  <div className="col-md-6 col-lg-4" key={p.menteeId}>
                                    <div className="card shadow-sm border-0 h-100">
                                      <div className="card-body p-3">
                                        <div className="d-flex justify-content-between align-items-start mb-2">
                                          <div className="fw-medium text-truncate">{p.menteeName}</div>
                                          <StatusBadge value={p.status} />
                                        </div>
                                        {p.submissionDocId ? (
                                          <button className="btn btn-sm btn-outline-primary w-100 mb-2 text-truncate" onClick={() => download(p.submissionDocId, p.submissionName)}>
                                            <i className="bi bi-download me-1"></i> {p.submissionName}
                                          </button>
                                        ) : (
                                          <div className="text-muted small mb-2 text-center py-1">No submission</div>
                                        )}
                                        {p.grade != null && (
                                          <div className="small mb-2">
                                            <strong>Grade:</strong> <span className="text-success">{p.grade}</span>
                                          </div>
                                        )}
                                        {p.feedback && (
                                          <div className="small text-muted mb-2 fst-italic">"{p.feedback}"</div>
                                        )}
                                        <button className="btn btn-sm btn-light border w-100 mt-auto" onClick={() => openGrade(a.id, p)}>
                                          <i className="bi bi-award me-1"></i> Grade
                                        </button>
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

      {/* Create / Edit Modal */}
      <Modal
        show={showCreate}
        title={editing ? 'Edit Assignment' : 'New Assignment'}
        onClose={() => setShowCreate(false)}
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setShowCreate(false)}>
              Cancel
            </button>
            <button className="btn btn-primary" form="assign-form" disabled={saving}>
              {saving && <span className="spinner-border spinner-border-sm me-2"></span>}
              Save
            </button>
          </>
        }
      >
        <form id="assign-form" onSubmit={save}>
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
              rows="3"
              value={form.description}
              onChange={(e) => setForm({ ...form, description: e.target.value })}
            />
          </div>
          <div className="mb-1">
            <label className="form-label">Due date</label>
            <input
              type="date"
              className="form-control"
              value={form.dueDate}
              onChange={(e) => setForm({ ...form, dueDate: e.target.value })}
            />
          </div>
        </form>
      </Modal>

      {/* Grade Modal */}
      <Modal
        show={!!grading}
        title={grading ? `Grade ${grading.menteeName}` : ''}
        onClose={() => setGrading(null)}
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setGrading(null)}>
              Cancel
            </button>
            <button className="btn btn-success" form="grade-form" disabled={saving}>
              {saving && <span className="spinner-border spinner-border-sm me-2"></span>}
              Save grade
            </button>
          </>
        }
      >
        <form id="grade-form" onSubmit={submitGrade}>
          <ErrorAlert error={formError} onClose={() => setFormError(null)} />
          <div className="mb-3">
            <label className="form-label">Grade</label>
            <input
              type="number"
              step="any"
              className="form-control"
              value={gradeForm.grade}
              onChange={(e) => setGradeForm({ ...gradeForm, grade: e.target.value })}
            />
          </div>
          <div className="mb-1">
            <label className="form-label">Feedback</label>
            <textarea
              className="form-control"
              rows="3"
              value={gradeForm.feedback}
              onChange={(e) => setGradeForm({ ...gradeForm, feedback: e.target.value })}
            />
          </div>
        </form>
      </Modal>
    </div>
  );
}
