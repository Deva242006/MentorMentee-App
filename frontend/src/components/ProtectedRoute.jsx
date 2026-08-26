import { Navigate, useLocation } from 'react-router-dom';
import { useAuth, homePathFor } from '../auth.jsx';

// Gates a route on authentication and (optionally) a set of allowed roles.
export default function ProtectedRoute({ children, roles }) {
  const { user } = useAuth();
  const location = useLocation();

  if (!user) {
    return <Navigate to="/login" state={{ from: location }} replace />;
  }
  if (roles && !roles.includes(user.role)) {
    // Signed in but wrong role — send them to their own home.
    return <Navigate to={homePathFor(user)} replace />;
  }
  return children;
}
