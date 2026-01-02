import React from 'react';
import type { WidgetConfig } from './types';

interface WidgetButtonProps {
  config: WidgetConfig;
  isOpen: boolean;
  onClick: () => void;
}

const WidgetButton: React.FC<WidgetButtonProps> = ({ config, isOpen, onClick }) => {
  const theme = config.theme || {};
  const styles = config.customStyles || {};
  const position = config.position || 'bottom-right';

  const buttonSize = styles.buttonSize || '56px';

  const buttonStyle: React.CSSProperties = {
    position: 'fixed',
    width: buttonSize,
    height: buttonSize,
    borderRadius: '50%',
    backgroundColor: theme.primaryColor || '#3b82f6',
    border: 'none',
    cursor: 'pointer',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
    transition: 'transform 0.2s, box-shadow 0.2s',
    zIndex: (styles.zIndex || 9999) + 1,
    ...getPositionStyles(position),
  };

  const iconStyle: React.CSSProperties = {
    width: '24px',
    height: '24px',
    fill: '#ffffff',
    transition: 'transform 0.3s',
    transform: isOpen ? 'rotate(45deg)' : 'rotate(0)',
  };

  return (
    <button
      onClick={onClick}
      style={buttonStyle}
      onMouseEnter={(e) => {
        e.currentTarget.style.transform = 'scale(1.05)';
        e.currentTarget.style.boxShadow = '0 6px 16px rgba(0, 0, 0, 0.2)';
      }}
      onMouseLeave={(e) => {
        e.currentTarget.style.transform = 'scale(1)';
        e.currentTarget.style.boxShadow = '0 4px 12px rgba(0, 0, 0, 0.15)';
      }}
      aria-label={isOpen ? 'Close chat' : 'Open chat'}
    >
      {isOpen ? (
        <svg style={iconStyle} viewBox="0 0 24 24">
          <path d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z" />
        </svg>
      ) : (
        <svg style={iconStyle} viewBox="0 0 24 24">
          <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z" />
        </svg>
      )}
    </button>
  );
};

function getPositionStyles(
  position: 'bottom-right' | 'bottom-left' | 'top-right' | 'top-left'
): React.CSSProperties {
  switch (position) {
    case 'bottom-right':
      return { bottom: '20px', right: '20px' };
    case 'bottom-left':
      return { bottom: '20px', left: '20px' };
    case 'top-right':
      return { top: '20px', right: '20px' };
    case 'top-left':
      return { top: '20px', left: '20px' };
    default:
      return { bottom: '20px', right: '20px' };
  }
}

export default WidgetButton;
