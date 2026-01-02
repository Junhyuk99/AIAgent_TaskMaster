import React, { useState, useRef, useEffect } from 'react';
import type { WidgetConfig, Message, ChatResponse } from './types';

interface WidgetChatProps {
  config: WidgetConfig;
  isOpen: boolean;
  onClose: () => void;
  onMessage?: (message: Message) => void;
  onError?: (error: Error) => void;
}

const WidgetChat: React.FC<WidgetChatProps> = ({
  config,
  isOpen,
  onClose,
  onMessage,
  onError,
}) => {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [conversationId, setConversationId] = useState<string | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const apiUrl = config.apiUrl || 'http://localhost:8080';

  // Add welcome message on first open
  useEffect(() => {
    if (isOpen && messages.length === 0 && config.welcomeMessage) {
      const welcomeMsg: Message = {
        id: 'welcome',
        role: 'assistant',
        content: config.welcomeMessage,
        timestamp: new Date(),
      };
      setMessages([welcomeMsg]);
    }
  }, [isOpen, config.welcomeMessage, messages.length]);

  // Scroll to bottom when messages change
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const sendMessage = async () => {
    if (!input.trim() || isLoading) return;

    const userMessage: Message = {
      id: Date.now().toString(),
      role: 'user',
      content: input.trim(),
      timestamp: new Date(),
    };

    setMessages((prev) => [...prev, userMessage]);
    setInput('');
    setIsLoading(true);

    try {
      const response = await fetch(
        `${apiUrl}/api/v1/external/agents/${config.agentSlug}/chat`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'X-API-Key': config.apiKey,
          },
          body: JSON.stringify({
            message: userMessage.content,
            conversationId: conversationId,
          }),
        }
      );

      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Failed to send message');
      }

      const data: ChatResponse = await response.json();

      if (data.conversationId) {
        setConversationId(data.conversationId);
      }

      const assistantMessage: Message = {
        id: Date.now().toString() + '-assistant',
        role: 'assistant',
        content: data.response,
        timestamp: new Date(),
        sources: data.sources,
      };

      setMessages((prev) => [...prev, assistantMessage]);
      onMessage?.(assistantMessage);
    } catch (error) {
      const err = error instanceof Error ? error : new Error('Unknown error');
      onError?.(err);

      const errorMessage: Message = {
        id: Date.now().toString() + '-error',
        role: 'assistant',
        content: 'Sorry, an error occurred. Please try again.',
        timestamp: new Date(),
      };
      setMessages((prev) => [...prev, errorMessage]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  const theme = config.theme || {};
  const styles = config.customStyles || {};

  const containerStyle: React.CSSProperties = {
    position: 'fixed',
    width: styles.width || '380px',
    height: styles.height || '520px',
    backgroundColor: theme.backgroundColor || '#ffffff',
    borderRadius: theme.borderRadius || '12px',
    boxShadow: '0 10px 40px rgba(0, 0, 0, 0.15)',
    display: isOpen ? 'flex' : 'none',
    flexDirection: 'column',
    overflow: 'hidden',
    fontFamily: theme.fontFamily || 'system-ui, -apple-system, sans-serif',
    zIndex: styles.zIndex || 9999,
    ...getPositionStyles(config.position || 'bottom-right'),
  };

  const headerStyle: React.CSSProperties = {
    backgroundColor: theme.primaryColor || '#3b82f6',
    color: '#ffffff',
    padding: '16px',
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  };

  const messagesContainerStyle: React.CSSProperties = {
    flex: 1,
    overflowY: 'auto',
    padding: '16px',
    display: 'flex',
    flexDirection: 'column',
    gap: '12px',
  };

  const inputContainerStyle: React.CSSProperties = {
    padding: '12px 16px',
    borderTop: '1px solid #e5e7eb',
    display: 'flex',
    gap: '8px',
    alignItems: 'center',
  };

  return (
    <div style={containerStyle}>
      {/* Header */}
      <div style={headerStyle}>
        <span style={{ fontWeight: 600, fontSize: '16px' }}>
          {config.title || 'Chat'}
        </span>
        <button
          onClick={onClose}
          style={{
            background: 'none',
            border: 'none',
            color: '#ffffff',
            cursor: 'pointer',
            fontSize: '20px',
            padding: '4px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
          }}
        >
          ×
        </button>
      </div>

      {/* Messages */}
      <div style={messagesContainerStyle}>
        {messages.map((message) => (
          <MessageBubble
            key={message.id}
            message={message}
            primaryColor={theme.primaryColor}
          />
        ))}
        {isLoading && <TypingIndicator primaryColor={theme.primaryColor} />}
        <div ref={messagesEndRef} />
      </div>

      {/* Input */}
      <div style={inputContainerStyle}>
        <input
          type="text"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder={config.placeholder || 'Type a message...'}
          disabled={isLoading}
          style={{
            flex: 1,
            padding: '10px 14px',
            border: '1px solid #e5e7eb',
            borderRadius: '8px',
            fontSize: '14px',
            outline: 'none',
            color: theme.textColor || '#1f2937',
          }}
        />
        <button
          onClick={sendMessage}
          disabled={isLoading || !input.trim()}
          style={{
            backgroundColor: theme.primaryColor || '#3b82f6',
            color: '#ffffff',
            border: 'none',
            borderRadius: '8px',
            padding: '10px 16px',
            cursor: isLoading || !input.trim() ? 'not-allowed' : 'pointer',
            opacity: isLoading || !input.trim() ? 0.6 : 1,
            fontSize: '14px',
            fontWeight: 500,
          }}
        >
          {config.buttonText || 'Send'}
        </button>
      </div>
    </div>
  );
};

const MessageBubble: React.FC<{ message: Message; primaryColor?: string }> = ({
  message,
  primaryColor,
}) => {
  const isUser = message.role === 'user';

  const bubbleStyle: React.CSSProperties = {
    maxWidth: '80%',
    padding: '10px 14px',
    borderRadius: '12px',
    fontSize: '14px',
    lineHeight: '1.5',
    alignSelf: isUser ? 'flex-end' : 'flex-start',
    backgroundColor: isUser ? primaryColor || '#3b82f6' : '#f3f4f6',
    color: isUser ? '#ffffff' : '#1f2937',
  };

  return (
    <div style={bubbleStyle}>
      <div style={{ whiteSpace: 'pre-wrap' }}>{message.content}</div>
      {message.sources && message.sources.length > 0 && (
        <div
          style={{
            marginTop: '8px',
            paddingTop: '8px',
            borderTop: '1px solid rgba(0,0,0,0.1)',
            fontSize: '12px',
            opacity: 0.8,
          }}
        >
          Sources: {message.sources.map((s) => s.documentName).join(', ')}
        </div>
      )}
    </div>
  );
};

const TypingIndicator: React.FC<{ primaryColor?: string }> = ({ primaryColor }) => (
  <div
    style={{
      display: 'flex',
      gap: '4px',
      padding: '10px 14px',
      backgroundColor: '#f3f4f6',
      borderRadius: '12px',
      alignSelf: 'flex-start',
      maxWidth: '60px',
    }}
  >
    {[0, 1, 2].map((i) => (
      <div
        key={i}
        style={{
          width: '8px',
          height: '8px',
          borderRadius: '50%',
          backgroundColor: primaryColor || '#3b82f6',
          opacity: 0.6,
          animation: `pulse 1.4s ease-in-out ${i * 0.2}s infinite`,
        }}
      />
    ))}
    <style>{`
      @keyframes pulse {
        0%, 100% { opacity: 0.3; transform: scale(0.8); }
        50% { opacity: 1; transform: scale(1); }
      }
    `}</style>
  </div>
);

function getPositionStyles(
  position: 'bottom-right' | 'bottom-left' | 'top-right' | 'top-left'
): React.CSSProperties {
  switch (position) {
    case 'bottom-right':
      return { bottom: '90px', right: '20px' };
    case 'bottom-left':
      return { bottom: '90px', left: '20px' };
    case 'top-right':
      return { top: '20px', right: '20px' };
    case 'top-left':
      return { top: '20px', left: '20px' };
    default:
      return { bottom: '90px', right: '20px' };
  }
}

export default WidgetChat;
