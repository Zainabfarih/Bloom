import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams, useNavigate } from 'react-router-dom';
import { Flower2, CheckCircle2, Mail } from 'lucide-react';
import { authApi } from '../api/auth.api';
import { Spinner } from '../components/ui/Spinner';
import styles from './Auth.module.css';

type Status = 'verifying' | 'success' | 'error';

/**
 * Landing page for the link sent in the verification email
 * (FRONTEND_URL/verify-email?token=...).
 *
 * Flow: shows a "verifying…" state while it calls the backend, then either a
 * success state (auto-redirect to /login) or an error state with the option to
 * request a new link.
 */
export const VerifyEmailPage = () => {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();
  const token = searchParams.get('token') ?? '';

  const [status, setStatus] = useState<Status>('verifying');
  const [resendState, setResendState] = useState<'idle' | 'sending' | 'sent'>('idle');
  const [email, setEmail] = useState('');
  // Guard against React 18/19 StrictMode double-invocation of effects.
  const ranRef = useRef(false);

  useEffect(() => {
    if (ranRef.current) return;
    ranRef.current = true;

    if (!token) {
      setStatus('error');
      return;
    }

    authApi
      .verifyEmail(token)
      .then(() => {
        setStatus('success');
        // Send the user back to login once they've seen the confirmation.
        setTimeout(() => navigate('/login', { replace: true }), 3000);
      })
      .catch(() => setStatus('error'));
  }, [token, navigate]);

  const handleResend = async () => {
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
        <div className={styles.logo}>
          <div className={styles.logoIcon}><Flower2 size={18} /></div>
          <span className={styles.logoName}>Bloom</span>
        </div>

        {status === 'verifying' && (
          <>
            <div className={styles.successIcon}>
              <Spinner size={24} />
            </div>
            <h1 className={styles.title}>Verifying your email…</h1>
            <p className={styles.subtitle}>
              Hang tight, we're activating your account.
            </p>
          </>
        )}

        {status === 'success' && (
          <>
            <div className={styles.successIcon}>
              <CheckCircle2 size={26} />
            </div>
            <h1 className={styles.title}>Email verified!</h1>
            <p className={styles.subtitle}>
              Your account is now active. Redirecting you to sign in…
            </p>
            <div style={{ marginTop: 8 }}>
              <Link to="/login" className="btn btn--primary btn--full">
                Sign in now
              </Link>
            </div>
          </>
        )}

        {status === 'error' && (
          <>
            <h1 className={styles.title}>Verification failed</h1>
            <p className={styles.subtitle}>
              This verification link is invalid or has expired. Enter your email
              to receive a new one.
            </p>

            {resendState === 'sent' ? (
              <p className={styles.successMsg} role="status">
                <Mail size={14} />If an account exists for that email, a new link is on its way.
              </p>
            ) : (
              <div className={styles.form}>
                <div className="field">
                  <label htmlFor="email">Email</label>
                  <input
                    id="email"
                    type="email"
                    placeholder="you@example.com"
                    autoComplete="email"
                    value={email}
                    onChange={e => setEmail(e.target.value)}
                  />
                </div>
                <button
                  type="button"
                  className="btn btn--primary btn--full btn--lg"
                  onClick={handleResend}
                  disabled={resendState === 'sending' || !email}
                >
                  {resendState === 'sending'
                    ? <Spinner size={18} color="#fff" />
                    : 'Resend verification link'}
                </button>
              </div>
            )}

            <p className={styles.footer}>
              <Link to="/login">← Back to sign in</Link>
            </p>
          </>
        )}
      </div>
    </div>
  );
};
