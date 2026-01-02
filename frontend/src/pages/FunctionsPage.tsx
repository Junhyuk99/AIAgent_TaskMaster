import { useState, useEffect } from 'react';
import { useFunctionStore, useFilteredFunctions, countParameters } from '../stores/functionStore';
import type { Function } from '../services/functionService';
import { FunctionModal, DeleteConfirmModal } from '../components/functions';
import { LoadingSpinner } from '../components/ui';

const implementationTypeLabels: Record<string, string> = {
  HTTP_API: 'HTTP API',
  CODE: 'Code',
  TEMPLATE: 'Template',
};

export default function FunctionsPage() {
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingFunction, setEditingFunction] = useState<Function | null>(null);
  const [deleteFunctionId, setDeleteFunctionId] = useState<number | null>(null);

  const {
    fetchFunctions,
    deleteFunction,
    toggleFunctionActive,
    isLoading,
    error,
    searchQuery,
    setSearchQuery,
  } = useFunctionStore();

  const filteredFunctions = useFilteredFunctions();

  useEffect(() => {
    fetchFunctions();
  }, [fetchFunctions]);

  const handleEdit = (func: Function) => {
    setEditingFunction(func);
    setIsModalOpen(true);
  };

  const handleCreate = () => {
    setEditingFunction(null);
    setIsModalOpen(true);
  };

  const handleCloseModal = () => {
    setIsModalOpen(false);
    setEditingFunction(null);
  };

  const handleDelete = async () => {
    if (deleteFunctionId) {
      try {
        await deleteFunction(deleteFunctionId);
        setDeleteFunctionId(null);
      } catch {
        // Error handled by store
      }
    }
  };

  const handleToggleActive = async (id: number) => {
    try {
      await toggleFunctionActive(id);
    } catch {
      // Error handled by store
    }
  };

  if (isLoading && filteredFunctions.length === 0) {
    return (
      <div className="flex justify-center items-center h-64">
        <LoadingSpinner size="lg" />
      </div>
    );
  }

  return (
    <div>
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Functions</h1>
        <div className="flex items-center gap-3 w-full sm:w-auto">
          <div className="relative flex-1 sm:flex-initial">
            <input
              type="text"
              placeholder="Search functions..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full sm:w-64 px-4 py-2 pl-10 border border-gray-300 dark:border-gray-600 rounded-lg bg-white dark:bg-gray-700 text-gray-900 dark:text-white focus:outline-none focus:ring-2 focus:ring-primary-500"
            />
            <svg
              className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400"
              fill="none"
              stroke="currentColor"
              viewBox="0 0 24 24"
            >
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z" />
            </svg>
          </div>
          <button
            onClick={handleCreate}
            className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors whitespace-nowrap"
          >
            + New Function
          </button>
        </div>
      </div>

      {error && (
        <div className="mb-4 bg-red-50 dark:bg-red-900/20 border border-red-200 dark:border-red-800 text-red-700 dark:text-red-400 px-4 py-3 rounded">
          {error}
        </div>
      )}

      {filteredFunctions.length === 0 ? (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow">
          <div className="p-6 text-center text-gray-500 dark:text-gray-400">
            <div className="text-5xl mb-4">&#9889;</div>
            <h3 className="text-lg font-medium text-gray-900 dark:text-white mb-2">
              {searchQuery ? 'No functions found' : 'No functions yet'}
            </h3>
            <p className="mb-4">
              {searchQuery ? 'Try a different search term' : 'Create your first function to extend your agents'}
            </p>
            {!searchQuery && (
              <button
                onClick={handleCreate}
                className="px-4 py-2 bg-primary-600 text-white rounded-lg hover:bg-primary-700 transition-colors"
              >
                Create Function
              </button>
            )}
          </div>
        </div>
      ) : (
        <div className="bg-white dark:bg-gray-800 rounded-lg shadow overflow-hidden">
          <div className="overflow-x-auto">
            <table className="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
              <thead className="bg-gray-50 dark:bg-gray-700">
                <tr>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                    Name
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                    Description
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                    Parameters
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                    Type
                  </th>
                  <th className="px-6 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                    Status
                  </th>
                  <th className="px-6 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-300 uppercase tracking-wider">
                    Actions
                  </th>
                </tr>
              </thead>
              <tbody className="bg-white dark:bg-gray-800 divide-y divide-gray-200 dark:divide-gray-700">
                {filteredFunctions.map((func) => (
                  <tr key={func.id} className="hover:bg-gray-50 dark:hover:bg-gray-700/50">
                    <td className="px-6 py-4 whitespace-nowrap">
                      <div className="flex items-center">
                        <div className="flex-shrink-0 h-10 w-10 bg-purple-100 dark:bg-purple-900 rounded-lg flex items-center justify-center">
                          <span className="text-lg">&#9889;</span>
                        </div>
                        <div className="ml-4">
                          <div className="text-sm font-medium text-gray-900 dark:text-white">
                            {func.name}
                          </div>
                          <div className="text-xs text-gray-500 dark:text-gray-400">
                            {func.returnType || 'void'}
                          </div>
                        </div>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="text-sm text-gray-900 dark:text-gray-300 max-w-xs truncate">
                        {func.description || '-'}
                      </div>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className="px-2 py-1 text-xs font-medium bg-blue-100 dark:bg-blue-900/30 text-blue-800 dark:text-blue-300 rounded-full">
                        {countParameters(func.parametersSchema)} params
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <span className="px-2 py-1 text-xs font-medium bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-300 rounded">
                        {implementationTypeLabels[func.implementationType] || func.implementationType}
                      </span>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap">
                      <button
                        onClick={() => handleToggleActive(func.id)}
                        className={`px-2 py-1 text-xs font-medium rounded-full ${
                          func.isActive
                            ? 'bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-300'
                            : 'bg-gray-100 dark:bg-gray-700 text-gray-800 dark:text-gray-400'
                        }`}
                      >
                        {func.isActive ? 'Active' : 'Inactive'}
                      </button>
                    </td>
                    <td className="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                      <button
                        onClick={() => handleEdit(func)}
                        className="text-primary-600 hover:text-primary-900 dark:text-primary-400 dark:hover:text-primary-300 mr-4"
                      >
                        Edit
                      </button>
                      <button
                        onClick={() => setDeleteFunctionId(func.id)}
                        className="text-red-600 hover:text-red-900 dark:text-red-400 dark:hover:text-red-300"
                      >
                        Delete
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}

      <FunctionModal
        isOpen={isModalOpen}
        onClose={handleCloseModal}
        editingFunction={editingFunction}
      />

      <DeleteConfirmModal
        isOpen={deleteFunctionId !== null}
        title="Delete Function"
        message="Are you sure you want to delete this function? This action cannot be undone. Agents using this function will no longer have access to it."
        onConfirm={handleDelete}
        onCancel={() => setDeleteFunctionId(null)}
        isLoading={isLoading}
      />
    </div>
  );
}
