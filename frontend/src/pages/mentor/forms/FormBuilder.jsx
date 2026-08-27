import { useState } from 'react';
import { api } from '../../../api';
import { ErrorAlert } from '../../../components/ui.jsx';

const QUESTION_TYPES = [
  { value: 'TEXT', label: 'Short answer' },
  { value: 'PARAGRAPH', label: 'Paragraph' },
  { value: 'MULTIPLE_CHOICE', label: 'Multiple choice' },
  { value: 'CHECKBOX', label: 'Checkboxes' },
  { value: 'DROPDOWN', label: 'Dropdown' },
  { value: 'SCALE', label: 'Linear scale (1–5)' },
];

const CHOICE_TYPES = ['MULTIPLE_CHOICE', 'CHECKBOX', 'DROPDOWN'];
const newQuestion = () => ({ type: 'TEXT', title: '', required: false, options: [''] });

export default function FormBuilder({ mentees, onCreated, onCancel }) {
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [questions, setQuestions] = useState([newQuestion()]);
  const [assigned, setAssigned] = useState([]);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState(null);

  function updateQuestion(i, patch) {
    setQuestions((qs) => qs.map((q, idx) => (idx === i ? { ...q, ...patch } : q)));
  }

  function addQuestion() {
    setQuestions((qs) => [...qs, newQuestion()]);
  }

  function removeQuestion(i) {
    setQuestions((qs) => qs.filter((_, idx) => idx !== i));
  }

  function updateOption(qi, oi, value) {
    setQuestions((qs) =>
      qs.map((q, idx) =>
        idx === qi ? { ...q, options: q.options.map((o, j) => (j === oi ? value : o)) } : q
      )
    );
  }

  function addOption(qi) {
    setQuestions((qs) => qs.map((q, idx) => (idx === qi ? { ...q, options: [...q.options, ''] } : q)));
  }

  function removeOption(qi, oi) {
    setQuestions((qs) =>
      qs.map((q, idx) => (idx === qi ? { ...q, options: q.options.filter((_, j) => j !== oi) } : q))
    );
  }

  function toggleMentee(id) {
    setAssigned((a) => (a.includes(id) ? a.filter((x) => x !== id) : [...a, id]));
  }

  async function submit(e) {
    e.preventDefault();
    setError(null);

    // Basic client-side validation before hitting Google.
    for (const q of questions) {
      if (!q.title.trim()) {
        setError('Every question needs a title.');
        return;
      }
      if (CHOICE_TYPES.includes(q.type) && q.options.filter((o) => o.trim()).length === 0) {
        setError(`"${q.title}" needs at least one option.`);
        return;
      }
    }

    const payload = {
      title,
      description: description || null,
      questions: questions.map((q) => ({
        type: q.type,
        title: q.title,
        required: q.required,
        options: CHOICE_TYPES.includes(q.type) ? q.options.filter((o) => o.trim()) : [],
      })),
      assignedMenteeIds: assigned,
    };

    setSaving(true);
    try {
      await api.post('/api/mentor/forms', payload);
      onCreated();
    } catch (err) {
      setError(err);
    } finally {
      setSaving(false);
    }
  }

  return (
    <form onSubmit={submit}>
      <ErrorAlert error={error} onClose={() => setError(null)} />

      <div className="mb-3">
        <label className="form-label fw-semibold">Form title</label>
        <input className="form-control" value={title} onChange={(e) => setTitle(e.target.value)} required />
      </div>
      <div className="mb-3">
        <label className="form-label fw-semibold">Description</label>
        <textarea
          className="form-control"
          rows="2"
          value={description}
          onChange={(e) => setDescription(e.target.value)}
        />
      </div>

      <hr />
      <div className="d-flex justify-content-between align-items-center mb-2">
        <h6 className="mb-0">Questions</h6>
        <button type="button" className="btn btn-sm btn-outline-primary" onClick={addQuestion}>
          <i className="bi bi-plus-lg me-1"></i>Add question
        </button>
      </div>

      {questions.map((q, i) => (
        <div className="card mb-3 border" key={i}>
          <div className="card-body">
            <div className="d-flex justify-content-between mb-2">
              <span className="badge bg-secondary">Q{i + 1}</span>
              {questions.length > 1 && (
                <button
                  type="button"
                  className="btn btn-sm btn-link text-danger p-0"
                  onClick={() => removeQuestion(i)}
                >
                  <i className="bi bi-x-lg"></i> Remove
                </button>
              )}
            </div>
            <div className="row g-2">
              <div className="col-md-7">
                <input
                  className="form-control"
                  placeholder="Question text"
                  value={q.title}
                  onChange={(e) => updateQuestion(i, { title: e.target.value })}
                  required
                />
              </div>
              <div className="col-md-5">
                <select
                  className="form-select"
                  value={q.type}
                  onChange={(e) => updateQuestion(i, { type: e.target.value })}
                >
                  {QUESTION_TYPES.map((t) => (
                    <option key={t.value} value={t.value}>
                      {t.label}
                    </option>
                  ))}
                </select>
              </div>
            </div>

            {CHOICE_TYPES.includes(q.type) && (
              <div className="mt-3 ps-2 border-start">
                <label className="form-label small text-muted">Options</label>
                {q.options.map((o, oi) => (
                  <div className="input-group input-group-sm mb-1" key={oi}>
                    <span className="input-group-text">
                      <i className="bi bi-circle"></i>
                    </span>
                    <input
                      className="form-control"
                      value={o}
                      placeholder={`Option ${oi + 1}`}
                      onChange={(e) => updateOption(i, oi, e.target.value)}
                    />
                    {q.options.length > 1 && (
                      <button
                        type="button"
                        className="btn btn-outline-secondary"
                        onClick={() => removeOption(i, oi)}
                      >
                        <i className="bi bi-x"></i>
                      </button>
                    )}
                  </div>
                ))}
                <button
                  type="button"
                  className="btn btn-sm btn-link p-0"
                  onClick={() => addOption(i)}
                >
                  <i className="bi bi-plus"></i> Add option
                </button>
              </div>
            )}

            <div className="form-check form-switch mt-3">
              <input
                className="form-check-input"
                type="checkbox"
                id={`req-${i}`}
                checked={q.required}
                onChange={(e) => updateQuestion(i, { required: e.target.checked })}
              />
              <label className="form-check-label small" htmlFor={`req-${i}`}>
                Required
              </label>
            </div>
          </div>
        </div>
      ))}

      {mentees.length > 0 && (
        <>
          <hr />
          <h6>Assign to mentees</h6>
          <p className="text-muted small">
            Assigned mentees see this form on their dashboard. (The Google form itself is open to anyone with the link.)
          </p>
          <div className="row">
            {mentees.map((m) => (
              <div className="col-md-6" key={m.id}>
                <div className="form-check">
                  <input
                    className="form-check-input"
                    type="checkbox"
                    id={`m-${m.id}`}
                    checked={assigned.includes(m.id)}
                    onChange={() => toggleMentee(m.id)}
                  />
                  <label className="form-check-label" htmlFor={`m-${m.id}`}>
                    {m.fullName}
                  </label>
                </div>
              </div>
            ))}
          </div>
        </>
      )}

      <div className="d-flex justify-content-end gap-2 mt-4">
        <button type="button" className="btn btn-secondary" onClick={onCancel}>
          Cancel
        </button>
        <button className="btn btn-primary" disabled={saving}>
          {saving && <span className="spinner-border spinner-border-sm me-2"></span>}
          Create in Google Forms
        </button>
      </div>
    </form>
  );
}
