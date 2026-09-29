import { useState } from 'react';
import { useNavigate, useLocation, Navigate } from 'react-router-dom';
import { useAuth, homePathFor } from '../auth.jsx';
import { ErrorAlert } from '../components/ui.jsx';

export default function Login() {
  const { user, login } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [role, setRole] = useState(null); // 'ADMIN', 'MENTOR', 'MENTEE'
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  // Already signed in → skip the form.
  if (user) return <Navigate to={homePathFor(user)} replace />;

  async function handleSubmit(e) {
    e.preventDefault();
    setError(null);
    setBusy(true);
    try {
      const u = await login(email.trim(), password);
      // Optional: Check if the logged-in user matches the selected role
      // if (u.role !== role) { ... } (skipping for now, backend enforces actual role)
      
      const dest = location.state?.from?.pathname || homePathFor(u);
      navigate(dest, { replace: true });
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  }

  const roleConfig = {
    ADMIN: { title: 'Admin Login', icon: 'shield-lock', color: 'text-danger' },
    MENTOR: { title: 'Mentor Login', icon: 'person-workspace', color: 'text-primary' },
    MENTEE: { title: 'Mentee Login', icon: 'journal-bookmark', color: 'text-success' },
  };

  if (!role) {
    return (
      <div className="d-flex vh-100 align-items-center justify-content-center bg-light">
        <div className="container text-center" style={{ maxWidth: '800px' }}>
          <div className="mb-5">
            <i className="bi bi-mortarboard-fill text-primary" style={{ fontSize: '3.5rem' }}></i>
            <h1 className="display-6 fw-bold mt-3">Welcome to MentorTrack</h1>
            <p className="text-muted fs-5">Please select your portal to continue</p>
          </div>
          
          <div className="row g-4 justify-content-center">
            {Object.entries(roleConfig).map(([r, config]) => (
              <div className="col-12 col-md-4" key={r}>
                <div 
                  className="card h-100 border-0 shadow-sm text-center py-4 px-2 hover-shadow cursor-pointer transition-all"
                  style={{ cursor: 'pointer', transition: 'transform 0.2s' }}
                  onClick={() => setRole(r)}
                  onMouseOver={(e) => e.currentTarget.style.transform = 'translateY(-5px)'}
                  onMouseOut={(e) => e.currentTarget.style.transform = 'translateY(0)'}
                >
                  <div className="card-body">
                    <i className={`bi bi-${config.icon} ${config.color} mb-3 d-block`} style={{ fontSize: '3rem' }}></i>
                    <h5 className="card-title fw-bold mb-0">{config.title.replace(' Login', '')}</h5>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="d-flex vh-100 align-items-center justify-content-center bg-light">
      <div className="card shadow border-0" style={{ width: '24rem', borderRadius: '1rem' }}>
        <div className="card-body p-4 p-md-5">
          <button 
            className="btn btn-link text-decoration-none p-0 mb-4 text-secondary d-flex align-items-center"
            onClick={() => { setRole(null); setError(null); }}
          >
            <i className="bi bi-arrow-left me-2"></i> Back to selection
          </button>
          
          <div className="text-center mb-4">
            <div className={`d-inline-flex align-items-center justify-content-center bg-opacity-10 rounded-circle mb-3 ${roleConfig[role].color.replace('text-', 'bg-')} `} style={{ width: '80px', height: '80px' }}>
               <i className={`bi bi-${roleConfig[role].icon} ${roleConfig[role].color}`} style={{ fontSize: '2.5rem' }}></i>
            </div>
            <h1 className="h4 fw-bold">{roleConfig[role].title}</h1>
            <p className="text-muted small">Enter your credentials to access your account</p>
          </div>

          <ErrorAlert error={error} onClose={() => setError(null)} />

          <form onSubmit={handleSubmit}>
            <div className="form-floating mb-3">
              <input
                type="email"
                className="form-control"
                id="emailInput"
                placeholder="name@example.com"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                autoFocus
                required
              />
              <label htmlFor="emailInput">Email address</label>
            </div>
            <div className="form-floating mb-4">
              <input
                type="password"
                className="form-control"
                id="passwordInput"
                placeholder="Password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                required
              />
              <label htmlFor="passwordInput">Password</label>
            </div>
            <button className={`btn btn-lg w-100 fw-bold ${role === 'ADMIN' ? 'btn-danger' : role === 'MENTEE' ? 'btn-success' : 'btn-primary'}`} disabled={busy}>
              {busy ? (
                <span className="spinner-border spinner-border-sm me-2"></span>
              ) : (
                <i className="bi bi-box-arrow-in-right me-2"></i>
              )}
              Sign In
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}
