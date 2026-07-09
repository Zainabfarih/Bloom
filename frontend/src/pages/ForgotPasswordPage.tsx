import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link } from 'react-router-dom';
import { useState } from 'react';
import { Flower2, AlertCircle, Mail, ArrowLeft } from 'lucide-react';
import { authApi } from '../api/auth.api';
import { Spinner } from '../components/ui/Spinner';
import styles from './Auth.module.css';

const schema = z.object({
  email: z.string().email('Enter a valid email address'),
});
type FormData = z.infer<typeof schema>;

export const ForgotPasswordPage = () => {
  const [serverError, setServerError] = useState('');
  const [sent, setSent] = useState(false);

  const {
    register,
    handleSubmit,
    getValues,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  const onSubmit = async (data: FormData) => {
    setServerError('');
    try {
      await authApi.requestPasswordReset(data.email);
      setSent(true);
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } };
      setServerError(e?.response?.data?.message ?? 'Something went wrong. Please try again.');
    }
  };

  return (
    <div className={styles.authPage}>
      <div className={styles.card}>
        <div className={styles.logo}>
          <div className={styles.logoIcon}>
            <Flower2 size={18} />
          </div>
          <span className={styles.logoName}>Bloom</span>
        </div>

        {sent ? (
          <>
            <div className={styles.successIcon}>
              <Mail size={24} />
            </div>
            <h1 className={styles.title}>Check your email</h1>
            <p className={styles.subtitle}>
              We sent a password reset link to <strong style={{ color: 'var(--text)' }}>
                {getValues('email')}
              </strong>. It expires in 1 hour.
            </p>
            <div style={{ marginTop: 8 }}>
              <Link to="/login" className="btn btn--ghost btn--full">
                <ArrowLeft size={15} /> Back to sign in
              </Link>
            </div>
          </>
        ) : (
          <>
            <h1 className={styles.title}>Forgot password?</h1>
            <p className={styles.subtitle}>
              Enter your email and we'll send you a reset link.
            </p>

            <form onSubmit={handleSubmit(onSubmit)} className={styles.form} noValidate>
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
                {isSubmitting ? <Spinner size={18} color="#fff" /> : 'Send Reset Link'}
              </button>
            </form>

            <p className={styles.footer}>
              Remembered it? <Link to="/login">Sign in</Link>
            </p>
          </>
        )}
      </div>
    </div>
  );
};
