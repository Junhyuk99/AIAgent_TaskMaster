import { useState, useRef, useEffect } from 'react';
import { useTranslation } from 'react-i18next';
import chatService from '../../services/chatService';
import type { ChatMessage, DocumentSource, FunctionExecution } from '../../services/chatService';
import { useChatStore } from '../../stores/chatStore';

interface ActiveFunction {
  name: string;
  status: 'calling' | 'completed' | 'failed';
  executionTimeMs?: number;
}

interface ChatInterfaceProps {
  agentId: number;
  agentName?: string;
}

export default function ChatInterface({ agentId, agentName }: ChatInterfaceProps) {
  const { t } = useTranslation();
  const { getMessages, getConversationId, addMessage, setConversationId: setStoreConversationId, clearChat } = useChatStore();

  const messages = getMessages(agentId);
  const conversationId = getConversationId(agentId);

  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [streamingContent, setStreamingContent] = useState('');
  const [useStreaming] = useState(true);
  const [activeFunctions, setActiveFunctions] = useState<ActiveFunction[]>([]);
  const [currentStage, setCurrentStage] = useState<string>('');
  // These are collected during streaming but only used at completion
  const streamingSourcesRef = useRef<DocumentSource[]>([]);
  const streamingExecutionsRef = useRef<FunctionExecution[]>([]);
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

    // Add user message to the store
    addMessage(agentId, { role: 'user', content: userMessage });

    // Build history without sources for API
    const historyForApi: ChatMessage[] = messages.map(({ role, content }) => ({ role, content }));

    if (useStreaming) {
      // Streaming mode with function calling support
      try {
        let fullResponse = '';
        let collectedSources: DocumentSource[] = [];
        let collectedExecutions: FunctionExecution[] = [];

        // Reset streaming state
        setActiveFunctions([]);
        setCurrentStage(t('chat.stages.thinking'));
        streamingSourcesRef.current = [];
        streamingExecutionsRef.current = [];

        await chatService.chatStreamWithEvents(
          agentId,
          {
            message: userMessage,
            conversationId: conversationId || undefined,
            history: historyForApi,
          },
          {
            onText: (text) => {
              fullResponse += text;
              setStreamingContent(fullResponse);
              setCurrentStage(t('chat.stages.responding'));
            },
            onFunctionCall: (name) => {
              setActiveFunctions(prev => [...prev, { name, status: 'calling' }]);
              // Show appropriate stage based on function name
              if (name === 'search_knowledge_base') {
                setCurrentStage(t('chat.stages.searchingKB'));
              } else {
                setCurrentStage(t('chat.stages.executingFunction', { name }));
              }
            },
            onFunctionResult: (name, success, executionTimeMs) => {
              setActiveFunctions(prev =>
                prev.map(f =>
                  f.name === name && f.status === 'calling'
                    ? { ...f, status: success ? 'completed' : 'failed', executionTimeMs }
                    : f
                )
              );
            },
            onSources: (sources) => {
              collectedSources = sources;
              streamingSourcesRef.current = sources;
            },
            onFunctionExecutions: (executions) => {
              collectedExecutions = executions;
              streamingExecutionsRef.current = executions;
            },
            onError: (err) => {
              setError(err.message || 'Failed to get response');
              setIsLoading(false);
              setActiveFunctions([]);
              setCurrentStage('');
            },
            onComplete: () => {
              if (fullResponse) {
                addMessage(agentId, {
                  role: 'assistant',
                  content: fullResponse,
                  sources: collectedSources.length > 0 ? collectedSources : undefined,
                  functionExecutions: collectedExecutions.length > 0 ? collectedExecutions : undefined,
                });
              }
              setStreamingContent('');
              setActiveFunctions([]);
              setCurrentStage('');
              streamingSourcesRef.current = [];
              streamingExecutionsRef.current = [];
              setIsLoading(false);
            },
          }
        );
      } catch {
        setError('Failed to send message');
        setIsLoading(false);
        setActiveFunctions([]);
        setCurrentStage('');
      }
    } else {
      // Non-streaming mode - sources available
      try {
        const response = await chatService.chat(agentId, {
          message: userMessage,
          conversationId: conversationId || undefined,
          history: historyForApi,
        });

        setStoreConversationId(agentId, response.conversationId);
        addMessage(agentId, {
          role: 'assistant',
          content: response.response,
          sources: response.sources,
          functionExecutions: response.functionExecutions,
        });
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

  const handleClearChat = () => {
    clearChat(agentId);
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
              {agentName || t('agents.agentChat')}
            </h3>
            <p className="text-xs text-gray-500 dark:text-gray-400">
              {t('common.messages', { count: messages.length })}
            </p>
          </div>
        </div>
        <div className="flex items-center gap-3">
          <button
            onClick={handleClearChat}
            className="text-sm text-gray-500 hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
          >
            {t('common.clear')}
          </button>
        </div>
      </div>

      {/* Messages */}
      <div className="flex-1 overflow-y-auto p-4 space-y-4">
        {messages.length === 0 && !streamingContent && (
          <div className="text-center text-gray-500 dark:text-gray-400 py-8">
            <div className="text-4xl mb-2">&#128172;</div>
            <p>{t('chat.startConversation')}</p>
          </div>
        )}

        {messages.map((message, index) => (
          <div key={index}>
            {/* Sources display ABOVE assistant messages */}
            {message.role === 'assistant' && message.sources && message.sources.length > 0 && (
              <div className="flex justify-start mb-1 ml-2">
                <div className="max-w-[80%]">
                  <div className="text-xs text-gray-500 dark:text-gray-400 mb-1">
                    &#128218; {t('chat.sources')}:
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

            {/* Function executions display for assistant messages */}
            {message.role === 'assistant' && message.functionExecutions && message.functionExecutions.length > 0 && (
              <div className="flex justify-start mt-2 ml-2">
                <div className="max-w-[80%]">
                  <div className="text-xs text-gray-500 dark:text-gray-400 mb-1">
                    &#9889; {t('chat.usedFunctions')}:
                  </div>
                  <div className="flex flex-wrap gap-2">
                    {message.functionExecutions.map((execution, idx) => (
                      <div
                        key={idx}
                        className={`inline-flex items-center px-2 py-1 rounded text-xs ${
                          execution.error
                            ? 'bg-red-50 dark:bg-red-900/20 text-red-700 dark:text-red-300'
                            : 'bg-green-50 dark:bg-green-900/20 text-green-700 dark:text-green-300'
                        }`}
                        title={execution.error || `${execution.executionTimeMs}ms`}
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
                            d="M13 10V3L4 14h7v7l9-11h-7z"
                          />
                        </svg>
                        {execution.functionName}
                        <span className="ml-1 opacity-60">
                          ({execution.executionTimeMs}ms)
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
          <div className="flex flex-col items-start gap-2">
            {/* Current stage indicator */}
            {currentStage && (
              <div className="flex items-center gap-2 ml-2 text-sm text-gray-600 dark:text-gray-300">
                <svg className="w-4 h-4 animate-spin" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                <span>{currentStage}</span>
              </div>
            )}
            {/* Active function calls indicator */}
            {activeFunctions.length > 0 && (
              <div className="flex flex-wrap gap-2 ml-2">
                {activeFunctions.map((fn, idx) => (
                  <div
                    key={idx}
                    className={`inline-flex items-center px-2 py-1 rounded text-xs ${
                      fn.status === 'calling'
                        ? 'bg-yellow-50 dark:bg-yellow-900/20 text-yellow-700 dark:text-yellow-300 animate-pulse'
                        : fn.status === 'completed'
                        ? 'bg-green-50 dark:bg-green-900/20 text-green-700 dark:text-green-300'
                        : 'bg-red-50 dark:bg-red-900/20 text-red-700 dark:text-red-300'
                    }`}
                  >
                    <svg className="w-3 h-3 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 10V3L4 14h7v7l9-11h-7z" />
                    </svg>
                    {fn.status === 'calling' ? t('chat.callingFunction') : fn.name}
                    {fn.status === 'calling' && `: ${fn.name}`}
                    {fn.executionTimeMs && (
                      <span className="ml-1 opacity-60">({fn.executionTimeMs}ms)</span>
                    )}
                  </div>
                ))}
              </div>
            )}
            {/* Loading dots */}
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
            placeholder={t('chat.typeMessage')}
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
