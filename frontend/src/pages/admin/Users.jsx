import { useEffect, useState, useCallback } from 'react';
import { api } from '../../api';
import { Spinner, ErrorAlert, EmptyState, Modal, formatDate } from '../../components/ui.jsx';

const ROLES = ['ADMIN', 'MENTOR', 'MENTEE'];
const emptyForm = {
  fullName: '',
  email: '',
  password: '',
  role: 'MENTEE',
  mentorId: '',
  phone: '',
  program: '',
  year: '',
};

export default function AdminUsers() {
  const [users, setUsers] = useState([]);
  const [mentors, setMentors] = useState([]);
  const [stats, setStats] = useState(null);
  const [roleFilter, setRoleFilter] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  const [showModal, setShowModal] = useState(false);
  const [editing, setEditing] = useState(null); // user being edited, or null for create
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [formError, setFormError] = useState(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const q = roleFilter ? `?role=${roleFilter}` : '';
      const [u, m, s] = await Promise.all([
        api.get(`/api/admin/users${q}`),
        api.get('/api/admin/mentors'),
        api.get('/api/admin/stats'),
      ]);
      setUsers(u);
      setMentors(m);
      setStats(s);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  }, [roleFilter]);

  useEffect(() => {
    load();
  }, [load]);

  function openCreate() {
    setEditing(null);
    setForm(emptyForm);
    setFormError(null);
    setShowModal(true);
  }

  function openEdit(u) {
    setEditing(u);
    setForm({
      fullName: u.fullName || '',
      email: u.email || '',
      password: '',
      role: u.role,
      mentorId: u.mentorId || '',
      phone: u.phone || '',
      program: u.program || '',
      year: u.year || '',
      active: u.active,
    });
    setFormError(null);
    setShowModal(true);
  }

  async function save(e) {
    e.preventDefault();
    setSaving(true);
    setFormError(null);
    try {
      if (editing) {
        await api.put(`/api/admin/users/${editing.id}`, {
          fullName: form.fullName,
          active: form.active,
          mentorId: form.role === 'MENTEE' ? form.mentorId || null : null,
          phone: form.phone || null,
          program: form.program || null,
          year: form.year || null,
        });
      } else {
        await api.post('/api/admin/users', {
          fullName: form.fullName,
          email: form.email,
          password: form.password,
          role: form.role,
          mentorId: form.role === 'MENTEE' ? form.mentorId || null : null,
          phone: form.phone || null,
          program: form.program || null,
          year: form.year || null,
        });
      }
      setShowModal(false);
      await load();
    } catch (err) {
      setFormError(err);
    } finally {
      setSaving(false);
    }
  }

  async function remove(u) {
    if (!confirm(`Delete ${u.fullName}? This also removes their tracked data.`)) return;
    try {
      await api.del(`/api/admin/users/${u.id}`);
      await load();
    } catch (err) {
      setError(err);
    }
  }

  async function reassign(u, mentorId) {
    try {
      await api.put(`/api/admin/users/${u.id}/mentor`, { mentorId: mentorId || null });
      await load();
    } catch (err) {
      setError(err);
    }
  }

  if (loading) return <Spinner />;

  return (
    <div>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h1 className="h3 mb-0">User Management</h1>
        <button className="btn btn-primary" onClick={openCreate}>
          <i className="bi bi-person-plus me-2"></i>Add User
        </button>
      </div>

      <ErrorAlert error={error} onClose={() => setError(null)} />

      {stats && (
        <div className="row g-3 mb-4">
          <StatCard icon="mortarboard" label="Mentors" value={stats.mentors} color="primary" />
          <StatCard icon="people" label="Mentees" value={stats.mentees} color="success" />
          <StatCard icon="shield-lock" label="Admins" value={stats.admins} color="secondary" />
          <StatCard icon="ui-checks-grid" label="Forms" value={stats.forms} color="info" />
        </div>
      )}

      <div className="btn-group mb-3">
        <button
          className={`btn btn-sm ${roleFilter === '' ? 'btn-dark' : 'btn-outline-dark'}`}
          onClick={() => setRoleFilter('')}
        >
          All
        </button>
        {ROLES.map((r) => (
          <button
            key={r}
            className={`btn btn-sm ${roleFilter === r ? 'btn-dark' : 'btn-outline-dark'}`}
            onClick={() => setRoleFilter(r)}
          >
            {r}
          </button>
        ))}
      </div>

      {users.length === 0 ? (
        <EmptyState icon="people">No users match this filter.</EmptyState>
      ) : (
        <div className="card shadow-sm border-0">
          <div className="table-responsive">
            <table className="table table-hover align-middle mb-0">
              <thead className="table-light">
                <tr>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Role</th>
                  <th>Mentor</th>
                  <th>Status</th>
                  <th>Created</th>
                  <th className="text-end">Actions</th>
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id}>
                    <td className="fw-medium">{u.fullName}</td>
                    <td className="text-muted">{u.email}</td>
                    <td>
                      <span className="badge bg-secondary">{u.role}</span>
                    </td>
                    <td>
                      {u.role === 'MENTEE' ? (
                        <select
                          className="form-select form-select-sm"
                          style={{ minWidth: '10rem' }}
                          value={u.mentorId || ''}
                          onChange={(e) => reassign(u, e.target.value)}
                        >
                          <option value="">— Unassigned —</option>
                          {mentors.map((m) => (
                            <option key={m.id} value={m.id}>
                              {m.fullName}
                            </option>
                          ))}
                        </select>
                      ) : (
                        <span className="text-muted">—</span>
                      )}
                    </td>
                    <td>
                      {u.active ? (
                        <span className="badge bg-success-subtle text-success-emphasis">Active</span>
                      ) : (
                        <span className="badge bg-danger-subtle text-danger-emphasis">Disabled</span>
                      )}
                    </td>
                    <td className="text-muted small">{formatDate(u.createdAt)}</td>
                    <td className="text-end text-nowrap">
                      <button
                        className="btn btn-sm btn-outline-secondary me-1"
                        onClick={() => openEdit(u)}
                        title="Edit"
                      >
                        <i className="bi bi-pencil"></i>
                      </button>
                      <button
                        className="btn btn-sm btn-outline-danger"
                        onClick={() => remove(u)}
                        title="Delete"
                      >
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

      <Modal
        show={showModal}
        title={editing ? `Edit ${editing.fullName}` : 'Add User'}
        onClose={() => setShowModal(false)}
        footer={
          <>
            <button className="btn btn-secondary" onClick={() => setShowModal(false)}>
              Cancel
            </button>
            <button className="btn btn-primary" form="user-form" disabled={saving}>
              {saving && <span className="spinner-border spinner-border-sm me-2"></span>}
              {editing ? 'Save changes' : 'Create user'}
            </button>
          </>
        }
      >
        <form id="user-form" onSubmit={save}>
          <ErrorAlert error={formError} onClose={() => setFormError(null)} />
          <div className="mb-3">
            <label className="form-label">Full name</label>
            <input
              className="form-control"
              value={form.fullName}
              onChange={(e) => setForm({ ...form, fullName: e.target.value })}
              required
            />
          </div>
          <div className="mb-3">
            <label className="form-label">Email</label>
            <input
              type="email"
              className="form-control"
              value={form.email}
              disabled={!!editing}
              onChange={(e) => setForm({ ...form, email: e.target.value })}
              required
            />
          </div>
          {!editing && (
            <>
              <div className="mb-3">
                <label className="form-label">Password</label>
                <input
                  type="password"
                  className="form-control"
                  value={form.password}
                  onChange={(e) => setForm({ ...form, password: e.target.value })}
                  minLength={6}
                  required
                />
              </div>
              <div className="mb-3">
                <label className="form-label">Role</label>
                <select
                  className="form-select"
                  value={form.role}
                  onChange={(e) => setForm({ ...form, role: e.target.value })}
                >
                  {ROLES.map((r) => (
                    <option key={r} value={r}>
                      {r}
                    </option>
                  ))}
                </select>
              </div>
            </>
          )}
          {form.role === 'MENTEE' && (
            <div className="mb-3">
              <label className="form-label">Assigned mentor</label>
              <select
                className="form-select"
                value={form.mentorId}
                onChange={(e) => setForm({ ...form, mentorId: e.target.value })}
              >
                <option value="">— Unassigned —</option>
                {mentors.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.fullName}
                  </option>
                ))}
              </select>
            </div>
          )}
          <div className="row">
            <div className="col-md-4 mb-3">
              <label className="form-label">Phone</label>
              <input
                className="form-control"
                value={form.phone}
                onChange={(e) => setForm({ ...form, phone: e.target.value })}
              />
            </div>
            <div className="col-md-5 mb-3">
              <label className="form-label">Program</label>
              <input
                className="form-control"
                value={form.program}
                onChange={(e) => setForm({ ...form, program: e.target.value })}
              />
            </div>
            <div className="col-md-3 mb-3">
              <label className="form-label">Year</label>
              <input
                className="form-control"
                value={form.year}
                onChange={(e) => setForm({ ...form, year: e.target.value })}
              />
            </div>
          </div>
          {editing && (
            <div className="form-check form-switch">
              <input
                className="form-check-input"
                type="checkbox"
                id="active-switch"
                checked={!!form.active}
                onChange={(e) => setForm({ ...form, active: e.target.checked })}
              />
              <label className="form-check-label" htmlFor="active-switch">
                Account active
              </label>
            </div>
          )}
        </form>
      </Modal>
    </div>
  );
}

function StatCard({ icon, label, value, color }) {
  return (
    <div className="col-6 col-md-3">
      <div className="card border-0 shadow-sm h-100">
        <div className="card-body d-flex align-items-center">
          <div className={`rounded-circle bg-${color}-subtle p-3 me-3`}>
            <i className={`bi bi-${icon} text-${color} fs-4`}></i>
          </div>
          <div>
            <div className="h4 mb-0">{value}</div>
            <div className="text-muted small">{label}</div>
          </div>
        </div>
      </div>
    </div>
  );
}
