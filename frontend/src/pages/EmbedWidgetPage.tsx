import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { agentService } from '../services/agentService';
import type { Agent } from '../services/agentService';

interface WidgetConfig {
  position: 'bottom-right' | 'bottom-left' | 'top-right' | 'top-left';
  primaryColor: string;
  backgroundColor: string;
  textColor: string;
  title: string;
  welcomeMessage: string;
  placeholder: string;
  buttonText: string;
  width: string;
  height: string;
}

const defaultConfig: WidgetConfig = {
  position: 'bottom-right',
  primaryColor: '#3b82f6',
  backgroundColor: '#ffffff',
  textColor: '#1f2937',
  title: 'Chat with AI',
  welcomeMessage: 'Hello! How can I help you today?',
  placeholder: 'Type a message...',
  buttonText: 'Send',
  width: '380px',
  height: '520px',
};

export default function EmbedWidgetPage() {
  const { agentId } = useParams<{ agentId: string }>();
  const navigate = useNavigate();
  const [agent, setAgent] = useState<Agent | null>(null);
  const [loading, setLoading] = useState(true);
  const [config, setConfig] = useState<WidgetConfig>(defaultConfig);
  const [embedType, setEmbedType] = useState<'script' | 'iframe'>('script');
  const [copied, setCopied] = useState(false);
  const [apiKey, setApiKey] = useState('YOUR_API_KEY');

  useEffect(() => {
    if (agentId) {
      loadAgent(parseInt(agentId));
    }
  }, [agentId]);

  const loadAgent = async (id: number) => {
    try {
      setLoading(true);
      const data = await agentService.getAgent(id);
      setAgent(data);
      setConfig((prev) => ({
        ...prev,
        title: `Chat with ${data.name}`,
      }));
    } catch (error) {
      console.error('Failed to load agent:', error);
    } finally {
      setLoading(false);
    }
  };

  const generateScriptEmbed = (): string => {
    if (!agent?.slug) return '<!-- Agent slug not configured -->';

    return `<!-- AI Agent Widget -->
<script src="https://cdn.yourdomain.com/widget.umd.js"></script>
<script>
  window.AIAgentWidget.init({
    apiKey: '${apiKey}',
    agentSlug: '${agent.slug}',
    apiUrl: 'https://api.yourdomain.com',
    position: '${config.position}',
    title: '${config.title}',
    welcomeMessage: '${config.welcomeMessage}',
    placeholder: '${config.placeholder}',
    buttonText: '${config.buttonText}',
    theme: {
      primaryColor: '${config.primaryColor}',
      backgroundColor: '${config.backgroundColor}',
      textColor: '${config.textColor}',
    },
    customStyles: {
      width: '${config.width}',
      height: '${config.height}',
    }
  });
</script>`;
  };

  const generateIframeEmbed = (): string => {
    if (!agent?.slug) return '<!-- Agent slug not configured -->';

    const params = new URLSearchParams({
      apiKey: apiKey,
      position: config.position,
      primaryColor: config.primaryColor,
      backgroundColor: config.backgroundColor,
      title: config.title,
    });

    return `<!-- AI Agent Widget (iframe) -->
<iframe
  src="https://yourdomain.com/widget/${agent.slug}?${params.toString()}"
  width="${parseInt(config.width)}"
  height="${parseInt(config.height)}"
  style="border: none; position: fixed; ${getPositionStyle(config.position)} z-index: 9999;"
  allow="clipboard-write"
></iframe>`;
  };

  const getPositionStyle = (position: string): string => {
    switch (position) {
      case 'bottom-right':
        return 'bottom: 20px; right: 20px;';
      case 'bottom-left':
        return 'bottom: 20px; left: 20px;';
      case 'top-right':
        return 'top: 20px; right: 20px;';
      case 'top-left':
        return 'top: 20px; left: 20px;';
      default:
        return 'bottom: 20px; right: 20px;';
    }
  };

  const embedCode = embedType === 'script' ? generateScriptEmbed() : generateIframeEmbed();

  const copyToClipboard = async () => {
    try {
      await navigator.clipboard.writeText(embedCode);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (error) {
      console.error('Failed to copy:', error);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center h-64">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600"></div>
      </div>
    );
  }

  if (!agent) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-500">Agent not found</p>
        <button
          onClick={() => navigate('/agents')}
          className="mt-4 text-blue-600 hover:text-blue-700"
        >
          Back to Agents
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto p-6">
      {/* Header */}
      <div className="mb-8">
        <button
          onClick={() => navigate(`/agents/${agentId}`)}
          className="text-gray-500 hover:text-gray-700 mb-4 flex items-center gap-1"
        >
          <span>&larr;</span> Back to Agent
        </button>
        <h1 className="text-2xl font-bold text-gray-900">Embed Widget</h1>
        <p className="text-gray-500 mt-1">
          Configure and embed the chat widget for <strong>{agent.name}</strong>
        </p>
        {!agent.slug && (
          <div className="mt-4 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
            <p className="text-yellow-800">
              This agent doesn't have a slug configured.
              <button
                onClick={async () => {
                  try {
                    await agentService.generateSlug(agent.id);
                    loadAgent(agent.id);
                  } catch (error) {
                    console.error('Failed to generate slug:', error);
                  }
                }}
                className="ml-2 text-yellow-900 underline hover:no-underline"
              >
                Generate slug now
              </button>
            </p>
          </div>
        )}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
        {/* Configuration Panel */}
        <div className="space-y-6">
          {/* API Key */}
          <div className="bg-white rounded-lg border border-gray-200 p-6">
            <h2 className="text-lg font-semibold mb-4">API Key</h2>
            <input
              type="text"
              value={apiKey}
              onChange={(e) => setApiKey(e.target.value)}
              placeholder="Enter your API key"
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
            <p className="text-sm text-gray-500 mt-2">
              Create an API key in the API Keys section to authenticate widget requests.
            </p>
          </div>

          {/* Position */}
          <div className="bg-white rounded-lg border border-gray-200 p-6">
            <h2 className="text-lg font-semibold mb-4">Position</h2>
            <div className="grid grid-cols-2 gap-3">
              {(['bottom-right', 'bottom-left', 'top-right', 'top-left'] as const).map(
                (pos) => (
                  <button
                    key={pos}
                    onClick={() => setConfig({ ...config, position: pos })}
                    className={`p-3 border rounded-lg text-sm ${
                      config.position === pos
                        ? 'border-blue-500 bg-blue-50 text-blue-700'
                        : 'border-gray-200 hover:border-gray-300'
                    }`}
                  >
                    {pos.replace('-', ' ')}
                  </button>
                )
              )}
            </div>
          </div>

          {/* Colors */}
          <div className="bg-white rounded-lg border border-gray-200 p-6">
            <h2 className="text-lg font-semibold mb-4">Colors</h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Primary Color
                </label>
                <div className="flex gap-2">
                  <input
                    type="color"
                    value={config.primaryColor}
                    onChange={(e) =>
                      setConfig({ ...config, primaryColor: e.target.value })
                    }
                    className="h-10 w-14 rounded border border-gray-300 cursor-pointer"
                  />
                  <input
                    type="text"
                    value={config.primaryColor}
                    onChange={(e) =>
                      setConfig({ ...config, primaryColor: e.target.value })
                    }
                    className="flex-1 px-3 border border-gray-300 rounded-lg"
                  />
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Background Color
                </label>
                <div className="flex gap-2">
                  <input
                    type="color"
                    value={config.backgroundColor}
                    onChange={(e) =>
                      setConfig({ ...config, backgroundColor: e.target.value })
                    }
                    className="h-10 w-14 rounded border border-gray-300 cursor-pointer"
                  />
                  <input
                    type="text"
                    value={config.backgroundColor}
                    onChange={(e) =>
                      setConfig({ ...config, backgroundColor: e.target.value })
                    }
                    className="flex-1 px-3 border border-gray-300 rounded-lg"
                  />
                </div>
              </div>
            </div>
          </div>

          {/* Text Settings */}
          <div className="bg-white rounded-lg border border-gray-200 p-6">
            <h2 className="text-lg font-semibold mb-4">Text Settings</h2>
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Title
                </label>
                <input
                  type="text"
                  value={config.title}
                  onChange={(e) => setConfig({ ...config, title: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Welcome Message
                </label>
                <textarea
                  value={config.welcomeMessage}
                  onChange={(e) =>
                    setConfig({ ...config, welcomeMessage: e.target.value })
                  }
                  rows={2}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg resize-none"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Input Placeholder
                </label>
                <input
                  type="text"
                  value={config.placeholder}
                  onChange={(e) =>
                    setConfig({ ...config, placeholder: e.target.value })
                  }
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                />
              </div>
            </div>
          </div>

          {/* Size */}
          <div className="bg-white rounded-lg border border-gray-200 p-6">
            <h2 className="text-lg font-semibold mb-4">Size</h2>
            <div className="grid grid-cols-2 gap-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Width
                </label>
                <input
                  type="text"
                  value={config.width}
                  onChange={(e) => setConfig({ ...config, width: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Height
                </label>
                <input
                  type="text"
                  value={config.height}
                  onChange={(e) => setConfig({ ...config, height: e.target.value })}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg"
                />
              </div>
            </div>
          </div>
        </div>

        {/* Preview & Code Panel */}
        <div className="space-y-6">
          {/* Preview */}
          <div className="bg-white rounded-lg border border-gray-200 p-6">
            <h2 className="text-lg font-semibold mb-4">Preview</h2>
            <div
              className="relative bg-gray-100 rounded-lg overflow-hidden"
              style={{ height: '400px' }}
            >
              {/* Mock widget preview */}
              <div
                style={{
                  position: 'absolute',
                  width: config.width,
                  height: '300px',
                  maxWidth: '100%',
                  backgroundColor: config.backgroundColor,
                  borderRadius: '12px',
                  boxShadow: '0 10px 40px rgba(0, 0, 0, 0.15)',
                  display: 'flex',
                  flexDirection: 'column',
                  overflow: 'hidden',
                  ...getPreviewPosition(config.position),
                }}
              >
                {/* Header */}
                <div
                  style={{
                    backgroundColor: config.primaryColor,
                    color: '#ffffff',
                    padding: '12px 16px',
                    fontWeight: 600,
                  }}
                >
                  {config.title}
                </div>
                {/* Messages area */}
                <div style={{ flex: 1, padding: '16px' }}>
                  <div
                    style={{
                      backgroundColor: '#f3f4f6',
                      padding: '10px 14px',
                      borderRadius: '12px',
                      maxWidth: '80%',
                      fontSize: '14px',
                      color: config.textColor,
                    }}
                  >
                    {config.welcomeMessage}
                  </div>
                </div>
                {/* Input area */}
                <div
                  style={{
                    padding: '12px 16px',
                    borderTop: '1px solid #e5e7eb',
                    display: 'flex',
                    gap: '8px',
                  }}
                >
                  <div
                    style={{
                      flex: 1,
                      padding: '8px 12px',
                      border: '1px solid #e5e7eb',
                      borderRadius: '8px',
                      color: '#9ca3af',
                      fontSize: '14px',
                    }}
                  >
                    {config.placeholder}
                  </div>
                  <div
                    style={{
                      backgroundColor: config.primaryColor,
                      color: '#ffffff',
                      padding: '8px 14px',
                      borderRadius: '8px',
                      fontSize: '14px',
                    }}
                  >
                    {config.buttonText}
                  </div>
                </div>
              </div>

              {/* Mock button */}
              <div
                style={{
                  position: 'absolute',
                  width: '48px',
                  height: '48px',
                  borderRadius: '50%',
                  backgroundColor: config.primaryColor,
                  boxShadow: '0 4px 12px rgba(0, 0, 0, 0.15)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  ...getButtonPreviewPosition(config.position),
                }}
              >
                <svg width="20" height="20" fill="#ffffff" viewBox="0 0 24 24">
                  <path d="M20 2H4c-1.1 0-2 .9-2 2v18l4-4h14c1.1 0 2-.9 2-2V4c0-1.1-.9-2-2-2zm0 14H6l-2 2V4h16v12z" />
                </svg>
              </div>
            </div>
          </div>

          {/* Embed Code */}
          <div className="bg-white rounded-lg border border-gray-200 p-6">
            <div className="flex justify-between items-center mb-4">
              <h2 className="text-lg font-semibold">Embed Code</h2>
              <div className="flex gap-2">
                <button
                  onClick={() => setEmbedType('script')}
                  className={`px-3 py-1 text-sm rounded ${
                    embedType === 'script'
                      ? 'bg-blue-100 text-blue-700'
                      : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                  }`}
                >
                  JavaScript
                </button>
                <button
                  onClick={() => setEmbedType('iframe')}
                  className={`px-3 py-1 text-sm rounded ${
                    embedType === 'iframe'
                      ? 'bg-blue-100 text-blue-700'
                      : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                  }`}
                >
                  iframe
                </button>
              </div>
            </div>
            <div className="relative">
              <pre className="bg-gray-900 text-gray-100 p-4 rounded-lg overflow-x-auto text-sm">
                <code>{embedCode}</code>
              </pre>
              <button
                onClick={copyToClipboard}
                className="absolute top-2 right-2 px-3 py-1 bg-gray-700 hover:bg-gray-600 text-white text-sm rounded"
              >
                {copied ? 'Copied!' : 'Copy'}
              </button>
            </div>
            <p className="text-sm text-gray-500 mt-3">
              {embedType === 'script'
                ? 'Add this code to your website before the closing </body> tag.'
                : 'Add this iframe to your website where you want the widget to appear.'}
            </p>
          </div>

          {/* Instructions */}
          <div className="bg-blue-50 rounded-lg border border-blue-200 p-6">
            <h3 className="font-semibold text-blue-900 mb-3">Quick Start Guide</h3>
            <ol className="list-decimal list-inside space-y-2 text-blue-800 text-sm">
              <li>Create an API key in the API Keys section</li>
              <li>Copy the embed code above</li>
              <li>Paste it into your website's HTML</li>
              <li>The widget will appear in the configured position</li>
            </ol>
          </div>
        </div>
      </div>
    </div>
  );
}

function getPreviewPosition(
  position: string
): { top?: string; bottom?: string; left?: string; right?: string } {
  switch (position) {
    case 'bottom-right':
      return { bottom: '60px', right: '10px' };
    case 'bottom-left':
      return { bottom: '60px', left: '10px' };
    case 'top-right':
      return { top: '10px', right: '10px' };
    case 'top-left':
      return { top: '10px', left: '10px' };
    default:
      return { bottom: '60px', right: '10px' };
  }
}

function getButtonPreviewPosition(
  position: string
): { top?: string; bottom?: string; left?: string; right?: string } {
  switch (position) {
    case 'bottom-right':
      return { bottom: '10px', right: '10px' };
    case 'bottom-left':
      return { bottom: '10px', left: '10px' };
    case 'top-right':
      return { top: '10px', right: '10px' };
    case 'top-left':
      return { top: '10px', left: '10px' };
    default:
      return { bottom: '10px', right: '10px' };
  }
}
