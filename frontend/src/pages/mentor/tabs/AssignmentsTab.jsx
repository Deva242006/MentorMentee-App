import { useEffect, useState, useCallback } from 'react';
import { api, downloadFile } from '../../../api';
import { Spinner, ErrorAlert, EmptyState, Modal, StatusBadge, formatDate } from '../../../components/ui.jsx';

export default function AssignmentsTab({ menteeId }) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [grading, setGrading] = useState(null); // assignment being graded
  const [gradeForm, setGradeForm] = useState({ grade: '', feedback: '' });
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await api.get(`/api/mentor/mentees/${menteeId}/assignments`));
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, [menteeId]);

  useEffect(() => {
    load();
  }, [load]);

  function openGrade(a) {
    setGrading(a);
    setGradeForm({ grade: a.grade ?? '', feedback: a.feedback || '' });
    setFormError(null);
  }

  async function submitGrade(e) {
    e.preventDefault();
    setSaving(true);
    setFormError(null);
    try {
      await api.put(`/api/mentor/assignments/${grading.id}/mentees/${menteeId}/grade`, {
        grade: gradeForm.grade === '' ? null : Number(gradeForm.grade),
        feedback: gradeForm.feedback || null,
      });
      setGrading(null);
      await load();
    } catch (err) {
      setFormError(err);
    } finally {
      setSaving(false);
    }
  }

  async function download(a) {
    try {
      await downloadFile(`/api/mentor/documents/${a.submissionDocId}/download`, a.submissionName);
    } catch (err) {
      setError(err);
    }
  }

  if (loading) return <Spinner />;

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h2 className="h5 mb-0">Assignments</h2>
      </div>

      <ErrorAlert error={error} onClose={() => setError(null)} />

      {items.length === 0 ? (
        <EmptyState icon="journal-text">No assignments yet.</EmptyState>
      ) : (
        <div className="row g-3">
          {items.map((a) => (
            <div className="col-12" key={a.id}>
              <div className="card border-0 shadow-sm">
                <div className="card-body">
                  <div className="d-flex justify-content-between align-items-start">
                    <div className="flex-grow-1">
                      <div className="d-flex align-items-center gap-2 mb-1">
                        <h3 className="h6 mb-0">{a.title}</h3>
                        <StatusBadge value={a.status} />
                      </div>
                      {a.description && <p className="text-muted small mb-2">{a.description}</p>}
                      <div className="small text-muted">
                        {a.dueDate && (
                          <span className="me-3">
                            <i className="bi bi-calendar-event me-1"></i>Due {formatDate(a.dueDate)}
                          </span>
                        )}
                        {a.submittedAt && (
                          <span className="me-3">
                            <i className="bi bi-upload me-1"></i>Submitted {formatDate(a.submittedAt)}
                          </span>
                        )}
                        {a.grade != null && (
                          <span className="me-3 text-success fw-semibold">
                            <i className="bi bi-award me-1"></i>Grade: {a.grade}
                          </span>
                        )}
                      </div>
                      {a.submissionDocId && (
                        <button
                          className="btn btn-sm btn-outline-primary mt-2"
                          onClick={() => download(a)}
                        >
                          <i className="bi bi-download me-1"></i>
                          {a.submissionName || 'Submission'}
                        </button>
                      )}
                      {a.feedback && (
                        <div className="alert alert-light border mt-2 mb-0 py-2 small">
                          <strong>Feedback:</strong> {a.feedback}
                        </div>
                      )}
                    </div>
                    <div className="text-nowrap ms-3">
                      <button className="btn btn-sm btn-outline-success" onClick={() => openGrade(a)}>
                        <i className="bi bi-award me-1"></i>Grade
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Grade Modal */}
      <Modal
        show={!!grading}
        title={grading ? `Grade: ${grading.title}` : ''}
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
