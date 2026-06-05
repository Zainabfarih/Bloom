import { NavLink, useNavigate } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import {
  LayoutDashboard, FileText, Briefcase, Map, User,
  LogOut, Flower2, ShieldCheck,
} from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import { useAuthStore } from '../../store/auth.store';
import { authApi } from '../../api/auth.api';
import styles from './Sidebar.module.css';

interface NavItem {
  to: string;
  icon: LucideIcon;
  label: string;
}

// Student-facing features
const STUDENT_NAV: NavItem[] = [
  { to: '/dashboard', icon: LayoutDashboard, label: 'Dashboard' },
  { to: '/cv',        icon: FileText,        label: 'My CV' },
  { to: '/jobs',      icon: Briefcase,       label: 'Jobs' },
  { to: '/roadmap',   icon: Map,             label: 'Roadmap' },
];

// Admin-only features
const ADMIN_NAV: NavItem[] = [
  { to: '/admin', icon: ShieldCheck, label: 'Admin Panel' },
];

export const Sidebar = () => {
  const { user, clearAuth, refreshToken } = useAuthStore();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const isAdmin = user?.role === 'ADMIN';

  // Admins get a focused, admin-only experience; students get the learner features.
  const primaryNav = isAdmin ? ADMIN_NAV : STUDENT_NAV;

  const handleLogout = async () => {
    try {
      await authApi.logout(refreshToken);
    } finally {
      clearAuth();
      // Wipe cached data so the next user who logs in doesn't see this user's data
      queryClient.clear();
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
        <p className={styles.navSection}>{isAdmin ? 'Administration' : 'Menu'}</p>
        {primaryNav.map(({ to, icon: Icon, label }) => (
          <NavLink
            key={to}
            to={to}
            className={({ isActive }) =>
              `${styles.link} ${isAdmin ? styles.adminLink : ''} ${isActive ? styles.active : ''}`
            }
          >
            <Icon size={17} />
            <span>{label}</span>
          </NavLink>
        ))}

        {/* Account section — available to everyone */}
        <p className={styles.navSection} style={{ marginTop: 20 }}>Account</p>
        <NavLink
          to="/profile"
          className={({ isActive }) =>
            `${styles.link} ${isActive ? styles.active : ''}`
          }
        >
          <User size={17} />
          <span>Profile</span>
        </NavLink>
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
