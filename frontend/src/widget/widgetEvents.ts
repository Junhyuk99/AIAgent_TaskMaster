import type { WidgetEventType, WidgetEventCallback } from './types';

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
