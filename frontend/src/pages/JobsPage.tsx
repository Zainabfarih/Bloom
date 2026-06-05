import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Search, Bookmark, BookmarkCheck, MapPin, Building2,
  X, ExternalLink, Loader2, CheckCircle2, Target,
} from 'lucide-react';
import { useState } from 'react';
import { jobApi } from '../api/job.api';
import { Spinner } from '../components/ui/Spinner';
import { useToast } from '../components/ui/Toast';
import type { JobSearchResponse, SavedJobResponse, JobDetailResponse } from '../types';
import styles from './JobsPage.module.css';

/**
 * Guaranteed "view the posting" link.
 * Prefers the real apply URL; otherwise falls back to a Google search for the
 * title + company so there is ALWAYS a working link.
 */
const postingUrl = (title?: string, company?: string, applyUrl?: string): string => {
  if (applyUrl && /^https?:\/\//i.test(applyUrl)) return applyUrl;
  const q = encodeURIComponent([title, company, 'job'].filter(Boolean).join(' '));
  return `https://www.google.com/search?q=${q}`;
};

/** Identifies which job's detail the modal is showing. */
interface OpenJob {
  jobExternalId: string;
  /** The saved-job record, when opened from the Saved tab — used as fallback. */
  saved?: SavedJobResponse;
}

export const JobsPage = () => {
  const qc = useQueryClient();
  const toast = useToast();
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

  // Fetch the full detail (description + extracted skills) from the real route.
  // Works for both search results and saved jobs as long as the search cache is warm.
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
    // Accept the raw search result, then enrich with the REAL AI-extracted skills
    // from the detail route before persisting. Falls back to whatever is passed only
    // if the detail call fails (so a save never blocks).
    mutationFn: async (job: {
      jobExternalId: string;
      jobTitle: string;
      jobCompany?: string;
      jobLocation?: string;
      jobApplyUrl?: string;
      fallbackSkills?: string[];
    }) => {
      let requiredSkills = job.fallbackSkills ?? [];
      try {
        const detail = await jobApi.getJobDetails(job.jobExternalId);
        if (detail.extractedSkills?.length) {
          requiredSkills = detail.extractedSkills;
        }
      } catch {
        // detail route unavailable — keep fallback (may be empty; better than badges)
      }
      return jobApi.saveJob({
        jobExternalId: job.jobExternalId,
        jobTitle: job.jobTitle,
        jobCompany: job.jobCompany,
        jobLocation: job.jobLocation,
        jobApplyUrl: job.jobApplyUrl,
        requiredSkills,
      });
    },
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['savedJobs'] });
      toast.success('Job saved to your list');
    },
    onError: () => toast.error('Could not save this job'),
  });


  const removeMutation = useMutation({
    // Backend deletes by the job's EXTERNAL id, not the saved-job uuid.
    mutationFn: (jobExternalId: string) => jobApi.deleteSavedJob(jobExternalId),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['savedJobs'] });
      toast.info('Job removed from saved');
    },
    onError: () => toast.error('Could not remove this job'),
  });

  // Map savedJobs by externalId for quick lookup
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

  return (
    <div>
      <h1 className="section-title">Jobs</h1>
      <p className="section-subtitle">Search opportunities and track your applications</p>

      {/* Search bar */}
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

      {/* Tabs */}
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

      {/* Search results */}
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

          {results?.jobs.map(job => {
            const saved = savedByExternalId.get(job.jobId ?? '');
            return (
              <JobCard
                key={job.jobId}
                title={job.title}
                company={job.companyName}
                location={job.location}
                tags={job.extensions ?? []}
                tagsAreMeta
                isSaved={!!saved}
                onSave={() => saveMutation.mutate({
                  jobExternalId: job.jobId!,
                  jobTitle: job.title,
                  jobCompany: job.companyName,
                  jobLocation: job.location,
                  jobApplyUrl: job.applyOptions?.[0]?.apply_link,
                  fallbackSkills: [],
                })}
                onRemove={() => saved && removeMutation.mutate(saved.jobExternalId)}
                saving={saveMutation.isPending || removeMutation.isPending}
                onOpen={() => job.jobId && setOpenJob({ jobExternalId: job.jobId })}
              />

            );
          })}
        </div>
      )}

      {/* Saved jobs */}
      {activeTab === 'saved' && (
        <div className={styles.jobList}>
          {loadingSaved && <Spinner center />}

          {!loadingSaved && savedJobs?.length === 0 && (
            <div className={styles.empty}>
              <Bookmark size={40} color="var(--text-3)" />
              <p>No saved jobs yet</p>
              <p style={{ fontSize: 13 }}>Search and bookmark jobs to track them here</p>
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
              isSaved
              onSave={() => {}}
              onRemove={() => removeMutation.mutate(saved.jobExternalId)}
              saving={removeMutation.isPending}
              // Try the real detail route; the saved record is the fallback.
              onOpen={() => setOpenJob({ jobExternalId: saved.jobExternalId, saved })}
              applyUrl={saved.jobApplyUrl}
            />
          ))}
        </div>
      )}

      {/* Job detail modal */}
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

            {loadingDetail ? (
              <Spinner center />
            ) : detail ? (
              <JobDetailView
                title={detail.title}
                company={detail.companyName}
                location={detail.location}
                description={detail.description}
                skills={detail.extractedSkills ?? []}
                applyUrl={detail.applyOptions?.[0]?.apply_link ?? openJob.saved?.jobApplyUrl}
                saved={openJob.saved}
                onClose={closeModal}
              />
            ) : openJob.saved ? (
              <JobDetailView
                title={openJob.saved.jobTitle}
                company={openJob.saved.jobCompany}
                location={openJob.saved.jobLocation}
                description={undefined}
                skills={openJob.saved.requiredSkills?.length
                  ? openJob.saved.requiredSkills
                  : openJob.saved.matchedSkills ?? []}
                applyUrl={openJob.saved.jobApplyUrl}
                saved={openJob.saved}
                staleNotice
                onClose={closeModal}
              />
            ) : detailError ? (
              <div className={styles.modalContent}>
                <p className={styles.empty}>
                  We couldn't load the full description (it may have expired from the
                  search cache). Try searching for this job again to refresh it.
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

// ── Unified detail view ───────────────────────────────────────────────
const JobDetailView = ({
  title, company, location, description, skills, applyUrl, saved, staleNotice, onClose,
}: {
  title: string;
  company?: string;
  location?: string;
  description?: string;
  skills: string[];
  applyUrl?: string;
  saved?: SavedJobResponse;
  staleNotice?: boolean;
  onClose: () => void;
}) => {
  const link = postingUrl(title, company, applyUrl);
  const score = saved?.compatibilityScore;

  return (
    <div className={styles.modalContent}>
      <h2 className={styles.modalTitle}>{title}</h2>
      <div className={styles.modalMeta}>
        {company && <span><Building2 size={13} />{company}</span>}
        {location && <span><MapPin size={13} />{location}</span>}
        {saved?.savedAt && (
          <span>Saved {new Date(saved.savedAt).toLocaleDateString('en-GB', {
            day: '2-digit', month: 'short', year: 'numeric',
          })}</span>
        )}
      </div>

      {/* CV compatibility (saved jobs only) */}
      {score != null && (
        <div className={styles.matchBanner}>
          <span className={`badge ${score >= 70 ? 'badge--green' : score >= 40 ? 'badge--yellow' : 'badge--red'}`}>
            {score}% match with your CV
          </span>
        </div>
      )}

      {/* Skills you have / to learn (saved jobs carry this analysis) */}
      {saved && saved.matchedSkills?.length > 0 && (
        <div className={styles.detailSection}>
          <h4 className={styles.detailHeading}><CheckCircle2 size={14} /> Skills you have</h4>
          <div className={styles.modalSkills}>
            {saved.matchedSkills.map(s => <span key={s} className="badge badge--green">{s}</span>)}
          </div>
        </div>
      )}
      {saved && saved.missingSkills?.length > 0 && (
        <div className={styles.detailSection}>
          <h4 className={styles.detailHeading}><Target size={14} /> Skills to learn</h4>
          <div className={styles.modalSkills}>
            {saved.missingSkills.map(s => <span key={s} className="badge badge--red">{s}</span>)}
          </div>
        </div>
      )}

      {/* Extracted skills (search-result detail, when not a saved job) */}
      {!saved && skills.length > 0 && (
        <div className={styles.modalSkills}>
          {skills.slice(0, 12).map(s => <span key={s} className="badge badge--accent">{s}</span>)}
        </div>
      )}

      {/* Full description (when the detail route served it) */}
      {description ? (
        <div className={styles.modalBody}>
          <p>{description}</p>
        </div>
      ) : staleNotice ? (
        <p className={styles.detailNote}>
          The full description isn't cached anymore. Everything you saved is shown above —
          open the original posting for the complete details.
        </p>
      ) : null}

      <div className={styles.modalActions}>
        <a href={link} target="_blank" rel="noopener noreferrer" className="btn btn--primary">
          <ExternalLink size={14} /> View original posting
        </a>
        <button className="btn btn--ghost" onClick={onClose}>Close</button>
      </div>
    </div>
  );
};

// ── Job Card ──────────────────────────────────────────────────────────
const JobCard = ({
  title, company, location, tags, compatibilityScore, tagsAreMeta,
  isSaved, onSave, onRemove, saving, onOpen, applyUrl,
}: {
  title: string;
  company?: string;
  location?: string;
  tags?: string[];
  compatibilityScore?: number;
  tagsAreMeta?: boolean;
  isSaved: boolean;
  onSave: () => void;
  onRemove: () => void;
  saving: boolean;
  onOpen?: () => void;
  applyUrl?: string;
}) => (
  <div className={styles.jobCard} onClick={() => onOpen?.()}>
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

      <div className={styles.jobActions} onClick={e => e.stopPropagation()}>
        {applyUrl && (
          <a
            href={applyUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="btn btn--soft btn--sm"
            title="Apply"
          >
            <ExternalLink size={13} />
          </a>
        )}
        <button
          className={`btn btn--ghost btn--sm ${styles.bookmarkBtn}`}
          onClick={() => (isSaved ? onRemove : onSave)()}
          disabled={saving}
          title={isSaved ? 'Remove from saved' : 'Save job'}
        >
          {isSaved
            ? <BookmarkCheck size={15} color="var(--accent)" />
            : <Bookmark size={15} />
          }
        </button>
      </div>
    </div>
  </div>
);
