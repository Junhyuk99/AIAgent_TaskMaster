/**
 * Widget configuration options
 */
export interface WidgetConfig {
  /** API key for authentication */
  apiKey: string;
  /** Agent slug to chat with */
  agentSlug: string;
  /** API base URL */
  apiUrl?: string;
  /** Widget position */
  position?: 'bottom-right' | 'bottom-left' | 'top-right' | 'top-left';
  /** Theme configuration */
  theme?: WidgetTheme;
  /** Custom CSS styles */
  customStyles?: Partial<WidgetStyles>;
  /** Initial message to show */
  welcomeMessage?: string;
  /** Placeholder text for input */
  placeholder?: string;
  /** Button text */
  buttonText?: string;
  /** Title shown in header */
  title?: string;
  /** Auto-open widget on load */
  autoOpen?: boolean;
}

export interface WidgetTheme {
  /** Primary color */
  primaryColor?: string;
  /** Secondary color */
  secondaryColor?: string;
  /** Background color */
  backgroundColor?: string;
  /** Text color */
  textColor?: string;
  /** Font family */
  fontFamily?: string;
  /** Border radius */
  borderRadius?: string;
}

export interface WidgetStyles {
  /** Widget container width */
  width?: string;
  /** Widget container height */
  height?: string;
  /** Button size */
  buttonSize?: string;
  /** Z-index */
  zIndex?: number;
}

export interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
  sources?: DocumentSource[];
}

export interface DocumentSource {
  documentId: string;
  documentName: string;
  score: number;
}

export interface ChatResponse {
  response: string;
  conversationId: string;
  model: string;
  sources?: DocumentSource[];
}

export type WidgetEventType = 'open' | 'close' | 'message' | 'error' | 'ready';

export interface WidgetEventPayload {
  open: undefined;
  close: undefined;
  message: Message;
  error: Error;
  ready: undefined;
}

export type WidgetEventCallback<T extends WidgetEventType> = (
  payload: WidgetEventPayload[T]
) => void;
