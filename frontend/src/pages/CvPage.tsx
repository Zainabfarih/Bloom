import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Upload, Trash2, ChevronDown, ChevronUp, FileText,
  CheckCircle2, AlertTriangle, Zap, FilePlus, Download,
} from 'lucide-react';
import { useRef, useState } from 'react';
import { cvApi } from '../api/cv.api';
import { Spinner } from '../components/ui/Spinner';
import { useToast } from '../components/ui/Toast';
import { ManualCvForm } from '../components/cv/ManualCvForm';
import { saveBlob } from '../lib/download';
import type { CvResponse, CvAnalysisResponse } from '../types';
import styles from './CvPage.module.css';

export const CvPage = () => {
  const qc = useQueryClient();
  const toast = useToast();
  const fileRef = useRef<HTMLInputElement>(null);
  const [expandedUuid, setExpandedUuid] = useState<string | null>(null);
  const [uploadError, setUploadError] = useState('');
  const [dragOver, setDragOver] = useState(false);
  const [manualOpen, setManualOpen] = useState(false);

  const { data: cvs, isLoading } = useQuery({
    queryKey: ['cvs'],
    queryFn: cvApi.getAllMyCvs,
  });

  const uploadMutation = useMutation({
    mutationFn: (file: File) => cvApi.upload(file),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['cvs'] });
      setUploadError('');
      toast.success('CV uploaded and analysed');
    },
    onError: (err: unknown) => {
      const e = err as { response?: { data?: { message?: string } } };
      const msg = e?.response?.data?.message ?? 'Upload failed. Please try again.';
      setUploadError(msg);
      toast.error(msg);
    },
  });

  const deleteMutation = useMutation({
    mutationFn: cvApi.deleteCv,
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['cvs'] });
      toast.info('CV deleted');
    },
    onError: () => toast.error('Could not delete this CV'),
  });

  const isPdf = (file: File) =>
    file.type === 'application/pdf' || file.name.toLowerCase().endsWith('.pdf');

  const submitFile = (file: File) => {
    if (!isPdf(file)) {
      const msg = 'Only PDF files are supported.';
      setUploadError(msg);
      toast.error(msg);
      return;
    }
    uploadMutation.mutate(file);
  };

  const handleFile = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) submitFile(file);
    e.target.value = '';
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    const file = e.dataTransfer.files[0];
    if (file) submitFile(file);
  };

  return (
    <div>
      <div className={styles.pageHeader}>
        <div>
          <h1 className="section-title">My CVs</h1>
          <p className="section-subtitle">Upload, create and analyse your resumes</p>
        </div>
        <button className="btn btn--primary" onClick={() => setManualOpen(true)}>
          <FilePlus size={16} /> Create manually
        </button>
      </div>

      <div
        className={`${styles.dropzone} ${dragOver ? styles.dropzoneDrag : ''}`}
        onClick={() => fileRef.current?.click()}
        onDrop={handleDrop}
        onDragOver={e => { e.preventDefault(); setDragOver(true); }}
        onDragLeave={() => setDragOver(false)}
        role="button"
        tabIndex={0}
        aria-label="Upload CV"
        onKeyDown={e => e.key === 'Enter' && fileRef.current?.click()}
      >
        <input ref={fileRef} type="file" accept="application/pdf,.pdf" hidden onChange={handleFile} />
        {uploadMutation.isPending ? (
          <>
            <Spinner size={32} />
            <p>Uploading and analysing…</p>
          </>
        ) : (
          <>
            <div className={styles.dropzoneIcon}>
              <Upload size={24} />
            </div>
            <p>Drop your CV here or <span>browse files</span></p>
            <small>PDF only · up to 10 MB</small>
          </>
        )}
      </div>

      {uploadError && (
        <p className={styles.uploadError} role="alert">
          <AlertTriangle size={14} />{uploadError}
        </p>
      )}

      {isLoading ? (
        <Spinner center />
      ) : !cvs || cvs.length === 0 ? (
        <div className={styles.empty}>
          <FileText size={40} color="var(--text-3)" />
          <p>No CVs yet. Upload one above or create it manually.</p>
        </div>
      ) : (
        <div className={styles.cvList}>
          {cvs.map(cv => (
            <CvCard
              key={cv.uuid}
              cv={cv}
              expanded={expandedUuid === cv.uuid}
              onToggle={() => setExpandedUuid(expandedUuid === cv.uuid ? null : cv.uuid)}
              onDelete={() => deleteMutation.mutate(cv.uuid)}
              deleting={deleteMutation.isPending}
            />
          ))}
        </div>
      )}

      {manualOpen && <ManualCvForm onClose={() => setManualOpen(false)} />}
    </div>
  );
};

