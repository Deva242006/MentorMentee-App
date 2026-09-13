import { useState, useEffect, useRef } from 'react';
import { api } from '../../api';
import { useAuth } from '../../auth';
import { formatDate } from './ui';

export default function ChatBox({ targetUserId, targetName }) {
  const { user } = useAuth();
  const [messages, setMessages] = useState([]);
  const [content, setContent] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [sending, setSending] = useState(false);
  const messagesEndRef = useRef(null);

  const fetchMessages = async () => {
    try {
      const data = await api.get(`/api/messages/${targetUserId}`);
      setMessages(data);
      setError(null);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    setLoading(true);
    fetchMessages();
    const intervalId = setInterval(fetchMessages, 5000); // Poll every 5 seconds
    return () => clearInterval(intervalId);
  }, [targetUserId]);

  useEffect(() => {
    // Scroll to bottom when messages change
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const sendMessage = async (e) => {
    e.preventDefault();
    if (!content.trim()) return;
    setSending(true);
    try {
      const newMsg = await api.post('/api/messages', {
        receiverId: targetUserId,
        content: content.trim(),
      });
      setMessages([...messages, newMsg]);
      setContent('');
    } catch (err) {
      setError(err);
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="card border-0 shadow-sm d-flex flex-column" style={{ height: '500px' }}>
      <div className="card-header bg-light border-bottom-0 d-flex justify-content-between align-items-center">
        <h6 className="mb-0">Chat with {targetName}</h6>
        {loading && <span className="spinner-border spinner-border-sm text-primary"></span>}
      </div>
      <div className="card-body overflow-auto bg-light bg-opacity-50">
        {error && (
          <div className="alert alert-danger p-2 small mb-2">{error.message || 'Error loading chat'}</div>
        )}
        {messages.length === 0 && !loading && (
          <div className="text-center text-muted mt-5 small">No messages yet. Say hi!</div>
        )}
        {messages.map((m) => {
          const isMine = m.senderId === user.id;
          return (
            <div key={m.id} className={`d-flex mb-3 ${isMine ? 'justify-content-end' : 'justify-content-start'}`}>
              <div
                className={`p-2 rounded ${isMine ? 'bg-primary text-white' : 'bg-white border'}`}
                style={{ maxWidth: '75%' }}
              >
                <div style={{ whiteSpace: 'pre-wrap' }}>{m.content}</div>
                <div
                  className={`small mt-1 text-end ${isMine ? 'text-white-50' : 'text-muted'}`}
                  style={{ fontSize: '0.7rem' }}
                >
                  {new Date(m.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                </div>
              </div>
            </div>
          );
        })}
        <div ref={messagesEndRef} />
      </div>
      <div className="card-footer bg-white border-top-0">
        <form onSubmit={sendMessage} className="d-flex gap-2">
          <input
            type="text"
            className="form-control"
            placeholder="Type a message..."
            value={content}
            onChange={(e) => setContent(e.target.value)}
            disabled={sending}
          />
          <button type="submit" className="btn btn-primary" disabled={sending || !content.trim()}>
            {sending ? <span className="spinner-border spinner-border-sm"></span> : <i className="bi bi-send"></i>}
          </button>
        </form>
      </div>
    </div>
  );
}
