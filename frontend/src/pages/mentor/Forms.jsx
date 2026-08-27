import { useEffect, useState, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api } from '../../api';
import { useAuth } from '../../auth.jsx';
import { Spinner, ErrorAlert, EmptyState, Modal, formatDate } from '../../components/ui.jsx';
import FormBuilder from './forms/FormBuilder.jsx';
import ResponsesView from './forms/ResponsesView.jsx';

export default function MentorForms() {
  const { refresh } = useAuth();
  const [params, setParams] = useSearchParams();

  const [status, setStatus] = useState(null); // {enabled, connected, email}
  const [forms, setForms] = useState([]);
  const [mentees, setMentees] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [notice, setNotice] = useState(null);

  const [showBuilder, setShowBuilder] = useState(false);
  const [detail, setDetail] = useState(null); // FormDetailView
  const [busyId, setBusyId] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const [st, fs, ms] = await Promise.all([
        api.get('/api/mentor/google/status'),
        api.get('/api/mentor/forms'),
        api.get('/api/mentor/mentees'),
      ]);
      setStatus(st);
      setForms(fs);
      setMentees(ms.map((s) => s.mentee));
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  // Handle the OAuth callback redirect (?google=connected|error).
  useEffect(() => {
    const g = params.get('google');
    if (!g) return;
    if (g === 'connected') {
      setNotice({ type: 'success', text: 'Google account connected.' });
      refresh().catch(() => {});
      load();
    } else if (g === 'error') {
      setNotice({ type: 'danger', text: 'Google connection failed. Please try again.' });
    }
    params.delete('google');
    setParams(params, { replace: true });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function connect() {
    try {
      const { authUrl } = await api.get('/api/mentor/google/connect');
      window.location.href = authUrl; // leave the SPA for Google's consent screen
    } catch (err) {
      setError(err);
    }
  }

  async function disconnect() {
    if (!confirm('Disconnect your Google account? Existing forms stay, but you cannot create or sync until you reconnect.')) return;
    try {
      await api.del('/api/mentor/google');
      await refresh().catch(() => {});
      await load();
    } catch (err) {
      setError(err);
    }
  }

  async function sync(f) {
    setBusyId(f.id);
    try {
      const updated = await api.post(`/api/mentor/forms/${f.id}/sync`);
      setForms((list) => list.map((x) => (x.id === f.id ? updated : x)));
      setNotice({ type: 'success', text: `Synced "${f.title}" — ${updated.responseCount} response(s).` });
    } catch (err) {
      setError(err);
    } finally {
      setBusyId(null);
    }
  }

  async function view(f) {
    setBusyId(f.id);
    try {
      // Sync first so responses are fresh, then open the detail.
      await api.post(`/api/mentor/forms/${f.id}/sync`).catch(() => {});
      const d = await api.get(`/api/mentor/forms/${f.id}`);
      setDetail(d);
    } catch (err) {
      setError(err);
    } finally {
      setBusyId(null);
    }
  }

  async function remove(f) {
    if (!confirm(`Delete "${f.title}" and its stored responses? (The form itself remains in Google Forms.)`)) return;
    try {
      await api.del(`/api/mentor/forms/${f.id}`);
      await load();
    } catch (err) {
      setError(err);
    }
  }

  async function onCreated() {
    setShowBuilder(false);
    setNotice({ type: 'success', text: 'Form created in Google Forms.' });
    await load();
  }

  if (loading) return <Spinner />;

  const connected = status?.connected;

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h1 className="h3 mb-0">Forms</h1>
        <button
          className="btn btn-primary"
          disabled={!connected}
          onClick={() => setShowBuilder(true)}
          title={connected ? '' : 'Connect Google first'}
        >
          <i className="bi bi-plus-lg me-2"></i>Create form
        </button>
      </div>

      {notice && (
        <div className={`alert alert-${notice.type} alert-dismissible`}>
          {notice.text}
          <button className="btn-close" onClick={() => setNotice(null)}></button>
        </div>
      )}
      <ErrorAlert error={error} onClose={() => setError(null)} />

      {/* Google connection banner */}
      <div className="card border-0 shadow-sm mb-4">
        <div className="card-body d-flex align-items-center justify-content-between flex-wrap gap-2">
          <div className="d-flex align-items-center">
            <i className={`bi bi-google fs-3 me-3 ${connected ? 'text-success' : 'text-muted'}`}></i>
            <div>
              {!status?.enabled ? (
                <>
                  <div className="fw-semibold">Google Forms is not configured on the server</div>
                  <div className="text-muted small">
                    Set the Google client credentials and enable the feature to create forms. See GOOGLE_SETUP.md.
                  </div>
                </>
              ) : connected ? (
                <>
                  <div className="fw-semibold text-success">
                    <i className="bi bi-check-circle-fill me-1"></i>Connected
                  </div>
                  <div className="text-muted small">{status.email}</div>
                </>
              ) : (
                <>
                  <div className="fw-semibold">Connect your Google account</div>
                  <div className="text-muted small">
                    Authorize MentorTrack to create forms and read responses on your behalf.
                  </div>
                </>
              )}
            </div>
          </div>
          {status?.enabled &&
            (connected ? (
              <button className="btn btn-outline-danger btn-sm" onClick={disconnect}>
                <i className="bi bi-box-arrow-right me-1"></i>Disconnect
              </button>
            ) : (
              <button className="btn btn-danger" onClick={connect}>
                <i className="bi bi-google me-2"></i>Connect Google
              </button>
            ))}
        </div>
      </div>

      {forms.length === 0 ? (
        <EmptyState icon="ui-checks-grid">
          No forms yet.{connected ? ' Click “Create form” to build one.' : ' Connect Google to get started.'}
        </EmptyState>
      ) : (
        <div className="row g-3">
          {forms.map((f) => (
            <div className="col-md-6 col-lg-4" key={f.id}>
              <div className="card h-100 border-0 shadow-sm">
                <div className="card-body d-flex flex-column">
                  <h5 className="card-title">{f.title}</h5>
                  {f.description && <p className="text-muted small">{f.description}</p>}
                  <div className="small text-muted mb-2">
                    <span className="badge bg-primary-subtle text-primary-emphasis me-2">
                      {f.responseCount} response{f.responseCount === 1 ? '' : 's'}
                    </span>
                    <span className="badge bg-secondary-subtle text-secondary-emphasis">
                      {f.questions?.length || 0} question{f.questions?.length === 1 ? '' : 's'}
                    </span>
                  </div>
                  <div className="small text-muted mb-3">
                    {f.assignedMenteeIds?.length > 0 && (
                      <div>
                        <i className="bi bi-people me-1"></i>Assigned to {f.assignedMenteeIds.length} mentee(s)
                      </div>
                    )}
                    {f.lastSyncedAt && (
                      <div>
                        <i className="bi bi-arrow-repeat me-1"></i>Synced {formatDate(f.lastSyncedAt)}
                      </div>
                    )}
                  </div>

                  <div className="mt-auto">
                    <div className="d-flex gap-2 mb-2">
                      <a
                        href={f.responderUri}
                        target="_blank"
                        rel="noreferrer"
                        className="btn btn-sm btn-outline-secondary flex-fill"
                      >
                        <i className="bi bi-box-arrow-up-right me-1"></i>Open
                      </a>
                      {f.editUri && (
                        <a
                          href={f.editUri}
                          target="_blank"
                          rel="noreferrer"
                          className="btn btn-sm btn-outline-secondary flex-fill"
                        >
                          <i className="bi bi-pencil-square me-1"></i>Edit
                        </a>
                      )}
                    </div>
                    <div className="d-flex gap-2">
                      <button
                        className="btn btn-sm btn-primary flex-fill"
                        onClick={() => view(f)}
                        disabled={busyId === f.id}
                      >
                        {busyId === f.id ? (
                          <span className="spinner-border spinner-border-sm"></span>
                        ) : (
                          <>
                            <i className="bi bi-eye me-1"></i>Responses
                          </>
                        )}
                      </button>
                      <button
                        className="btn btn-sm btn-outline-primary"
                        onClick={() => sync(f)}
                        disabled={busyId === f.id}
                        title="Sync responses"
                      >
                        <i className="bi bi-arrow-repeat"></i>
                      </button>
                      <button
                        className="btn btn-sm btn-outline-danger"
                        onClick={() => remove(f)}
                        title="Delete"
                      >
                        <i className="bi bi-trash"></i>
                      </button>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Create form builder */}
      <Modal
        show={showBuilder}
        title="Create Google Form"
        size="lg"
        onClose={() => setShowBuilder(false)}
      >
        <FormBuilder mentees={mentees} onCreated={onCreated} onCancel={() => setShowBuilder(false)} />
      </Modal>

      {/* Responses */}
      <Modal
        show={!!detail}
        title={detail ? detail.form.title : ''}
        size="lg"
        onClose={() => setDetail(null)}
        footer={
          <button className="btn btn-secondary" onClick={() => setDetail(null)}>
            Close
          </button>
        }
      >
        {detail && <ResponsesView detail={detail} />}
      </Modal>
    </div>
  );
}
