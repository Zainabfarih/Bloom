import { Link, Navigate } from 'react-router-dom';
import {
  Flower2, Sparkles, FileText, Briefcase, Map, ArrowRight,
} from 'lucide-react';
import { useAuthStore } from '../store/auth.store';
import styles from './HomePage.module.css';

const FEATURES = [
  {
    icon: FileText,
    title: 'CV analysis',
    text: 'Upload your PDF resume and get an instant ATS score, strengths and the skills we detect.',
  },
  {
    icon: Briefcase,
    title: 'Smart job matching',
    text: 'Search real openings and match each one against your CV to see how well you fit.',
  },
  {
    icon: Map,
    title: 'Learning roadmaps',
    text: 'Turn the skills you are missing into a clear, step-by-step path with curated resources.',
  },
];

export const HomePage = () => {
  const { isAuthenticated, user } = useAuthStore();

  if (isAuthenticated && user) {
    return <Navigate to={user.role === 'ADMIN' ? '/admin' : '/dashboard'} replace />;
  }

  return (
    <div className={styles.wrap}>
      <header className={styles.nav}>
        <div className={styles.brand}>
          <span className={styles.brandIcon}><Flower2 size={18} /></span>
          Bloom
        </div>
        <nav className={styles.navLinks}>
          <Link to="/about" className={styles.navLink}>About</Link>
          <Link to="/login" className={styles.navLink}>Sign in</Link>
          <Link to="/register" className="btn btn--primary btn--sm">Get started</Link>
        </nav>
      </header>

      <section className={styles.hero}>
        <span className={styles.eyebrow}><Sparkles size={14} /> Grow your career skills</span>
        <h1 className={styles.title}>
          Land the job you want with a <span className="text-gradient">plan that fits you</span>
        </h1>
        <p className={styles.subtitle}>
          Bloom analyses your CV, matches it to real job openings and builds personalised learning
          roadmaps for the skills you still need.
        </p>
        <div className={styles.ctaRow}>
          <Link to="/register" className="btn btn--primary btn--lg">
            Start for free <ArrowRight size={16} />
          </Link>
          <Link to="/about" className="btn btn--ghost btn--lg">Learn more</Link>
        </div>
      </section>

      <section className={styles.features}>
        {FEATURES.map(({ icon: Icon, title, text }) => (
          <div key={title} className={styles.feature}>
            <div className={styles.featureIcon}><Icon size={20} /></div>
            <h3 className={styles.featureTitle}>{title}</h3>
            <p className={styles.featureText}>{text}</p>
          </div>
        ))}
      </section>

      <footer className={styles.footer}>
        © {new Date().getFullYear()} Bloom · Built for learners
      </footer>
    </div>
  );
};
