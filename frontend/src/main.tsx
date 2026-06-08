import React from 'react';
import ReactDOM from 'react-dom/client';
import { BrowserRouter, Routes, Route, Link } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ProtectedRoute } from './components/ProtectedRoute';
import { AppLayout } from './components/layout/AppLayout';
import { ErrorBoundary } from './components/ErrorBoundary';
import { ToastProvider } from './components/ui/Toast';
import { ConfirmProvider } from './components/ui/ConfirmDialog';
import './index.css';


import { LoginPage }          from './pages/LoginPage';
import { RegisterPage }       from './pages/RegisterPage';
import { ForgotPasswordPage } from './pages/ForgotPasswordPage';
import { ResetPasswordPage }  from './pages/ResetPasswordPage';
import { HomePage }           from './pages/HomePage';
import { AboutPage }          from './pages/AboutPage';
import { DashboardPage }      from './pages/DashboardPage';
import { CvPage }             from './pages/CvPage';
import { JobsPage }           from './pages/JobsPage';
import { RoadmapPage }        from './pages/RoadmapPage';
import { ProfilePage }        from './pages/ProfilePage';
import { AdminPage }          from './pages/AdminPage';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 1000 * 60 * 5,
      refetchOnWindowFocus: false,
    },
  },
});

const NotFoundPage = () => (
  <div style={{
    minHeight: '100vh',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    background: 'var(--bg)',
    gap: 16,
    textAlign: 'center',
    padding: 24,
  }}>
    <p style={{ fontSize: 72, fontFamily: 'var(--font-display)', fontWeight: 800, color: 'var(--text-3)', lineHeight: 1 }}>
      404
    </p>
    <h1 style={{ fontFamily: 'var(--font-display)', fontSize: 22, color: 'var(--text)' }}>
      Page not found
    </h1>
    <p style={{ color: 'var(--text-2)', fontSize: 14 }}>
      The page you're looking for doesn't exist.
    </p>
    <Link to="/dashboard" className="btn btn--primary">
      Go to Dashboard
    </Link>
  </div>
);

export function App() {
  return (
    <ErrorBoundary>
      <QueryClientProvider client={queryClient}>
        <ToastProvider>
          <ConfirmProvider>
            <BrowserRouter>
              <Routes>
                {/* Public */}
                <Route path="/"                element={<HomePage />} />
                <Route path="/about"           element={<AboutPage />} />
                <Route path="/login"           element={<LoginPage />} />
                <Route path="/register"        element={<RegisterPage />} />
                <Route path="/forgot-password" element={<ForgotPasswordPage />} />
                <Route path="/reset-password"  element={<ResetPasswordPage />} />

                {/* Protected — all share AppLayout */}
                <Route element={<ProtectedRoute />}>
                  <Route element={<AppLayout />}>
                    <Route path="/dashboard" element={<DashboardPage />} />
                    <Route path="/cv"        element={<CvPage />} />
                    <Route path="/jobs"      element={<JobsPage />} />
                    <Route path="/roadmap"   element={<RoadmapPage />} />
                    <Route path="/profile"   element={<ProfilePage />} />
                  </Route>
                </Route>

                {/* Admin — requires ADMIN role */}
                <Route element={<ProtectedRoute requiredRole="ADMIN" />}>
                  <Route element={<AppLayout />}>
                    <Route path="/admin" element={<AdminPage />} />
                  </Route>
                </Route>

                {/* 404 */}
                <Route path="*" element={<NotFoundPage />} />
              </Routes>
            </BrowserRouter>
          </ConfirmProvider>
        </ToastProvider>
      </QueryClientProvider>
    </ErrorBoundary>
  );
}

// ── Defensive guard against third-party DOM mutations (Google Translate,
//    Grammarly, etc.) that crash React 19 with:
//    "NotFoundError: Failed to execute 'removeChild' on 'Node'".
//    These extensions replace/move text nodes, so React later tries to remove
//    or insert relative to a node that's no longer where it expects. ──
if (typeof window !== 'undefined' && !(window as unknown as { __domGuard?: boolean }).__domGuard) {
  (window as unknown as { __domGuard?: boolean }).__domGuard = true;

  const originalRemoveChild = Node.prototype.removeChild;
  Node.prototype.removeChild = function <T extends Node>(this: Node, child: T): T {
    if (child.parentNode !== this) return child;
    return originalRemoveChild.call(this, child) as T;
  };

  const originalInsertBefore = Node.prototype.insertBefore;
  Node.prototype.insertBefore = function <T extends Node>(this: Node, newNode: T, referenceNode: Node | null): T {
    if (referenceNode && referenceNode.parentNode !== this) return newNode;
    return originalInsertBefore.call(this, newNode, referenceNode) as T;
  };
}

ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode><App /></React.StrictMode>
);
