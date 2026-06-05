import { Component, type ErrorInfo, type ReactNode } from 'react';
import { AlertTriangle } from 'lucide-react';

interface Props {
  children: ReactNode;
}

interface State {
  hasError: boolean;
  message?: string;
}

export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false };

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, message: error.message };
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    // In production this is where you'd forward to an error-tracking service.
    console.error('Uncaught UI error:', error, info);
  }

  handleReload = () => {
    this.setState({ hasError: false, message: undefined });
    window.location.assign('/dashboard');
  };

  render() {
    if (!this.state.hasError) return this.props.children;

    return (
      <div
        style={{
          minHeight: '100vh',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          gap: 16,
          textAlign: 'center',
          padding: 24,
          background: 'var(--bg)',
        }}
      >
        <div
          style={{
            width: 56, height: 56, borderRadius: '50%',
            background: 'var(--red-glow)', color: 'var(--red)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}
        >
          <AlertTriangle size={26} />
        </div>
        <h1 style={{ fontFamily: 'var(--font-display)', fontSize: 22, color: 'var(--text)' }}>
          Something went wrong
        </h1>
        <p style={{ color: 'var(--text-2)', fontSize: 14, maxWidth: 380 }}>
          An unexpected error occurred while rendering this page. You can return to your
          dashboard and try again.
        </p>
        <button className="btn btn--primary" onClick={this.handleReload}>
          Back to Dashboard
        </button>
      </div>
    );
  }
}
