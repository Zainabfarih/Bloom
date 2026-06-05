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

const schema = z.object({
  email:    z.string().email('Enter a valid email address'),
  password: z.string().min(1, 'Password is required'),
});

type FormData = z.infer<typeof schema>;

export const LoginPage = () => {
  const navigate = useNavigate();
  const setTokens = useAuthStore(s => s.setTokens);
  const setUser   = useAuthStore(s => s.setUser);
  const [serverError, setServerError] = useState('');
  const [showPassword, setShowPassword] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  const onSubmit = async (data: FormData) => {
    setServerError('');
    try {
      const res = await authApi.login(data);
      setTokens(res.accessToken, res.refreshToken);
      setUser(res.user);
      navigate('/dashboard', { replace: true });
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } };
      setServerError(e?.response?.data?.message ?? 'Invalid email or password');
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
