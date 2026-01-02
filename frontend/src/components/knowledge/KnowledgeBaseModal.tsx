import { useState, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { useKnowledgeStore } from '../../stores/knowledgeStore';
import type { KnowledgeBase, ChunkingStrategy } from '../../services/knowledgeService';

interface KnowledgeBaseModalProps {
  isOpen: boolean;
  onClose: () => void;
  editingKnowledgeBase: KnowledgeBase | null;
  onSuccess?: () => void;
}

const chunkingStrategies: { value: ChunkingStrategy; labelKey: string; descKey: string }[] = [
  { value: 'FIXED_SIZE', labelKey: 'knowledge.chunkingStrategies.fixedSize', descKey: 'knowledge.chunkingStrategies.fixedSizeDesc' },
  { value: 'SENTENCE', labelKey: 'knowledge.chunkingStrategies.sentence', descKey: 'knowledge.chunkingStrategies.sentenceDesc' },
  { value: 'PARAGRAPH', labelKey: 'knowledge.chunkingStrategies.paragraph', descKey: 'knowledge.chunkingStrategies.paragraphDesc' },
];

export default function KnowledgeBaseModal({
  isOpen,
  onClose,
  editingKnowledgeBase,
  onSuccess,
}: KnowledgeBaseModalProps) {
  const { t } = useTranslation();
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [chunkSize, setChunkSize] = useState(500);
  const [chunkOverlap, setChunkOverlap] = useState(50);
  const [chunkingStrategy, setChunkingStrategy] = useState<ChunkingStrategy>('FIXED_SIZE');
  const [errors, setErrors] = useState<Record<string, string>>({});

  const { createKnowledgeBase, updateKnowledgeBase, isLoading, error, clearError } = useKnowledgeStore();

  const isEditing = !!editingKnowledgeBase;

  const resetForm = useCallback(() => {
    setName('');
    setDescription('');
    setChunkSize(500);
    setChunkOverlap(50);
    setChunkingStrategy('FIXED_SIZE');
    setErrors({});
  }, []);

  // Track previous editing knowledge base to detect changes
  const editingKbId = editingKnowledgeBase?.id ?? null;
  const [prevEditingKbId, setPrevEditingKbId] = useState<number | null>(editingKbId);

  // Initialize form with editing knowledge base data using controlled state pattern
  if (editingKbId !== prevEditingKbId) {
    setPrevEditingKbId(editingKbId);
    if (editingKnowledgeBase) {
      setName(editingKnowledgeBase.name);
      setDescription(editingKnowledgeBase.description || '');
      setChunkSize(editingKnowledgeBase.chunkSize);
      setChunkOverlap(editingKnowledgeBase.chunkOverlap);
      setChunkingStrategy(editingKnowledgeBase.chunkingStrategy);
      setErrors({});
    } else {
      resetForm();
    }
  }

  const validate = (): boolean => {
    const newErrors: Record<string, string> = {};

    if (!name.trim()) {
      newErrors.name = 'Name is required';
    } else if (name.length > 100) {
      newErrors.name = 'Name must be at most 100 characters';
    }

    if (description && description.length > 500) {
      newErrors.description = 'Description must be at most 500 characters';
    }

    if (chunkSize < 100 || chunkSize > 8000) {
      newErrors.chunkSize = 'Chunk size must be between 100 and 8000';
    }

    if (chunkOverlap < 0 || chunkOverlap > 1000) {
      newErrors.chunkOverlap = 'Chunk overlap must be between 0 and 1000';
    }

    if (chunkOverlap >= chunkSize) {
      newErrors.chunkOverlap = 'Chunk overlap must be less than chunk size';
    }

    setErrors(newErrors);
    return Object.keys(newErrors).length === 0;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    clearError();

    if (!validate()) return;

    const data = {
      name: name.trim(),
      description: description.trim() || undefined,
      chunkSize,
      chunkOverlap,
      chunkingStrategy,
    };

    try {
      if (isEditing && editingKnowledgeBase) {
        await updateKnowledgeBase(editingKnowledgeBase.id, data);
      } else {
        await createKnowledgeBase(data);
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

  if (!isOpen) return null;

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
                  {isEditing ? t('knowledge.editKnowledgeBase') : t('knowledge.createNewKnowledgeBase')}
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
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    {t('knowledge.name')} <span className="text-red-500">*</span>
                  </label>
                  <input
                    type="text"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    className={`block w-full rounded-md border ${
                      errors.name ? 'border-red-300 dark:border-red-600' : 'border-gray-300 dark:border-gray-600'
                    } px-3 py-2 text-gray-900 dark:text-white dark:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-primary-500`}
                    placeholder={t('knowledge.namePlaceholder')}
                  />
                  {errors.name && (
                    <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.name}</p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                    {t('knowledge.description')}
                  </label>
                  <textarea
                    rows={3}
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    className={`block w-full rounded-md border ${
                      errors.description ? 'border-red-300 dark:border-red-600' : 'border-gray-300 dark:border-gray-600'
                    } px-3 py-2 text-gray-900 dark:text-white dark:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-primary-500`}
                    placeholder={t('knowledge.descriptionPlaceholder')}
                  />
                  {errors.description && (
                    <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.description}</p>
                  )}
                </div>

                <div>
                  <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">
                    {t('knowledge.chunkingStrategy')}
                  </label>
                  <div className="grid grid-cols-3 gap-3">
                    {chunkingStrategies.map((strategy) => (
                      <button
                        key={strategy.value}
                        type="button"
                        onClick={() => setChunkingStrategy(strategy.value)}
                        className={`p-3 rounded-lg border text-left ${
                          chunkingStrategy === strategy.value
                            ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20'
                            : 'border-gray-300 dark:border-gray-600 hover:bg-gray-50 dark:hover:bg-gray-700'
                        }`}
                      >
                        <div className="text-sm font-medium text-gray-900 dark:text-white">
                          {t(strategy.labelKey)}
                        </div>
                        <div className="text-xs text-gray-500 dark:text-gray-400 mt-1">
                          {t(strategy.descKey)}
                        </div>
                      </button>
                    ))}
                  </div>
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                      {t('knowledge.chunkSize')}
                    </label>
                    <input
                      type="number"
                      min={100}
                      max={8000}
                      value={chunkSize}
                      onChange={(e) => setChunkSize(parseInt(e.target.value) || 500)}
                      className={`block w-full rounded-md border ${
                        errors.chunkSize ? 'border-red-300 dark:border-red-600' : 'border-gray-300 dark:border-gray-600'
                      } px-3 py-2 text-gray-900 dark:text-white dark:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-primary-500`}
                    />
                    {errors.chunkSize && (
                      <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.chunkSize}</p>
                    )}
                    <p className="mt-1 text-xs text-gray-500">{t('knowledge.chunkSizeRange')}</p>
                  </div>

                  <div>
                    <label className="block text-sm font-medium text-gray-700 dark:text-gray-300 mb-1">
                      {t('knowledge.chunkOverlap')}
                    </label>
                    <input
                      type="number"
                      min={0}
                      max={1000}
                      value={chunkOverlap}
                      onChange={(e) => setChunkOverlap(parseInt(e.target.value) || 0)}
                      className={`block w-full rounded-md border ${
                        errors.chunkOverlap ? 'border-red-300 dark:border-red-600' : 'border-gray-300 dark:border-gray-600'
                      } px-3 py-2 text-gray-900 dark:text-white dark:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-primary-500`}
                    />
                    {errors.chunkOverlap && (
                      <p className="mt-1 text-sm text-red-600 dark:text-red-400">{errors.chunkOverlap}</p>
                    )}
                    <p className="mt-1 text-xs text-gray-500">{t('knowledge.chunkOverlapRange')}</p>
                  </div>
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
                    {isEditing ? t('knowledge.updating') : t('knowledge.creating')}
                  </span>
                ) : isEditing ? (
                  t('knowledge.updateKnowledgeBase')
                ) : (
                  t('knowledge.createKnowledgeBase')
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
