import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useAgentStore } from '../../stores/agentStore';

interface CreateAgentModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess?: () => void;
}

export default function CreateAgentModal({ isOpen, onClose, onSuccess }: CreateAgentModalProps) {
  const { t } = useTranslation();
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [systemPrompt, setSystemPrompt] = useState('');
  const [errors, setErrors] = useState<Record<string, string>>({});

  const { createAgent, isLoading, error, clearError } = useAgentStore();

  if (!isOpen) return null;

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!name.trim()) {
      newErrors.name = t('agents.nameRequired');
    } else if (name.length > 100) {
      newErrors.name = t('agents.nameTooLong');
    }

    if (description && description.length > 500) {
      newErrors.description = t('agents.descriptionTooLong');
    }

    if (systemPrompt && systemPrompt.length > 10000) {
      newErrors.systemPrompt = t('agents.systemPromptTooLong');
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    clearError();

    if (!validate()) return;

    try {
      await createAgent({
        name: name.trim(),
        description: description.trim() || undefined,
        systemPrompt: systemPrompt.trim() || undefined,
      });
      handleClose();
      onSuccess?.();
    } catch {
      // Error is handled by the store
    }
  };

  const handleClose = () => {
    setName('');
    setDescription('');
    setSystemPrompt('');
    setErrors({});
    clearError();
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 overflow-y-auto">
      <div className="flex min-h-full items-end justify-center p-4 text-center sm:items-center sm:p-0">
        <div
          className="fixed inset-0 bg-gray-500 bg-opacity-75 transition-opacity"
          onClick={handleClose}
        />

        <div className="relative transform overflow-hidden rounded-lg bg-white dark:bg-gray-800 text-left shadow-xl transition-all sm:my-8 sm:w-full sm:max-w-lg">
          <form onSubmit={handleSubmit}>
            <div className="px-4 pb-4 pt-5 sm:p-6">
              <div className="flex items-center justify-between mb-4">
                <h3 className="text-lg font-semibold text-gray-900 dark:text-white">
                  {t('agents.createNewAgent')}
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

              {error && (
                <div className="mb-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-400 px-4 py-3 rounded">
                  {error}
                </div>
              )}

              <div className="space-y-4">
                <div>
                  <label htmlFor="name" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    {t('agents.name')} <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    id="name"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    className={`block w-full rounded-md border ${
                      errors.name ? 'border-red-300 dark:border-red-600' : 'border-gray-300 dark:border-gray-600'
                    } px-3 py-2 text-gray-900 dark:text-white dark:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-primary-500`}
                    placeholder={t('agents.namePlaceholder')}
                  />
                  {errors.name && (
                    <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.name}</p>
                  )}
                </div>

                <div>
                  <label htmlFor="description" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    {t('agents.description')}
                  </label>
                  <input
                    type="text"
                    id="description"
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    className={`block w-full rounded-md border ${
                      errors.description ? 'border-red-300 dark:border-red-600' : 'border-gray-300 dark:border-gray-600'
                    } px-3 py-2 text-gray-900 dark:text-white dark:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-primary-500`}
                    placeholder={t('agents.descriptionPlaceholder')}
                  />
                  {errors.description && (
                    <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.description}</p>
                  )}
                </div>

                <div>
                  <label htmlFor="systemPrompt" className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    {t('agents.systemPrompt')}
                  </label>
                  <textarea
                    id="systemPrompt"
                    rows={4}
                    value={systemPrompt}
                    onChange={(e) => setSystemPrompt(e.target.value)}
                    className={`block w-full rounded-md border ${
                      errors.systemPrompt ? 'border-red-300 dark:border-red-600' : 'border-gray-300 dark:border-gray-600'
                    } px-3 py-2 text-gray-900 dark:text-white dark:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-primary-500`}
                    placeholder={t('agents.systemPromptPlaceholder')}
                  />
                  {errors.systemPrompt && (
                    <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.systemPrompt}</p>
                  )}
                </div>
              </div>
            </div>

            <div className="bg-gray-50 dark:bg-gray-700/50 px-4 py-3 sm:flex sm:flex-row-reverse sm:px-6">
              <button
                type="submit"
                disabled={isLoading}
                className="inline-flex w-full justify-center rounded-md bg-primary-600 px-4 py-2 text-sm font-semibold text-white shadow-sm hover:bg-primary-700 sm:ml-3 sm:w-auto disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isLoading ? (
                  <span className="flex items-center">
                    <svg className="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    {t('agents.creating')}
                  </span>
                ) : (
                  t('agents.createAgent')
                )}
              </button>
              <button
                type="button"
                onClick={handleClose}
                disabled={isLoading}
                className="mt-3 inline-flex w-full justify-center rounded-md bg-white dark:bg-gray-600 px-4 py-2 text-sm font-semibold text-gray-900 dark:text-white shadow-sm ring-1 ring-inset ring-gray-300 dark:ring-gray-500 hover:bg-gray-50 dark:hover:bg-gray-500 sm:mt-0 sm:w-auto disabled:opacity-50"
              >
                {t('common.cancel')}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
}
