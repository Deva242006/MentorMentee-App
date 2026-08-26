import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { api } from '../../api';
import { Spinner, ErrorAlert, EmptyState } from '../../components/ui.jsx';

export default function MentorDashboard() {
  const [summaries, setSummaries] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    let cancelled = false;
    (async () => {
      try {
        const data = await api.get('/api/mentor/mentees');
        if (!cancelled) setSummaries(data);
      } catch (err) {
        if (!cancelled) setError(err);
      } finally {
        if (!cancelled) setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, []);

  if (loading) return <Spinner />;

  return (
    <div>
      <h1 className="h3 mb-3">My Mentees</h1>
      <ErrorAlert error={error} onClose={() => setError(null)} />

      {summaries.length === 0 ? (
        <EmptyState icon="people">
          No mentees assigned yet. An administrator assigns mentees to you.
        </EmptyState>
      ) : (
        <div className="row g-3">
          {summaries.map((s) => (
            <div className="col-md-6 col-lg-4" key={s.mentee.id}>
              <div className="card h-100 shadow-sm border-0">
                <div className="card-body">
                  <div className="d-flex justify-content-between align-items-start">
                    <div>
                      <h5 className="card-title mb-0">{s.mentee.fullName}</h5>
                      <p className="text-muted small mb-2">{s.mentee.email}</p>
                    </div>
                    {!s.mentee.active && (
                      <span className="badge bg-danger-subtle text-danger-emphasis">Disabled</span>
                    )}
                  </div>
                  {(s.mentee.program || s.mentee.year) && (
                    <p className="small text-muted mb-3">
                      <i className="bi bi-book me-1"></i>
                      {s.mentee.program}
                      {s.mentee.year ? ` · Year ${s.mentee.year}` : ''}
                    </p>
                  )}
                  <div className="row text-center g-2 mb-3">
                    <Metric label="Assess." value={s.assessments} />
                    <Metric label="Pending" value={s.pendingAssignments} highlight={s.pendingAssignments > 0} />
                    <Metric label="Tasks" value={s.openTasks} highlight={s.openTasks > 0} />
                    <Metric label="Docs" value={s.documents} />
                  </div>
                  <Link to={`/mentor/mentees/${s.mentee.id}`} className="btn btn-outline-primary w-100">
                    Open profile <i className="bi bi-arrow-right ms-1"></i>
                  </Link>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function Metric({ label, value, highlight }) {
  return (
    <div className="col-3">
      <div className={`fw-bold ${highlight ? 'text-danger' : ''}`}>{value}</div>
      <div className="text-muted" style={{ fontSize: '0.7rem' }}>
        {label}
      </div>
    </div>
  );
}
