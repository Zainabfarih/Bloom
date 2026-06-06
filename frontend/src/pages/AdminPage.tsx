import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Users, UserCheck, UserX, Search, RefreshCw,
  Trash2, ShieldOff, ChevronDown, UserPlus, TrendingUp,
} from 'lucide-react';
import { useState, useMemo } from 'react';
import { authApi } from '../api/auth.api';
import { Spinner } from '../components/ui/Spinner';
import { useToast } from '../components/ui/Toast';
import { useConfirm } from '../components/ui/ConfirmDialog';
import type { UserDTO, AdminStatsResponse } from '../types';
import styles from './AdminPage.module.css';

const isUserDeleted = (u: UserDTO) => u.deleted === true || u.enabled === false;

export const AdminPage = () => {
  const qc = useQueryClient();
  const toast = useToast();
  const confirm = useConfirm();
  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState<'all' | 'active' | 'deleted'>('all');
  const [sortBy, setSortBy] = useState<'name' | 'email' | 'role'>('name');

  const { data: users, isLoading, error } = useQuery<UserDTO[]>({
    queryKey: ['admin-users'],
    queryFn: authApi.getAllUsers,
    retry: false,
  });

  const { data: serverStats } = useQuery<AdminStatsResponse>({
    queryKey: ['admin-stats'],
    queryFn: authApi.getAdminStats,
    retry: false,
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => authApi.deleteUser(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-users'] });
      qc.invalidateQueries({ queryKey: ['admin-stats'] });
      toast.success('User deleted');
    },
    onError: () => toast.error('Could not delete user'),
  });

  const recoverMutation = useMutation({
    mutationFn: (id: number) => authApi.recoverUser(id),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: ['admin-users'] });
      qc.invalidateQueries({ queryKey: ['admin-stats'] });
      toast.success('User restored');
    },
    onError: () => toast.error('Could not restore user'),
  });

  const filtered = useMemo(() => {
    if (!users) return [];
    let list = [...users];

    if (filter === 'active')  list = list.filter(u => !isUserDeleted(u));
    if (filter === 'deleted') list = list.filter(isUserDeleted);

    if (search.trim()) {
      const q = search.toLowerCase();
      list = list.filter(u =>
        `${u.firstName} ${u.lastName}`.toLowerCase().includes(q) ||
        u.email.toLowerCase().includes(q) ||
        u.role.toLowerCase().includes(q)
      );
    }

    list.sort((a, b) => {
      if (sortBy === 'email') return a.email.localeCompare(b.email);
      if (sortBy === 'role')  return a.role.localeCompare(b.role);
      return `${a.firstName} ${a.lastName}`.localeCompare(`${b.firstName} ${b.lastName}`);
    });

    return list;
  }, [users, filter, search, sortBy]);

  const totalUsers   = serverStats?.totalUsers   ?? users?.length ?? 0;
  const activeUsers  = serverStats?.activeUsers  ?? users?.filter(u => !isUserDeleted(u)).length ?? 0;
  const deletedUsers = serverStats?.deletedUsers ?? users?.filter(isUserDeleted).length ?? 0;
  const adminCount   = serverStats?.adminCount   ?? users?.filter(u => u.role === 'ADMIN').length ?? 0;
  const newThisMonth = serverStats?.newUsersThisMonth ?? 0;

  const stats = [
    { label: 'Total Users',   value: totalUsers,   icon: Users,     color: 'var(--accent)', glow: 'var(--accent-glow)' },
    { label: 'Active Users',  value: activeUsers,  icon: UserCheck, color: 'var(--green)',  glow: 'var(--green-glow)'  },
    { label: 'New This Month', value: newThisMonth, icon: UserPlus, color: 'var(--pink)',   glow: 'var(--pink-glow)'   },
    { label: 'Deleted Users', value: deletedUsers, icon: UserX,     color: 'var(--red)',    glow: 'var(--red-glow)'    },
  ];

  const handleDelete = async (u: UserDTO) => {
    const ok = await confirm({
      title: `Delete ${u.firstName} ${u.lastName}?`,
      message: 'This soft-deletes the account. You can restore it later from this panel.',
      confirmText: 'Delete user',
      danger: true,
    });
    if (ok) deleteMutation.mutate(u.id);
  };

  return (
    <div>
      <h1 className="section-title">Admin Panel</h1>
      <p className="section-subtitle">Manage users and monitor platform activity</p>

      {/* Stats */}
      <div className={styles.statsGrid}>
        {stats.map(({ label, value, icon: Icon, color, glow }) => (
          <div key={label} className={styles.statCard}>
            <div className={styles.statIcon} style={{ background: glow, color }}>
              <Icon size={19} />
            </div>
            <div>
              <p className={styles.statValue}>{value}</p>
              <p className={styles.statLabel}>{label}</p>
            </div>
          </div>
        ))}
      </div>

      {/* Signups trend */}
      {serverStats && serverStats.signupsByDay?.length > 0 && (
        <SignupsTrend data={serverStats} />
      )}

      {/* User management */}
      <div className={`card ${styles.tableCard}`}>
        <div className={styles.tableHeader}>
          <h3 className={styles.tableTitle}>User Management</h3>

          <div className={styles.tableControls}>
            <div className={styles.searchBox}>
              <Search size={14} color="var(--text-3)" />
              <input
                value={search}
                onChange={e => setSearch(e.target.value)}
                placeholder="Search users…"
                aria-label="Search users"
              />
            </div>

            <div className={styles.selectWrap}>
              <select
                value={filter}
                onChange={e => setFilter(e.target.value as typeof filter)}
                className={styles.select}
                aria-label="Filter users"
              >
                <option value="all">All users</option>
                <option value="active">Active only</option>
                <option value="deleted">Deleted only</option>
              </select>
              <ChevronDown size={14} className={styles.selectIcon} />
            </div>

            <div className={styles.selectWrap}>
              <select
                value={sortBy}
                onChange={e => setSortBy(e.target.value as typeof sortBy)}
                className={styles.select}
                aria-label="Sort users"
              >
                <option value="name">Sort: Name</option>
                <option value="email">Sort: Email</option>
                <option value="role">Sort: Role</option>
              </select>
              <ChevronDown size={14} className={styles.selectIcon} />
            </div>
          </div>
        </div>

        {isLoading ? (
          <Spinner center />
        ) : error ? (
          <div className={styles.tableError}>
            <ShieldOff size={36} color="var(--red)" />
            <p>Failed to load users. Make sure you have admin permissions and that the
              auth-service is running.</p>
          </div>
        ) : filtered.length === 0 ? (
          <div className={styles.tableEmpty}>
            <Users size={36} color="var(--text-3)" />
            <p>{search ? `No users match “${search}”` : 'No users found'}</p>
          </div>
        ) : (
          <div className={styles.tableWrapper}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>User</th><th>Email</th><th>Role</th><th>Joined</th><th>Status</th><th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map(u => {
                  const deleted = isUserDeleted(u);
                  return (
                    <tr key={u.id} className={deleted ? styles.rowDeleted : ''}>
                      <td>
                        <div className={styles.userCell}>
                          <div className={styles.avatar}>
                            {u.firstName?.[0]}{u.lastName?.[0]}
                          </div>
                          <div>
                            <p className={styles.userName}>{u.firstName} {u.lastName}</p>
                            <p className={styles.userId}>ID: {u.id}</p>
                          </div>
                        </div>
                      </td>
                      <td className={styles.emailCell}>{u.email}</td>
                      <td>
                        <span className={`badge ${u.role === 'ADMIN' ? 'badge--pink' : 'badge--accent'}`}>
                          {u.role.toLowerCase()}
                        </span>
                      </td>
                      <td className={styles.dateCell}>
                        {u.createdAt
                          ? new Date(u.createdAt).toLocaleDateString('en-GB', {
                              day: '2-digit', month: 'short', year: 'numeric',
                            })
                          : '—'}
                      </td>
                      <td>
                        <span className={`badge ${deleted ? 'badge--red' : 'badge--green'}`}>
                          {deleted ? 'deleted' : 'active'}
                        </span>
                      </td>
                      <td>
                        <div className={styles.actionButtons}>
                          {deleted ? (
                            <button
                              className="btn btn--soft btn--sm"
                              onClick={() => recoverMutation.mutate(u.id)}
                              disabled={recoverMutation.isPending}
                              title="Restore user"
                            >
                              <RefreshCw size={13} /> Restore
                            </button>
                          ) : (
                            <button
                              className="btn btn--danger btn--sm"
                              onClick={() => handleDelete(u)}
                              disabled={deleteMutation.isPending}
                              title="Soft delete user"
                            >
                              <Trash2 size={13} /> Delete
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}

        {!isLoading && !error && (
          <p className={styles.tableFooter}>
            Showing {filtered.length} of {totalUsers} users
            {adminCount > 0 && ` · ${adminCount} admin${adminCount === 1 ? '' : 's'}`}
          </p>
        )}
      </div>
    </div>
  );
};

// ── Signups trend (30 days) ───────────────────────────────────────────
const SignupsTrend = ({ data }: { data: AdminStatsResponse }) => {
  const days = data.signupsByDay;
  const max = Math.max(1, ...days.map(d => d.count));
  const total = days.reduce((sum, d) => sum + d.count, 0);

  return (
    <div className={`card ${styles.trendCard}`}>
      <div className={styles.trendHeader}>
        <div>
          <h3 className={styles.trendTitle}><TrendingUp size={15} /> Sign-ups</h3>
          <p className={styles.trendSub}>Last 30 days · {total} new {total === 1 ? 'account' : 'accounts'}</p>
        </div>
      </div>
      <div className={styles.trendChart} role="img" aria-label={`Sign-ups over the last 30 days, ${total} total`}>
        {days.map(d => (
          <div key={d.date} className={styles.trendCol} title={`${d.date}: ${d.count}`}>
            <div
              className={styles.trendBar}
              style={{ height: `${Math.max(6, (d.count / max) * 100)}%`, opacity: d.count === 0 ? 0.35 : 1 }}
            />
          </div>
        ))}
      </div>
    </div>
  );
};
