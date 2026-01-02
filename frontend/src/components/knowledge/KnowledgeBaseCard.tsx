import { Link } from 'react-router-dom';
import type { KnowledgeBase } from '../../services/knowledgeService';

interface KnowledgeBaseCardProps {
  knowledgeBase: KnowledgeBase;
  onDelete: (id: number) => void;
  onToggleActive: (id: number) => void;
}

export default function KnowledgeBaseCard({
  knowledgeBase,
  onDelete,
  onToggleActive,
}: KnowledgeBaseCardProps) {
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric',
    });
  };

  return (
    <div className="bg-white dark:bg-gray-800 rounded-lg shadow hover:shadow-md transition-shadow">
      <div className="p-6">
        <div className="flex items-start justify-between mb-4">
          <div className="flex items-center">
            <div className="w-12 h-12 bg-purple-100 dark:bg-purple-900/30 rounded-lg flex items-center justify-center">
              <span className="text-2xl">&#128218;</span>
            </div>
            <div className="ml-4">
              <Link
                to={`/knowledge/${knowledgeBase.id}`}
                className="text-lg font-semibold text-gray-900 dark:text-white hover:text-primary-600 dark:hover:text-primary-400"
              >
                {knowledgeBase.name}
              </Link>
              <div className="flex items-center gap-2 mt-1">
                <button
                  onClick={() => onToggleActive(knowledgeBase.id)}
                  className={`px-2 py-0.5 text-xs font-medium rounded-full ${
                    knowledgeBase.isActive
                      ? 'bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-300'
                      : 'bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-400'
                  }`}
                >
                  {knowledgeBase.isActive ? 'Active' : 'Inactive'}
                </button>
              </div>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <Link
              to={`/knowledge/${knowledgeBase.id}`}
              className="p-2 text-gray-400 hover:text-primary-600 dark:hover:text-primary-400"
              title="View details"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 12a3 3 0 11-6 0 3 3 0 016 0z" />
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M2.458 12C3.732 7.943 7.523 5 12 5c4.478 0 8.268 2.943 9.542 7-1.274 4.057-5.064 7-9.542 7-4.477 0-8.268-2.943-9.542-7z" />
              </svg>
            </Link>
            <button
              onClick={() => onDelete(knowledgeBase.id)}
              className="p-2 text-gray-400 hover:text-red-600 dark:hover:text-red-400"
              title="Delete"
            >
              <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
              </svg>
            </button>
          </div>
        </div>

        <p className="text-sm text-gray-600 dark:text-gray-400 mb-4 line-clamp-2">
          {knowledgeBase.description || 'No description'}
        </p>

        <div className="grid grid-cols-2 gap-4 text-sm">
          <div className="flex items-center text-gray-500 dark:text-gray-400">
            <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
            </svg>
            {knowledgeBase.documentCount} documents
          </div>
          <div className="flex items-center text-gray-500 dark:text-gray-400">
            <svg className="w-4 h-4 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 7v10c0 2.21 3.582 4 8 4s8-1.79 8-4V7M4 7c0 2.21 3.582 4 8 4s8-1.79 8-4M4 7c0-2.21 3.582-4 8-4s8 1.79 8 4" />
            </svg>
            {knowledgeBase.chunkSize} chunk size
          </div>
        </div>

        <div className="mt-4 pt-4 border-t border-gray-100 dark:border-gray-700 flex items-center justify-between text-xs text-gray-500 dark:text-gray-400">
          <span>Strategy: {knowledgeBase.chunkingStrategy.replace('_', ' ')}</span>
          <span>Updated {formatDate(knowledgeBase.updatedAt)}</span>
        </div>
      </div>
    </div>
  );
}
