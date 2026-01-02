import { useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { useFunctionStore, parseParametersFromSchema } from '../../stores/functionStore';
import type { Function, ImplementationType } from '../../services/functionService';
import ParameterBuilder from './ParameterBuilder';
import type { Parameter } from './ParameterBuilder';
import JsonSchemaPreview from './JsonSchemaPreview';
import FunctionTestPanel from './FunctionTestPanel';
import functionService from '../../services/functionService';

interface FunctionModalProps {
  isOpen: boolean;
  onClose: () => void;
  editingFunction: Function | null;
  onSuccess?: () => void;
}

type TabType = 'basic' | 'parameters' | 'implementation' | 'test';

const tabs: { id: TabType; labelKey: string; editOnly?: boolean }[] = [
  { id: 'basic', labelKey: 'functions.basicInfo' },
  { id: 'parameters', labelKey: 'functions.parameters' },
  { id: 'implementation', labelKey: 'functions.implementation' },
  { id: 'test', labelKey: 'functions.test', editOnly: true },
];

const returnTypes = [
  { value: 'string', labelKey: 'functions.returnTypes.string' },
  { value: 'number', labelKey: 'functions.returnTypes.number' },
  { value: 'boolean', labelKey: 'functions.returnTypes.boolean' },
  { value: 'object', labelKey: 'functions.returnTypes.object' },
  { value: 'array', labelKey: 'functions.returnTypes.array' },
  { value: 'void', labelKey: 'functions.returnTypes.void' },
];

const implementationTypes: { value: ImplementationType; labelKey: string; descKey: string }[] = [
  { value: 'HTTP_API', labelKey: 'functions.implementationTypes.httpApi', descKey: 'functions.implementationTypes.httpApiDesc' },
  { value: 'CODE', labelKey: 'functions.implementationTypes.code', descKey: 'functions.implementationTypes.codeDesc' },
  { value: 'TEMPLATE', labelKey: 'functions.implementationTypes.template', descKey: 'functions.implementationTypes.templateDesc' },
];

const functionTemplates = [
  { id: 'weather', nameKey: 'functions.templates.weather', descKey: 'functions.templates.weatherDesc' },
  { id: 'calculator', nameKey: 'functions.templates.calculator', descKey: 'functions.templates.calculatorDesc' },
  { id: 'web_search', nameKey: 'functions.templates.webSearch', descKey: 'functions.templates.webSearchDesc' },
  { id: 'datetime', nameKey: 'functions.templates.datetime', descKey: 'functions.templates.datetimeDesc' },
];

export default function FunctionModal({ isOpen, onClose, editingFunction, onSuccess }: FunctionModalProps) {
  const { t } = useTranslation();
  const [activeTab, setActiveTab] = useState<TabType>('basic');
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [returnType, setReturnType] = useState('string');
  const [parameters, setParameters] = useState<Parameter[]>([]);
  const [implementationType, setImplementationType] = useState<ImplementationType>('HTTP_API');
  const [httpConfig, setHttpConfig] = useState({
    url: '',
    method: 'GET' as 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH',
    headers: [] as { key: string; value: string }[],
    bodyTemplate: '',
  });
  const [codeConfig, setCodeConfig] = useState({
    language: 'javascript' as 'javascript' | 'python',
    code: '',
  });
  const [selectedTemplate, setSelectedTemplate] = useState<string>('');
  const [errors, setErrors] = useState<Record<string, string>>({});

  const { createFunction, updateFunction, isLoading, error, clearError } = useFunctionStore();

  const isEditing = !!editingFunction;

  const resetForm = useCallback(() => {
    setActiveTab('basic');
    setName('');
    setDescription('');
    setReturnType('string');
    setParameters([]);
    setImplementationType('HTTP_API');
    setHttpConfig({ url: '', method: 'GET', headers: [], bodyTemplate: '' });
    setCodeConfig({ language: 'javascript', code: '' });
    setSelectedTemplate('');
    setErrors({});
  }, []);

  // Track previous editing function to detect changes
  const editingFunctionId = editingFunction?.id ?? null;
  const [prevEditingId, setPrevEditingId] = useState<number | null>(editingFunctionId);

  // Initialize form with editing function data using controlled state pattern
  if (editingFunctionId !== prevEditingId) {
    setPrevEditingId(editingFunctionId);
    if (editingFunction) {
      setActiveTab('basic');
      setName(editingFunction.name);
      setDescription(editingFunction.description || '');
      setReturnType(editingFunction.returnType || 'string');
      setImplementationType(editingFunction.implementationType);

      // Parse parameters from schema
      const parsedParams = parseParametersFromSchema(editingFunction.parametersSchema);
      setParameters(
        parsedParams.map((p, i) => ({
          id: `param_${i}`,
          name: p.name,
          type: p.type as Parameter['type'],
          required: p.required,
          description: p.description || '',
        }))
      );

      // Parse implementation config
      if (editingFunction.implementationConfig) {
        try {
          const config = JSON.parse(editingFunction.implementationConfig);
          if (editingFunction.implementationType === 'HTTP_API') {
            setHttpConfig({
              url: config.url || '',
              method: config.method || 'GET',
              headers: config.headers
                ? Object.entries(config.headers).map(([key, value]) => ({
                    key,
                    value: value as string,
                  }))
                : [],
              bodyTemplate: config.bodyTemplate || '',
            });
          } else if (editingFunction.implementationType === 'CODE') {
            setCodeConfig({
              language: config.language || 'javascript',
              code: config.code || '',
            });
          } else if (editingFunction.implementationType === 'TEMPLATE') {
            setSelectedTemplate(config.templateId || '');
          }
        } catch {
          // Invalid JSON, use defaults
        }
      } else {
        setHttpConfig({ url: '', method: 'GET', headers: [], bodyTemplate: '' });
        setCodeConfig({ language: 'javascript', code: '' });
        setSelectedTemplate('');
      }
      setErrors({});
    } else {
      resetForm();
    }
  }

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!name.trim()) {
      newErrors.name = 'Name is required';
    } else if (!/^[a-zA-Z][a-zA-Z0-9_]*$/.test(name)) {
      newErrors.name = 'Name must start with a letter and contain only letters, numbers, and underscores';
    }

    // Validate parameters have names
    const hasInvalidParams = parameters.some((p) => !p.name.trim());
    if (hasInvalidParams) {
      newErrors.parameters = 'All parameters must have names';
    }

    // Validate implementation config
    if (implementationType === 'HTTP_API' && !httpConfig.url.trim()) {
      newErrors.implementation = 'URL is required for HTTP API';
    } else if (implementationType === 'CODE' && !codeConfig.code.trim()) {
      newErrors.implementation = 'Code is required';
    } else if (implementationType === 'TEMPLATE' && !selectedTemplate) {
      newErrors.implementation = 'Please select a template';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const buildParametersSchema = (): string => {
    const properties: Record<string, object> = {};
    const required: string[] = [];

    parameters.forEach((param) => {
      if (param.name) {
        properties[param.name] = {
          type: param.type,
          ...(param.description && { description: param.description }),
        };
        if (param.required) {
          required.push(param.name);
        }
      }
    });

    return JSON.stringify({
      type: 'object',
      properties,
      ...(required.length > 0 && { required }),
    });
  };

  const buildImplementationConfig = (): string => {
    if (implementationType === 'HTTP_API') {
      const headers: Record<string, string> = {};
      httpConfig.headers.forEach((h) => {
        if (h.key && h.value) {
          headers[h.key] = h.value;
        }
      });
      return JSON.stringify({
        url: httpConfig.url,
        method: httpConfig.method,
        ...(Object.keys(headers).length > 0 && { headers }),
        ...(httpConfig.bodyTemplate && { bodyTemplate: httpConfig.bodyTemplate }),
      });
    } else if (implementationType === 'CODE') {
      return JSON.stringify({
        language: codeConfig.language,
        code: codeConfig.code,
      });
    } else {
      return JSON.stringify({
        templateId: selectedTemplate,
        templateName: selectedTemplate,
      });
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    clearError();

    if (!validate()) {
      // Switch to tab with errors
      if (errors.name) setActiveTab('basic');
      else if (errors.parameters) setActiveTab('parameters');
      else if (errors.implementation) setActiveTab('implementation');
      return;
    }

    const functionData = {
      name: name.trim(),
      description: description.trim() || undefined,
      parametersSchema: buildParametersSchema(),
      returnType: returnType || undefined,
      implementationType,
      implementationConfig: buildImplementationConfig(),
    };

    try {
      if (isEditing && editingFunction) {
        await updateFunction(editingFunction.id, functionData);
      } else {
        await createFunction(functionData);
      }
      handleClose();
      onSuccess?.();
    } catch {
      // Error is handled by the store
    }
  };

  const handleClose = () => {
    resetForm();
    clearError();
    onClose();
  };

  const addHeader = () => {
    setHttpConfig((prev) => ({
      ...prev,
      headers: [...prev.headers, { key: '', value: '' }],
    }));
  };

  const removeHeader = (index: number) => {
    setHttpConfig((prev) => ({
      ...prev,
      headers: prev.headers.filter((_, i) => i !== index),
    }));
  };

  const updateHeader = (index: number, field: 'key' | 'value', value: string) => {
    setHttpConfig((prev) => ({
      ...prev,
      headers: prev.headers.map((h, i) => (i === index ? { ...h, [field]: value } : h)),
    }));
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex min-h-full items-center justify-center p-4">
        <div
          className="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity"
          onClick={handleClose}
        />

        <div className="relative transform overflow-hidden rounded-lg bg-white dark:bg-gray-800 text-left shadow-xl transition-all w-full max-w-4xl max-h-[90vh] flex flex-col">
          <form onSubmit={handleSubmit} className="flex flex-col h-full">
            {/* Header */}
            <div className="px-6 py-4 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between">
              <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                {isEditing ? t('functions.editFunction') : t('functions.createNewFunction')}
              </h3>
              <button
                type="button"
                onClick={handleClose}
                className="text-gray-400 hover:text-gray-500 dark:hover:text-gray-300"
              >
                <svg className="h-6 w-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                </svg>
              </button>
            </div>

            {/* Tabs */}
            <div className="border-b border-gray-200 dark:border-gray-700 px-6">
              <nav className="flex space-x-8">
                {tabs
                  .filter((tab) => !tab.editOnly || isEditing)
                  .map((tab) => (
                    <button
                      key={tab.id}
                      type="button"
                      onClick={() => setActiveTab(tab.id)}
                      className={`py-3 px-1 border-b-2 font-medium text-sm ${
                        activeTab === tab.id
                          ? 'border-primary-500 text-primary-600 dark:text-primary-400'
                          : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300 dark:text-gray-400 dark:hover:text-gray-300'
                      }`}
                    >
                      {t(tab.labelKey)}
                    </button>
                  ))}
              </nav>
            </div>

            {error && (
              <div className="mx-6 mt-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-400 px-4 py-3 rounded">
                {error}
              </div>
            )}

            {/* Content */}
            <div className="flex-1 overflow-y-auto p-6">
              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Left side - Form */}
                <div className="space-y-6">
                  {activeTab === 'basic' && (
                    <>
                      <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                          {t('functions.functionName')} <span className="text-red-500">*</span>
                        </label>
                        <input
                          type="text"
                          value={name}
                          onChange={(e) => setName(e.target.value)}
                          placeholder={t('functions.functionNamePlaceholder')}
                          className={`block w-full rounded-md border ${
                            errors.name ? 'border-red-300 dark:border-red-600' : 'border-gray-300 dark:border-gray-600'
                          } px-3 py-2 text-gray-900 dark:text-white dark:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-primary-500`}
                        />
                        {errors.name && (
                          <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.name}</p>
                        )}
                        <p className="mt-1 text-xs text-gray-500 dark:text-gray-400">
                          {t('functions.functionNameHint')}
                        </p>
                      </div>

                      <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                          {t('agents.description')}
                        </label>
                        <textarea
                          rows={3}
                          value={description}
                          onChange={(e) => setDescription(e.target.value)}
                          placeholder={t('functions.descriptionPlaceholder')}
                          className="block w-full rounded-md border border-gray-300 dark:border-gray-600 px-3 py-2 text-gray-900 dark:text-white dark:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-primary-500"
                        />
                      </div>

                      <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                          {t('functions.returnType')}
                        </label>
                        <select
                          value={returnType}
                          onChange={(e) => setReturnType(e.target.value)}
                          className="block w-full rounded-md border border-gray-300 dark:border-gray-600 px-3 py-2 text-gray-900 dark:text-white dark:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-primary-500"
                        >
                          {returnTypes.map((type) => (
                            <option key={type.value} value={type.value}>
                              {t(type.labelKey)}
                            </option>
                          ))}
                        </select>
                      </div>
                    </>
                  )}

                  {activeTab === 'parameters' && (
                    <>
                      <ParameterBuilder parameters={parameters} onChange={setParameters} />
                      {errors.parameters && (
                        <p className="text-sm text-red-600 dark:text-red-400">{errors.parameters}</p>
                      )}
                    </>
                  )}

                  {activeTab === 'implementation' && (
                    <>
                      <div>
                        <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                          {t('functions.implementationType')}
                        </label>
                        <div className="grid grid-cols-3 gap-3">
                          {implementationTypes.map((type) => (
                            <button
                              key={type.value}
                              type="button"
                              onClick={() => setImplementationType(type.value)}
                              className={`p-3 rounded-lg border text-left ${
                                implementationType === type.value
                                  ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20'
                                  : 'border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-700'
                              }`}
                            >
                              <div className="text-sm font-medium text-gray-900 dark:text-white">
                                {t(type.labelKey)}
                              </div>
                              <div className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                                {t(type.descKey)}
                              </div>
                            </button>
                          ))}
                        </div>
                      </div>

                      {implementationType === 'HTTP_API' && (
                        <div className="space-y-4">
                          <div className="grid grid-cols-4 gap-3">
                            <div className="col-span-1">
                              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                                {t('functions.method')}
                              </label>
                              <select
                                value={httpConfig.method}
                                onChange={(e) =>
                                  setHttpConfig((prev) => ({
                                    ...prev,
                                    method: e.target.value as typeof httpConfig.method,
                                  }))
                                }
                                className="block w-full rounded-md border border-gray-300 dark:border-gray-600 px-3 py-2 text-gray-900 dark:text-white dark:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-primary-500"
                              >
                                <option value="GET">GET</option>
                                <option value="POST">POST</option>
                                <option value="PUT">PUT</option>
                                <option value="DELETE">DELETE</option>
                                <option value="PATCH">PATCH</option>
                              </select>
                            </div>
                            <div className="col-span-3">
                              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                                {t('functions.url')} <span className="text-red-500">*</span>
                              </label>
                              <input
                                type="text"
                                value={httpConfig.url}
                                onChange={(e) =>
                                  setHttpConfig((prev) => ({ ...prev, url: e.target.value }))
                                }
                                placeholder="https://api.example.com/endpoint"
                                className="block w-full rounded-md border border-gray-300 dark:border-gray-600 px-3 py-2 text-gray-900 dark:text-white dark:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-primary-500"
                              />
                            </div>
                          </div>

                          <div>
                            <div className="flex items-center justify-between mb-2">
                              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300">
                                {t('functions.headers')}
                              </label>
                              <button
                                type="button"
                                onClick={addHeader}
                                className="text-sm text-primary-600 hover:text-primary-700 dark:text-primary-400"
                              >
                                {t('functions.addHeader')}
                              </button>
                            </div>
                            {httpConfig.headers.length > 0 && (
                              <div className="space-y-2">
                                {httpConfig.headers.map((header, index) => (
                                  <div key={index} className="flex gap-2">
                                    <input
                                      type="text"
                                      value={header.key}
                                      onChange={(e) => updateHeader(index, 'key', e.target.value)}
                                      placeholder={t('functions.headerName')}
                                      className="flex-1 rounded-md border border-gray-300 dark:border-gray-600 px-3 py-1.5 text-sm text-gray-900 dark:text-white dark:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-primary-500"
                                    />
                                    <input
                                      type="text"
                                      value={header.value}
                                      onChange={(e) => updateHeader(index, 'value', e.target.value)}
                                      placeholder={t('functions.headerValue')}
                                      className="flex-1 rounded-md border border-gray-300 dark:border-gray-600 px-3 py-1.5 text-sm text-gray-900 dark:text-white dark:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-primary-500"
                                    />
                                    <button
                                      type="button"
                                      onClick={() => removeHeader(index)}
                                      className="p-1.5 text-gray-400 hover:text-red-600"
                                    >
                                      <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                      </svg>
                                    </button>
                                  </div>
                                ))}
                              </div>
                            )}
                          </div>

                          {['POST', 'PUT', 'PATCH'].includes(httpConfig.method) && (
                            <div>
                              <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                                {t('functions.requestBodyTemplate')}
                              </label>
                              <textarea
                                rows={4}
                                value={httpConfig.bodyTemplate}
                                onChange={(e) =>
                                  setHttpConfig((prev) => ({ ...prev, bodyTemplate: e.target.value }))
                                }
                                placeholder='{"key": "{{paramName}}"}'
                                className="block w-full rounded-md border border-gray-300 dark:border-gray-600 px-3 py-2 text-sm font-mono text-gray-900 dark:text-white dark:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-primary-500"
                              />
                              <p className="mt-1 text-xs text-gray-500">
                                {t('functions.requestBodyHint')}
                              </p>
                            </div>
                          )}
                        </div>
                      )}

                      {implementationType === 'CODE' && (
                        <div className="space-y-4">
                          <div>
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                              {t('functions.language')}
                            </label>
                            <select
                              value={codeConfig.language}
                              onChange={(e) =>
                                setCodeConfig((prev) => ({
                                  ...prev,
                                  language: e.target.value as typeof codeConfig.language,
                                }))
                              }
                              className="block w-full rounded-md border border-gray-300 dark:border-gray-600 px-3 py-2 text-gray-900 dark:text-white dark:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-primary-500"
                            >
                              <option value="javascript">JavaScript</option>
                              <option value="python">Python</option>
                            </select>
                          </div>
                          <div>
                            <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                              {t('functions.code')} <span className="text-red-500">*</span>
                            </label>
                            <textarea
                              rows={10}
                              value={codeConfig.code}
                              onChange={(e) =>
                                setCodeConfig((prev) => ({ ...prev, code: e.target.value }))
                              }
                              placeholder={
                                codeConfig.language === 'javascript'
                                  ? '// Access parameters via args object\nconst result = args.paramName;\nreturn result;'
                                  : '# Access parameters via args dict\nresult = args["paramName"]\nreturn result'
                              }
                              className="block w-full rounded-md border border-gray-300 dark:border-gray-600 px-3 py-2 text-sm font-mono text-gray-900 dark:text-white dark:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-primary-500"
                            />
                          </div>
                        </div>
                      )}

                      {implementationType === 'TEMPLATE' && (
                        <div>
                          <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                            {t('functions.selectTemplate')} <span className="text-red-500">*</span>
                          </label>
                          <div className="grid grid-cols-2 gap-3">
                            {functionTemplates.map((template) => (
                              <button
                                key={template.id}
                                type="button"
                                onClick={() => setSelectedTemplate(template.id)}
                                className={`p-4 rounded-lg border text-left ${
                                  selectedTemplate === template.id
                                    ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20'
                                    : 'border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-700'
                                }`}
                              >
                                <div className="text-sm font-medium text-gray-900 dark:text-white">
                                  {t(template.nameKey)}
                                </div>
                                <div className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                                  {t(template.descKey)}
                                </div>
                              </button>
                            ))}
                          </div>
                        </div>
                      )}

                      {errors.implementation && (
                        <p className="text-sm text-red-600 dark:text-red-400">{errors.implementation}</p>
                      )}
                    </>
                  )}

                  {activeTab === 'test' && editingFunction && (
                    <FunctionTestPanel
                      func={editingFunction}
                      onTest={async (parameters) => {
                        const result = await functionService.test(editingFunction.id, { parameters });
                        return result;
                      }}
                      isLoading={isLoading}
                    />
                  )}
                </div>

                {/* Right side - JSON Schema Preview */}
                <div className="hidden lg:block">
                  <JsonSchemaPreview
                    functionName={name}
                    description={description}
                    parameters={parameters}
                    returnType={returnType}
                  />
                </div>
              </div>
            </div>

            {/* Footer */}
            <div className="px-6 py-4 border-t border-gray-200 dark:border-gray-700 flex justify-end gap-3">
              <button
                type="button"
                onClick={handleClose}
                disabled={isLoading}
                className="px-4 py-2 text-sm font-medium text-gray-700 dark:text-gray-300 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-md hover:bg-gray-50 dark:hover:bg-gray-600 disabled:opacity-50"
              >
                {t('common.cancel')}
              </button>
              <button
                type="submit"
                disabled={isLoading}
                className="px-4 py-2 text-sm font-medium text-white bg-primary-600 rounded-md hover:bg-primary-700 disabled:opacity-50"
              >
                {isLoading ? (
                  <span className="flex items-center">
                    <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" />
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                    </svg>
                    {isEditing ? t('functions.updating') : t('functions.creating')}
                  </span>
                ) : isEditing ? (
                  t('functions.updateFunction')
                ) : (
                  t('functions.createFunction')
                )}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
