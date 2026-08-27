import { useEffect, useState, useCallback, useRef } from 'react';
import { api, downloadFile } from '../../api';
import {
  Spinner,
  ErrorAlert,
  EmptyState,
  StatusBadge,
  formatDate,
  formatBytes,
} from '../../components/ui.jsx';

const TABS = [
  { key: 'overview', label: 'Overview', icon: 'grid' },
  { key: 'assessments', label: 'Assessments', icon: 'clipboard-data' },
  { key: 'assignments', label: 'Assignments', icon: 'journal-text' },
  { key: 'tasks', label: 'Tasks', icon: 'check2-square' },
  { key: 'documents', label: 'Documents', icon: 'folder' },
  { key: 'forms', label: 'Forms', icon: 'ui-checks-grid' },
];

export default function MenteeDashboard() {
  const [tab, setTab] = useState('overview');
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    (async () => {
      try {
        setProfile(await api.get('/api/mentee/profile'));
      } catch (err) {
        setError(err);
      } finally {
        setLoading(false);
      }
    })();
  }, []);

  if (loading) return <Spinner />;
  if (error) return <ErrorAlert error={error} />;

  return (
    <div>
      <div className="card border-0 shadow-sm mb-4">
        <div className="card-body">
          <h1 className="h3 mb-1">Welcome, {profile?.fullName}</h1>
          <p className="text-muted mb-0">
            {profile?.program && (
              <span className="me-3">
                <i className="bi bi-book me-1"></i>
                {profile.program}
                {profile.year ? ` · Year ${profile.year}` : ''}
              </span>
            )}
            {profile?.mentorName && (
              <span>
                <i className="bi bi-person-badge me-1"></i>Mentor: {profile.mentorName}
              </span>
            )}
          </p>
        </div>
      </div>

      <ul className="nav nav-tabs mb-3">
        {TABS.map((t) => (
          <li className="nav-item" key={t.key}>
            <button className={`nav-link ${tab === t.key ? 'active' : ''}`} onClick={() => setTab(t.key)}>
              <i className={`bi bi-${t.icon} me-1`}></i>
              {t.label}
            </button>
          </li>
        ))}
      </ul>

      {tab === 'overview' && <Overview />}
      {tab === 'assessments' && <Assessments />}
      {tab === 'assignments' && <Assignments />}
      {tab === 'tasks' && <Tasks />}
      {tab === 'documents' && <Documents />}
      {tab === 'forms' && <Forms />}
    </div>
  );
}

