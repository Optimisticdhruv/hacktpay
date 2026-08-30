import { useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import './styles.css';

const api = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080/api';

type RecoveryCase = {
  id: string; caseReference: string; customerName: string; customerEmail?: string; contactAllowed: boolean;
  riskType: string; amountAtRisk: number; paymentMethod: string; status: string; diagnosis?: string;
  recoverabilityScore?: number; recommendedAction?: string; reasons?: string[]; strategySource?: string;
  attemptCount: number; activePaymentLink: boolean; amountRecovered: number;
};
type Audit = { id: string; eventType: string; message: string; createdAt: string };
type Summary = { totalRevenueAtRisk: number; revenueRecovered: number; recoveryRate: number; activeCases: number; recoveredCases: number; escalatedCases: number };
type Evaluation = { dataClassification: string; datasetSize: number; seed: number; totalAtRisk: number; totalAttempted: number; totalRecovered: number; recoveryRate: number; policyApproved: number; policyBlocked: number; byRiskType: Record<string, { atRisk: number; attempted: number; recovered: number }> };
type EvaluationRun = { id: string; result: Evaluation; createdAt: string };
type RecoveryTask = { id: string; type: string; status: string; dueAt: string; completedAt?: string };

const label = (value?: string) => value?.replaceAll('_', ' ') ?? '—';
const money = (paise: number) => new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 0 }).format(paise / 100);
const statusClass = (status?: string) => `status-${(status ?? '').toLowerCase().replaceAll('_', '-')}`;
const isDetectedRazorpayCase = (recoveryCase?: RecoveryCase) => Boolean(recoveryCase?.caseReference.startsWith('RZP-'));
const actionLabel = (action?: string) => {
  if (action === 'CREATE_PAYMENT_LINK') return 'Create secure payment link';
  if (action === 'WAIT_AND_RETRY') return 'Schedule retry review';
  if (action === 'SEND_REMINDER') return 'Schedule customer reminder';
  if (action === 'ESCALATE_TO_HUMAN') return 'Escalate for human follow-up';
  return 'Execute approved action';
};
const terminalStatuses = ['RECOVERED', 'STOPPED', 'UNRECOVERABLE'];
const riskPreset = (riskType: string) => ({
  PAYMENT_FAILURE: { paymentMethod: 'UPI', failureReason: 'customer_requested_retry', transactionStatus: 'FAILED', previousSuccessfulPayments: 3 },
  CHECKOUT_ABANDONMENT: { paymentMethod: 'CARD', failureReason: 'checkout_expired', transactionStatus: 'CREATED', previousSuccessfulPayments: 2 },
  SUBSCRIPTION_FAILURE: { paymentMethod: 'CARD', failureReason: 'renewal_payment_failed', transactionStatus: 'FAILED', previousSuccessfulPayments: 4 },
  OVERDUE_RECEIVABLE: { paymentMethod: 'BANK_TRANSFER', failureReason: 'invoice_overdue', transactionStatus: 'FAILED', previousSuccessfulPayments: 5 }
}[riskType] ?? { paymentMethod: 'UPI', failureReason: 'customer_requested_retry', transactionStatus: 'FAILED', previousSuccessfulPayments: 3 });

async function get<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(api + path, { headers: { 'Content-Type': 'application/json' }, ...init });
  if (!response.ok) {
    const body = await response.json().catch(() => null);
    throw new Error(body?.message ?? `Request failed (${response.status})`);
  }
  return response.json();
}

function Metric({ value, name, good }: { value: string; name: string; good?: boolean }) {
  return <article className={`metric ${good ? 'good' : ''}`}><small>{name}</small><strong>{value}</strong></article>;
}

