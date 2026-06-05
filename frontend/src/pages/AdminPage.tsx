import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import {
  Users, UserCheck, UserX, Search, RefreshCw,
  Trash2, Shield, ShieldOff, ChevronDown,
} from 'lucide-react';
import { useState, useMemo } from 'react';
import { authApi } from '../api/auth.api';
import { Spinner } from '../components/ui/Spinner';
import type { UserDTO } from '../types';
import styles from './AdminPage.module.css';

export const AdminPage = () => {
  const qc = useQueryClient();
  const [search, setSearch] = useState('');
  const [filter, setFilter] = useState<'all' | 'active' | 'deleted'>('all');
  const [sortBy, setSortBy] = useState<'name' | 'email' | 'role'>('name');

  const { data: users, isLoading, error } = useQuery<UserDTO[]>({
    queryKey: ['admin-users'],
    queryFn: authApi.getAllUsers,
    retry: false,
  });

  const deleteMutation = useMutation({
    mutationFn: (id: number) => authApi.deleteUser(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin-users'] }),
  });

  const recoverMutation = useMutation({
    mutationFn: (id: number) => authApi.recoverUser(id),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['admin-users'] }),
  });

  const filtered = useMemo(() => {
    if (!users) return [];
    let list = [...users];

    if (filter === 'active')  list = list.filter(u => u.enabled !== false && !u.deleted);
    if (filter === 'deleted') list = list.filter(u => u.deleted || u.enabled === false);

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

  // Derive stats from fetched users
  const totalUsers   = users?.length ?? 0;
  const activeUsers  = users?.filter(u => u.enabled !== false && !u.deleted).length ?? 0;
  const deletedUsers = users?.filter(u => u.deleted || u.enabled === false).length ?? 0;
  const adminCount   = users?.filter(u => u.role === 'ADMIN').length ?? 0;

  const stats = [
    { label: 'Total Users',   value: totalUsers,   icon: Users,      color: 'var(--accent)', glow: 'var(--accent-glow)' },
    { label: 'Active Users',  value: activeUsers,  icon: UserCheck,  color: 'var(--green)',  glow: 'var(--green-glow)'  },
    { label: 'Deleted Users', value: deletedUsers, icon: UserX,      color: 'var(--red)',    glow: 'var(--red-glow)'    },
    { label: 'Admins',        value: adminCount,   icon: Shield,     color: 'var(--pink)',   glow: 'var(--pink-glow)'   },
  ];

  return (
    <div>
      <h1 className="section-title">Admin Panel</h1>
      <p className="section-subtitle">Manage users and monitor platform activity</p>

      {/* Stats */}
      <div className={styles.statsGrid}>
        {stats.map(({ label, value, icon: Icon, color, glow }) => (
          <div key={label} className={styles.statCard}>
            <div className={styles.statIcon} style={{ background: glow, color, border: `1px solid ${color}30` }}>
              <Icon size={19} />
            </div>
            <div>
              <p className={styles.statValue}>{value}</p>
              <p className={styles.statLabel}>{label}</p>
            </div>
          </div>
        ))}
      </div>

      {/* User management */}
      <div className={`card ${styles.tableCard}`}>
        <div className={styles.tableHeader}>
          <h3 className={styles.tableTitle}>User Management</h3>

          <div className={styles.tableControls}>
            {/* Search */}
            <div className={styles.searchBox}>
              <Search size={14} color="var(--text-3)" />
              <input
                value={search}
                onChange={e => setSearch(e.target.value)}
                placeholder="Search users…"
              />
            </div>

            {/* Filter */}
            <div className={styles.selectWrap}>
              <select
                value={filter}
                onChange={e => setFilter(e.target.value as typeof filter)}
                className={styles.select}
              >
                <option value="all">All users</option>
                <option value="active">Active only</option>
                <option value="deleted">Deleted only</option>
              </select>
              <ChevronDown size={14} className={styles.selectIcon} />
            </div>

            {/* Sort */}
            <div className={styles.selectWrap}>
              <select
                value={sortBy}
                onChange={e => setSortBy(e.target.value as typeof sortBy)}
                className={styles.select}
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
            <p>Failed to load users. Make sure you have admin permissions.</p>
          </div>
        ) : filtered.length === 0 ? (
          <div className={styles.tableEmpty}>
            <Users size={36} color="var(--text-3)" />
            <p>No users found</p>
          </div>
        ) : (
          <div className={styles.tableWrapper}>
            <table className={styles.table}>
              <thead>
                <tr>
                  <th>User</th>
                  <th>Email</th>
                  <th>Role</th>
                  <th>Joined</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map(u => {
                  const isDeleted = u.deleted || u.enabled === false;
                  return (
                    <tr key={u.id} className={isDeleted ? styles.rowDeleted : ''}>
                      <td>
                        <div className={styles.userCell}>
                          <div className={styles.avatar}>
                            {u.firstName?.[0]}{u.lastName?.[0]}
                          </div>
                          <div>
                            <p className={styles.userName}>
                              {u.firstName} {u.lastName}
                            </p>
                            <p className={styles.userId}>ID: {u.id}</p>
                          </div>
                        </div>
                      </td>
                      <td className={styles.emailCell}>{u.email}</td>
                      <td>
                        <span className={`badge ${
                          u.role === 'ADMIN'   ? 'badge--pink' :
                          u.role === 'STUDENT' ? 'badge--accent' : 'badge--yellow'
                        }`}>
                          {u.role.toLowerCase()}
                        </span>
                      </td>
                      <td className={styles.dateCell}>
                        {u.createdAt
                          ? new Date(u.createdAt).toLocaleDateString('en-GB', {
                              day: '2-digit', month: 'short', year: 'numeric',
                            })
                          : '—'
                        }
                      </td>
                      <td>
                        <span className={`badge ${isDeleted ? 'badge--red' : 'badge--green'}`}>
                          {isDeleted ? 'deleted' : 'active'}
                        </span>
                      </td>
                      <td>
                        <div className={styles.actionButtons}>
                          {isDeleted ? (
                            <button
                              className="btn btn--soft btn--sm"
                              onClick={() => recoverMutation.mutate(u.id)}
                              disabled={recoverMutation.isPending}
                              title="Restore user"
                            >
                              {recoverMutation.isPending
                                ? <Spinner size={13} />
                                : <><RefreshCw size={13} /> Restore</>
                              }
                            </button>
                          ) : (
                            <button
                              className="btn btn--danger btn--sm"
                              onClick={() => {
                                if (confirm(`Delete ${u.firstName} ${u.lastName}?`)) {
                                  deleteMutation.mutate(u.id);
                                }
                              }}
                              disabled={deleteMutation.isPending}
                              title="Soft delete user"
                            >
                              {deleteMutation.isPending
                                ? <Spinner size={13} />
                                : <><Trash2 size={13} /> Delete</>
                              }
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
          </p>
        )}
      </div>

      {/* Backend recommendations notice */}
      <div className={`card ${styles.noticeCard}`}>
        <h4 className={styles.noticeTitle}>
          <Shield size={14} /> Analytics Enhancement Recommendations
        </h4>
        <p className={styles.noticeText}>
          For richer analytics (activity trends, feature usage, login history), add a
          <code>GET /api/admin/stats</code> endpoint returning aggregated counts, and expose
          <code>createdAt</code>, <code>enabled</code>, <code>locked</code>, and <code>deleted</code> fields
          in the <code>UserDTO</code> when fetched by an ADMIN role.
          Also fix the duplicate path bug in <code>UserController</code>: <code>@GetMapping("/api/users")</code>
          should be <code>@GetMapping</code> (no path) since the controller is already mapped to <code>/api/users</code>.
        </p>
      </div>
    </div>
  );
};