function useList(path) {
  const [items, setItems] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const load = useCallback(async () => {
    setLoading(true);
    try {
      setItems(await api.get(path));
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, [path]);
  useEffect(() => {
    load();
  }, [load]);
  return { items, loading, error, setError, reload: load };
}

function Overview() {
  const a = useList('/api/mentee/assignments');
  const t = useList('/api/mentee/tasks');
  if (a.loading || t.loading) return <Spinner />;

  const pending = a.items.filter((x) => x.status !== 'GRADED');
  const openTasks = t.items.filter((x) => x.status !== 'DONE');

  return (
    <div className="row g-3">
      <div className="col-md-6">
        <div className="card border-0 shadow-sm h-100">
          <div className="card-body">
            <h2 className="h6 text-muted">
              <i className="bi bi-journal-text me-2"></i>Assignments to work on
            </h2>
            {pending.length === 0 ? (
              <p className="text-muted mb-0">You're all caught up. 🎉</p>
            ) : (
              <ul className="list-group list-group-flush">
                {pending.map((x) => (
                  <li key={x.id} className="list-group-item d-flex justify-content-between px-0">
                    <span>{x.title}</span>
                    <StatusBadge value={x.status} />
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      </div>
      <div className="col-md-6">
        <div className="card border-0 shadow-sm h-100">
          <div className="card-body">
            <h2 className="h6 text-muted">
              <i className="bi bi-check2-square me-2"></i>Open tasks
            </h2>
            {openTasks.length === 0 ? (
              <p className="text-muted mb-0">No open tasks.</p>
            ) : (
              <ul className="list-group list-group-flush">
                {openTasks.map((x) => (
                  <li key={x.id} className="list-group-item d-flex justify-content-between px-0">
                    <span>{x.title}</span>
                    <StatusBadge value={x.priority} />
                  </li>
                ))}
              </ul>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function Assessments() {
  const { items, loading, error, setError } = useList('/api/mentee/assessments');
  if (loading) return <Spinner />;
  return (
    <div>
      <ErrorAlert error={error} onClose={() => setError(null)} />
      {items.length === 0 ? (
        <EmptyState icon="clipboard-data">No assessments recorded yet.</EmptyState>
      ) : (
        <div className="card border-0 shadow-sm">
          <div className="table-responsive">
            <table className="table mb-0 align-middle">
              <thead className="table-light">
                <tr>
                  <th>Title</th>
                  <th>Type</th>
                  <th>Score</th>
                  <th>Date</th>
                  <th>Remarks</th>
                </tr>
              </thead>
              <tbody>
                {items.map((a) => (
                  <tr key={a.id}>
                    <td className="fw-medium">{a.title}</td>
                    <td>{a.type || '—'}</td>
                    <td>
                      {a.score != null ? (
                        <>
                          {a.score}
                          {a.maxScore != null && <span className="text-muted"> / {a.maxScore}</span>}
                        </>
                      ) : (
                        '—'
                      )}
                    </td>
                    <td className="text-muted small">{formatDate(a.assessedOn)}</td>
                    <td className="text-muted small">{a.remarks || '—'}</td>
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

function Assignments() {
  const { items, loading, error, setError, reload } = useList('/api/mentee/assignments');
  const [busyId, setBusyId] = useState(null);
  const fileRefs = useRef({});

  async function submit(a) {
    const file = fileRefs.current[a.id]?.files?.[0];
    if (!file) {
      setError('Choose a file to submit first.');
      return;
    }
    setBusyId(a.id);
    setError(null);
    try {
      const fd = new FormData();
      fd.append('file', file);
      await api.postForm(`/api/mentee/assignments/${a.id}/submit`, fd);
      await reload();
    } catch (err) {
      setError(err);
    } finally {
      setBusyId(null);
    }
  }

  async function download(a) {
    try {
      await downloadFile(`/api/mentee/documents/${a.submissionDocId}/download`, a.submissionName);
    } catch (err) {
      setError(err);
    }
  }

  if (loading) return <Spinner />;

  return (
    <div>
      <ErrorAlert error={error} onClose={() => setError(null)} />
      {items.length === 0 ? (
        <EmptyState icon="journal-text">No assignments yet.</EmptyState>
      ) : (
        <div className="row g-3">
          {items.map((a) => (
            <div className="col-12" key={a.id}>
              <div className="card border-0 shadow-sm">
                <div className="card-body">
                  <div className="d-flex align-items-center gap-2 mb-1">
                    <h3 className="h6 mb-0">{a.title}</h3>
                    <StatusBadge value={a.status} />
                  </div>
                  {a.description && <p className="text-muted small mb-2">{a.description}</p>}
                  <div className="small text-muted mb-2">
                    {a.dueDate && (
                      <span className="me-3">
                        <i className="bi bi-calendar-event me-1"></i>Due {formatDate(a.dueDate)}
                      </span>
                    )}
                    {a.grade != null && (
                      <span className="text-success fw-semibold">
                        <i className="bi bi-award me-1"></i>Grade: {a.grade}
                      </span>
                    )}
                  </div>
                  {a.feedback && (
                    <div className="alert alert-light border py-2 small">
                      <strong>Feedback:</strong> {a.feedback}
                    </div>
                  )}

                  {a.submissionDocId && (
                    <button className="btn btn-sm btn-outline-primary mb-2" onClick={() => download(a)}>
                      <i className="bi bi-download me-1"></i>
                      {a.submissionName || 'Your submission'}
                    </button>
                  )}

                  {a.status !== 'GRADED' && (
                    <div className="input-group input-group-sm" style={{ maxWidth: '30rem' }}>
                      <input
                        type="file"
                        className="form-control"
                        ref={(el) => (fileRefs.current[a.id] = el)}
                      />
                      <button
                        className="btn btn-primary"
                        onClick={() => submit(a)}
                        disabled={busyId === a.id}
                      >
                        {busyId === a.id ? (
                          <span className="spinner-border spinner-border-sm"></span>
                        ) : (
                          <>
                            <i className="bi bi-upload me-1"></i>
                            {a.submissionDocId ? 'Replace' : 'Submit'}
                          </>
                        )}
                      </button>
                    </div>
                  )}
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

function Tasks() {
  const { items, loading, error, setError, reload } = useList('/api/mentee/tasks');
  const STATUSES = ['TODO', 'IN_PROGRESS', 'DONE'];

  async function changeStatus(t, status) {
    try {
      await api.put(`/api/mentee/tasks/${t.id}/status`, { status });
      await reload();
    } catch (err) {
      setError(err);
    }
  }

  if (loading) return <Spinner />;

  return (
    <div>
      <ErrorAlert error={error} onClose={() => setError(null)} />
      {items.length === 0 ? (
        <EmptyState icon="check2-square">No tasks assigned.</EmptyState>
      ) : (
        <div className="card border-0 shadow-sm">
          <div className="table-responsive">
            <table className="table align-middle mb-0">
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

function Documents() {
  const { items, loading, error, setError, reload } = useList('/api/mentee/documents');
  const [category, setCategory] = useState('');
  const [uploading, setUploading] = useState(false);
  const fileRef = useRef(null);

  async function upload(e) {
    e.preventDefault();
    const file = fileRef.current?.files?.[0];
    if (!file) return;
    setUploading(true);
    setError(null);
    try {
      const fd = new FormData();
      fd.append('file', file);
      if (category) fd.append('category', category);
      await api.postForm('/api/mentee/documents', fd);
      fileRef.current.value = '';
      setCategory('');
      await reload();
    } catch (err) {
      setError(err);
    } finally {
      setUploading(false);
    }
  }

  async function download(d) {
    try {
      await downloadFile(`/api/mentee/documents/${d.id}/download`, d.originalName);
    } catch (err) {
      setError(err);
    }
  }

  async function remove(d) {
    if (!confirm(`Delete "${d.originalName}"?`)) return;
    try {
      await api.del(`/api/mentee/documents/${d.id}`);
      await reload();
    } catch (err) {
      setError(err);
    }
  }

  return (
    <div>
      <ErrorAlert error={error} onClose={() => setError(null)} />
      <form className="card border-0 shadow-sm mb-4" onSubmit={upload}>
        <div className="card-body">
          <div className="row g-2 align-items-end">
            <div className="col-md-5">
              <label className="form-label small">File</label>
              <input ref={fileRef} type="file" className="form-control" required />
            </div>
            <div className="col-md-4">
              <label className="form-label small">Category (optional)</label>
              <input
                className="form-control"
                value={category}
                onChange={(e) => setCategory(e.target.value)}
              />
            </div>
            <div className="col-md-3">
              <button className="btn btn-primary w-100" disabled={uploading}>
                {uploading ? (
                  <span className="spinner-border spinner-border-sm me-2"></span>
                ) : (
                  <i className="bi bi-upload me-2"></i>
                )}
                Upload
              </button>
            </div>
          </div>
        </div>
      </form>

      {loading ? (
        <Spinner />
      ) : items.length === 0 ? (
        <EmptyState icon="folder">No documents yet.</EmptyState>
      ) : (
        <div className="card border-0 shadow-sm">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th>Name</th>
                  <th>Category</th>
                  <th>Size</th>
                  <th>Uploaded by</th>
                  <th>Date</th>
                  <th className="text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {items.map((d) => (
                  <tr key={d.id}>
                    <td className="fw-medium">
                      <i className="bi bi-file-earmark me-2 text-muted"></i>
                      {d.originalName}
                    </td>
                    <td>
                      {d.category ? (
                        <span className="badge bg-info-subtle text-info-emphasis">{d.category}</span>
                      ) : (
                        '—'
                      )}
                    </td>
                    <td className="text-muted small">{formatBytes(d.size)}</td>
                    <td className="text-muted small">{d.uploadedByName || '—'}</td>
                    <td className="text-muted small">{formatDate(d.uploadedAt)}</td>
                    <td className="text-end text-nowrap">
                      <button className="btn btn-sm btn-outline-primary me-1" onClick={() => download(d)}>
                        <i className="bi bi-download"></i>
                      </button>
                      {/* Mentees may only delete documents they uploaded themselves. */}
                      <button className="btn btn-sm btn-outline-danger" onClick={() => remove(d)}>
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
    </div>
  );
}

function Forms() {
  const { items, loading, error, setError } = useList('/api/mentee/forms');
  if (loading) return <Spinner />;
  return (
    <div>
      <ErrorAlert error={error} onClose={() => setError(null)} />
      {items.length === 0 ? (
        <EmptyState icon="ui-checks-grid">No forms assigned to you.</EmptyState>
      ) : (
        <div className="row g-3">
          {items.map((f) => (
            <div className="col-md-6 col-lg-4" key={f.id}>
              <div className="card h-100 border-0 shadow-sm">
                <div className="card-body d-flex flex-column">
                  <h5 className="card-title">{f.title}</h5>
                  {f.description && <p className="text-muted small flex-grow-1">{f.description}</p>}
                  <a
                    href={f.responderUri}
                    target="_blank"
                    rel="noreferrer"
                    className="btn btn-primary mt-auto"
                  >
                    <i className="bi bi-box-arrow-up-right me-2"></i>Fill out form
                  </a>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
