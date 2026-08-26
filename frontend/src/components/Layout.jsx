import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth, homePathFor } from '../auth.jsx';

// App shell: top navbar with role-aware links + the routed page below.
export default function Layout() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();

  function handleLogout() {
    logout();
    navigate('/login', { replace: true });
  }

  return (
    <>
      <nav className="navbar navbar-expand navbar-dark bg-dark">
        <div className="container-fluid">
          <Link className="navbar-brand fw-bold" to={homePathFor(user)}>
            <i className="bi bi-mortarboard-fill me-2"></i>MentorTrack
          </Link>
          <ul className="navbar-nav me-auto">
            {user?.role === 'ADMIN' && (
              <li className="nav-item">
                <NavLink className="nav-link" to="/admin/users">
                  Users
                </NavLink>
              </li>
            )}
            {user?.role === 'MENTOR' && (
              <>
                <li className="nav-item">
                  <NavLink end className="nav-link" to="/mentor">
                    My Mentees
                  </NavLink>
                </li>
                <li className="nav-item">
                  <NavLink className="nav-link" to="/mentor/forms">
                    Forms
                  </NavLink>
                </li>
              </>
            )}
            {user?.role === 'MENTEE' && (
              <li className="nav-item">
                <NavLink end className="nav-link" to="/mentee">
                  My Dashboard
                </NavLink>
              </li>
            )}
          </ul>
          <div className="d-flex align-items-center text-light">
            <span className="me-3 small">
              {user?.fullName}
              <span className="badge bg-secondary ms-2">{user?.role}</span>
            </span>
            <button className="btn btn-outline-light btn-sm" onClick={handleLogout}>
              <i className="bi bi-box-arrow-right me-1"></i>Sign out
            </button>
          </div>
        </div>
      </nav>

      <main className="container py-4">
        <Outlet />
      </main>
    </>
  );
}
