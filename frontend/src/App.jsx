import { Routes, Route, Navigate } from 'react-router-dom';
import { useAuth, homePathFor } from './auth.jsx';
import ProtectedRoute from './components/ProtectedRoute.jsx';
import Layout from './components/Layout.jsx';
import Login from './pages/Login.jsx';
import AdminUsers from './pages/admin/Users.jsx';
import MentorDashboard from './pages/mentor/Dashboard.jsx';
import MenteeDetail from './pages/mentor/MenteeDetail.jsx';
import MentorForms from './pages/mentor/Forms.jsx';
import MenteeDashboard from './pages/mentee/Dashboard.jsx';

export default function App() {
  const { user, loading } = useAuth();

  if (loading) {
    return (
      <div className="d-flex vh-100 align-items-center justify-content-center">
        <div className="spinner-border text-primary" role="status">
          <span className="visually-hidden">Loading…</span>
        </div>
      </div>
    );
  }

  return (
    <Routes>
      <Route path="/login" element={<Login />} />

      <Route
        element={
          <ProtectedRoute>
            <Layout />
          </ProtectedRoute>
        }
      >
        {/* Admin */}
        <Route
          path="/admin/users"
          element={
            <ProtectedRoute roles={['ADMIN']}>
              <AdminUsers />
            </ProtectedRoute>
          }
        />

        {/* Mentor */}
        <Route
          path="/mentor"
          element={
            <ProtectedRoute roles={['MENTOR']}>
              <MentorDashboard />
            </ProtectedRoute>
          }
        />
        <Route
          path="/mentor/mentees/:menteeId"
          element={
            <ProtectedRoute roles={['MENTOR']}>
              <MenteeDetail />
            </ProtectedRoute>
          }
        />
        <Route
          path="/mentor/forms"
          element={
            <ProtectedRoute roles={['MENTOR']}>
              <MentorForms />
            </ProtectedRoute>
          }
        />

        {/* Mentee */}
        <Route
          path="/mentee"
          element={
            <ProtectedRoute roles={['MENTEE']}>
              <MenteeDashboard />
            </ProtectedRoute>
          }
        />
      </Route>

      <Route path="/" element={<Navigate to={homePathFor(user)} replace />} />
      <Route path="*" element={<Navigate to={homePathFor(user)} replace />} />
    </Routes>
  );
}
