import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { useMutation } from '@tanstack/react-query';
import { useState, useEffect, useRef } from 'react';
import { CheckCircle2, AlertCircle, User } from 'lucide-react';
import { useAuthStore } from '../store/auth.store';
import { authApi } from '../api/auth.api';
import { Spinner } from '../components/ui/Spinner';
import styles from './ProfilePage.module.css';

const schema = z.object({
  firstName: z.string().min(1, 'Required').max(50),
  lastName:  z.string().min(1, 'Required').max(50),
  email:     z.string().email('Invalid email'),
});

type FormData = z.infer<typeof schema>;

export const ProfilePage = () => {
  const { user, updateUser } = useAuthStore();
  const [saved, setSaved] = useState(false);
  const [serverError, setServerError] = useState('');
  const timerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  // Clear timeout on unmount to avoid memory leak
  useEffect(() => () => {
    if (timerRef.current) clearTimeout(timerRef.current);
  }, []);

  const { register, handleSubmit, formState: { errors, isSubmitting, isDirty } } = useForm<FormData>({
    resolver: zodResolver(schema),
    defaultValues: {
      firstName: user?.firstName ?? '',
      lastName:  user?.lastName  ?? '',
      email:     user?.email     ?? '',
    },
  });

  const mutation = useMutation({
    mutationFn: (data: FormData) => authApi.updateUser(user!.id, data),
    onSuccess: (updated) => {
      updateUser(updated);
      setSaved(true);
      setServerError('');
      if (timerRef.current) clearTimeout(timerRef.current);
      timerRef.current = setTimeout(() => setSaved(false), 3500);
    },
    onError: (err: unknown) => {
      const e = err as { response?: { data?: { message?: string } } };
      setServerError(e?.response?.data?.message ?? 'Update failed. Please try again.');
    },
  });

  const onSubmit = (data: FormData) => mutation.mutate(data);

  const initials = `${user?.firstName?.[0] ?? ''}${user?.lastName?.[0] ?? ''}`.toUpperCase();

  return (
    <div>
      <h1 className="section-title">Profile</h1>
      <p className="section-subtitle">Manage your account information</p>

      <div className={styles.layout}>
        {/* Avatar card */}
        <div className={`card ${styles.avatarCard}`}>
          <div className={styles.avatar}>
            {initials || <User size={22} />}
          </div>
          <p className={styles.avatarName}>{user?.firstName} {user?.lastName}</p>
          <p className={styles.avatarEmail}>{user?.email}</p>
          <span className={`badge badge--accent ${styles.roleBadge}`}>
            {user?.role?.toLowerCase()}
          </span>
        </div>

        {/* Edit form */}
        <div className="card">
          <h2 className={styles.formTitle}>Edit Information</h2>
          <form onSubmit={handleSubmit(onSubmit)} className={styles.form} noValidate>
            <div className={styles.row}>
              <div className="field">
                <label htmlFor="firstName">First name</label>
                <input id="firstName" autoComplete="given-name" {...register('firstName')} />
                {errors.firstName && (
                  <span className="error">
                    <AlertCircle size={12} />{errors.firstName.message}
                  </span>
                )}
              </div>
              <div className="field">
                <label htmlFor="lastName">Last name</label>
                <input id="lastName" autoComplete="family-name" {...register('lastName')} />
                {errors.lastName && (
                  <span className="error">
                    <AlertCircle size={12} />{errors.lastName.message}
                  </span>
                )}
              </div>
            </div>

            <div className="field">
              <label htmlFor="email">Email</label>
              <input id="email" type="email" autoComplete="email" {...register('email')} />
              {errors.email && (
                <span className="error">
                  <AlertCircle size={12} />{errors.email.message}
                </span>
              )}
            </div>

            {serverError && (
              <p className={styles.serverError} role="alert">
                <AlertCircle size={14} />{serverError}
              </p>
            )}

            {saved && (
              <p className={styles.success}>
                <CheckCircle2 size={14} /> Profile updated successfully
              </p>
            )}

            <button
              type="submit"
              className="btn btn--primary"
              disabled={isSubmitting || mutation.isPending || !isDirty}
            >
              {mutation.isPending ? <Spinner size={16} color="#fff" /> : 'Save Changes'}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
};
