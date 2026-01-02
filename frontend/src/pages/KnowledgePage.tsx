import { useEffect, useState } from 'react';
import { useKnowledgeStore, useFilteredKnowledgeBases } from '../stores/knowledgeStore';
import type { KnowledgeBase } from '../services/knowledgeService';
import { KnowledgeBaseCard, KnowledgeBaseModal, DeleteConfirmModal } from '../components/knowledge';

export default function KnowledgePage() {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isDeleteModalOpen, setIsDeleteModalOpen] = useState(false);
  const [editingKnowledgeBase, setEditingKnowledgeBase] = useState<KnowledgeBase | null>(null);
  const [deletingKnowledgeBase, setDeletingKnowledgeBase] = useState<KnowledgeBase | null>(null);

  const {
    fetchKnowledgeBases,
    deleteKnowledgeBase,
    toggleKnowledgeBaseActive,
    isLoading,
    error,
    searchQuery,
    setSearchQuery,
  } = useKnowledgeStore();

  const knowledgeBases = useFilteredKnowledgeBases();

  useEffect(() => {
    fetchKnowledgeBases();
  }, [fetchKnowledgeBases]);

  const handleCreate = () => {
    setEditingKnowledgeBase(null);
    setIsModalOpen(true);
  };

  const handleDelete = (id: number) => {
    const kb = knowledgeBases.find((k) => k.id === id);
    if (kb) {
      setDeletingKnowledgeBase(kb);
      setIsDeleteModalOpen(true);
    }
  };

  const confirmDelete = async () => {
    if (deletingKnowledgeBase) {
      try {
        await deleteKnowledgeBase(deletingKnowledgeBase.id);
        setIsDeleteModalOpen(false);
        setDeletingKnowledgeBase(null);
      } catch {
        // Error handled by store
      }
    }
  };

  const handleToggleActive = async (id: number) => {
    try {
      await toggleKnowledgeBaseActive(id);
    } catch {
      // Error handled by store
    }
  };

  return (
    <div>
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Knowledge Bases</h1>
        <button
          onClick={handleCreate}
          className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors flex items-center gap-2"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          New Knowledge Base
        </button>
      </div>

      {/* Search Bar */}
      <div className="mb-6">
        <div className="relative">
          <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none">
            <svg className="h-5 w-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Search knowledge bases..."
            className="block w-full pl-10 pr-4 py-2 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-primary-500"
          />
        </div>
      </div>

      {error && (
        <div className="mb-6 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-400 px-4 py-3 rounded-lg">
          {error}
        </div>
      )}

      {isLoading && knowledgeBases.length === 0 ? (
        <div className="flex justify-center items-center py-12">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
        </div>
      ) : knowledgeBases.length === 0 ? (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow">
          <div className="p-12 text-center text-gray-500 dark:text-gray-400">
            <div className="w-16 h-16 mx-auto mb-4 bg-purple-100 dark:bg-purple-900/30 rounded-full flex items-center justify-center">
              <span className="text-3xl">&#128218;</span>
            </div>
            <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-2">
              {searchQuery ? 'No matching knowledge bases' : 'No knowledge bases yet'}
            </h3>
            <p className="mb-6">
              {searchQuery
                ? 'Try adjusting your search terms'
                : 'Create a knowledge base to upload documents and enable RAG for your agents'}
            </p>
            {!searchQuery && (
              <button
                onClick={handleCreate}
                className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors"
              >
                Create Knowledge Base
              </button>
            )}
          </div>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {knowledgeBases.map((kb) => (
            <KnowledgeBaseCard
              key={kb.id}
              knowledgeBase={kb}
              onDelete={handleDelete}
              onToggleActive={handleToggleActive}
            />
          ))}
        </div>
      )}

      {/* Create/Edit Modal */}
      <KnowledgeBaseModal
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        editingKnowledgeBase={editingKnowledgeBase}
        onSuccess={() => fetchKnowledgeBases()}
      />

      {/* Delete Confirmation Modal */}
      <DeleteConfirmModal
        isOpen={isDeleteModalOpen}
        title="Delete Knowledge Base"
        message={`Are you sure you want to delete "${deletingKnowledgeBase?.name}"? This will also delete all documents and embeddings. This action cannot be undone.`}
        onConfirm={confirmDelete}
        onCancel={() => {
          setIsDeleteModalOpen(false);
          setDeletingKnowledgeBase(null);
        }}
        isLoading={isLoading}
      />
    </div>
  );
}
