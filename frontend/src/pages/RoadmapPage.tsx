import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import {
  Plus, ChevronRight, CheckCircle2, Circle, Clock,
  ExternalLink, BookOpen, Trash2, X,
} from 'lucide-react';
import { roadmapApi } from '../api/roadmap.api';
import { jobApi } from '../api/job.api';
import { Spinner } from '../components/ui/Spinner';
import type { RoadmapResponse, StepDTO, StepStatus, SavedJobResponse } from '../types';
import styles from './RoadmapPage.module.css';

export const RoadmapPage = () => {
  const qc = useQueryClient();
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [showGen, setShowGen] = useState(false);
  const [targetJobId, setTargetJobId] = useState('');
  const [genError, setGenError] = useState('');

  const { data: roadmaps, isLoading } = useQuery<RoadmapResponse[]>({
    queryKey: ['roadmaps'],
    queryFn: roadmapApi.getAll,
  });

  /* ─── BUG FIX: keep previous data while refetching to prevent blank panel ─── */
  const { data: detail, isLoading: loadingDetail } = useQuery<RoadmapResponse>({
    queryKey: ['roadmap', selectedId],
    queryFn: () => roadmapApi.getById(selectedId!),
    enabled: selectedId !== null,
    placeholderData: (prev) => prev,   // keeps old data visible during refetch
  });

  /* Active roadmap: prefer explicitly-selected detail, else first in list */
  const activeRoadmap = selectedId !== null
    ? (detail ?? roadmaps?.find(r => r.id === selectedId))
    : roadmaps?.[0];

  const activeId = selectedId ?? roadmaps?.[0]?.id ?? null;

  /* Saved jobs for the picker */
  const { data: savedJobs } = useQuery<SavedJobResponse[]>({
    queryKey: ['savedJobs'],
    queryFn: jobApi.getSavedJobs,
    enabled: showGen,
  });

  const handleGenerate = (e: React.FormEvent) => {
    e.preventDefault();
    const id = Number(targetJobId);
    if (!Number.isNaN(id) && id > 0) {
      generateMutation.mutate({ targetJobId: id });
    } else {
      setGenError('Please select or enter a valid job ID');
    }
  };

  const generateMutation = useMutation<RoadmapResponse, unknown, { targetJobId: number }>({
    mutationFn: ({ targetJobId }) => roadmapApi.generate({ targetJobId }),
    onSuccess: (data) => {
      qc.invalidateQueries({ queryKey: ['roadmaps'] });
      setSelectedId(data.id);
      setShowGen(false);
      setTargetJobId('');
      setGenError('');
    },
    onError: (err: unknown) => {
      const error = err as { response?: { data?: { message?: string } } };
      setGenError(error?.response?.data?.message ?? 'Generation failed');
    },
  });

  const statusMutation = useMutation({
    mutationFn: ({ stepId, status }: { stepId: number; status: StepStatus }) =>
      roadmapApi.updateStepStatus(stepId.toString(), status),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['roadmap', activeId] });
      qc.invalidateQueries({ queryKey: ['roadmaps'] });
    },
  });

  const deleteMutation = useMutation({
    mutationFn: (roadmapId: number) => roadmapApi.deleteRoadmap(roadmapId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['roadmaps'] });
      setSelectedId(null);
    },
  });

  return (
    <div className={styles.page}>
      <div className={styles.pageHeader}>
        <div>
          <h1 className="section-title">Learning Roadmaps</h1>
          <p className="section-subtitle">Your personalised skill progression paths</p>
        </div>
        <button
          className="btn btn--primary"
          onClick={() => setShowGen(v => !v)}
        >
          {showGen ? <X size={15} /> : <Plus size={15} />}
          {showGen ? 'Cancel' : 'Generate Roadmap'}
        </button>
      </div>

      {/* Generate form */}
      {showGen && (
        <div className={`card ${styles.genCard}`}>
          <h3 className={styles.genTitle}>Generate a new roadmap</h3>
          <p className={styles.genSubtitle}>
            Select a saved job or enter a job ID to generate a personalised learning path.
          </p>
          <form onSubmit={handleGenerate} className={styles.genForm}>
            {savedJobs && savedJobs.length > 0 ? (
              <div className={styles.jobPicker}>
                <p className={styles.genLabel}>Pick from saved jobs</p>
                <div className={styles.savedJobList}>
                  {savedJobs.map(job => (
                    <button
                      key={job.uuid}
                      type="button"
                      className={`${styles.savedJobBtn} ${
                        targetJobId === String(job.jobExternalId) ? styles.savedJobBtnActive : ''
                      }`}
                      onClick={() => setTargetJobId(job.jobExternalId)}
                    >
                      <span className={styles.savedJobTitle}>{job.jobTitle}</span>
                      <span className={styles.savedJobCompany}>{job.jobCompany}</span>
                    </button>
                  ))}
                </div>
                <div className={styles.genDivider}>
                  <span>or enter ID manually</span>
                </div>
              </div>
            ) : null}

            <div className={styles.genRow}>
              <div className={`field ${styles.fieldGrow}`}>
                <label>Job ID</label>
                <input
                  value={targetJobId}
                  onChange={e => setTargetJobId(e.target.value)}
                  placeholder="e.g. 42"
                />
              </div>
              <button
                type="submit"
                className="btn btn--primary"
                disabled={!targetJobId.trim() || generateMutation.isPending}
              >
                {generateMutation.isPending ? <Spinner size={16} color="#fff" /> : 'Generate'}
              </button>
            </div>
            {genError && <p className={styles.genError}>{genError}</p>}
          </form>
        </div>
      )}

      {isLoading ? (
        <Spinner center />
      ) : !roadmaps || roadmaps.length === 0 ? (
        <div className={styles.empty}>
          <BookOpen size={40} color="var(--text-3)" />
          <p>No roadmaps yet.</p>
          <button
            className="btn btn--primary"
            onClick={() => setShowGen(true)}
          >
            <Plus size={15} /> Generate your first roadmap
          </button>
        </div>
      ) : (
        <div className={styles.layout}>
          {/* Sidebar list */}
          <div className={styles.roadmapList}>
            {roadmaps.map(r => {
              const done  = r.steps.filter(s => s.status === 'COMPLETED').length;
              const total = r.steps.length;
              const pct   = total > 0 ? Math.round((done / total) * 100) : 0;
              const isActive = activeId === r.id;

              return (
                <button
                  key={r.id}
                  className={`${styles.roadmapItem} ${isActive ? styles.roadmapItemActive : ''}`}
                  onClick={() => setSelectedId(r.id)}
                >
                  <div className={styles.roadmapItemHeader}>
                    <span className={styles.roadmapItemTitle}>{r.targetJobTitle}</span>
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

          {/* Steps detail */}
          <div className={styles.stepsPanel}>
            {loadingDetail && !activeRoadmap ? (
              <Spinner center />
            ) : activeRoadmap ? (
              <RoadmapDetail
                roadmap={activeRoadmap}
                onStatusChange={(stepId, status) =>
                  statusMutation.mutate({ stepId, status })
                }
                onDelete={() => deleteMutation.mutate(activeRoadmap.id)}
                updating={statusMutation.isPending}
                deleting={deleteMutation.isPending}
              />
            ) : null}
          </div>
        </div>
      )}
    </div>
  );
};

// ── Status cycle ──────────────────────────────────────────────────────
const STATUS_CYCLE: Record<StepStatus, StepStatus> = {
  NOT_STARTED: 'IN_PROGRESS',
  IN_PROGRESS:  'COMPLETED',
  COMPLETED:    'NOT_STARTED',
};

const StepIcon = ({ status }: { status: StepStatus }) => {
  if (status === 'COMPLETED')   return <CheckCircle2 size={20} color="var(--green)" />;
  if (status === 'IN_PROGRESS') return <Clock size={20} color="var(--yellow)" />;
  return <Circle size={20} color="var(--text-3)" />;
};

// ── Roadmap Detail ────────────────────────────────────────────────────
const RoadmapDetail = ({
  roadmap, onStatusChange, onDelete, updating, deleting,
}: {
  roadmap: RoadmapResponse;
  onStatusChange: (stepId: number, status: StepStatus) => void;
  onDelete: () => void;
  updating: boolean;
  deleting: boolean;
}) => {
  const done  = roadmap.steps.filter(s => s.status === 'COMPLETED').length;
  const total = roadmap.steps.length;
  const pct   = total > 0 ? Math.round((done / total) * 100) : 0;

  return (
    <div>
      <div className={styles.detailHeader}>
        <div>
          <h2 className={styles.detailTitle}>{roadmap.targetJobTitle}</h2>
          <p className={styles.detailMeta}>{done}/{total} steps completed</p>
        </div>
        <button
          className="btn btn--danger btn--sm"
          onClick={onDelete}
          disabled={deleting}
          title="Delete roadmap"
        >
          {deleting ? <Spinner size={14} /> : <Trash2 size={14} />}
        </button>
      </div>

      {/* Progress bar */}
      <div className={styles.progressRow}>
        <div className={styles.progressBar}>
          <div className={styles.progressFill} style={{ width: `${pct}%` }} />
        </div>
        <span className={styles.progressPct}>{pct}%</span>
      </div>

      {/* Steps */}
      <div className={styles.stepsList}>
        {[...roadmap.steps]
          .sort((a, b) => a.orderIndex - b.orderIndex)
          .map((step, i) => (
            <StepCard
              key={step.id}
              step={step}
              index={i + 1}
              onToggle={() => onStatusChange(step.id, STATUS_CYCLE[step.status])}
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
    step.status === 'COMPLETED'   ? styles.stepCompleted :
    step.status === 'IN_PROGRESS' ? styles.stepInProgress : '';

  return (
    <div className={`${styles.stepCard} ${statusClass}`}>
      <button
        className={styles.stepToggle}
        onClick={onToggle}
        disabled={disabled}
        aria-label={`Mark step ${index} as ${STATUS_CYCLE[step.status].toLowerCase().replace('_', ' ')}`}
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
              step.status === 'COMPLETED'   ? 'badge--green' :
              step.status === 'IN_PROGRESS' ? 'badge--yellow' : ''
            }`}>
              {step.status.replace('_', ' ')}
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
              <button
                className={styles.expandBtn}
                onClick={() => setExpanded(v => !v)}
              >
                {expanded ? 'Show less' : 'Read more'}
              </button>
            )}
          </>
        )}

        {/* Resources */}
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
