import React, { useState, useEffect, useCallback } from 'react';
import WidgetButton from './WidgetButton';
import WidgetChat from './WidgetChat';
import type { WidgetConfig, Message, WidgetEventType, WidgetEventCallback } from './types';

interface WidgetProps {
  config: WidgetConfig;
  onReady?: () => void;
}

// Event emitter for widget events
type EventListeners = {
  [K in WidgetEventType]: Set<WidgetEventCallback<K>>;
};

const eventListeners: EventListeners = {
  open: new Set(),
  close: new Set(),
  message: new Set(),
  error: new Set(),
  ready: new Set(),
};

export function emitEvent<T extends WidgetEventType>(
  event: T,
  payload?: Parameters<WidgetEventCallback<T>>[0]
) {
  eventListeners[event].forEach((callback) => {
    (callback as WidgetEventCallback<T>)(payload as Parameters<WidgetEventCallback<T>>[0]);
  });
}

export function addEventListener<T extends WidgetEventType>(
  event: T,
  callback: WidgetEventCallback<T>
) {
  eventListeners[event].add(callback as WidgetEventCallback<T>);
  return () => eventListeners[event].delete(callback as WidgetEventCallback<T>);
}

export function removeEventListener<T extends WidgetEventType>(
  event: T,
  callback: WidgetEventCallback<T>
) {
  eventListeners[event].delete(callback as WidgetEventCallback<T>);
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