function OpenCasesMetric({ count, review, stopAll, busy }: { count: number; review: () => void; stopAll: () => void; busy: boolean }) {
  return <article className="metric open-cases"><small>Open cases</small><strong>{count}</strong>{count > 0 ? <div><button onClick={review} style={{ background: '#eff5ff', borderColor: '#cbdcf3', color: '#245caa', padding: '6px 8px', fontSize: 11 }}>Review queue</button><button onClick={stopAll} disabled={busy} style={{ background: '#fff3f2', borderColor: '#f0ccc8', color: '#9b3933', padding: '6px 8px', fontSize: 11 }}>Stop all</button></div> : <span style={{ marginTop: 10, color: '#6d829b', fontSize: 11 }}>All workflows are resolved.</span>}</article>;
}

function App() {
  const [view, setView] = useState<'overview' | 'cases' | 'simulation'>('overview');
  const [cases, setCases] = useState<RecoveryCase[]>([]);
  const [summary, setSummary] = useState<Summary>();
  const [selected, setSelected] = useState('');
  const [audit, setAudit] = useState<Audit[]>([]);
  const [tasks, setTasks] = useState<RecoveryTask[]>([]);
  const [evaluation, setEvaluation] = useState<Evaluation>();
  const [evaluationHistory, setEvaluationHistory] = useState<EvaluationRun[]>([]);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [showCreate, setShowCreate] = useState(false);
  const [paymentUrl, setPaymentUrl] = useState('');
  const [draft, setDraft] = useState({ customerName: '', customerEmail: '', amount: '4999', riskType: 'PAYMENT_FAILURE' });
  const [reviewDraft, setReviewDraft] = useState({ customerName: '', customerEmail: '', contactAllowed: false });
  const [caseFilter, setCaseFilter] = useState('OPEN');
  const [handoffDraft, setHandoffDraft] = useState({ outcome: 'PROMISE_TO_PAY', amount: '' });

  const openCases = useMemo(() => cases.filter(recoveryCase => !terminalStatuses.includes(recoveryCase.status)), [cases]);
  const queueCases = useMemo(() => [...openCases, ...cases.filter(recoveryCase => !openCases.some(openCase => openCase.id === recoveryCase.id))], [cases, openCases]);
  const filteredCases = useMemo(() => caseFilter === 'ALL' ? cases : caseFilter === 'OPEN' ? openCases : cases.filter(recoveryCase => recoveryCase.status === caseFilter), [cases, caseFilter, openCases]);
  const current = cases.find(recoveryCase => recoveryCase.id === selected);

  const load = async () => {
    setError('');
    try {
      const [recoveryCases, dashboardSummary, history] = await Promise.all([
        get<RecoveryCase[]>('/recovery-cases'),
        get<Summary>('/dashboard/summary'),
        get<EvaluationRun[]>('/evaluation/history').catch(() => [])
      ]);
      setCases(recoveryCases);
      setSummary(dashboardSummary);
      setEvaluationHistory(history);
      setSelected(currentId => currentId || recoveryCases[0]?.id || '');
    } catch (loadError) {
      setError(loadError instanceof Error ? loadError.message : 'Backend unavailable');
    }
  };

  const loadAudit = async (id: string) => {
    try { setAudit(await get<Audit[]>(`/recovery-cases/${id}/audit`)); } catch { /* Case data remains available even when audit retrieval is delayed. */ }
  };
  const loadTasks = async (id: string) => {
    try { setTasks(await get<RecoveryTask[]>(`/recovery-cases/${id}/tasks`)); } catch { setTasks([]); }
  };

  const selectCase = (id: string) => {
    const recoveryCase = cases.find(item => item.id === id);
    setSelected(id);
    setPaymentUrl('');
    if (recoveryCase && isDetectedRazorpayCase(recoveryCase)) {
      setReviewDraft({ customerName: recoveryCase.customerName === 'Razorpay detected payment' ? '' : recoveryCase.customerName, customerEmail: recoveryCase.customerEmail ?? '', contactAllowed: recoveryCase.contactAllowed });
    }
  };

  useEffect(() => {
    load();
    const timer = window.setInterval(load, 10_000);
    return () => window.clearInterval(timer);
  }, []);

  useEffect(() => { if (selected) { loadAudit(selected); loadTasks(selected); } }, [selected]);
  useEffect(() => {
    if (!selected || current?.status !== 'WAITING_CUSTOMER') return;
    const timer = window.setInterval(() => { load(); loadAudit(selected); loadTasks(selected); }, 5_000);
    return () => window.clearInterval(timer);
  }, [selected, current?.status]);

  const act = async (action: 'analyze' | 'execute' | 'stop') => {
    if (!current) return;
    setBusy(true); setError('');
    try {
      const result = await get<any>(`/recovery-cases/${current.id}/${action}`, { method: 'POST' });
      if (result?.paymentLink?.shortUrl) setPaymentUrl(result.paymentLink.shortUrl);
      setMessage(action === 'analyze' ? 'Recommendation created and policy-ready.' : action === 'execute' ? 'Approved recovery action completed.' : 'Recovery stopped by merchant.');
      await load(); await loadAudit(current.id); await loadTasks(current.id);
    } catch (actionError) {
      setError(actionError instanceof Error ? actionError.message : 'Could not update recovery action');
    } finally { setBusy(false); }
  };

  const createLiveCase = async () => {
    const amount = Math.round(Number(draft.amount) * 100);
    if (!draft.customerName.trim() || !Number.isFinite(amount) || amount <= 0) {
      setError('Enter a customer name and a valid positive amount.'); return;
    }
    setBusy(true); setError('');
    try {
      const preset = riskPreset(draft.riskType);
      const created = await get<RecoveryCase>('/recovery-cases', {
        method: 'POST', body: JSON.stringify({ customerName: draft.customerName.trim(), customerEmail: draft.customerEmail.trim(), contactAllowed: true, riskType: draft.riskType, amountAtRisk: amount, paymentMethod: preset.paymentMethod, failureReason: preset.failureReason, transactionStatus: preset.transactionStatus, previousSuccessfulPayments: preset.previousSuccessfulPayments, previousFailedPayments: 0 })
      });
      setSelected(created.id); setShowCreate(false); setView('cases');
      setMessage('Live recovery case created. Analyze it to receive a policy-qualified intervention.');
      await load();
    } catch (createError) {
      setError(createError instanceof Error ? createError.message : 'Could not create recovery case');
    } finally { setBusy(false); }
  };

  const reviewDetected = async () => {
    if (!current || !reviewDraft.customerName.trim()) { setError('Enter the reviewed customer name.'); return; }
    setBusy(true); setError('');
    try {
      await get<RecoveryCase>(`/recovery-cases/${current.id}/review`, { method: 'POST', body: JSON.stringify({ customerName: reviewDraft.customerName.trim(), customerEmail: reviewDraft.customerEmail.trim(), contactAllowed: reviewDraft.contactAllowed }) });
      setMessage(reviewDraft.contactAllowed ? 'Human review saved. Contact is approved; analyze the case to continue.' : 'Human review saved. Contact remains disabled, so recovery stays review-only.');
      await load(); await loadAudit(current.id);
    } catch (reviewError) {
      setError(reviewError instanceof Error ? reviewError.message : 'Could not save human review');
    } finally { setBusy(false); }
  };

  const stopAllOpen = async () => {
    if (openCases.length === 0) return;
    if (!window.confirm(`Stop ${openCases.length} open recovery case${openCases.length === 1 ? '' : 's'}? This keeps every case and its audit trail, but prevents further recovery actions.`)) return;
    setBusy(true); setError('');
    try {
      const result = await get<{ stoppedCount: number }>('/recovery-cases/stop-open', { method: 'POST' });
      setPaymentUrl('');
      setMessage(`${result.stoppedCount} open recovery case${result.stoppedCount === 1 ? '' : 's'} stopped. Audit trails were preserved.`);
      await load();
    } catch (stopError) {
      setError(stopError instanceof Error ? stopError.message : 'Could not stop open recovery cases');
    } finally { setBusy(false); }
  };

  const completeTask = async (taskId: string) => {
    if (!current) return;
    setBusy(true); setError('');
    try {
      await get<RecoveryTask>(`/recovery-cases/${current.id}/tasks/${taskId}/complete`, { method: 'POST' });
      setMessage('Recovery task completed. The case is ready for a fresh review.');
      await load(); await loadAudit(current.id); await loadTasks(current.id);
    } catch (taskError) {
      setError(taskError instanceof Error ? taskError.message : 'Could not complete recovery task');
    } finally { setBusy(false); }
  };

  const resolveHandoff = async () => {
    if (!current) return;
    const amountRecovered = Math.round(Number(handoffDraft.amount) * 100);
    if (handoffDraft.outcome === 'RECOVERED_EXTERNALLY' && (!Number.isFinite(amountRecovered) || amountRecovered <= 0)) { setError('Enter the externally recovered amount.'); return; }
    setBusy(true); setError('');
    try {
      await get<RecoveryCase>(`/recovery-cases/${current.id}/handoff-outcome`, { method: 'POST', body: JSON.stringify({ outcome: handoffDraft.outcome, amountRecovered: Number.isFinite(amountRecovered) ? amountRecovered : 0 }) });
      setMessage('Human handoff outcome recorded in the audit trail.');
      await load(); await loadAudit(current.id); await loadTasks(current.id);
    } catch (handoffError) {
      setError(handoffError instanceof Error ? handoffError.message : 'Could not record human handoff outcome');
    } finally { setBusy(false); }
  };

  const addDemoPack = async () => {
    setBusy(true); setError('');
    try {
      const pack = await get<RecoveryCase[]>('/recovery-cases/demo-pack', { method: 'POST' });
      setSelected(pack[0]?.id ?? ''); setView('cases');
      setMessage('Fresh demo pack added: checkout, subscription, and invoice recovery cases.');
      await load();
    } catch (demoError) {
      setError(demoError instanceof Error ? demoError.message : 'Could not create demo pack');
    } finally { setBusy(false); }
  };

  const simulate = async () => {
    setBusy(true);
    try {
      setEvaluation(await get<Evaluation>('/evaluation/run', { method: 'POST', body: JSON.stringify({ datasetSize: 240, seed: 42 }) }));
      setEvaluationHistory(await get<EvaluationRun[]>('/evaluation/history'));
      setView('simulation');
    } catch (simulationError) {
      setError(simulationError instanceof Error ? simulationError.message : 'Simulation failed');
    } finally { setBusy(false); }
  };

  return <div className="shell">
    <aside>
      <div className="brand">RECOVER<span>AI</span></div>
      <p>NOVACART<br /><small>Revenue operations</small></p>
      {(['overview', 'cases', 'simulation'] as const).map(item => <button className={view === item ? 'active' : ''} onClick={() => setView(item)} key={item}>{item === 'overview' ? 'Overview' : item === 'cases' ? 'Recovery cases' : 'Simulation lab'}</button>)}
      <footer>SAFETY MODE<br /><b>Policy controlled</b></footer>
    </aside>
    <main>
      <header>
        <div><em>REVENUE RECOVERY CONTROL CENTER</em><h1>{label(view)}</h1><p>Bounded recovery actions with deterministic policy checks.</p></div>
        <button className="secondary" onClick={load}>Refresh</button>
      </header>
      {error && <div className="notice error">{error}</div>}
      {message && <div className="notice">{message}</div>}

      {view === 'overview' && <>
        <section className="live-card"><div><em>LIVE RECOVERY INTAKE</em><h2>Start a revenue-risk workflow</h2><p>Create payment, checkout, subscription, or invoice recovery cases. Card details and OTP remain only on Razorpay’s hosted payment page.</p></div><div><button className="secondary" onClick={addDemoPack} disabled={busy}>Add demo pack</button><button onClick={() => setShowCreate(!showCreate)}>{showCreate ? 'Close form' : 'Start live recovery'}</button></div></section>
        {showCreate && <section className="create-form"><label>Risk type<select style={{ height: 40, border: '1px solid #cad8e8', borderRadius: 7, padding: '0 9px', color: '#18314f', background: '#fff', font: 'inherit' }} value={draft.riskType} onChange={event => setDraft({ ...draft, riskType: event.target.value })}>{['PAYMENT_FAILURE', 'CHECKOUT_ABANDONMENT', 'SUBSCRIPTION_FAILURE', 'OVERDUE_RECEIVABLE'].map(type => <option key={type} value={type}>{label(type)}</option>)}</select></label><label>Customer name<input value={draft.customerName} onChange={event => setDraft({ ...draft, customerName: event.target.value })} placeholder="Customer name" /></label><label>Email (optional)<input value={draft.customerEmail} onChange={event => setDraft({ ...draft, customerEmail: event.target.value })} placeholder="customer@example.com" /></label><label>Revenue at risk (INR)<input type="number" min="1" value={draft.amount} onChange={event => setDraft({ ...draft, amount: event.target.value })} /></label><button onClick={createLiveCase} disabled={busy}>Create risk case</button></section>}
        <section className="metrics"><Metric name="Revenue currently at risk" value={money(summary?.totalRevenueAtRisk ?? 0)} /><Metric name="Revenue recovered" value={money(summary?.revenueRecovered ?? 0)} good /><Metric name="Recovery rate (tracked)" value={`${(summary?.recoveryRate ?? 0).toFixed(2)}%`} /><OpenCasesMetric count={summary?.activeCases ?? openCases.length} review={() => setView('cases')} stopAll={stopAllOpen} busy={busy} /></section>
        <section className="columns"><article className="card"><h2>Open recovery queue</h2><p className="queue-help">Cases needing a merchant or customer action appear first.</p><Rows cases={queueCases.slice(0, 4)} select={id => { selectCase(id); setView('cases'); }} /></article><article className="card"><h2>Safety guardrails</h2><ul><li>Captured payments stop recovery</li><li>Contact permission is enforced</li><li>Duplicate links are blocked</li><li>Webhook events are idempotent</li></ul><button onClick={simulate} disabled={busy}>Run 240-case simulation</button></article></section>
      </>}

      {view === 'cases' && <section className="casegrid">
        <article className="card"><h2>Firestore recovery queue</h2><div className="actions" style={{ margin: '12px 0' }}>{['OPEN', 'WAITING_CUSTOMER', 'ESCALATED', 'RECOVERED', 'ALL'].map(filter => <button key={filter} className={caseFilter === filter ? '' : 'secondary'} style={{ padding: '7px 9px', fontSize: 11 }} onClick={() => setCaseFilter(filter)}>{filter === 'OPEN' ? 'Open' : label(filter)}</button>)}</div><Rows cases={filteredCases} selected={selected} select={selectCase} /></article>
        {current && <article className="detail">
          <div className="headline"><div><em>{current.caseReference}</em><h2>{current.customerName}</h2><p>{isDetectedRazorpayCase(current) ? 'Auto-detected from signed Razorpay event' : `${label(current.riskType)} · ${current.paymentMethod}`}</p></div><b className={`badge ${statusClass(current.status)}`}>{label(current.status)}</b></div>
          <div className="amount"><small>Revenue at risk</small><strong>{money(current.amountAtRisk)}</strong>{current.amountRecovered > 0 && <b>{money(current.amountRecovered)} recovered</b>}</div>
          <section className="facts"><div><small>Strategy source</small><b>{current.strategySource ? label(current.strategySource) : 'Analyze first'}</b></div><div><small>Diagnosis</small><b>{label(current.diagnosis)}</b></div><div><small>Recoverability</small><b>{current.recoverabilityScore === undefined ? 'Analyze first' : `${Math.round(current.recoverabilityScore * 100)}%`}</b></div><div><small>Action</small><b>{label(current.recommendedAction)}</b></div><div><small>Attempts</small><b>{current.attemptCount} / 3</b></div><div><small>Contact permission</small><b>{current.contactAllowed ? 'Approved' : 'Review required'}</b></div></section>
          {isDetectedRazorpayCase(current) && current.status === 'DETECTED' && <section className="review-form"><h3>Merchant review required</h3><p>This signed Razorpay failure was detected automatically. Add approved details before any recovery action.</p><label>Customer name<input value={reviewDraft.customerName} onChange={event => setReviewDraft({ ...reviewDraft, customerName: event.target.value })} placeholder="Approved customer name" /></label><label>Email (optional)<input value={reviewDraft.customerEmail} onChange={event => setReviewDraft({ ...reviewDraft, customerEmail: event.target.value })} placeholder="customer@example.com" /></label><label><input type="checkbox" checked={reviewDraft.contactAllowed} onChange={event => setReviewDraft({ ...reviewDraft, contactAllowed: event.target.checked })} /> I confirm contact permission for this recovery</label><button onClick={reviewDetected} disabled={busy}>Save human review</button></section>}
          <section className="why"><h3>Decision rationale</h3>{(current.reasons ?? ['No analysis yet.']).map(reason => <p key={reason}>{reason}</p>)}</section>
          {current.status === 'ESCALATED' && <section className="why human-handoff"><h3>Why this needs a human</h3><p>RecoverAI paused automation because this case is outside the approved autonomous recovery boundary. Sending a payment link or contacting the customer without review could be inappropriate.</p><p><b>Case-specific reason:</b> {(current.reasons ?? ['This case requires a merchant decision before any further action.']).join(' ')}</p><h3>Record the human outcome</h3><div className="create-form" style={{ margin: '8px 0 0', gridTemplateColumns: '1fr 1fr auto' }}><label>Outcome<select style={{ height: 40, border: '1px solid #cad8e8', borderRadius: 7, padding: '0 9px', color: '#18314f', background: '#fff', font: 'inherit' }} value={handoffDraft.outcome} onChange={event => setHandoffDraft({ ...handoffDraft, outcome: event.target.value })}><option value="PROMISE_TO_PAY">Promise to pay</option><option value="RECOVERED_EXTERNALLY">Recovered externally</option><option value="NOT_RECOVERABLE">Not recoverable</option></select></label>{handoffDraft.outcome === 'RECOVERED_EXTERNALLY' && <label>Recovered amount (INR)<input type="number" min="1" value={handoffDraft.amount} onChange={event => setHandoffDraft({ ...handoffDraft, amount: event.target.value })} /></label>}<button onClick={resolveHandoff} disabled={busy}>Save outcome</button></div><h3>What happens next</h3><ol><li>Review the failed payment and customer context outside this dashboard.</li><li>Choose a compliant manual follow-up through your approved merchant process.</li><li>Record the outcome above, or close the handoff when no further action is required.</li></ol></section>}
          <section className="actions">{current.status === 'ESCALATED' ? <button className="secondary" onClick={() => act('stop')} disabled={busy}>Close human handoff</button> : <>{!current.recommendedAction ? <button onClick={() => act('analyze')} disabled={busy || terminalStatuses.includes(current.status)}>Analyze case</button> : <button onClick={() => act('execute')} disabled={busy || terminalStatuses.includes(current.status) || current.status === 'ACTION_EXECUTED'}>{current.status === 'ACTION_EXECUTED' ? 'Scheduled task pending' : actionLabel(current.recommendedAction)}</button>}<button className="secondary" onClick={() => act('stop')} disabled={busy || terminalStatuses.includes(current.status)}>Stop recovery</button></>}</section>
          {tasks.length > 0 && <section className="why"><h3>Recovery task queue</h3>{tasks.map(task => <p key={task.id}><b>{label(task.type)}</b> · {label(task.status)} · due {new Date(task.dueAt).toLocaleString('en-IN')} {task.status === 'SCHEDULED' && <button style={{ marginLeft: 8, padding: '5px 7px', fontSize: 10 }} onClick={() => completeTask(task.id)} disabled={busy}>Mark completed</button>}</p>)}</section>}
          {paymentUrl && current.status === 'WAITING_CUSTOMER' && <section className="pay-next"><b>Customer payment step</b><p>Open the secure Razorpay Test Mode page. RecoverAI never sees the card number or OTP.</p><a href={paymentUrl} target="_blank" rel="noreferrer">Continue to Razorpay payment</a></section>}
          <section className="timeline"><h3>Audit timeline</h3>{audit.map(event => <div key={event.id}><b>{label(event.eventType)}</b><p>{event.message}</p><small>{new Date(event.createdAt).toLocaleString('en-IN')}</small></div>)}</section>
        </article>}
      </section>}

      {view === 'simulation' && <article className="card simulation"><h2>Synthetic evaluation</h2><p>Run a deterministic 240-case policy benchmark. It never contacts Razorpay or represents real merchant revenue.</p><button onClick={simulate} disabled={busy}>Run 240-case benchmark</button>{evaluation && <><mark>SIMULATED · {evaluation.datasetSize} cases · seed {evaluation.seed}</mark><section className="metrics"><Metric name="At risk" value={money(evaluation.totalAtRisk)} /><Metric name="Attempted" value={money(evaluation.totalAttempted)} /><Metric name="Recovered" value={money(evaluation.totalRecovered)} good /><Metric name="Recovery rate" value={`${Math.round(evaluation.recoveryRate * 100)}%`} /></section><p><b>{evaluation.policyApproved} approved</b> · {evaluation.policyBlocked} blocked by policy</p><div className="breakdown">{Object.entries(evaluation.byRiskType).map(([type, values]) => <div key={type}><span>{label(type)}</span><i><b style={{ width: `${values.atRisk ? Math.round(values.recovered / values.atRisk * 100) : 0}%` }} /></i><small>{money(values.recovered)} simulated recovered</small></div>)}</div></>}{evaluationHistory.length > 0 && <section className="evaluation-history"><h3>Recent evaluation runs</h3>{evaluationHistory.map(run => { const rate = Math.round(run.result.recoveryRate * 100); return <div key={run.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 20, flexWrap: 'wrap' }}><div style={{ display: 'grid', gap: 4 }}><b style={{ fontSize: 13 }}>Policy benchmark</b><small>{new Date(run.createdAt).toLocaleString('en-IN')} · {run.result.datasetSize} synthetic cases</small></div><div style={{ display: 'grid', gridTemplateColumns: 'auto auto', gap: '5px 10px', alignItems: 'center', minWidth: 220 }}><span style={{ fontSize: 11, color: '#6d829b' }}>Recovery rate</span><strong style={{ fontSize: 13, textAlign: 'right' }}>{rate}%</strong><i style={{ gridColumn: '1 / -1', width: '100%' }}><b style={{ width: `${rate}%` }} /></i></div></div>; })}</section>}</article>}
    </main>
  </div>;
}

function Rows({ cases, select, selected }: { cases: RecoveryCase[]; select: (id: string) => void; selected?: string }) {
  return <div className="rows">{cases.map(recoveryCase => <button className={selected === recoveryCase.id ? 'selected' : ''} onClick={() => select(recoveryCase.id)} key={recoveryCase.id}><span><b>{recoveryCase.caseReference}</b><small>{isDetectedRazorpayCase(recoveryCase) ? 'AUTO-DETECTED · RAZORPAY' : label(recoveryCase.riskType)}</small></span><span><b>{money(recoveryCase.amountAtRisk)}</b><small className={`case-status ${statusClass(recoveryCase.status)}`}>{label(recoveryCase.status)}</small></span></button>)}</div>;
}

createRoot(document.getElementById('root')!).render(<App />);
