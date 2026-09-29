import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth, homePathFor } from '../auth.jsx';
import NotificationsDropdown from './NotificationsDropdown.jsx';

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
      <nav className="navbar navbar-expand-lg navbar-dark shadow-sm py-3" style={{ background: 'linear-gradient(135deg, #4F46E5 0%, #3730A3 100%)' }}>
        <div className="container">
          <Link className="navbar-brand fw-bold d-flex align-items-center" to={homePathFor(user)}>
            <div className="bg-white text-primary rounded p-1 me-2 d-inline-flex">
              <i className="bi bi-mortarboard-fill fs-5"></i>
            </div>
            MentorTrack
          </Link>
          
          <button className="navbar-toggler border-0" type="button" data-bs-toggle="collapse" data-bs-target="#navbarContent">
            <span className="navbar-toggler-icon"></span>
          </button>
          
          <div className="collapse navbar-collapse" id="navbarContent">
            <ul className="navbar-nav me-auto fw-medium">
              {user?.role === 'ADMIN' && (
                <li className="nav-item">
                  <NavLink className="nav-link px-3" to="/admin/users">
                    <i className="bi bi-people me-1"></i>Users
                  </NavLink>
                </li>
              )}
              {user?.role === 'MENTOR' && (
                <>
                  <li className="nav-item">
                    <NavLink end className="nav-link px-3" to="/mentor">
                      <i className="bi bi-grid me-1"></i>Dashboard
                    </NavLink>
                  </li>
                  <li className="nav-item">
                    <NavLink className="nav-link px-3" to="/mentor/forms">
                      <i className="bi bi-ui-checks me-1"></i>Forms
                    </NavLink>
                  </li>
                </>
              )}
              {user?.role === 'MENTEE' && (
                <li className="nav-item">
                  <NavLink end className="nav-link px-3" to="/mentee">
                    <i className="bi bi-speedometer2 me-1"></i>My Dashboard
                  </NavLink>
                </li>
              )}
            </ul>
            <div className="d-flex align-items-center text-light ms-lg-auto mt-3 mt-lg-0">
              <NotificationsDropdown />
              <div className="d-flex align-items-center mx-3">
                <div className="bg-white bg-opacity-25 rounded-circle d-flex align-items-center justify-content-center me-2" style={{width: '32px', height: '32px'}}>
                  <i className="bi bi-person fw-bold"></i>
                </div>
                <div className="d-flex flex-column lh-1">
                  <span className="small fw-semibold">{user?.fullName}</span>
                  <span className="text-white-50" style={{fontSize: '0.65rem', textTransform: 'uppercase', letterSpacing: '0.05em'}}>{user?.role}</span>
                </div>
              </div>
              <button className="btn btn-sm btn-outline-light rounded-pill px-3 fw-medium" onClick={handleLogout}>
                Sign out
              </button>
            </div>
          </div>
        </div>
      </nav>

      <main className="container py-4">
        <Outlet />
      </main>
    </>
  );
}
