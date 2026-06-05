import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import {
  Plus, ChevronRight, CheckCircle2, Circle, Clock,
  ExternalLink, BookOpen, X, Briefcase,
} from 'lucide-react';
import { roadmapApi } from '../api/roadmap.api';
import { jobApi } from '../api/job.api';
import { Spinner } from '../components/ui/Spinner';
import { useToast } from '../components/ui/Toast';
import type { RoadmapResponse, StepDTO, StepStatus, SavedJobResponse } from '../types';
import styles from './RoadmapPage.module.css';

export const RoadmapPage = () => {
  const qc = useQueryClient();
  const toast = useToast();
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [showGen, setShowGen] = useState(false);
  const [targetJobId, setTargetJobId] = useState<number | null>(null);
  const [genError, setGenError] = useState('');

  const { data: roadmaps, isLoading } = useQuery<RoadmapResponse[]>({
    queryKey: ['roadmaps'],
    queryFn: roadmapApi.getAll,
  });

  /* Keep previous data while refetching so the panel never blanks out. */
  const { data: detail, isLoading: loadingDetail } = useQuery<RoadmapResponse>({
    queryKey: ['roadmap', selectedId],
    queryFn: () => roadmapApi.getById(selectedId!),
    enabled: selectedId !== null,
    placeholderData: (prev) => prev,
  });

  const activeRoadmap = selectedId !== null
    ? (detail ?? roadmaps?.find(r => r.id === selectedId))
    : roadmaps?.[0];

  const activeId = selectedId ?? roadmaps?.[0]?.id ?? null;

  /* Saved jobs — used both for the picker and to resolve the real job title/link. */
  const { data: savedJobs } = useQuery<SavedJobResponse[]>({
    queryKey: ['savedJobs'],
    queryFn: jobApi.getSavedJobs,
  });

  /* Look up a saved job by its id (== roadmap.targetJobId). */
  const savedJobById = new Map((savedJobs ?? []).map(j => [j.id, j]));
  /** Real job name for a roadmap — falls back to the stored title if the job was removed. */
  const titleFor = (r: RoadmapResponse) =>
    savedJobById.get(r.targetJobId)?.jobTitle ?? r.targetJobTitle;

  const generateMutation = useMutation<RoadmapResponse, unknown, { targetJobId: number }>({
    mutationFn: ({ targetJobId }) => roadmapApi.generate({ targetJobId }),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ['roadmaps'] });
      setSelectedId(data.id);
      setShowGen(false);
      setTargetJobId(null);
      setGenError('');
      toast.success('Roadmap generated');
    },
    onError: (err: unknown) => {
      const error = err as { response?: { data?: { message?: string } } };
      const msg = error?.response?.data?.message ?? 'Generation failed. Please try again.';
      setGenError(msg);
      toast.error(msg);
    },
  });

  const statusMutation = useMutation({
    mutationFn: ({ stepId, status }: { stepId: number; status: StepStatus }) =>
      roadmapApi.updateStepStatus(stepId, status),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['roadmap', activeId] });
      qc.invalidateQueries({ queryKey: ['roadmaps'] });
    },
    onError: () => toast.error('Could not update step. Please try again.'),
  });

  const handleGenerate = (e: React.FormEvent) => {
    e.preventDefault();
    if (targetJobId && targetJobId > 0) {
      generateMutation.mutate({ targetJobId });
    } else {
      setGenError('Please select a saved job first.');
    }
  };

  return (
    <div className={styles.page}>
      <div className={styles.pageHeader}>
        <div>
          <h1 className="section-title">Learning Roadmaps</h1>
          <p className="section-subtitle">Your personalised skill progression paths</p>
        </div>
        <button className="btn btn--primary" onClick={() => { setShowGen(v => !v); setGenError(''); }}>
          {showGen ? <X size={15} /> : <Plus size={15} />}
          {showGen ? 'Cancel' : 'Generate Roadmap'}
        </button>
      </div>

      {showGen && (
        <div className={`card ${styles.genCard}`}>
          <h3 className={styles.genTitle}>Generate a new roadmap</h3>
          <p className={styles.genSubtitle}>
            Pick one of your saved jobs — we'll map the skills you're missing into a step-by-step path.
          </p>
          <form onSubmit={handleGenerate} className={styles.genForm}>
            {!savedJobs ? (
              <Spinner center />
            ) : savedJobs.length === 0 ? (
              <div className={styles.genEmpty}>
                <Briefcase size={28} color="var(--text-3)" />
                <p>You haven't saved any jobs yet. Save a job first, then come back to generate a roadmap.</p>
              </div>
            ) : (
              <>
                <div className={styles.savedJobList}>
                  {savedJobs.map(job => (
                    <button
                      key={job.id}
                      type="button"
                      className={`${styles.savedJobBtn} ${targetJobId === job.id ? styles.savedJobBtnActive : ''}`}
                      onClick={() => { setTargetJobId(job.id); setGenError(''); }}
                      aria-pressed={targetJobId === job.id}
                    >
                      <span className={styles.savedJobTitle}>{job.jobTitle}</span>
                      <span className={styles.savedJobCompany}>{job.jobCompany}</span>
                      {job.missingSkills?.length > 0 && (
                        <span className={styles.savedJobGap}>{job.missingSkills.length} skills to learn</span>
                      )}
                    </button>
                  ))}
                </div>
                <button
                  type="submit"
                  className="btn btn--primary"
                  disabled={!targetJobId || generateMutation.isPending}
                >
                  {generateMutation.isPending ? <Spinner size={16} color="#fff" /> : 'Generate roadmap'}
                </button>
              </>
            )}
            {genError && <p className={styles.genError} role="alert">{genError}</p>}
          </form>
        </div>
      )}

      {isLoading ? (
        <Spinner center />
      ) : !roadmaps || roadmaps.length === 0 ? (
        <div className={styles.empty}>
          <BookOpen size={40} color="var(--text-3)" />
          <p>No roadmaps yet.</p>
          <button className="btn btn--primary" onClick={() => setShowGen(true)}>
            <Plus size={15} /> Generate your first roadmap
          </button>
        </div>
      ) : (
        <div className={styles.layout}>
          <div className={styles.roadmapList}>
            {roadmaps.map(r => {
              const done  = r.steps.filter(s => isDone(s.status)).length;
              const total = r.steps.length;
              const pct   = total > 0 ? Math.round((done / total) * 100) : 0;
              const isActive = activeId === r.id;

              return (
                <button
                  key={r.id}
                  className={`${styles.roadmapItem} ${isActive ? styles.roadmapItemActive : ''}`}
                  onClick={() => setSelectedId(r.id)}
                  aria-current={isActive}
                >
                  <div className={styles.roadmapItemHeader}>
                    <span className={styles.roadmapItemTitle}>{titleFor(r)}</span>
                    <ChevronRight size={14} />
                  </div>
                  <div className={styles.miniBar}>
                    <div style={{ width: `${pct}%` }} />
                  </div>
                  <span className={styles.roadmapItemMeta}>{done}/{total} steps · {pct}%</span>
                </button>
              );
            })}
          </div>

          <div className={styles.stepsPanel}>
            {loadingDetail && !activeRoadmap ? (
              <Spinner center />
            ) : activeRoadmap ? (
              <RoadmapDetail
                roadmap={activeRoadmap}
                jobTitle={titleFor(activeRoadmap)}
                savedJob={savedJobById.get(activeRoadmap.targetJobId)}
                onStatusChange={(stepId, status) => statusMutation.mutate({ stepId, status })}
                updating={statusMutation.isPending}
              />
            ) : null}
          </div>
        </div>
      )}
    </div>
  );
};

