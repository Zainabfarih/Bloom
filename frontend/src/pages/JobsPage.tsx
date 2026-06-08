import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import {
  Search, MapPin, Building2, X, ExternalLink, Loader2,
  CheckCircle2, Target, Trash2, Sparkles, Map as MapIcon,
  Bookmark, ArrowLeft,
} from 'lucide-react';
import { useState } from 'react';
import { jobApi } from '../api/job.api';
import { cvApi } from '../api/cv.api';
import { roadmapApi } from '../api/roadmap.api';
import { Spinner } from '../components/ui/Spinner';
import { useToast } from '../components/ui/Toast';
import type {
  JobSearchResponse, SavedJobResponse, JobDetailResponse, ApplyOption,
} from '../types';
import styles from './JobsPage.module.css';

// Always-working posting link: real apply link or a Google fallback.
const fallbackPosting = (title?: string, company?: string): string => {
  const q = encodeURIComponent([title, company, 'job'].filter(Boolean).join(' '));
  return `https://www.google.com/search?q=${q}`;
};

const httpLink = (url?: string): string | undefined =>
  url && /^https?:\/\//i.test(url) ? url : undefined;

interface MatchResult {
  score: number;
  matched: string[];
  missing: string[];
}

// Mirrors the backend SkillMatchingService so the preview equals the saved match.
const normalizeSkill = (s: string) => s.toLowerCase().replace(/[^a-z0-9]/g, '');

const computeMatch = (required: string[], cvSkills: string[]): MatchResult => {
  const have = new Set(cvSkills.map(normalizeSkill));
  const matched = required.filter(s => have.has(normalizeSkill(s)));
  const missing = required.filter(s => !have.has(normalizeSkill(s)));
  const score = required.length ? Math.round((matched.length / required.length) * 100) : 0;
  return { score, matched, missing };
};

interface OpenJob {
  jobExternalId: string;
  saved?: SavedJobResponse;
}

