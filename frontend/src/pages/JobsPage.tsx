import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Search, Bookmark, BookmarkCheck, MapPin, Building2,
  X, ExternalLink, Loader2,
} from 'lucide-react';
import { useState } from 'react';
import { jobApi } from '../api/job.api';
import { Spinner } from '../components/ui/Spinner';
import type { JobSearchResponse, JobSearchResult, SavedJobResponse, JobDetailResponse } from '../types';
import styles from './JobsPage.module.css';

export const JobsPage = () => {
  const qc = useQueryClient();
  const [query, setQuery] = useState('');
  const [location, setLocation] = useState('');
  const [submitted, setSubmitted] = useState('');
  const [submittedLocation, setSubmittedLocation] = useState('');
  const [activeTab, setActiveTab] = useState<'search' | 'saved'>('search');
  const [selectedJobId, setSelectedJobId] = useState<string | null>(null);

  const { data: results, isLoading: searching } = useQuery<JobSearchResponse>({
    queryKey: ['job-search', submitted, submittedLocation],
    queryFn: () => jobApi.searchJobs(submitted, submittedLocation),
    enabled: submitted.length > 0,
  });

  const { data: savedJobs, isLoading: loadingSaved } = useQuery<SavedJobResponse[]>({
    queryKey: ['savedJobs'],
    queryFn: jobApi.getSavedJobs,
  });

  const { data: jobDetail, isLoading: loadingDetail } = useQuery<JobDetailResponse>({
    queryKey: ['jobDetail', selectedJobId],
    queryFn: () => jobApi.getJobDetails(selectedJobId!),
    enabled: !!selectedJobId,
  });

  const saveMutation = useMutation({
    mutationFn: (payload: Parameters<typeof jobApi.saveJob>[0]) => jobApi.saveJob(payload),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['savedJobs'] }),
  });

  const removeMutation = useMutation({
    mutationFn: (uuid: string) => jobApi.deleteSavedJob(uuid),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['savedJobs'] }),
  });

  // Map savedJobs by externalId for quick lookup
  const savedByExternalId = new Map(savedJobs?.map(s => [s.jobExternalId, s]) ?? []);

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
                isSaved={!!saved}
                onSave={() => saveMutation.mutate({
                  jobExternalId: job.jobId!,
                  jobTitle: job.title,
                  jobCompany: job.companyName,
                  jobLocation: job.location,
                  requiredSkills: job.extensions ?? [],
                })}
                onRemove={() => saved && removeMutation.mutate(saved.uuid)}
                saving={saveMutation.isPending || removeMutation.isPending}
                onOpen={() => setSelectedJobId(job.jobId ?? null)}
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
              onRemove={() => removeMutation.mutate(saved.uuid)}
              saving={removeMutation.isPending}
              onOpen={() => setSelectedJobId(saved.jobExternalId)}
              applyUrl={saved.jobApplyUrl}
            />
          ))}
        </div>
      )}

      {/* Job detail modal */}
      {selectedJobId && (
        <div
          className={styles.overlay}
          onClick={() => setSelectedJobId(null)}
          role="dialog"
          aria-modal="true"
          aria-label="Job details"
        >
          <div className={styles.modal} onClick={e => e.stopPropagation()}>
            <button
              className={styles.modalClose}
              onClick={() => setSelectedJobId(null)}
              aria-label="Close"
            >
              <X size={18} />
            </button>

            {loadingDetail ? (
              <Spinner center />
            ) : jobDetail ? (
              <div className={styles.modalContent}>
                <h2 className={styles.modalTitle}>{jobDetail.title}</h2>
                <div className={styles.modalMeta}>
                  {jobDetail.companyName && (
                    <span><Building2 size={13} />{jobDetail.companyName}</span>
                  )}
                  {jobDetail.location && (
                    <span><MapPin size={13} />{jobDetail.location}</span>
                  )}
                </div>

                {jobDetail.extractedSkills?.length > 0 && (
                  <div className={styles.modalSkills}>
                    {jobDetail.extractedSkills.slice(0, 10).map(s => (
                      <span key={s} className="badge badge--accent">{s}</span>
                    ))}
                  </div>
                )}

                <div className={styles.modalBody}>
                  <p>{jobDetail.description}</p>
                </div>

                <div className={styles.modalActions}>
                  {jobDetail.applyOptions?.[0]?.apply_link && (
                    <a
                      href={jobDetail.applyOptions[0].apply_link}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="btn btn--primary"
                    >
                      <ExternalLink size={14} /> Apply Now
                    </a>
                  )}
                  <button
                    className="btn btn--ghost"
                    onClick={() => setSelectedJobId(null)}
                  >
                    Close
                  </button>
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

// ── Job Card ──────────────────────────────────────────────────────────
const JobCard = ({
  title, company, location, tags, compatibilityScore,
  isSaved, onSave, onRemove, saving, onOpen, applyUrl,
}: {
  title: string;
  company?: string;
  location?: string;
  tags?: string[];
  compatibilityScore?: number;
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
          <span key={s} className="badge badge--accent">{s}</span>
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