// ── Status helpers (mirror backend StepStatus enum) ───────────────────
const isDone = (s: StepStatus) => s === 'COMPLETED' || s === 'ACCEPTED';

const nextStatus = (s: StepStatus): StepStatus => {
  switch (s) {
    case 'PENDING':     return 'IN_PROGRESS';
    case 'IN_PROGRESS': return 'COMPLETED';
    case 'COMPLETED':
    case 'ACCEPTED':    return 'PENDING';
    case 'REJECTED':    return 'IN_PROGRESS';
    default:            return 'IN_PROGRESS';
  }
};

const statusLabel = (s: StepStatus) =>
  s.charAt(0) + s.slice(1).toLowerCase().replace('_', ' ');

const StepIcon = ({ status }: { status: StepStatus }) => {
  if (isDone(status))            return <CheckCircle2 size={20} color="var(--green)" />;
  if (status === 'IN_PROGRESS') return <Clock size={20} color="var(--yellow)" />;
  return <Circle size={20} color="var(--text-3)" />;
};

// ── Roadmap Detail ────────────────────────────────────────────────────
const RoadmapDetail = ({
  roadmap, jobTitle, savedJob, onStatusChange, updating,
}: {
  roadmap: RoadmapResponse;
  jobTitle: string;
  savedJob?: SavedJobResponse;
  onStatusChange: (stepId: number, status: StepStatus) => void;
  updating: boolean;
}) => {
  const done  = roadmap.steps.filter(s => isDone(s.status)).length;
  const total = roadmap.steps.length;
  const pct   = total > 0 ? Math.round((done / total) * 100) : 0;

  return (
    <div>
      <div className={styles.detailHeader}>
        <div>
          <h2 className={styles.detailTitle}>{jobTitle}</h2>
          <p className={styles.detailMeta}>
            {savedJob?.jobCompany ? `${savedJob.jobCompany} · ` : ''}{done}/{total} steps completed
          </p>
        </div>

        {savedJob && (
          savedJob.jobApplyUrl ? (
            <a
              className="btn btn--soft btn--sm"
              href={savedJob.jobApplyUrl}
              target="_blank"
              rel="noopener noreferrer"
              title="Open the job posting"
            >
              <Briefcase size={13} /> View job <ExternalLink size={12} />
            </a>
          ) : (
            <Link className="btn btn--soft btn--sm" to="/jobs" title="Go to your saved jobs">
              <Briefcase size={13} /> View job
            </Link>
          )
        )}
      </div>

      <div className={styles.progressRow}>
        <div className={styles.progressBar}>
          <div className={styles.progressFill} style={{ width: `${pct}%` }} />
        </div>
        <span className={styles.progressPct}>{pct}%</span>
      </div>

      <div className={styles.stepsList}>
        {[...roadmap.steps]
          .sort((a, b) => a.orderIndex - b.orderIndex)
          .map((step, i) => (
            <StepCard
              key={step.id}
              step={step}
              index={i + 1}
              onToggle={() => onStatusChange(step.id, nextStatus(step.status))}
              disabled={updating}
            />
          ))}
      </div>
    </div>
  );
};