export const JobsPage = () => {
  const qc = useQueryClient();
  const toast = useToast();
  const navigate = useNavigate();

  const [query, setQuery] = useState('');
  const [location, setLocation] = useState('');
  const [submitted, setSubmitted] = useState('');
  const [submittedLocation, setSubmittedLocation] = useState('');
  const [activeTab, setActiveTab] = useState<'search' | 'saved'>('search');
  const [openJob, setOpenJob] = useState<OpenJob | null>(null);

  const { data: results, isLoading: searching } = useQuery<JobSearchResponse>({
    queryKey: ['job-search', submitted, submittedLocation],
    queryFn: () => jobApi.searchJobs(submitted, submittedLocation),
    enabled: submitted.length > 0,
  });

  const { data: savedJobs, isLoading: loadingSaved } = useQuery<SavedJobResponse[]>({
    queryKey: ['savedJobs'],
    queryFn: jobApi.getSavedJobs,
  });

  const {
    data: detail,
    isLoading: loadingDetail,
    isError: detailError,
  } = useQuery<JobDetailResponse>({
    queryKey: ['jobDetail', openJob?.jobExternalId],
    queryFn: () => jobApi.getJobDetails(openJob!.jobExternalId),
    enabled: !!openJob,
    retry: false,
  });

  const saveMutation = useMutation({
    mutationFn: (jobId: string) => jobApi.saveJob(jobId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['savedJobs'] });
      toast.success('Job saved to your list');
    },
    onError: (err: unknown) => {
      const e = err as { response?: { status?: number } };
      if (e?.response?.status === 503) {
        toast.error('Upload a CV first, then save this job');
      } else {
        toast.error('Could not save this job');
      }
    },
  });

  const removeMutation = useMutation({
    mutationFn: (jobExternalId: string) => jobApi.deleteSavedJob(jobExternalId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['savedJobs'] });
      toast.info('Job removed from saved');
    },
    onError: () => toast.error('Could not remove this job'),
  });

  const roadmapMutation = useMutation({
    mutationFn: (targetJobId: number) => roadmapApi.generate({ targetJobId }),
    onSuccess: () => {
      toast.success('Learning roadmap generated');
      navigate('/roadmap');
    },
    onError: () => toast.error('Could not generate the roadmap'),
  });

  const savedByExternalId = new Map(savedJobs?.map(s => [s.jobExternalId, s]) ?? []);
  const closeModal = () => setOpenJob(null);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    const q = query.trim();
    if (q) {
      setSubmitted(q);
      setSubmittedLocation(location.trim());
    }
  };

  const openSaved = openJob
    ? savedByExternalId.get(openJob.jobExternalId) ?? openJob.saved
    : undefined;

  return (
    <div>
      <h1 className="section-title">Jobs</h1>
      <p className="section-subtitle">Search opportunities, match them to your CV and plan your skills</p>

      <form onSubmit={handleSearch} className={styles.searchBar}>
        <Search size={16} color="var(--text-3)" style={{ flexShrink: 0 }} />
        <input
          value={query}
          onChange={e => setQuery(e.target.value)}
          placeholder="Search by title, skill, company…"
          aria-label="Job search query"
        />
        <div className={styles.searchDivider} />
        <MapPin size={14} color="var(--text-3)" style={{ flexShrink: 0 }} />
        <input
          value={location}
          onChange={e => setLocation(e.target.value)}
          placeholder="Location (optional)"
          className={styles.locationInput}
          aria-label="Location"
        />
        <button type="submit" className="btn btn--primary btn--sm">
          {searching ? <Loader2 size={14} style={{ animation: 'spin 0.75s linear infinite' }} /> : 'Search'}
        </button>
      </form>

      <div className={styles.tabs}>
        <button
          className={`${styles.tab} ${activeTab === 'search' ? styles.tabActive : ''}`}
          onClick={() => setActiveTab('search')}
        >
          Search Results
          {results && <span className={styles.tabCount}>{results.jobs.length}</span>}
        </button>
        <button
          className={`${styles.tab} ${activeTab === 'saved' ? styles.tabActive : ''}`}
          onClick={() => setActiveTab('saved')}
        >
          Saved Jobs
          {savedJobs && <span className={styles.tabCount}>{savedJobs.length}</span>}
        </button>
      </div>

      {activeTab === 'search' && (
        <div className={styles.jobList}>
          {searching && <Spinner center />}

          {!searching && !submitted && (
            <div className={styles.empty}>
              <Search size={40} color="var(--text-3)" />
              <p>Enter a job title or skill to start searching</p>
            </div>
          )}

          {!searching && submitted && results?.jobs.length === 0 && (
            <div className={styles.empty}>
              <p>No results for "<strong>{submitted}</strong>"</p>
              <p style={{ fontSize: 13 }}>Try different keywords or a broader search</p>
            </div>
          )}

          {results?.jobs.map(job => (
            <JobCard
              key={job.jobId}
              title={job.title}
              company={job.companyName}
              location={job.location}
              tags={job.extensions ?? []}
              tagsAreMeta
              matched={savedByExternalId.has(job.jobId)}
              onOpen={() => setOpenJob({ jobExternalId: job.jobId })}
            />
          ))}
        </div>
      )}

      {activeTab === 'saved' && (
        <div className={styles.jobList}>
          {loadingSaved && <Spinner center />}

          {!loadingSaved && savedJobs?.length === 0 && (
            <div className={styles.empty}>
              <Bookmark size={40} color="var(--text-3)" />
              <p>No saved jobs yet</p>
              <p style={{ fontSize: 13 }}>Open a job, match it with your CV and save it to track it here</p>
            </div>
          )}

          {savedJobs?.map(saved => (
            <JobCard
              key={saved.uuid}
              title={saved.jobTitle}
              company={saved.jobCompany}
              location={saved.jobLocation}
              tags={saved.matchedSkills}
              compatibilityScore={saved.compatibilityScore}
              matched
              onOpen={() => setOpenJob({ jobExternalId: saved.jobExternalId, saved })}
              onRemove={() => removeMutation.mutate(saved.jobExternalId)}
              removing={removeMutation.isPending}
            />
          ))}
        </div>
      )}

      {openJob && (
        <div
          className={styles.overlay}
          onClick={closeModal}
          role="dialog"
          aria-modal="true"
          aria-label="Job details"
        >
          <div className={styles.modal} onClick={e => e.stopPropagation()}>
            <button className={styles.modalClose} onClick={closeModal} aria-label="Close">
              <X size={18} />
            </button>

            {loadingDetail && !openSaved ? (
              <Spinner center />
            ) : detail || openSaved ? (
              <JobDetailView
                title={detail?.title ?? openSaved!.jobTitle}
                company={detail?.companyName ?? openSaved?.jobCompany}
                location={detail?.location ?? openSaved?.jobLocation}
                description={detail?.description}
                requiredSkills={detail?.extractedSkills ?? openSaved?.requiredSkills}
                applyOptions={detail?.applyOptions}
                fallbackApplyUrl={openSaved?.jobApplyUrl}
                saved={openSaved}
                saving={saveMutation.isPending}
                generating={roadmapMutation.isPending}
                onSave={() => saveMutation.mutate(openJob.jobExternalId)}
                onGenerateRoadmap={() => openSaved && roadmapMutation.mutate(openSaved.id)}
                onClose={closeModal}
              />
            ) : detailError ? (
              <div className={styles.modalContent}>
                <p className={styles.empty}>
                  We couldn't load this job. It may have expired from the search cache —
                  try searching for it again.
                </p>
                <div className={styles.modalActions}>
                  <button className="btn btn--ghost" onClick={closeModal}>Close</button>
                </div>
              </div>
            ) : (
              <p className={styles.empty}>No details available</p>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

const JobDetailView = ({
  title, company, location, description, requiredSkills, applyOptions, fallbackApplyUrl,
  saved, saving, generating, onSave, onGenerateRoadmap, onClose,
}: {
  title: string;
  company?: string;
  location?: string;
  description?: string;
  requiredSkills?: string[];
  applyOptions?: ApplyOption[];
  fallbackApplyUrl?: string;
  saved?: SavedJobResponse;
  saving: boolean;
  generating: boolean;
  onSave: () => void;
  onGenerateRoadmap: () => void;
  onClose: () => void;
}) => {
  const toast = useToast();
  const [preview, setPreview] = useState<MatchResult | null>(null);
  const [view, setView] = useState<'detail' | 'match'>(saved ? 'match' : 'detail');

  const links = (applyOptions ?? []).filter(o => httpLink(o.link));
  const fallback = httpLink(fallbackApplyUrl) ?? fallbackPosting(title, company);

  const matchMutation = useMutation({
    mutationFn: async () => {
      const cv = await cvApi.getActiveCv();
      return computeMatch(requiredSkills ?? [], cv.skills ?? []);
    },
    onSuccess: (res) => {
      setPreview(res);
      setView('match');
    },
    onError: () => toast.error('Upload a CV first, then match this job'),
  });

  // Saved record (server) wins; otherwise the local preview.
  const result: MatchResult | null = saved
    ? { score: saved.compatibilityScore, matched: saved.matchedSkills, missing: saved.missingSkills }
    : preview;

  const ApplySection = (
    <div className={styles.detailSection}>
      <h4 className={styles.detailHeading}><ExternalLink size={14} /> Apply</h4>
      <div className={styles.applyList}>
        {links.length > 0 ? (
          links.map((o, i) => (
            <a key={i} href={o.link} target="_blank" rel="noopener noreferrer" className="btn btn--soft btn--sm">
              {o.title ?? 'Apply'} <ExternalLink size={12} />
            </a>
          ))
        ) : (
          <a href={fallback} target="_blank" rel="noopener noreferrer" className="btn btn--soft btn--sm">
            View posting <ExternalLink size={12} />
          </a>
        )}
      </div>
    </div>
  );

  const Meta = (
    <div className={styles.modalMeta}>
      {company && <span><Building2 size={13} />{company}</span>}
      {location && <span><MapPin size={13} />{location}</span>}
      {saved?.savedAt && (
        <span>Saved {new Date(saved.savedAt).toLocaleDateString('en-GB', {
          day: '2-digit', month: 'short', year: 'numeric',
        })}</span>
      )}
    </div>
  );

  // ── Match result view ──
  if (view === 'match' && result) {
    const { score, matched, missing } = result;
    return (
      <div className={styles.modalContent}>
        <h2 className={styles.modalTitle}>{title}</h2>
        {Meta}

        <div className={styles.scoreHero}>
          <div className={`${styles.scoreRing} ${
            score >= 70 ? styles.scoreGreen : score >= 40 ? styles.scoreYellow : styles.scoreRed
          }`}>
            <span>{score}%</span>
          </div>
          <div>
            <p className={styles.scoreHeroTitle}>CV compatibility</p>
            <p className={styles.scoreHeroDesc}>
              {score >= 70 ? 'Strong fit for your profile'
                : score >= 40 ? 'Partial fit — a few skills to learn'
                : 'Some upskilling needed for this role'}
            </p>
          </div>
        </div>

        {matched.length > 0 && (
          <div className={styles.skillGroup}>
            <p className={styles.skillGroupLabel}><CheckCircle2 size={13} /> Skills you have ({matched.length})</p>
            <div className={styles.modalSkillsInline}>
              {matched.map(s => <span key={s} className="badge badge--green">{s}</span>)}
            </div>
          </div>
        )}

        {missing.length > 0 && (
          <div className={styles.skillGroup}>
            <p className={styles.skillGroupLabel}><Target size={13} /> Skills to learn ({missing.length})</p>
            <div className={styles.modalSkillsInline}>
              {missing.map(s => <span key={s} className="badge badge--red">{s}</span>)}
            </div>
          </div>
        )}

        {ApplySection}

        <div className={styles.modalActions}>
          {!saved ? (
            <>
              <button className="btn btn--ghost" onClick={() => setView('detail')}>
                <ArrowLeft size={14} /> Back
              </button>
              <button className="btn btn--primary" onClick={onSave} disabled={saving}>
                {saving ? <Spinner size={16} color="#fff" /> : <Bookmark size={14} />}
                Save job
              </button>
            </>
          ) : (
            <>
              <button className="btn btn--ghost" onClick={onClose}>Close</button>
              {missing.length > 0 && (
                <button className="btn btn--primary" onClick={onGenerateRoadmap} disabled={generating}>
                  {generating ? <Spinner size={16} color="#fff" /> : <MapIcon size={14} />}
                  Generate learning resources
                </button>
              )}
            </>
          )}
        </div>
      </div>
    );
  }

  // ── Detail view ──
  return (
    <div className={styles.modalContent}>
      <h2 className={styles.modalTitle}>{title}</h2>
      {Meta}

      {description && (
        <div className={styles.modalBody}>
          <p>{description}</p>
        </div>
      )}

      {requiredSkills && requiredSkills.length > 0 && (
        <div className={styles.detailSection}>
          <h4 className={styles.detailHeading}><Target size={14} /> Required skills</h4>
          <div className={styles.modalSkillsInline}>
            {requiredSkills.slice(0, 14).map(s => <span key={s} className="badge badge--accent">{s}</span>)}
          </div>
        </div>
      )}

      {ApplySection}

      <div className={styles.modalActions}>
        <button className="btn btn--ghost" onClick={onClose}>Close</button>
        <button
          className="btn btn--primary"
          onClick={() => matchMutation.mutate()}
          disabled={matchMutation.isPending || !requiredSkills?.length}
          title={!requiredSkills?.length ? 'No required skills detected for this job' : undefined}
        >
          {matchMutation.isPending ? <Spinner size={16} color="#fff" /> : <Sparkles size={14} />}
          Match with my CV
        </button>
      </div>
    </div>
  );
};

const JobCard = ({
  title, company, location, tags, compatibilityScore, tagsAreMeta,
  matched, onOpen, onRemove, removing,
}: {
  title: string;
  company?: string;
  location?: string;
  tags?: string[];
  compatibilityScore?: number;
  tagsAreMeta?: boolean;
  matched?: boolean;
  onOpen: () => void;
  onRemove?: () => void;
  removing?: boolean;
}) => (
  <div className={styles.jobCard} onClick={onOpen}>
    <div className={styles.jobHeader}>
      <div className={styles.jobTitleRow}>
        <h3 className={styles.jobTitle}>{title}</h3>
        {compatibilityScore != null && (
          <span className={`badge ${
            compatibilityScore >= 70 ? 'badge--green' :
            compatibilityScore >= 40 ? 'badge--yellow' : 'badge--red'
          }`}>
            {compatibilityScore}% match
          </span>
        )}
        {matched && compatibilityScore == null && (
          <span className="badge badge--green">Saved</span>
        )}
      </div>
      <div className={styles.jobMeta}>
        {company && <span><Building2 size={12} />{company}</span>}
        {location && <span><MapPin size={12} />{location}</span>}
      </div>
    </div>

    <div className={styles.jobFooter}>
      <div className={styles.skillBadges}>
        {(tags ?? []).slice(0, 5).map(s => (
          <span key={s} className={tagsAreMeta ? styles.metaTag : 'badge badge--accent'}>{s}</span>
        ))}
        {(tags?.length ?? 0) > 5 && (
          <span className={styles.moreTag}>+{(tags?.length ?? 0) - 5}</span>
        )}
      </div>

      {onRemove && (
        <div className={styles.jobActions} onClick={e => e.stopPropagation()}>
          <button
            className="btn btn--danger btn--sm"
            onClick={onRemove}
            disabled={removing}
            title="Remove from saved"
          >
            {removing ? <Spinner size={14} /> : <Trash2 size={14} />}
          </button>
        </div>
      )}
    </div>
  </div>
);
