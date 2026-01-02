import { useEffect, useState, useRef, useCallback } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useTranslation } from 'react-i18next';
import { useKnowledgeStore } from '../stores/knowledgeStore';
import type { Document, SearchRequest, SearchMatch } from '../services/knowledgeService';
import { KnowledgeBaseModal, DeleteConfirmModal } from '../components/knowledge';

type DocumentStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

const statusConfig: Record<DocumentStatus, { labelKey: string; color: string; bgColor: string }> = {
  PENDING: { labelKey: 'knowledge.documentStatus.pending', color: 'text-yellow-800 dark:text-yellow-300', bgColor: 'bg-yellow-100 dark:bg-yellow-900/30' },
  PROCESSING: { labelKey: 'knowledge.documentStatus.processing', color: 'text-blue-800 dark:text-blue-300', bgColor: 'bg-blue-100 dark:bg-blue-900/30' },
  COMPLETED: { labelKey: 'knowledge.documentStatus.completed', color: 'text-green-800 dark:text-green-300', bgColor: 'bg-green-100 dark:bg-green-900/30' },
  FAILED: { labelKey: 'knowledge.documentStatus.failed', color: 'text-red-800 dark:text-red-300', bgColor: 'bg-red-100 dark:bg-red-900/30' },
};

export default function KnowledgeBaseDetailPage() {
  const { t, i18n } = useTranslation();
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [isEditModalOpen, setIsEditModalOpen] = useState(false);
  const [isDeleteKbModalOpen, setIsDeleteKbModalOpen] = useState(false);
  const [isDeleteDocModalOpen, setIsDeleteDocModalOpen] = useState(false);
  const [deletingDocument, setDeletingDocument] = useState<Document | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [searchTopK, setSearchTopK] = useState(5);

  const {
    selectedKnowledgeBase,
    documents,
    searchResult,
    isLoading,
    isUploading,
    uploadProgress,
    isSearching,
    error,
    fetchKnowledgeBase,
    uploadDocument,
    deleteDocument,
    deleteKnowledgeBase,
    retryDocumentProcessing,
    refreshDocument,
    searchDocuments,
    clearSelectedKnowledgeBase,
    clearSearchResult,
    clearError,
  } = useKnowledgeStore();

  useEffect(() => {
    if (id) {
      fetchKnowledgeBase(parseInt(id));
    }
    return () => {
      clearSelectedKnowledgeBase();
      clearSearchResult();
    };
  }, [id, fetchKnowledgeBase, clearSelectedKnowledgeBase, clearSearchResult]);

  // Poll for document status updates
  useEffect(() => {
    if (!id) return;
    const processingDocs = documents.filter(
      (d) => d.status === 'PENDING' || d.status === 'PROCESSING'
    );
    if (processingDocs.length === 0) return;

    const interval = setInterval(() => {
      processingDocs.forEach((doc) => {
        refreshDocument(parseInt(id), doc.id);
      });
    }, 3000);

    return () => clearInterval(interval);
  }, [id, documents, refreshDocument]);

  const handleFileUpload = useCallback(async (files: FileList | null) => {
    if (!files || !id) return;
    clearError();

    for (const file of Array.from(files)) {
      try {
        await uploadDocument(parseInt(id), file);
      } catch {
        // Error handled by store
      }
    }

    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  }, [id, uploadDocument, clearError]);

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    handleFileUpload(e.dataTransfer.files);
  }, [handleFileUpload]);

  const handleDeleteDocument = async () => {
    if (deletingDocument && id) {
      try {
        await deleteDocument(parseInt(id), deletingDocument.id);
        setIsDeleteDocModalOpen(false);
        setDeletingDocument(null);
      } catch {
        // Error handled by store
      }
    }
  };

  const handleDeleteKnowledgeBase = async () => {
    if (selectedKnowledgeBase) {
      try {
        await deleteKnowledgeBase(selectedKnowledgeBase.id);
        navigate('/knowledge');
      } catch {
        // Error handled by store
      }
    }
  };

  const handleRetryProcessing = async (docId: number) => {
    if (id) {
      try {
        await retryDocumentProcessing(parseInt(id), docId);
      } catch {
        // Error handled by store
      }
    }
  };

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || !searchQuery.trim()) return;

    const request: SearchRequest = {
      query: searchQuery.trim(),
      topK: searchTopK,
    };
    await searchDocuments(parseInt(id), request);
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  const formatDate = (dateString: string) => {
    const locale = i18n.language === 'ko' ? 'ko-KR' : 'en-US';
    return new Date(dateString).toLocaleDateString(locale, {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const getStrategyLabel = (strategy: string) => {
    const strategyMap: Record<string, string> = {
      'FIXED_SIZE': t('knowledge.chunkingStrategies.fixedSize'),
      'SENTENCE': t('knowledge.chunkingStrategies.sentence'),
      'PARAGRAPH': t('knowledge.chunkingStrategies.paragraph'),
    };
    return strategyMap[strategy] || strategy.replace('_', ' ');
  };

  if (isLoading && !selectedKnowledgeBase) {
    return (
      <div className="flex justify-center items-center py-12">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
      </div>
    );
  }

  if (!selectedKnowledgeBase) {
    return (
      <div className="text-center py-12">
        <p className="text-gray-500 dark:text-gray-400">{t('knowledge.notFound')}</p>
        <Link to="/knowledge" className="text-primary-600 hover:underline mt-2 inline-block">
          {t('knowledge.backToList')}
        </Link>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row justify-between items-start gap-4">
        <div className="flex items-center gap-4">
          <Link
            to="/knowledge"
            className="p-2 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-700"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
            </svg>
          </Link>
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-bold text-gray-900 dark:text-white">
                {selectedKnowledgeBase.name}
              </h1>
              <span
                className={`px-2 py-0.5 text-xs font-medium rounded-full ${
                  selectedKnowledgeBase.isActive
                    ? 'bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-300'
                    : 'bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-400'
                }`}
              >
                {selectedKnowledgeBase.isActive ? t('common.active') : t('common.inactive')}
              </span>
            </div>
            {selectedKnowledgeBase.description && (
              <p className="text-gray-600 dark:text-gray-400 mt-1">
                {selectedKnowledgeBase.description}
              </p>
            )}
          </div>
        </div>
        <div className="flex gap-2">
          <button
            onClick={() => setIsEditModalOpen(true)}
            className="px-4 py-2 text-gray-700 dark:text-gray-200 bg-white dark:bg-gray-700 border border-gray-300 dark:border-gray-600 rounded-lg hover:bg-gray-50 dark:hover:bg-gray-600 transition-colors"
          >
            {t('common.edit')}
          </button>
          <button
            onClick={() => setIsDeleteKbModalOpen(true)}
            className="px-4 py-2 text-red-600 dark:text-red-400 bg-white dark:bg-gray-700 border border-red-300 dark:border-red-600 rounded-lg hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors"
          >
            {t('common.delete')}
          </button>
        </div>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        <div className="bg-white dark:bg-gray-800 rounded-lg p-4 shadow">
          <div className="text-sm text-gray-500 dark:text-gray-400">{t('knowledge.documents')}</div>
          <div className="text-2xl font-semibold text-gray-900 dark:text-white">
            {selectedKnowledgeBase.documentCount}
          </div>
        </div>
        <div className="bg-white dark:bg-gray-800 rounded-lg p-4 shadow">
          <div className="text-sm text-gray-500 dark:text-gray-400">{t('knowledge.chunkSize')}</div>
          <div className="text-2xl font-semibold text-gray-900 dark:text-white">
            {selectedKnowledgeBase.chunkSize}
          </div>
        </div>
        <div className="bg-white dark:bg-gray-800 rounded-lg p-4 shadow">
          <div className="text-sm text-gray-500 dark:text-gray-400">{t('knowledge.overlap')}</div>
          <div className="text-2xl font-semibold text-gray-900 dark:text-white">
            {selectedKnowledgeBase.chunkOverlap}
          </div>
        </div>
        <div className="bg-white dark:bg-gray-800 rounded-lg p-4 shadow">
          <div className="text-sm text-gray-500 dark:text-gray-400">{t('knowledge.strategy')}</div>
          <div className="text-xl font-semibold text-gray-900 dark:text-white">
            {getStrategyLabel(selectedKnowledgeBase.chunkingStrategy)}
          </div>
        </div>
      </div>

      {error && (
        <div className="bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-400 px-4 py-3 rounded-lg">
          {error}
        </div>
      )}

      {/* Document Upload */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow">
        <div className="p-4 border-b border-gray-200 dark:border-gray-700">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white">{t('knowledge.documents')}</h2>
        </div>
        <div className="p-4">
          <div
            onDrop={handleDrop}
            onDragOver={(e) => e.preventDefault()}
            className={`border-2 border-dashed rounded-lg p-8 text-center transition-colors ${
              isUploading
                ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20'
                : 'border-gray-300 dark:border-gray-600 hover:border-primary-500'
            }`}
          >
            {isUploading ? (
              <div>
                <div className="animate-spin rounded-full h-10 w-10 border-b-2 border-primary-600 mx-auto mb-3"></div>
                <p className="text-gray-600 dark:text-gray-400">
                  {t('knowledge.upload.uploading')} {uploadProgress}%
                </p>
                <div className="w-48 mx-auto mt-2 bg-gray-200 dark:bg-gray-700 rounded-full h-2">
                  <div
                    className="bg-primary-600 h-2 rounded-full transition-all"
                    style={{ width: `${uploadProgress}%` }}
                  ></div>
                </div>
              </div>
            ) : (
              <>
                <svg
                  className="w-12 h-12 mx-auto text-gray-400 mb-4"
                  fill="none"
                  stroke="currentColor"
                  viewBox="0 0 24 24"
                >
                  <path
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    strokeWidth={2}
                    d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"
                  />
                </svg>
                <p className="text-gray-600 dark:text-gray-400 mb-2">
                  {t('knowledge.upload.dragDrop')}
                </p>
                <p className="text-sm text-gray-500 dark:text-gray-500">
                  {t('knowledge.upload.supportedFormats')}
                </p>
                <input
                  ref={fileInputRef}
                  type="file"
                  multiple
                  accept=".pdf,.txt,.md,.doc,.docx"
                  onChange={(e) => handleFileUpload(e.target.files)}
                  className="hidden"
                />
                <button
                  onClick={() => fileInputRef.current?.click()}
                  className="mt-4 px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors"
                >
                  {t('knowledge.upload.browseFiles')}
                </button>
              </>
            )}
          </div>

          {/* Document List */}
          {documents.length > 0 && (
            <div className="mt-6">
              <div className="overflow-x-auto">
                <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
                  <thead>
                    <tr>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                        {t('knowledge.documentTable.name')}
                      </th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                        {t('knowledge.documentTable.size')}
                      </th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                        {t('knowledge.documentTable.chunks')}
                      </th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                        {t('knowledge.documentTable.status')}
                      </th>
                      <th className="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                        {t('knowledge.documentTable.uploaded')}
                      </th>
                      <th className="px-4 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                        {t('knowledge.documentTable.actions')}
                      </th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-200 dark:divide-gray-700">
                    {documents.map((doc) => {
                      const status = statusConfig[doc.status as DocumentStatus] || statusConfig.PENDING;
                      return (
                        <tr key={doc.id} className="hover:bg-gray-50 dark:hover:bg-gray-700/50">
                          <td className="px-4 py-3">
                            <div className="flex items-center">
                              <svg
                                className="w-5 h-5 text-gray-400 mr-2"
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
                              <span className="text-sm text-gray-900 dark:text-white">
                                {doc.fileName}
                              </span>
                            </div>
                          </td>
                          <td className="px-4 py-3 text-sm text-gray-500 dark:text-gray-400">
                            {formatFileSize(doc.fileSize)}
                          </td>
                          <td className="px-4 py-3 text-sm text-gray-500 dark:text-gray-400">
                            {doc.chunkCount || '-'}
                          </td>
                          <td className="px-4 py-3">
                            <span
                              className={`inline-flex items-center px-2 py-0.5 rounded-full text-xs font-medium ${status.bgColor} ${status.color}`}
                            >
                              {doc.status === 'PROCESSING' && (
                                <svg
                                  className="animate-spin -ml-0.5 mr-1.5 h-3 w-3"
                                  fill="none"
                                  viewBox="0 0 24 24"
                                >
                                  <circle
                                    className="opacity-25"
                                    cx="12"
                                    cy="12"
                                    r="10"
                                    stroke="currentColor"
                                    strokeWidth="4"
                                  ></circle>
                                  <path
                                    className="opacity-75"
                                    fill="currentColor"
                                    d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
                                  ></path>
                                </svg>
                              )}
                              {t(status.labelKey)}
                            </span>
                            {doc.status === 'FAILED' && doc.errorMessage && (
                              <p className="text-xs text-red-500 mt-1">{doc.errorMessage}</p>
                            )}
                          </td>
                          <td className="px-4 py-3 text-sm text-gray-500 dark:text-gray-400">
                            {formatDate(doc.createdAt)}
                          </td>
                          <td className="px-4 py-3 text-right">
                            <div className="flex items-center justify-end gap-2">
                              {doc.status === 'FAILED' && (
                                <button
                                  onClick={() => handleRetryProcessing(doc.id)}
                                  className="p-1 text-blue-600 hover:text-blue-800 dark:text-blue-400 dark:hover:text-blue-300"
                                  title={t('knowledge.retryProcessing')}
                                >
                                  <svg
                                    className="w-5 h-5"
                                    fill="none"
                                    stroke="currentColor"
                                    viewBox="0 0 24 24"
                                  >
                                    <path
                                      strokeLinecap="round"
                                      strokeLinejoin="round"
                                      strokeWidth={2}
                                      d="M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"
                                    />
                                  </svg>
                                </button>
                              )}
                              <button
                                onClick={() => {
                                  setDeletingDocument(doc);
                                  setIsDeleteDocModalOpen(true);
                                }}
                                className="p-1 text-red-600 hover:text-red-800 dark:text-red-400 dark:hover:text-red-300"
                                title={t('knowledge.deleteDoc')}
                              >
                                <svg
                                  className="w-5 h-5"
                                  fill="none"
                                  stroke="currentColor"
                                  viewBox="0 0 24 24"
                                >
                                  <path
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                    strokeWidth={2}
                                    d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"
                                  />
                                </svg>
                              </button>
                            </div>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {documents.length === 0 && !isUploading && (
            <p className="text-center text-gray-500 dark:text-gray-400 mt-4">
              {t('knowledge.noDocuments')}
            </p>
          )}
        </div>
      </div>

      {/* Search Test Panel */}
      <div className="bg-white dark:bg-gray-800 rounded-lg shadow">
        <div className="p-4 border-b border-gray-200 dark:border-gray-700">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white">{t('knowledge.searchTest.title')}</h2>
          <p className="text-sm text-gray-500 dark:text-gray-400 mt-1">
            {t('knowledge.searchTest.description')}
          </p>
        </div>
        <div className="p-4">
          <form onSubmit={handleSearch} className="flex gap-4 mb-4">
            <div className="flex-1">
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder={t('knowledge.searchTest.placeholder')}
                className="block w-full px-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500"
              />
            </div>
            <div className="w-24">
              <select
                value={searchTopK}
                onChange={(e) => setSearchTopK(parseInt(e.target.value))}
                className="block w-full px-3 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500"
              >
                <option value={3}>Top 3</option>
                <option value={5}>Top 5</option>
                <option value={10}>Top 10</option>
              </select>
            </div>
            <button
              type="submit"
              disabled={isSearching || !searchQuery.trim()}
              className="px-6 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
            >
              {isSearching ? (
                <>
                  <svg className="animate-spin h-4 w-4" fill="none" viewBox="0 0 24 24">
                    <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                    <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  {t('knowledge.searchTest.searching')}
                </>
              ) : (
                <>
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
                  </svg>
                  {t('common.search')}
                </>
              )}
            </button>
          </form>

          {/* Search Results */}
          {searchResult && (
            <div className="space-y-4">
              <div className="flex items-center justify-between text-sm text-gray-500 dark:text-gray-400">
                <span>
                  {t('knowledge.searchTest.foundResults', { count: searchResult.matches.length, time: searchResult.searchTimeMs })}
                </span>
                <button
                  onClick={clearSearchResult}
                  className="text-primary-600 hover:underline"
                >
                  {t('knowledge.searchTest.clearResults')}
                </button>
              </div>
              {searchResult.matches.map((result: SearchMatch, index: number) => (
                <div
                  key={index}
                  className="border border-gray-200 dark:border-gray-700 rounded-lg p-4"
                >
                  <div className="flex items-center justify-between mb-2">
                    <span className="text-sm font-medium text-gray-900 dark:text-white">
                      {result.documentName}
                    </span>
                    <span className="text-xs text-gray-500 dark:text-gray-400">
                      {t('knowledge.searchTest.score')}: {(result.score * 100).toFixed(1)}%
                    </span>
                  </div>
                  <p className="text-sm text-gray-600 dark:text-gray-400 whitespace-pre-wrap">
                    {result.content}
                  </p>
                  {result.metadata && Object.keys(result.metadata).length > 0 && (
                    <div className="mt-2 flex flex-wrap gap-2">
                      {Object.entries(result.metadata).map(([key, value]) => (
                        <span
                          key={key}
                          className="text-xs px-2 py-1 bg-gray-100 dark:bg-gray-700 rounded text-gray-600 dark:text-gray-400"
                        >
                          {key}: {String(value)}
                        </span>
                      ))}
                    </div>
                  )}
                </div>
              ))}
              {searchResult.matches.length === 0 && (
                <p className="text-center text-gray-500 dark:text-gray-400 py-4">
                  {t('knowledge.searchTest.noResults')}
                </p>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Edit Modal */}
      <KnowledgeBaseModal
        isOpen={isEditModalOpen}
        onClose={() => setIsEditModalOpen(false)}
        editingKnowledgeBase={selectedKnowledgeBase}
        onSuccess={() => id && fetchKnowledgeBase(parseInt(id))}
      />

      {/* Delete Knowledge Base Modal */}
      <DeleteConfirmModal
        isOpen={isDeleteKbModalOpen}
        title={t('knowledge.deleteKnowledgeBase')}
        message={t('knowledge.deleteWarning', { name: selectedKnowledgeBase.name })}
        onConfirm={handleDeleteKnowledgeBase}
        onCancel={() => setIsDeleteKbModalOpen(false)}
        isLoading={isLoading}
      />

      {/* Delete Document Modal */}
      <DeleteConfirmModal
        isOpen={isDeleteDocModalOpen}
        title={t('knowledge.deleteDocument.title')}
        message={t('knowledge.deleteDocument.message', { name: deletingDocument?.fileName })}
        onConfirm={handleDeleteDocument}
        onCancel={() => {
          setIsDeleteDocModalOpen(false);
          setDeletingDocument(null);
        }}
        isLoading={isLoading}
      />
    </div>
  );
}
