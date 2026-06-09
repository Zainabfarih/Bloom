import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { Flower2, Eye, EyeOff, AlertCircle } from 'lucide-react';
import { authApi } from '../api/auth.api';
import { useAuthStore } from '../store/auth.store';
import { Spinner } from '../components/ui/Spinner';
import styles from './Auth.module.css';
import { useQueryClient } from '@tanstack/react-query';

const schema = z.object({
  email:    z.string().email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
});

type FormData = z.infer<typeof schema>;

export const LoginPage = () => {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const setTokens = useAuthStore(s => s.setTokens);
  const setUser   = useAuthStore(s => s.setUser);
  const [serverError, setServerError] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  // When login is rejected because the email isn't verified, we surface a
  // "resend verification" action instead of a plain error.
  const [needsVerification, setNeedsVerification] = useState(false);
  const [resendState, setResendState] = useState<'idle' | 'sending' | 'sent'>('idle');

  const {
    register,
    handleSubmit,
    getValues,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  const onSubmit = async (data: FormData) => {
    setServerError('');
    setNeedsVerification(false);
    setResendState('idle');
    try {
      const res = await authApi.login(data);
      queryClient.clear();
      setTokens(res.accessToken, res.refreshToken);
      setUser(res.user);
      navigate(res.user.role === 'ADMIN' ? '/admin' : '/dashboard', { replace: true });
    } catch (err: unknown) {
      const e = err as { response?: { status?: number; data?: { error?: string; message?: string } } };
      // 403 "Email Not Verified" → offer to resend the verification email.
      if (e?.response?.status === 403 && e?.response?.data?.error === 'Email Not Verified') {
        setNeedsVerification(true);
        setServerError(e?.response?.data?.message ?? 'Please verify your email before signing in.');
      } else {
        setServerError(e?.response?.data?.message ?? 'Invalid email or password');
      }
    }
  };

  const handleResend = async () => {
    const email = getValues('email');
    if (!email) return;
    setResendState('sending');
    try {
      await authApi.resendVerification(email);
    } finally {
      setResendState('sent');
    }
  };

  return (
    <div className={styles.authPage}>
      <div className={styles.card}>
        {/* Logo */}
        <div className={styles.logo}>
          <div className={styles.logoIcon}>
            <Flower2 size={18} />
          </div>
          <span className={styles.logoName}>Bloom</span>
        </div>

        <h1 className={styles.title}>Welcome back</h1>
        <p className={styles.subtitle}>Sign in to continue your career journey</p>

        <form onSubmit={handleSubmit(onSubmit)} className={styles.form} noValidate>
          {/* Email */}
          <div className="field">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              placeholder="you@example.com"
              autoComplete="email"
              aria-invalid={!!errors.email}
              {...register('email')}
            />
            {errors.email && (
              <span className="error">
                <AlertCircle size={12} />{errors.email.message}
              </span>
            )}
          </div>

          {/* Password */}
          <div className="field">
            <label htmlFor="password">Password</label>
            <div className={styles.passwordField}>
              <input
                id="password"
                type={showPassword ? 'text' : 'password'}
                placeholder="••••••••"
                autoComplete="current-password"
                className={styles.passwordInput}
                aria-invalid={!!errors.password}
                {...register('password')}
              />
              <button
                type="button"
                className={styles.passwordToggle}
                onClick={() => setShowPassword(v => !v)}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
              >
                {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
            {errors.password && (
              <span className="error">
                <AlertCircle size={12} />{errors.password.message}
              </span>
            )}
          </div>

          <Link to="/forgot-password" className={styles.forgotLink}>
            Forgot password?
          </Link>

          {serverError && (
            <p className={styles.serverError} role="alert">
              <AlertCircle size={14} />{serverError}
            </p>
          )}

          {needsVerification && (
            resendState === 'sent' ? (
              <p className={styles.successMsg} role="status">
                <AlertCircle size={14} />Verification email sent. Check your inbox, then sign in.
              </p>
            ) : (
              <button
                type="button"
                className="btn btn--ghost btn--full"
                onClick={handleResend}
                disabled={resendState === 'sending'}
              >
                {resendState === 'sending'
                  ? <Spinner size={16} />
                  : 'Resend verification email'}
              </button>
            )
          )}

          <button
            type="submit"
            className="btn btn--primary btn--full btn--lg"
            disabled={isSubmitting}
          >
            {isSubmitting ? <Spinner size={18} color="#fff" /> : 'Sign In'}
          </button>
        </form>

        <p className={styles.footer}>
          No account? <Link to="/register">Create one →</Link>
        </p>
      </div>
    </div>
  );
};
