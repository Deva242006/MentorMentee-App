import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api';

export default function NotificationsDropdown() {
  const [notifications, setNotifications] = useState([]);
  const [unreadCount, setUnreadCount] = useState(0);
  const [open, setOpen] = useState(false);
  const navigate = useNavigate();

  const fetchNotifications = async () => {
    try {
      const data = await api.get('/api/notifications');
      setNotifications(data.notifications);
      setUnreadCount(data.unreadCount);
    } catch (err) {
      console.error('Failed to fetch notifications', err);
    }
  };

  useEffect(() => {
    fetchNotifications();
    const intervalId = setInterval(fetchNotifications, 15000); // Poll every 15 seconds
    return () => clearInterval(intervalId);
  }, []);

  const handleRead = async (n) => {
    if (!n.read) {
      try {
        await api.put(`/api/notifications/${n.id}/read`);
        setUnreadCount((prev) => Math.max(0, prev - 1));
        setNotifications((prev) => prev.map((x) => (x.id === n.id ? { ...x, read: true } : x)));
      } catch (err) {
        console.error(err);
      }
    }
    if (n.link) {
      navigate(n.link);
      setOpen(false);
    }
  };

  const markAllRead = async () => {
    try {
      await api.put('/api/notifications/read-all');
      setUnreadCount(0);
      setNotifications((prev) => prev.map((x) => ({ ...x, read: true })));
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className="nav-item dropdown">
      <button
        className="btn btn-link nav-link position-relative"
        onClick={() => setOpen(!open)}
        onBlur={() => setTimeout(() => setOpen(false), 200)}
      >
        <i className="bi bi-bell-fill fs-5"></i>
        {unreadCount > 0 && (
          <span className="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger" style={{ fontSize: '0.6rem' }}>
            {unreadCount > 99 ? '99+' : unreadCount}
          </span>
        )}
      </button>

      {open && (
        <div className="dropdown-menu dropdown-menu-end shadow-sm show" style={{ width: '320px', right: 0, left: 'auto', position: 'absolute' }}>
          <div className="d-flex justify-content-between align-items-center px-3 py-2 border-bottom">
            <h6 className="mb-0">Notifications</h6>
            {unreadCount > 0 && (
              <button className="btn btn-sm btn-link text-decoration-none p-0" onMouseDown={markAllRead}>
                Mark all read
              </button>
            )}
          </div>
          <div className="overflow-auto" style={{ maxHeight: '350px' }}>
            {notifications.length === 0 ? (
              <div className="text-center text-muted p-4 small">No notifications</div>
            ) : (
              notifications.map((n) => (
                <button
                  key={n.id}
                  className={`dropdown-item text-wrap py-2 border-bottom ${n.read ? 'text-muted' : 'fw-medium bg-light'}`}
                  onMouseDown={() => handleRead(n)}
                >
                  <div className="small">{n.message}</div>
                  <div className="text-muted" style={{ fontSize: '0.7rem' }}>
                    {new Date(n.timestamp).toLocaleString()}
                  </div>
                </button>
              ))
            )}
          </div>
        </div>
      )}
    </div>
  );
}
