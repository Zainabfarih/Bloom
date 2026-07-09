interface SpinnerProps {
  size?: number;
  center?: boolean;
  color?: string;
}

export const Spinner = ({ size = 24, center = false, color }: SpinnerProps) => {
  const spinner = (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      style={{ animation: 'spin 0.75s linear infinite', flexShrink: 0 }}
    >
      <circle
        cx="12" cy="12" r="10"
        stroke={color ?? 'var(--accent)'}
        strokeWidth="2.5"
        strokeOpacity="0.2"
      />
      <path
        d="M12 2a10 10 0 0 1 10 10"
        stroke={color ?? 'var(--accent)'}
        strokeWidth="2.5"
        strokeLinecap="round"
      />
    </svg>
  );

  if (center) {
    return (
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '48px 0',
      }}>
        {spinner}
      </div>
    );
  }

  return spinner;
};
