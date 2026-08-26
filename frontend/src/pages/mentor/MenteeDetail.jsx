import { useEffect, useState, useCallback } from 'react';
import { useParams, Link } from 'react-router-dom';
import { api } from '../../api';
import { Spinner, ErrorAlert } from '../../components/ui.jsx';
import AssessmentsTab from './tabs/AssessmentsTab.jsx';
import AssignmentsTab from './tabs/AssignmentsTab.jsx';
import TasksTab from './tabs/TasksTab.jsx';
import DocumentsTab from './tabs/DocumentsTab.jsx';

const TABS = [
  { key: 'assessments', label: 'Assessments', icon: 'clipboard-data' },
  { key: 'assignments', label: 'Assignments', icon: 'journal-text' },
  { key: 'tasks', label: 'Tasks', icon: 'check2-square' },
  { key: 'documents', label: 'Documents', icon: 'folder' },
];

export default function MenteeDetail() {
  const { menteeId } = useParams();
  const [mentee, setMentee] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [tab, setTab] = useState('assessments');

  const loadMentee = useCallback(async () => {
    try {
      const data = await api.get(`/api/mentor/mentees/${menteeId}`);
      setMentee(data);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, [menteeId]);

  useEffect(() => {
    loadMentee();
  }, [loadMentee]);

  if (loading) return <Spinner />;
  if (error) return <ErrorAlert error={error} />;
  if (!mentee) return null;

  return (
    <div>
      <Link to="/mentor" className="btn btn-link ps-0 mb-2 text-decoration-none">
        <i className="bi bi-arrow-left me-1"></i>Back to mentees
      </Link>

      <div className="card border-0 shadow-sm mb-4">
        <div className="card-body">
          <div className="d-flex justify-content-between flex-wrap">
            <div>
              <h1 className="h3 mb-1">{mentee.fullName}</h1>
              <p className="text-muted mb-1">
                <i className="bi bi-envelope me-1"></i>
                {mentee.email}
                {mentee.phone && (
                  <span className="ms-3">
                    <i className="bi bi-telephone me-1"></i>
                    {mentee.phone}
                  </span>
                )}
              </p>
              {(mentee.program || mentee.year) && (
                <p className="text-muted mb-0">
                  <i className="bi bi-book me-1"></i>
                  {mentee.program}
                  {mentee.year ? ` · Year ${mentee.year}` : ''}
                </p>
              )}
            </div>
            {!mentee.active && (
              <span className="badge bg-danger-subtle text-danger-emphasis align-self-start">
                Account disabled
              </span>
            )}
          </div>
        </div>
      </div>

      <ul className="nav nav-tabs mb-3">
        {TABS.map((t) => (
          <li className="nav-item" key={t.key}>
            <button
              className={`nav-link ${tab === t.key ? 'active' : ''}`}
              onClick={() => setTab(t.key)}
            >
              <i className={`bi bi-${t.icon} me-1`}></i>
              {t.label}
            </button>
          </li>
        ))}
      </ul>

      {tab === 'assessments' && <AssessmentsTab menteeId={menteeId} />}
      {tab === 'assignments' && <AssignmentsTab menteeId={menteeId} />}
      {tab === 'tasks' && <TasksTab menteeId={menteeId} />}
      {tab === 'documents' && <DocumentsTab menteeId={menteeId} />}
    </div>
  );
}
