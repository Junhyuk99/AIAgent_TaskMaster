import { useState, useEffect, useMemo } from 'react';
import type { Function, FunctionTestResponse } from '../../services/functionService';
import { parseParametersFromSchema } from '../../stores/functionStore';

interface ParameterSchema {
  name: string;
  type: string;
  required: boolean;
  description?: string;
}

interface TestHistoryEntry {
  id: string;
  timestamp: Date;
  parameters: Record<string, unknown>;
  result: FunctionTestResponse;
}

interface FunctionTestPanelProps {
  func: Function;
  onTest: (parameters: Record<string, unknown>) => Promise<FunctionTestResponse>;
  isLoading?: boolean;
}

export default function FunctionTestPanel({ func, onTest, isLoading: externalLoading }: FunctionTestPanelProps) {
  const [parameterValues, setParameterValues] = useState<Record<string, string>>({});
  const [testResult, setTestResult] = useState<FunctionTestResponse | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [testHistory, setTestHistory] = useState<TestHistoryEntry[]>([]);
  const [showHistory, setShowHistory] = useState(false);
  const [showRawData, setShowRawData] = useState(false);

  const parameters = useMemo<ParameterSchema[]>(() => {
    return parseParametersFromSchema(func.parametersSchema);
  }, [func.parametersSchema]);

  useEffect(() => {
    // Initialize parameter values
    const initialValues: Record<string, string> = {};
    parameters.forEach((param) => {
      initialValues[param.name] = '';
    });
    setParameterValues(initialValues);
  }, [parameters]);

  const handleParameterChange = (name: string, value: string) => {
    setParameterValues((prev) => ({
      ...prev,
      [name]: value,
    }));
  };

  const parseParameterValue = (value: string, type: string): unknown => {
    if (value === '') return undefined;

    switch (type) {
      case 'number':
        return parseFloat(value);
      case 'boolean':
        return value.toLowerCase() === 'true';
      case 'object':
      case 'array':
        try {
          return JSON.parse(value);
        } catch {
          return value;
        }
      default:
        return value;
    }
  };

  const handleTest = async () => {
    setIsLoading(true);
    setError(null);
    setTestResult(null);

    try {
      // Parse parameter values
      const parsedParams: Record<string, unknown> = {};
      parameters.forEach((param) => {
        const value = parameterValues[param.name];
        if (value !== '' && value !== undefined) {
          parsedParams[param.name] = parseParameterValue(value, param.type);
        }
      });

      const result = await onTest(parsedParams);
      setTestResult(result);

      // Add to history
      const historyEntry: TestHistoryEntry = {
        id: Date.now().toString(),
        timestamp: new Date(),
        parameters: parsedParams,
        result,
      };
      setTestHistory((prev) => [historyEntry, ...prev].slice(0, 10));
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Test failed');
    } finally {
      setIsLoading(false);
    }
  };

  const loadFromHistory = (entry: TestHistoryEntry) => {
    const values: Record<string, string> = {};
    parameters.forEach((param) => {
      const value = entry.parameters[param.name];
      if (value !== undefined) {
        values[param.name] = typeof value === 'object' ? JSON.stringify(value) : String(value);
      } else {
        values[param.name] = '';
      }
    });
    setParameterValues(values);
    setShowHistory(false);
  };

  const getInputType = (type: string): string => {
    switch (type) {
      case 'number':
        return 'number';
      case 'boolean':
        return 'text';
      default:
        return 'text';
    }
  };

  const getPlaceholder = (param: ParameterSchema): string => {
    switch (param.type) {
      case 'number':
        return 'Enter a number';
      case 'boolean':
        return 'true or false';
      case 'object':
        return '{"key": "value"}';
      case 'array':
        return '["item1", "item2"]';
      default:
        return `Enter ${param.name}`;
    }
  };

  const formatResult = (result: unknown): string => {
    if (result === null) return 'null';
    if (result === undefined) return 'undefined';
    if (typeof result === 'object') {
      return JSON.stringify(result, null, 2);
    }
    return String(result);
  };

  const loading = externalLoading || isLoading;

  return (
    <div className="space-y-4">
      {/* Parameter Inputs */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300">
            Test Parameters
          </h4>
          {testHistory.length > 0 && (
            <button
              type="button"
              onClick={() => setShowHistory(!showHistory)}
              className="text-sm text-primary-600 hover:text-primary-700 dark:text-primary-400"
            >
              {showHistory ? 'Hide History' : `History (${testHistory.length})`}
            </button>
          )}
        </div>

        {showHistory && testHistory.length > 0 && (
          <div className="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-3 space-y-2">
            <h5 className="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">
              Recent Tests
            </h5>
            {testHistory.map((entry) => (
              <button
                key={entry.id}
                type="button"
                onClick={() => loadFromHistory(entry)}
                className="w-full text-left p-2 bg-white dark:bg-gray-800 rounded border border-gray-200 dark:border-gray-600 hover:border-primary-500 transition-colors"
              >
                <div className="flex items-center justify-between">
                  <span className="text-xs text-gray-500 dark:text-gray-400">
                    {entry.timestamp.toLocaleTimeString()}
                  </span>
                  <span
                    className={`px-2 py-0.5 text-xs rounded-full ${
                      entry.result.success
                        ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300'
                        : 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300'
                    }`}
                  >
                    {entry.result.success ? 'Success' : 'Failed'}
                  </span>
                </div>
                <div className="text-xs text-gray-600 dark:text-gray-400 mt-1 truncate">
                  {Object.entries(entry.parameters)
                    .map(([k, v]) => `${k}: ${JSON.stringify(v)}`)
                    .join(', ')}
                </div>
              </button>
            ))}
          </div>
        )}

        {parameters.length === 0 ? (
          <p className="text-sm text-gray-500 dark:text-gray-400 italic">
            This function has no parameters
          </p>
        ) : (
          <div className="space-y-3">
            {parameters.map((param) => (
              <div key={param.name}>
                <label className="flex items-center text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                  {param.name}
                  {param.required && <span className="text-red-500 ml-1">*</span>}
                  <span className="ml-2 px-1.5 py-0.5 text-xs bg-gray-100 dark:bg-gray-700 text-gray-600 dark:text-gray-400 rounded">
                    {param.type}
                  </span>
                </label>
                {param.description && (
                  <p className="text-xs text-gray-500 dark:text-gray-400 mb-1">
                    {param.description}
                  </p>
                )}
                {param.type === 'object' || param.type === 'array' ? (
                  <textarea
                    rows={3}
                    value={parameterValues[param.name] || ''}
                    onChange={(e) => handleParameterChange(param.name, e.target.value)}
                    placeholder={getPlaceholder(param)}
                    className="block w-full rounded-md border border-gray-300 dark:border-gray-600 px-3 py-2 text-sm font-mono text-gray-900 dark:text-white dark:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                ) : param.type === 'boolean' ? (
                  <select
                    value={parameterValues[param.name] || ''}
                    onChange={(e) => handleParameterChange(param.name, e.target.value)}
                    className="block w-full rounded-md border border-gray-300 dark:border-gray-600 px-3 py-2 text-sm text-gray-900 dark:text-white dark:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-primary-500"
                  >
                    <option value="">-- Select --</option>
                    <option value="true">true</option>
                    <option value="false">false</option>
                  </select>
                ) : (
                  <input
                    type={getInputType(param.type)}
                    value={parameterValues[param.name] || ''}
                    onChange={(e) => handleParameterChange(param.name, e.target.value)}
                    placeholder={getPlaceholder(param)}
                    className="block w-full rounded-md border border-gray-300 dark:border-gray-600 px-3 py-2 text-sm text-gray-900 dark:text-white dark:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-primary-500"
                  />
                )}
              </div>
            ))}
          </div>
        )}
      </div>

      {/* Run Test Button */}
      <button
        type="button"
        onClick={handleTest}
        disabled={loading}
        className="w-full px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 disabled:opacity-50 flex items-center justify-center gap-2"
      >
        {loading ? (
          <>
            <svg className="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
              <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
              <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
            </svg>
            Running...
          </>
        ) : (
          <>
            <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14.752 11.168l-3.197-2.132A1 1 0 0010 9.87v4.263a1 1 0 001.555.832l3.197-2.132a1 1 0 000-1.664z" />
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
            </svg>
            Run Test
          </>
        )}
      </button>

      {/* Error Display */}
      {error && (
        <div className="p-3 bg-red-50 dark:bg-red-900/20 text-red-700 dark:text-red-300 rounded-lg text-sm">
          <div className="font-medium">Error</div>
          <div className="mt-1">{error}</div>
        </div>
      )}

      {/* Test Result */}
      {testResult && (
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300">
              Test Result
            </h4>
            <div className="flex items-center gap-2">
              <span
                className={`px-2 py-0.5 text-xs rounded-full ${
                  testResult.success
                    ? 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-300'
                    : 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-300'
                }`}
              >
                {testResult.success ? 'Success' : 'Failed'}
              </span>
              {testResult.executionTimeMs !== undefined && (
                <span className="text-xs text-gray-500 dark:text-gray-400">
                  {testResult.executionTimeMs}ms
                </span>
              )}
            </div>
          </div>

          {testResult.success ? (
            <div className="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-3">
              <div className="flex items-center justify-between mb-2">
                <span className="text-xs font-medium text-gray-500 dark:text-gray-400 uppercase">
                  Output
                </span>
                <button
                  type="button"
                  onClick={() => setShowRawData(!showRawData)}
                  className="text-xs text-primary-600 hover:text-primary-700 dark:text-primary-400"
                >
                  {showRawData ? 'Formatted' : 'Raw'}
                </button>
              </div>
              <pre className="text-sm text-gray-900 dark:text-gray-100 font-mono whitespace-pre-wrap overflow-x-auto max-h-64 overflow-y-auto">
                {formatResult(testResult.result)}
              </pre>
            </div>
          ) : (
            <div className="bg-red-50 dark:bg-red-900/20 rounded-lg p-3">
              <div className="text-xs font-medium text-red-600 dark:text-red-400 uppercase mb-2">
                Error Details
              </div>
              <pre className="text-sm text-red-700 dark:text-red-300 font-mono whitespace-pre-wrap overflow-x-auto max-h-64 overflow-y-auto">
                {testResult.error || 'Unknown error'}
              </pre>
            </div>
          )}
        </div>
      )}

      {/* Function Info */}
      <div className="border-t border-gray-200 dark:border-gray-700 pt-3">
        <details className="text-sm">
          <summary className="cursor-pointer text-gray-500 dark:text-gray-400 hover:text-gray-700 dark:hover:text-gray-300">
            Function Details
          </summary>
          <div className="mt-2 space-y-2 text-xs">
            <div className="flex gap-2">
              <span className="font-medium text-gray-600 dark:text-gray-400">Type:</span>
              <span className="text-gray-900 dark:text-gray-100">{func.implementationType}</span>
            </div>
            <div className="flex gap-2">
              <span className="font-medium text-gray-600 dark:text-gray-400">Return:</span>
              <span className="text-gray-900 dark:text-gray-100">{func.returnType || 'void'}</span>
            </div>
            {func.implementationConfig && (
              <div>
                <span className="font-medium text-gray-600 dark:text-gray-400">Config:</span>
                <pre className="mt-1 p-2 bg-gray-100 dark:bg-gray-700 rounded text-xs overflow-x-auto">
                  {JSON.stringify(JSON.parse(func.implementationConfig), null, 2)}
                </pre>
              </div>
            )}
          </div>
        </details>
      </div>
    </div>
  );
}
