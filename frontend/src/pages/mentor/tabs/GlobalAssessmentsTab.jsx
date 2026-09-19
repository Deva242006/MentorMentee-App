import React, { useEffect, useState, useCallback } from 'react';
import { api } from '../../../api';
import { Spinner, ErrorAlert, EmptyState, Modal, formatDate } from '../../../components/ui.jsx';

const empty = { title: '', type: '', score: '', maxScore: '', assessedOn: '', remarks: '' };

export default function GlobalAssessmentsTab() {
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

  const [scoring, setScoring] = useState(null); // { assessmentId, menteeId, menteeName, score, remarks }
  const [scoreForm, setScoreForm] = useState({ score: '', remarks: '' });

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await api.get('/api/mentor/assessments'));
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
      type: a.type || '',
      maxScore: a.maxScore ?? '',
      assessedOn: a.assessedOn || '',
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
      type: form.type || null,
      maxScore: form.maxScore === '' ? null : Number(form.maxScore),
      assessedOn: form.assessedOn || null,
    };
    try {
      if (editing) await api.put(`/api/mentor/assessments/${editing.id}`, body);
      else await api.post(`/api/mentor/assessments`, body);
      setShowCreate(false);
      await load();
    } catch (err) {
      setFormError(err);
    } finally {
      setSaving(false);
    }
  }

  async function remove(a) {
    if (!window.confirm(`Delete assessment "${a.title}" for all mentees?`)) return;
    try {
      await api.del(`/api/mentor/assessments/${a.id}`);
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
        const details = await api.get(`/api/mentor/assessments/${a.id}/progress`);
        setProgressDetails(details.rows);
      } catch (err) {
        setError(err);
      } finally {
        setLoadingProgress(false);
      }
    }
  }

  function openScore(aId, row) {
    setScoring({ assessmentId: aId, menteeId: row.menteeId, menteeName: row.menteeName });
    setScoreForm({ score: row.score ?? '', remarks: row.remarks || '' });
    setFormError(null);
  }

  async function submitScore(e) {
    e.preventDefault();
    setSaving(true);
    setFormError(null);
    try {
      await api.put(`/api/mentor/assessments/${scoring.assessmentId}/mentees/${scoring.menteeId}/score`, {
        score: scoreForm.score === '' ? null : Number(scoreForm.score),
        remarks: scoreForm.remarks || null,
      });
      // reload progress
      const details = await api.get(`/api/mentor/assessments/${scoring.assessmentId}/progress`);
      setProgressDetails(details.rows);
      setScoring(null);
      await load(); // refresh aggregate
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
        <h2 className="h5 mb-0">Cohort Assessments</h2>
        <button className="btn btn-sm btn-primary" onClick={openCreate}>
          <i className="bi bi-plus-lg me-1"></i>New assessment
        </button>
      </div>

      <ErrorAlert error={error} onClose={() => setError(null)} />

      {items.length === 0 ? (
        <EmptyState icon="clipboard-data">No assessments created yet.</EmptyState>
      ) : (
        <div className="card border-0 shadow-sm">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th>Assessment</th>
                  <th>Type</th>
                  <th>Date</th>
                  <th>Scored</th>
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
                        {a.maxScore != null && <div className="text-muted small ms-4">Max Score: {a.maxScore}</div>}
                      </td>
                      <td>{a.type || '—'}</td>
                      <td className="text-muted small">{formatDate(a.assessedOn)}</td>
                      <td>
                        <div className="small">
                          <span className="text-success fw-bold">{a.scored}</span> scored,{' '}
                          <span className="text-secondary">{a.pending}</span> pending
                        </div>
                        <div className="progress mt-1" style={{ height: '5px' }}>
                          <div className="progress-bar bg-success" style={{ width: `${(a.scored / Math.max(a.total, 1)) * 100}%` }}></div>
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
                        <td colSpan="5">
                          <div className="p-3">
                            <h6 className="mb-2">Mentee Scores</h6>
                            {loadingProgress ? (
                              <Spinner />
                            ) : progressDetails && progressDetails.length > 0 ? (
                              <div className="row g-2">
                                {progressDetails.map(p => (
                                  <div className="col-md-6 col-lg-3" key={p.menteeId}>
                                    <div className="card shadow-sm border-0 h-100">
                                      <div className="card-body p-2 d-flex flex-column">
                                        <div className="fw-medium text-truncate mb-2">{p.menteeName}</div>
                                        <div className="mb-2">
                                          {p.score != null ? (
                                            <span className="badge bg-success">Score: {p.score}</span>
                                          ) : (
                                            <span className="badge bg-secondary">Unscored</span>
                                          )}
                                        </div>
                                        {p.remarks && <div className="small text-muted mb-2 fst-italic">"{p.remarks}"</div>}
                                        <button className="btn btn-sm btn-light border w-100 mt-auto" onClick={() => openScore(a.id, p)}>
                                          <i className="bi bi-pencil me-1"></i> Edit Score
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
        title={editing ? 'Edit Assessment' : 'New Assessment'}
        onClose={() => setShowCreate(false)}
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setShowCreate(false)}>
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
              <label className="form-label">Max score</label>
              <input
                type="number"
                step="any"
                className="form-control"
                value={form.maxScore}
                onChange={(e) => setForm({ ...form, maxScore: e.target.value })}
              />
            </div>
            <div className="col-6 mb-3">
              <label className="form-label">Assessed on</label>
              <input
                type="date"
                className="form-control"
                value={form.assessedOn}
                onChange={(e) => setForm({ ...form, assessedOn: e.target.value })}
              />
            </div>
          </div>
        </form>
      </Modal>

      {/* Score Modal */}
      <Modal
        show={!!scoring}
        title={scoring ? `Score ${scoring.menteeName}` : ''}
        onClose={() => setScoring(null)}
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setScoring(null)}>
              Cancel
            </button>
            <button className="btn btn-success" form="score-form" disabled={saving}>
              {saving && <span className="spinner-border spinner-border-sm me-2"></span>}
              Save score
            </button>
          </>
        }
      >
        <form id="score-form" onSubmit={submitScore}>
          <ErrorAlert error={formError} onClose={() => setFormError(null)} />
          <div className="mb-3">
            <label className="form-label">Score</label>
            <input
              type="number"
              step="any"
              className="form-control"
              value={scoreForm.score}
              onChange={(e) => setScoreForm({ ...scoreForm, score: e.target.value })}
            />
          </div>
          <div className="mb-1">
            <label className="form-label">Remarks</label>
            <textarea
              className="form-control"
              rows="3"
              value={scoreForm.remarks}
              onChange={(e) => setScoreForm({ ...scoreForm, remarks: e.target.value })}
            />
          </div>
        </form>
      </Modal>
    </div>
  );
}