const CvCard = ({
  cv, expanded, onToggle, onDelete, deleting,
}: {
  cv: CvResponse;
  expanded: boolean;
  onToggle: () => void;
  onDelete: () => void;
  deleting: boolean;
}) => {
  const toast = useToast();
  const [downloading, setDownloading] = useState(false);

  const { data: analysis, isLoading: loadingAnalysis } = useQuery({
    queryKey: ['cv-analysis', cv.uuid],
    queryFn: () => cvApi.getCvAnalysis(cv.uuid),
    enabled: expanded,
  });

  const { data: skills } = useQuery({
    queryKey: ['cv-skills', cv.uuid],
    queryFn: () => cvApi.getCvSkills(cv.uuid),
    enabled: expanded,
  });

  const displayName = cv.originalFilename ?? cv.title ?? `CV ${cv.uuid.slice(0, 8)}`;

  const handleDownload = async () => {
    setDownloading(true);
    try {
      const blob = await cvApi.downloadCv(cv.uuid);
      const filename = cv.originalFilename ?? `${cv.uuid.slice(0, 8)}.pdf`;
      saveBlob(blob, filename);
    } catch {
      toast.error('Could not download this CV');
    } finally {
      setDownloading(false);
    }
  };

  return (
    <div className={styles.cvCard}>
      <div className={styles.cvCardHeader}>
        <div className={styles.cvCardInfo}>
          <div className={styles.cvIcon}>
            <FileText size={16} />
          </div>
          <div>
            <p className={styles.cvName}>{displayName}</p>
            <p className={styles.cvDate}>
              {new Date(cv.createdAt).toLocaleDateString('en-GB', {
                day: '2-digit', month: 'short', year: 'numeric',
              })}
              {cv.active && (
                <span className={`badge badge--green ${styles.activeBadge}`}>Active</span>
              )}
            </p>
          </div>
        </div>

        <div className={styles.cvCardActions}>
          <button
            className="btn btn--ghost btn--sm"
            onClick={handleDownload}
            disabled={downloading}
            aria-label="Download CV"
          >
            {downloading ? <Spinner size={14} /> : <Download size={15} />}
          </button>
          <button className="btn btn--ghost btn--sm" onClick={onToggle}>
            {expanded ? <ChevronUp size={15} /> : <ChevronDown size={15} />}
            {expanded ? 'Close' : 'Analyse'}
          </button>
          <button
            className="btn btn--danger btn--sm"
            onClick={onDelete}
            disabled={deleting}
            aria-label="Delete CV"
          >
            {deleting ? <Spinner size={14} /> : <Trash2 size={14} />}
          </button>
        </div>
      </div>

      {expanded && (
        <div className={styles.cvExpanded}>
          {loadingAnalysis ? (
            <Spinner center />
          ) : analysis ? (
            <CvAnalysis analysis={analysis} skills={skills?.map(s => s.name) ?? []} />
          ) : (
            <p className={styles.analysisEmpty}>
              Analysis not available for this CV.
            </p>
          )}
        </div>
      )}
    </div>
  );
};

const CvAnalysis = ({
  analysis,
  skills,
}: {
  analysis: CvAnalysisResponse;
  skills: string[];
}) => (
  <div className={styles.analysis}>
    <div className={styles.scoreRow}>
      <div className={styles.scoreCircle}>
        <svg viewBox="0 0 36 36" className={styles.scoreArc}>
          <circle cx="18" cy="18" r="15.9" fill="none" strokeWidth="3"
            stroke="var(--bg-4)" />
          <circle cx="18" cy="18" r="15.9" fill="none" strokeWidth="3"
            stroke={analysis.atsScore >= 70 ? 'var(--green)' :
              analysis.atsScore >= 40 ? 'var(--yellow)' : 'var(--red)'}
            strokeDasharray={`${analysis.atsScore} 100`}
            strokeLinecap="round"
            transform="rotate(-90 18 18)" />
        </svg>
        <span className={styles.scoreNumber}>{analysis.atsScore}</span>
      </div>
      <div>
        <p className={styles.scoreLabel}>ATS Score</p>
        <p className={styles.scoreDesc}>
          {analysis.atsScore >= 70 ? 'Great compatibility' :
           analysis.atsScore >= 40 ? 'Room for improvement' :
           'Needs work'}
        </p>
      </div>
    </div>

    {analysis.summary && (
      <div className={styles.analysisSection}>
        <h4><Zap size={13} /> Summary</h4>
        <p>{analysis.summary}</p>
      </div>
    )}

    {analysis.strengths?.length > 0 && (
      <div className={styles.analysisSection}>
        <h4><CheckCircle2 size={13} /> Strengths</h4>
        <ul className={styles.strengthList}>
          {analysis.strengths.map((s, i) => (
            <li key={i}>{s}</li>
          ))}
        </ul>
      </div>
    )}

    {analysis.issues?.length > 0 && (
      <div className={styles.analysisSection}>
        <h4><AlertTriangle size={13} /> Issues</h4>
        {analysis.issues.map((issue, i) => (
          <div key={i} className={`${styles.issueItem} ${styles[`issue_${issue.severity.toLowerCase()}`]}`}>
            <p className={styles.issueMessage}>{issue.message}</p>
            {issue.suggestion && (
              <p className={styles.issueSuggestion}>💡 {issue.suggestion}</p>
            )}
          </div>
        ))}
      </div>
    )}

    {skills.length > 0 && (
      <div className={styles.analysisSection}>
        <h4>Skills detected ({skills.length})</h4>
        <div className={styles.skillBadges}>
          {skills.map(s => (
            <span key={s} className="badge badge--accent">{s}</span>
          ))}
        </div>
      </div>
    )}
  </div>
);
