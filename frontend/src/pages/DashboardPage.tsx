import { useQuery } from '@tanstack/react-query';
import { FileText, Briefcase, Map, TrendingUp, ArrowRight, Sparkles } from 'lucide-react';
import { Link } from 'react-router-dom';
import { useAuthStore } from '../store/auth.store';
import { cvApi } from '../api/cv.api';
import { jobApi } from '../api/job.api';
import { roadmapApi } from '../api/roadmap.api';
import { Spinner } from '../components/ui/Spinner';
import styles from './DashboardPage.module.css';

export const DashboardPage = () => {
  const user = useAuthStore(s => s.user);

  const { data: cvs }       = useQuery({ queryKey: ['cvs'],       queryFn: cvApi.getAllMyCvs,   retry: false });
  const { data: savedJobs } = useQuery({ queryKey: ['savedJobs'], queryFn: jobApi.getSavedJobs, retry: false });
  const { data: roadmaps }  = useQuery({ queryKey: ['roadmaps'],  queryFn: roadmapApi.getAll,   retry: false });

  // Saved jobs come back ordered by compatibility DESC — the first is the best match.
  const topJob = savedJobs?.[0];

  const completedSteps = roadmaps?.flatMap(r => r.steps).filter(s => s.status === 'COMPLETED').length ?? 0;
  const totalSteps     = roadmaps?.flatMap(r => r.steps).length ?? 0;
  const progress       = totalSteps > 0 ? Math.round((completedSteps / totalSteps) * 100) : 0;

  const hour = new Date().getHours();
  const greeting = hour < 12 ? 'Good morning' : hour < 18 ? 'Good afternoon' : 'Good evening';

  const stats = [
    { icon: FileText,   label: 'CVs uploaded',    value: cvs?.length ?? 0,       to: '/cv',      color: 'var(--accent)', glow: 'var(--accent-glow)' },
    { icon: Briefcase,  label: 'Saved jobs',      value: savedJobs?.length ?? 0, to: '/jobs',    color: 'var(--green)',  glow: 'var(--green-glow)'  },
    { icon: Map,        label: 'Roadmaps',        value: roadmaps?.length ?? 0,  to: '/roadmap', color: 'var(--pink)',   glow: 'var(--pink-glow)'   },
    { icon: TrendingUp, label: 'Steps completed', value: `${completedSteps}/${totalSteps}`, to: '/roadmap', color: 'var(--yellow)', glow: 'var(--yellow-glow)' },
  ];

  return (
    <div className={styles.page}>
      {/* Header */}
      <div className={styles.header}>
        <div>
          <h1 className={styles.greeting}>
            {greeting}, {user?.firstName} 👋
          </h1>
          <p className="section-subtitle" style={{ marginBottom: 0 }}>
            Here's your career overview
          </p>
        </div>
        {cvs && cvs.length === 0 && (
          <Link to="/cv" className={`btn btn--soft ${styles.ctaBtn}`}>
            <Sparkles size={14} /> Upload your CV to get started
          </Link>
        )}
      </div>

      {/* Stats */}
      <div className={styles.statsGrid}>
        {stats.map(({ icon: Icon, label, value, to, color, glow }) => (
          <Link to={to} key={label} className={styles.statCard}>
            <div className={styles.statIcon} style={{ background: glow, color }}>
              <Icon size={19} />
            </div>
            <div className={styles.statBody}>
              <p className={styles.statValue}>{value}</p>
              <p className={styles.statLabel}>{label}</p>
            </div>
            <ArrowRight size={14} className={styles.statArrow} />
          </Link>
        ))}
      </div>

      {/* Progress bar */}
      {totalSteps > 0 && (
        <div className={`card ${styles.progressCard}`}>
          <div className={styles.progressHeader}>
            <span>Overall roadmap progress</span>
            <span className={styles.progressPct}>{progress}%</span>
          </div>
          <div className={styles.progressTrack}>
            <div className={styles.progressFill} style={{ width: `${progress}%` }} />
          </div>
          <p className={styles.progressSub}>
            {completedSteps} of {totalSteps} steps completed across {roadmaps?.length} roadmap{roadmaps?.length !== 1 ? 's' : ''}
          </p>
        </div>
      )}

      {/* Lower section */}
      <div className="grid-2" style={{ marginTop: 20 }}>
        {/* Skill Gap */}
        <div className="card">
          <h3 className={styles.cardTitle}>Skill Gap</h3>
          {!topJob ? (
            <div className={styles.emptyState}>
              <Briefcase size={32} color="var(--text-3)" />
              <p>Save a job to see your skill gap</p>
              <Link to="/jobs" className="btn btn--soft btn--sm">Browse Jobs →</Link>
            </div>
          ) : (
            <>
              <div className={styles.matchRow}>
                <span className={styles.matchScore}>
                  {Math.round(topJob.compatibilityScore ?? 0)}
                  <span>%</span>
                </span>
                <div>
                  <p className={styles.matchLabel}>match score</p>
                  <p className={styles.matchJob}>{topJob.jobTitle}</p>
                </div>
              </div>

              {(topJob.matchedSkills?.length ?? 0) > 0 && (
                <div className={styles.skillGroup}>
                  <p className={styles.skillGroupLabel}>✓ You have</p>
                  <div className={styles.skillBadges}>
                    {topJob.matchedSkills.slice(0, 5).map(s => (
                      <span key={s} className="badge badge--green">{s}</span>
                    ))}
                  </div>
                </div>
              )}

              {(topJob.missingSkills?.length ?? 0) > 0 && (
                <div className={styles.skillGroup} style={{ marginTop: 12 }}>
                  <p className={styles.skillGroupLabel}>🎯 To learn</p>
                  <div className={styles.skillBadges}>
                    {topJob.missingSkills.slice(0, 5).map(s => (
                      <span key={s} className="badge badge--red">{s}</span>
                    ))}
                  </div>
                </div>
              )}

              {(topJob.matchedSkills?.length ?? 0) === 0 &&
               (topJob.missingSkills?.length ?? 0) === 0 && (
                <p className={styles.skillGroupLabel} style={{ marginTop: 8 }}>
                  Save a job with your CV attached to see a detailed skill breakdown.
                </p>
              )}
            </>
          )}
        </div>

        {/* Recent Roadmaps */}
        <div className="card">
          <h3 className={styles.cardTitle}>Recent Roadmaps</h3>
          {!roadmaps ? (
            <Spinner center />
          ) : roadmaps.length === 0 ? (
            <div className={styles.emptyState}>
              <Map size={32} color="var(--text-3)" />
              <p>No roadmaps yet</p>
              <Link to="/roadmap" className="btn btn--soft btn--sm">Generate one →</Link>
            </div>
          ) : (
            <div className={styles.roadmapList}>
              {roadmaps.slice(0, 4).map(r => {
                const done  = r.steps.filter(s => s.status === 'COMPLETED').length;
                const total = r.steps.length;
                const pct   = total > 0 ? Math.round((done / total) * 100) : 0;
                return (
                  <Link to="/roadmap" key={r.id} className={styles.roadmapItem}>
                    <div className={styles.roadmapItemInfo}>
                      <p className={styles.roadmapTitle}>{r.targetJobTitle}</p>
                      <p className={styles.roadmapMeta}>{done}/{total} steps · {pct}%</p>
                    </div>
                    <div className={styles.miniBar}>
                      <div style={{ width: `${pct}%` }} />
                    </div>
                  </Link>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </div>
  );
};
