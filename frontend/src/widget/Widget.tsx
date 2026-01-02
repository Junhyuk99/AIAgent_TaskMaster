import React, { useState, useEffect, useCallback } from 'react';
import WidgetButton from './WidgetButton';
import WidgetChat from './WidgetChat';
import type { WidgetConfig, Message } from './types';
import { emitEvent } from './widgetEvents';

interface WidgetProps {
  config: WidgetConfig;
  onReady?: () => void;
}

const Widget: React.FC<WidgetProps> = ({ config, onReady }) => {
  const [isOpen, setIsOpen] = useState(config.autoOpen || false);

  useEffect(() => {
    onReady?.();
    emitEvent('ready', undefined);
  }, [onReady]);

  const handleOpen = useCallback(() => {
    setIsOpen(true);
    emitEvent('open', undefined);
  }, []);

  const handleClose = useCallback(() => {
    setIsOpen(false);
    emitEvent('close', undefined);
  }, []);

  const handleToggle = useCallback(() => {
    if (isOpen) {
      handleClose();
    } else {
      handleOpen();
    }
  }, [isOpen, handleOpen, handleClose]);

  const handleMessage = useCallback((message: Message) => {
    emitEvent('message', message);
  }, []);

  const handleError = useCallback((error: Error) => {
    emitEvent('error', error);
  }, []);

  // Expose methods for external control
  useEffect(() => {
    const widgetApi = {
      open: handleOpen,
      close: handleClose,
      toggle: handleToggle,
      isOpen: () => isOpen,
    };

    // Store on window for SDK access
    (window as Window & { __aiAgentWidgetApi?: typeof widgetApi }).__aiAgentWidgetApi = widgetApi;

    return () => {
      delete (window as Window & { __aiAgentWidgetApi?: typeof widgetApi }).__aiAgentWidgetApi;
    };
  }, [handleOpen, handleClose, handleToggle, isOpen]);

  return (
    <>
      <WidgetButton config={config} isOpen={isOpen} onClick={handleToggle} />
      <WidgetChat
        config={config}
        isOpen={isOpen}
        onClose={handleClose}
        onMessage={handleMessage}
        onError={handleError}
      />
    </>
  );
};

export default Widget;
