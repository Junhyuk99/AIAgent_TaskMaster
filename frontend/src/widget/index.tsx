import React from 'react';
import { createRoot } from 'react-dom/client';
import type { Root } from 'react-dom/client';
import Widget, { addEventListener, removeEventListener } from './Widget';
import type { WidgetConfig, WidgetEventType, WidgetEventCallback } from './types';

// Re-export types
export type { WidgetConfig, WidgetEventType, WidgetEventCallback, Message } from './types';

let widgetRoot: Root | null = null;
let widgetContainer: HTMLElement | null = null;
let currentConfig: WidgetConfig | null = null;

/**
 * Initialize the AI Agent Widget
 */
function init(config: WidgetConfig): void {
  // Validate required config
  if (!config.apiKey) {
    console.error('AIAgentWidget: apiKey is required');
    return;
  }
  if (!config.agentSlug) {
    console.error('AIAgentWidget: agentSlug is required');
    return;
  }

  // Destroy existing widget if any
  if (widgetRoot) {
    destroy();
  }

  currentConfig = config;

  // Create container
  widgetContainer = document.createElement('div');
  widgetContainer.id = 'ai-agent-widget-container';
  widgetContainer.style.cssText = 'position: fixed; z-index: 9999;';
  document.body.appendChild(widgetContainer);

  // Create React root and render
  widgetRoot = createRoot(widgetContainer);
  widgetRoot.render(
    <React.StrictMode>
      <Widget config={config} />
    </React.StrictMode>
  );

  console.log('AIAgentWidget: initialized');
}

/**
 * Open the widget
 */
function open(): void {
  const api = (window as Window & { __aiAgentWidgetApi?: { open: () => void } }).__aiAgentWidgetApi;
  if (api) {
    api.open();
  } else {
    console.warn('AIAgentWidget: Widget not initialized');
  }
}

/**
 * Close the widget
 */
function close(): void {
  const api = (window as Window & { __aiAgentWidgetApi?: { close: () => void } }).__aiAgentWidgetApi;
  if (api) {
    api.close();
  } else {
    console.warn('AIAgentWidget: Widget not initialized');
  }
}

/**
 * Toggle the widget open/close state
 */
function toggle(): void {
  const api = (window as Window & { __aiAgentWidgetApi?: { toggle: () => void } }).__aiAgentWidgetApi;
  if (api) {
    api.toggle();
  } else {
    console.warn('AIAgentWidget: Widget not initialized');
  }
}

/**
 * Check if widget is open
 */
function isOpen(): boolean {
  const api = (window as Window & { __aiAgentWidgetApi?: { isOpen: () => boolean } }).__aiAgentWidgetApi;
  if (api) {
    return api.isOpen();
  }
  return false;
}

/**
 * Destroy the widget and clean up
 */
function destroy(): void {
  if (widgetRoot) {
    widgetRoot.unmount();
    widgetRoot = null;
  }
  if (widgetContainer && widgetContainer.parentNode) {
    widgetContainer.parentNode.removeChild(widgetContainer);
    widgetContainer = null;
  }
  currentConfig = null;
  console.log('AIAgentWidget: destroyed');
}

/**
 * Register an event listener
 */
function on<T extends WidgetEventType>(event: T, callback: WidgetEventCallback<T>): () => void {
  return addEventListener(event, callback);
}

/**
 * Remove an event listener
 */
function off<T extends WidgetEventType>(event: T, callback: WidgetEventCallback<T>): void {
  removeEventListener(event, callback);
}

/**
 * Get current configuration
 */
function getConfig(): WidgetConfig | null {
  return currentConfig;
}

// SDK object
const AIAgentWidget = {
  init,
  open,
  close,
  toggle,
  isOpen,
  destroy,
  on,
  off,
  getConfig,
  version: '1.0.0',
};

// Expose to global scope
declare global {
  interface Window {
    AIAgentWidget: typeof AIAgentWidget;
  }
}

window.AIAgentWidget = AIAgentWidget;

export default AIAgentWidget;
