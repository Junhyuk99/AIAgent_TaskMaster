import { useState, useRef, useEffect } from 'react';
import chatService from '../../services/chatService';
import type { ChatMessage, DocumentSource } from '../../services/chatService';

interface ChatInterfaceProps {
  agentId: number;
  agentName?: string;
}

interface DisplayMessage extends ChatMessage {
  sources?: DocumentSource[];
}

export default function ChatInterface({ agentId, agentName }: ChatInterfaceProps) {
  const [messages, setMessages] = useState<DisplayMessage[]>([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [conversationId, setConversationId] = useState<string | null>(null);
  const [streamingContent, setStreamingContent] = useState('');
  const [useStreaming, setUseStreaming] = useState(true);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, streamingContent]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!input.trim() || isLoading) return;

    const userMessage = input.trim();
    setInput('');
    setError(null);
    setIsLoading(true);
    setStreamingContent('');

    // Add user message to the list
    const newUserMessage: DisplayMessage = { role: 'user', content: userMessage };
    setMessages((prev) => [...prev, newUserMessage]);

    // Build history without sources for API
    const historyForApi: ChatMessage[] = messages.map(({ role, content }) => ({ role, content }));

    if (useStreaming) {
      // Streaming mode - no sources available
      try {
        let fullResponse = '';

        await chatService.chatStream(
          agentId,
          {
            message: userMessage,
            conversationId: conversationId || undefined,
            history: historyForApi,
          },
          (chunk) => {
            fullResponse += chunk;
            setStreamingContent(fullResponse);
          },
          (err) => {
            setError(err.message || 'Failed to get response');
            setIsLoading(false);
          },
          () => {
            // On complete
            if (fullResponse) {
              setMessages((prev) => [...prev, { role: 'assistant', content: fullResponse }]);
            }
            setStreamingContent('');
            setIsLoading(false);
          }
        );
      } catch {
        setError('Failed to send message');
        setIsLoading(false);
      }
    } else {
      // Non-streaming mode - sources available
      try {
        const response = await chatService.chat(agentId, {
          message: userMessage,
          conversationId: conversationId || undefined,
          history: historyForApi,
        });

        setConversationId(response.conversationId);
        setMessages((prev) => [
          ...prev,
          {
            role: 'assistant',
            content: response.response,
            sources: response.sources,
          },
        ]);
        setIsLoading(false);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Failed to send message');
        setIsLoading(false);
      }
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e);
    }
  };

  const clearChat = () => {
    setMessages([]);
    setConversationId(null);
    setError(null);
    setStreamingContent('');
  };

  return (
    <div className="flex flex-col h-full bg-white dark:bg-gray-800 rounded-lg shadow">
      {/* Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200 dark:border-gray-700">
        <div className="flex items-center">
          <div className="w-8 h-8 bg-primary-100 dark:bg-primary-900 rounded-full flex items-center justify-center mr-3">
            <span className="text-lg">&#129302;</span>
          </div>
          <div>
            <h3 className="font-medium text-gray-900 dark:text-white">
              {agentName || 'Agent Chat'}
            </h3>
            <p className="text-xs text-gray-500 dark:text-gray-400">
              {messages.length} messages
            </p>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <label className="flex items-center text-xs text-gray-500 dark:text-gray-400">
            <input
              type="checkbox"
              checked={useStreaming}
              onChange={(e) => setUseStreaming(e.target.checked)}
              className="mr-1.5 h-3.5 w-3.5 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
            />
            Stream
          </label>
          <button
            onClick={clearChat}
            className="text-sm text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
          >
            Clear
          </button>
        </div>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {messages.length === 0 && !streamingContent && (
          <div className="text-center text-gray-500 dark:text-gray-400 py-8">
            <div className="text-4xl mb-2">&#128172;</div>
            <p>Start a conversation with the agent</p>
            <p className="text-xs mt-2">
              {useStreaming
                ? 'Streaming mode (no sources)'
                : 'Standard mode (with sources)'}
            </p>
          </div>
        )}

        {messages.map((message, index) => (
          <div key={index}>
            <div
              className={`flex ${message.role === 'user' ? 'justify-end' : 'justify-start'}`}
            >
              <div
                className={`max-w-[80%] rounded-lg px-4 py-2 ${
                  message.role === 'user'
                    ? 'bg-primary-600 text-white'
                    : 'bg-gray-100 dark:bg-gray-700 text-gray-900 dark:text-white'
                }`}
              >
                <p className="whitespace-pre-wrap">{message.content}</p>
              </div>
            </div>

            {/* Sources display for assistant messages */}
            {message.role === 'assistant' && message.sources && message.sources.length > 0 && (
              <div className="flex justify-start mt-2 ml-2">
                <div className="max-w-[80%]">
                  <div className="text-xs text-gray-500 dark:text-gray-400 mb-1">
                    &#128218; Sources:
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {message.sources.map((source, idx) => (
                      <div
                        key={idx}
                        className="inline-flex items-center px-2 py-1 bg-blue-50 dark:bg-blue-900/20 text-blue-700 dark:text-blue-300 rounded text-xs"
                        title={`Relevance: ${(source.score * 100).toFixed(0)}%`}
                      >
                        <svg
                          className="w-3 h-3 mr-1"
                          fill="none"
                          stroke="currentColor"
                          viewBox="0 0 24 24"
                        >
                          <path
                            strokeLinecap="round"
                            strokeLinejoin="round"
                            strokeWidth={2}
                            d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"
                          />
                        </svg>
                        {source.documentName || `Document ${source.documentId}`}
                        <span className="ml-1 opacity-60">
                          ({(source.score * 100).toFixed(0)}%)
                        </span>
                      </div>
                    ))}
                  </div>
                </div>
              </div>
            )}
          </div>
        ))}

        {streamingContent && (
          <div className="flex justify-start">
            <div className="max-w-[80%] rounded-lg px-4 py-2 bg-gray-100 dark:bg-gray-700 text-gray-900 dark:text-white">
              <p className="whitespace-pre-wrap">{streamingContent}</p>
              <span className="inline-block w-2 h-4 bg-gray-400 animate-pulse ml-1" />
            </div>
          </div>
        )}

        {isLoading && !streamingContent && (
          <div className="flex justify-start">
            <div className="rounded-lg px-4 py-2 bg-gray-100 dark:bg-gray-700">
              <div className="flex space-x-2">
                <div
                  className="w-2 h-2 bg-gray-400 rounded-full animate-bounce"
                  style={{ animationDelay: '0ms' }}
                />
                <div
                  className="w-2 h-2 bg-gray-400 rounded-full animate-bounce"
                  style={{ animationDelay: '150ms' }}
                />
                <div
                  className="w-2 h-2 bg-gray-400 rounded-full animate-bounce"
                  style={{ animationDelay: '300ms' }}
                />
              </div>
            </div>
          </div>
        )}

        {error && (
          <div className="flex justify-center">
            <div className="bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 px-4 py-2 rounded-lg text-sm">
              {error}
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Input */}
      <form onSubmit={handleSubmit} className="p-4 border-t border-gray-200 dark:border-gray-700">
        <div className="flex items-end space-x-2">
          <textarea
            ref={inputRef}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder="Type your message..."
            rows={1}
            disabled={isLoading}
            className="flex-1 resize-none rounded-lg border border-gray-300 dark:border-gray-600 bg-white dark:bg-gray-700 px-4 py-2 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500 disabled:opacity-50"
            style={{ minHeight: '40px', maxHeight: '120px' }}
          />
          <button
            type="submit"
            disabled={isLoading || !input.trim()}
            className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isLoading ? (
              <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24">
                <circle
                  className="opacity-25"
                  cx="12"
                  cy="12"
                  r="10"
                  stroke="currentColor"
                  strokeWidth="4"
                  fill="none"
                />
                <path
                  className="opacity-75"
                  fill="currentColor"
                  d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                />
              </svg>
            ) : (
              <svg className="h-5 w-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path
                  strokeLinecap="round"
                  strokeLinejoin="round"
                  strokeWidth={2}
                  d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8"
                />
              </svg>
            )}
          </button>
        </div>
      </form>
    </div>
  );
}
