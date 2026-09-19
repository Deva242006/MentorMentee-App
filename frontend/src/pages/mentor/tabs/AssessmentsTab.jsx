import { useEffect, useState, useCallback } from 'react';
import { api } from '../../../api';
import { Spinner, ErrorAlert, EmptyState, Modal, formatDate } from '../../../components/ui.jsx';

export default function AssessmentsTab({ menteeId }) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [scoring, setScoring] = useState(null);
  const [scoreForm, setScoreForm] = useState({ score: '', remarks: '' });
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

  function openScore(a) {
    setScoring(a);
    setScoreForm({
      score: a.score ?? '',
      remarks: a.remarks || '',
    });
    setFormError(null);
  }

  async function submitScore(e) {
    e.preventDefault();
    setSaving(true);
    setFormError(null);
    try {
      await api.put(`/api/mentor/assessments/${scoring.id}/mentees/${menteeId}/score`, {
        score: scoreForm.score === '' ? null : Number(scoreForm.score),
        remarks: scoreForm.remarks || null,
      });
      setScoring(null);
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
        <h2 className="h5 mb-0">Assessments</h2>
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
                      <button className="btn btn-sm btn-outline-success" onClick={() => openScore(a)}>
                        <i className="bi bi-pencil me-1"></i>Edit Score
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Score Modal */}
      <Modal
        show={!!scoring}
        title={scoring ? `Score: ${scoring.title}` : ''}
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
