import { Link } from 'react-router-dom';
import { Flower2, ArrowRight } from 'lucide-react';
import styles from './HomePage.module.css';

const STEPS = [
  { title: 'Upload your CV', text: 'Add your resume as a PDF — we extract your skills and score it for ATS readiness.' },
  { title: 'Search & match jobs', text: 'Find real openings and match any job against your CV to reveal the skills you already have and the ones you are missing.' },
  { title: 'Follow a roadmap', text: 'Generate a step-by-step learning path for the missing skills, with curated resources and progress tracking.' },
];

export const AboutPage = () => (
  <div className={styles.wrap}>
    <header className={styles.nav}>
      <Link to="/" className={styles.brand}>
        <span className={styles.brandIcon}><Flower2 size={18} /></span>
        Bloom
      </Link>
      <nav className={styles.navLinks}>
        <Link to="/login" className={styles.navLink}>Sign in</Link>
        <Link to="/register" className="btn btn--primary btn--sm">Get started</Link>
      </nav>
    </header>

    <main className={styles.content}>
      <h1 className={styles.contentTitle}>About Bloom</h1>
      <p className={styles.contentLead}>
        Bloom is a career-growth platform that connects what you can do today with the job you want
        next. Instead of guessing what to learn, you get a clear, personalised path grounded in your
        own CV and real market demand.
      </p>

      <div className={styles.contentSection}>
        <h2>Why we built it</h2>
        <p>
          Job seekers often know their target role but not the exact gap between their current skills
          and what employers expect. Bloom closes that gap by combining CV analysis, job matching and
          guided learning in one place.
        </p>
      </div>

      <div className={styles.contentSection}>
        <h2>How it works</h2>
        <div className={styles.steps}>
          {STEPS.map((s, i) => (
            <div key={s.title} className={styles.step}>
              <span className={styles.stepNum}>{i + 1}</span>
              <span className={styles.stepText}><strong>{s.title}.</strong> {s.text}</span>
            </div>
          ))}
        </div>
      </div>

      <Link to="/register" className="btn btn--primary btn--lg">
        Create your account <ArrowRight size={16} />
      </Link>
    </main>

    <footer className={styles.footer}>
      © {new Date().getFullYear()} Bloom · Built for learners
    </footer>
  </div>
);
