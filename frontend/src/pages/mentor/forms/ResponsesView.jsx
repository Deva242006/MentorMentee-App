import { EmptyState, formatDate } from '../../../components/ui.jsx';

// Renders gathered responses as a matrix: one row per response, one column per question.
export default function ResponsesView({ detail }) {
  const { form, responses } = detail;
  const questions = form.questions || [];

  if (!responses || responses.length === 0) {
    return (
      <EmptyState icon="inbox">
        No responses yet. Share the form link, then Sync to pull in submissions.
        <div className="mt-3">
          <a href={form.responderUri} target="_blank" rel="noreferrer" className="btn btn-sm btn-outline-primary">
            <i className="bi bi-box-arrow-up-right me-1"></i>Open form
          </a>
        </div>
      </EmptyState>
    );
  }

  return (
    <div>
      <p className="text-muted small">
        {responses.length} response{responses.length === 1 ? '' : 's'} · showing answers by question.
      </p>
      <div className="table-responsive">
        <table className="table table-bordered table-sm align-middle">
          <thead className="table-light">
            <tr>
              <th style={{ minWidth: '9rem' }}>Respondent</th>
              <th style={{ minWidth: '8rem' }}>Submitted</th>
              {questions.map((q, i) => (
                <th key={i} style={{ minWidth: '10rem' }}>
                  {q.title}
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {responses.map((r) => {
              const byTitle = {};
              (r.answers || []).forEach((a) => {
                byTitle[a.questionTitle] = a.values || [];
              });
              return (
                <tr key={r.id}>
                  <td className="small">{r.respondentEmail || <span className="text-muted">Anonymous</span>}</td>
                  <td className="small text-muted">{formatDate(r.submittedAt)}</td>
                  {questions.map((q, i) => (
                    <td key={i} className="small">
                      {byTitle[q.title]?.length ? byTitle[q.title].join(', ') : <span className="text-muted">—</span>}
                    </td>
                  ))}
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