// ── Step Card ─────────────────────────────────────────────────────────
const StepCard = ({
  step, index, onToggle, disabled,
}: {
  step: StepDTO;
  index: number;
  onToggle: () => void;
  disabled: boolean;
}) => {
  const [expanded, setExpanded] = useState(false);

  const statusClass =
    isDone(step.status)            ? styles.stepCompleted :
    step.status === 'IN_PROGRESS'  ? styles.stepInProgress : '';

  return (
    <div className={`${styles.stepCard} ${statusClass}`}>
      <button
        className={styles.stepToggle}
        onClick={onToggle}
        disabled={disabled}
        aria-label={`Mark "${step.title}" as ${statusLabel(nextStatus(step.status))}`}
      >
        <StepIcon status={step.status} />
      </button>

      <div className={styles.stepContent}>
        <div className={styles.stepHeader}>
          <span className={styles.stepIndex}>Step {index}</span>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            {step.estimatedDuration && (
              <span className={styles.stepDuration}>{step.estimatedDuration}</span>
            )}
            <span className={`badge ${
              isDone(step.status)           ? 'badge--green' :
              step.status === 'IN_PROGRESS' ? 'badge--yellow' : ''
            }`}>
              {statusLabel(step.status)}
            </span>
          </div>
        </div>

        <p className={styles.stepTitle}>{step.title}</p>

        {step.description && (
          <>
            <p className={`${styles.stepDesc} ${expanded ? styles.stepDescExpanded : ''}`}>
              {step.description}
            </p>
            {step.description.length > 120 && (
              <button className={styles.expandBtn} onClick={() => setExpanded(v => !v)}>
                {expanded ? 'Show less' : 'Read more'}
              </button>
            )}
          </>
        )}

        {step.resources && step.resources.length > 0 && (
          <div className={styles.resources}>
            {step.resources.map(r => (
              <a
                key={r.id}
                href={r.url}
                target="_blank"
                rel="noopener noreferrer"
                className={styles.resourceLink}
              >
                <ExternalLink size={11} />
                {r.title}
                <span className={styles.resourceType}>{r.type.toLowerCase()}</span>
              </a>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
