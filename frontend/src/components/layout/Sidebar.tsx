import { NavLink, useNavigate } from 'react-router-dom';
import {
  LayoutDashboard, FileText, Briefcase, Map, User,
  LogOut, Flower2, ShieldCheck,
} from 'lucide-react';
import { useAuthStore } from '../../store/auth.store';
import { authApi } from '../../api/auth.api';
import styles from './Sidebar.module.css';

const NAV = [
  { to: '/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/cv',        icon: FileText,        label: 'My CV' },
  { to: '/jobs',      icon: Briefcase,       label: 'Jobs' },
  { to: '/roadmap',   icon: Map,             label: 'Roadmap' },
  { to: '/profile',   icon: User,            label: 'Profile' },
];

export const Sidebar = () => {
  const { user, clearAuth, refreshToken } = useAuthStore();
  const navigate = useNavigate();
  const isAdmin = user?.role === 'ADMIN';

  const handleLogout = async () => {
    try {
      await authApi.logout(refreshToken);
    } finally {
      clearAuth();
      navigate('/login', { replace: true });
    }
  };

  return (
    <aside className={styles.sidebar}>
      {/* Logo */}
      <div className={styles.logo}>
        <div className={styles.logoIcon}>
          <Flower2 size={18} />
        </div>
        <span>Bloom</span>
      </div>

      {/* Nav */}
      <nav className={styles.nav}>
        <p className={styles.navSection}>Menu</p>
        {NAV.map(({ to, icon: Icon, label }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              `${styles.link} ${isActive ? styles.active : ''}`
            }
          >
            <Icon size={17} />
            <span>{label}</span>
          </NavLink>
        ))}

        {isAdmin && (
          <>
            <p className={styles.navSection} style={{ marginTop: 20 }}>Admin</p>
            <NavLink
              to="/admin"
              className={({ isActive }) =>
                `${styles.link} ${styles.adminLink} ${isActive ? styles.active : ''}`
              }
            >
              <ShieldCheck size={17} />
              <span>Admin Panel</span>
            </NavLink>
          </>
        )}
      </nav>

      {/* User info */}
      <div className={styles.bottom}>
        <div className={styles.userInfo}>
          <div className={styles.avatar}>
            {user?.firstName?.[0]}{user?.lastName?.[0]}
          </div>
          <div className={styles.userText}>
            <p className={styles.userName}>
              {user?.firstName} {user?.lastName}
            </p>
            <p className={styles.userRole}>{user?.role?.toLowerCase()}</p>
          </div>
        </div>
        <button
          className={styles.logout}
          onClick={handleLogout}
          title="Sign out"
        >
          <LogOut size={15} />
        </button>
      </div>
    </aside>
  );
};
