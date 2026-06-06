import {
  createContext, useCallback, useContext, useMemo, useState, type ReactNode,
} from 'react';
import { CheckCircle2, AlertCircle, Info, X } from 'lucide-react';
import styles from './Toast.module.css';

type ToastType = 'success' | 'error' | 'info';

interface ToastItem {
  id: number;
  type: ToastType;
  message: string;
}

interface ToastContextValue {
  toast: (message: string, type?: ToastType) => void;
  success: (message: string) => void;
  error: (message: string) => void;
  info: (message: string) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

// eslint-disable-next-line react-refresh/only-export-components
export const useToast = (): ToastContextValue => {
  const ctx = useContext(ToastContext);
  if (!ctx) throw new Error('useToast must be used within a ToastProvider');
  return ctx;
};

let counter = 0;

const ToastIcon = ({ type }: { type: ToastType }) => {
  if (type === 'success') return <CheckCircle2 size={17} />;
  if (type === 'error') return <AlertCircle size={17} />;
  return <Info size={17} />;
};

export const ToastProvider = ({ children }: { children: ReactNode }) => {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const remove = useCallback((id: number) => {
    setToasts(prev => prev.filter(t => t.id !== id));
  }, []);

  const toast = useCallback((message: string, type: ToastType = 'info') => {
    const id = ++counter;
    setToasts(prev => [...prev, { id, type, message }]);
    window.setTimeout(() => remove(id), 4500);
  }, [remove]);

  const value = useMemo<ToastContextValue>(() => ({
    toast,
    success: (m: string) => toast(m, 'success'),
    error: (m: string) => toast(m, 'error'),
    info: (m: string) => toast(m, 'info'),
  }), [toast]);

  return (
    <ToastContext.Provider value={value}>
      {children}
      <div className={styles.container} aria-live="polite" aria-label="Notifications">
        {toasts.map(t => (
          <div key={t.id} className={`${styles.toast} ${styles[t.type]}`} role="status">
            <span className={styles.icon}><ToastIcon type={t.type} /></span>
            <span className={styles.msg}>{t.message}</span>
            <button
              className={styles.close}
              onClick={() => remove(t.id)}
              aria-label="Dismiss notification"
            >
              <X size={14} />
            </button>
          </div>
        ))}
      </div>
    </ToastContext.Provider>
  );
};
