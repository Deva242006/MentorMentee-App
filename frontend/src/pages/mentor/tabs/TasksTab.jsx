import { useEffect, useState, useCallback } from 'react';
import { api } from '../../../api';
import { Spinner, ErrorAlert, EmptyState, StatusBadge, formatDate } from '../../../components/ui.jsx';

const STATUSES = ['TODO', 'IN_PROGRESS', 'DONE'];

export default function TasksTab({ menteeId }) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

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

  async function changeStatus(t, status) {
    try {
      await api.put(`/api/mentor/tasks/${t.id}/status`, { status });
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
