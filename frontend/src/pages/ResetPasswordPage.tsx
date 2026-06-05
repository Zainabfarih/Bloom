import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Link, useSearchParams, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { Flower2, Eye, EyeOff, AlertCircle, CheckCircle2 } from 'lucide-react';
import { authApi } from '../api/auth.api';
import { Spinner } from '../components/ui/Spinner';
import styles from './Auth.module.css';

const schema = z.object({
  newPassword: z
    .string()
    .min(8, 'At least 8 characters')
    .regex(/[A-Z]/, 'Include at least one uppercase letter')
    .regex(/[0-9]/, 'Include at least one number'),
  confirm: z.string(),
}).refine(d => d.newPassword === d.confirm, {
  message: "Passwords don't match",
  path: ['confirm'],
});

type FormData = z.infer<typeof schema>;

export const ResetPasswordPage = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token') ?? '';
  const [showNew, setShowNew] = useState(false);
  const [showConfirm, setShowConfirm] = useState(false);
  const [serverError, setServerError] = useState('');
  const [success, setSuccess] = useState(false);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<FormData>({ resolver: zodResolver(schema) });

  const onSubmit = async (data: FormData) => {
    if (!token) {
      setServerError('Invalid or expired reset link. Please request a new one.');
      return;
    }
    setServerError('');
    try {
      await authApi.updatePassword({ token, newPassword: data.newPassword });
      setSuccess(true);
      setTimeout(() => navigate('/login', { replace: true }), 3000);
    } catch (err: unknown) {
      const e = err as { response?: { data?: { message?: string } } };
      setServerError(e?.response?.data?.message ?? 'Reset failed. The link may have expired.');
    }
  };

  if (!token) {
    return (
      <div className={styles.authPage}>
        <div className={styles.card}>
          <div className={styles.logo}>
            <div className={styles.logoIcon}><Flower2 size={18} /></div>
            <span className={styles.logoName}>Bloom</span>
          </div>
          <h1 className={styles.title}>Invalid link</h1>
          <p className={styles.subtitle}>
            This password reset link is invalid or has expired.
          </p>
          <Link to="/forgot-password" className="btn btn--primary btn--full" style={{ marginTop: 8 }}>
            Request a new link
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.authPage}>
      <div className={styles.card}>
        <div className={styles.logo}>
          <div className={styles.logoIcon}><Flower2 size={18} /></div>
          <span className={styles.logoName}>Bloom</span>
        </div>

        {success ? (
          <>
            <div style={{
              width: 52, height: 52, borderRadius: '50%',
              background: 'var(--green-glow)',
              border: '1px solid rgba(52,211,153,0.3)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              marginBottom: 20,
            }}>
              <CheckCircle2 size={26} color="var(--green)" />
            </div>
            <h1 className={styles.title}>Password updated!</h1>
            <p className={styles.subtitle}>
              Your password has been reset. Redirecting you to sign in…
            </p>
          </>
        ) : (
          <>
            <h1 className={styles.title}>Set new password</h1>
            <p className={styles.subtitle}>Choose a strong password for your account.</p>

            <form onSubmit={handleSubmit(onSubmit)} className={styles.form} noValidate>
              <div className="field">
                <label htmlFor="newPassword">New password</label>
                <div className={styles.passwordField}>
                  <input
                    id="newPassword"
                    type={showNew ? 'text' : 'password'}
                    placeholder="Min. 8 characters"
                    className={styles.passwordInput}
                    aria-invalid={!!errors.newPassword}
                    {...register('newPassword')}
                  />
                  <button type="button" className={styles.passwordToggle}
                    onClick={() => setShowNew(v => !v)}
                    aria-label={showNew ? 'Hide' : 'Show'}>
                    {showNew ? <EyeOff size={16} /> : <Eye size={16} />}
                  </button>
                </div>
                {errors.newPassword && (
                  <span className="error"><AlertCircle size={12} />{errors.newPassword.message}</span>
                )}
              </div>

              <div className="field">
                <label htmlFor="confirm">Confirm password</label>
                <div className={styles.passwordField}>
                  <input
                    id="confirm"
                    type={showConfirm ? 'text' : 'password'}
                    placeholder="Repeat password"
                    className={styles.passwordInput}
                    aria-invalid={!!errors.confirm}
                    {...register('confirm')}
                  />
                  <button type="button" className={styles.passwordToggle}
                    onClick={() => setShowConfirm(v => !v)}
                    aria-label={showConfirm ? 'Hide' : 'Show'}>
                    {showConfirm ? <EyeOff size={16} /> : <Eye size={16} />}
                  </button>
                </div>
                {errors.confirm && (
                  <span className="error"><AlertCircle size={12} />{errors.confirm.message}</span>
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
                {isSubmitting ? <Spinner size={18} color="#fff" /> : 'Reset Password'}
              </button>
            </form>

            <p className={styles.footer}>
              <Link to="/login">← Back to sign in</Link>
            </p>
          </>
        )}
      </div>
    </div>
  );
};
