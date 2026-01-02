import React, { useState, useEffect, useCallback } from 'react';
import { useTranslation } from 'react-i18next';
import { agentVersionService } from '../../services/agentVersionService';
import type { AgentVersion } from '../../services/agentVersionService';

interface AgentVersionHistoryProps {
  agentId: number;
  agentName: string;
  isOpen: boolean;
  onClose: () => void;
  onRollback?: () => void;
}

const AgentVersionHistory: React.FC<AgentVersionHistoryProps> = ({
  agentId,
  agentName,
  isOpen,
  onClose,
  onRollback,
}) => {
  const { t } = useTranslation();
  const [versions, setVersions] = useState<AgentVersion[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedVersion, setSelectedVersion] = useState<AgentVersion | null>(null);
  const [compareVersion, setCompareVersion] = useState<AgentVersion | null>(null);
  const [isComparing, setIsComparing] = useState(false);
  const [rollbackLoading, setRollbackLoading] = useState(false);

  const loadVersions = useCallback(async () => {
    setLoading(true);
    try {
      const data = await agentVersionService.getVersionHistory(agentId);
      setVersions(data);
      if (data.length > 0) {
        setSelectedVersion(data[0]);
      }
    } catch (error) {
      console.error('Failed to load versions:', error);
    } finally {
      setLoading(false);
    }
  }, [agentId]);

  useEffect(() => {
    if (isOpen) {
      loadVersions();
    }
  }, [isOpen, loadVersions]);

  const handleRollback = async (versionNumber: number) => {
    if (!window.confirm(t('agents.version.confirmRollback', { version: versionNumber }))) {
      return;
    }

    setRollbackLoading(true);
    try {
      await agentVersionService.rollbackToVersion(agentId, versionNumber);
      onRollback?.();
      onClose();
    } catch (error) {
      console.error('Failed to rollback:', error);
      alert(t('agents.version.rollbackFailed'));
    } finally {
      setRollbackLoading(false);
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString();
  };

  if (!isOpen) return null;

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50">
      <div className="bg-white rounded-lg shadow-xl w-full max-w-6xl max-h-[90vh] overflow-hidden">
        {/* Header */}
        <div className="px-6 py-4 border-b border-gray-200 flex justify-between items-center">
          <h2 className="text-xl font-semibold text-gray-900">
            {t('agents.version.history')} - {agentName}
          </h2>
          <div className="flex items-center gap-4">
            <button
              onClick={() => setIsComparing(!isComparing)}
              className={`px-3 py-1.5 text-sm rounded-md ${
                isComparing
                  ? 'bg-blue-100 text-blue-700'
                  : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
              }`}
            >
              {isComparing ? t('agents.version.exitCompare') : t('agents.version.compare')}
            </button>
            <button
              onClick={onClose}
              className="text-gray-400 hover:text-gray-600"
            >
              <svg className="w-6 h-6" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
              </svg>
            </button>
          </div>
        </div>

        <div className="flex h-[calc(90vh-80px)]">
          {/* Version List */}
          <div className="w-80 border-r border-gray-200 overflow-y-auto">
            {loading ? (
              <div className="p-4 text-center text-gray-500">
                {t('common.loading')}...
              </div>
            ) : versions.length === 0 ? (
              <div className="p-4 text-center text-gray-500">
                {t('agents.version.noVersions')}
              </div>
            ) : (
              <ul className="divide-y divide-gray-200">
                {versions.map((version) => (
                  <li
                    key={version.id}
                    onClick={() => {
                      if (isComparing && selectedVersion) {
                        setCompareVersion(version);
                      } else {
                        setSelectedVersion(version);
                        setCompareVersion(null);
                      }
                    }}
                    className={`p-4 cursor-pointer hover:bg-gray-50 ${
                      selectedVersion?.id === version.id ? 'bg-blue-50' : ''
                    } ${compareVersion?.id === version.id ? 'bg-green-50' : ''}`}
                  >
                    <div className="flex justify-between items-start">
                      <div>
                        <span className="font-medium text-gray-900">
                          v{version.versionNumber}
                        </span>
                        {version.isCurrent && (
                          <span className="ml-2 px-2 py-0.5 text-xs bg-green-100 text-green-800 rounded-full">
                            {t('agents.version.current')}
                          </span>
                        )}
                      </div>
                      <span className="text-xs text-gray-500">
                        {formatDate(version.createdAt)}
                      </span>
                    </div>
                    {version.changeSummary && (
                      <p className="mt-1 text-sm text-gray-600 truncate">
                        {version.changeSummary}
                      </p>
                    )}
                    {version.createdByName && (
                      <p className="mt-1 text-xs text-gray-400">
                        {t('agents.version.by')} {version.createdByName}
                      </p>
                    )}
                  </li>
                ))}
              </ul>
            )}
          </div>

          {/* Version Details */}
          <div className="flex-1 overflow-y-auto p-6">
            {isComparing && compareVersion ? (
              <VersionComparison version1={selectedVersion} version2={compareVersion} t={t} />
            ) : selectedVersion ? (
              <VersionDetails
                version={selectedVersion}
                onRollback={handleRollback}
                rollbackLoading={rollbackLoading}
                t={t}
              />
            ) : (
              <div className="text-center text-gray-500 mt-20">
                {t('agents.version.selectVersion')}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

interface VersionDetailsProps {
  version: AgentVersion;
  onRollback: (versionNumber: number) => void;
  rollbackLoading: boolean;
  t: (key: string, options?: Record<string, unknown>) => string;
}

const VersionDetails: React.FC<VersionDetailsProps> = ({
  version,
  onRollback,
  rollbackLoading,
  t,
}) => {
  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center">
        <h3 className="text-lg font-medium text-gray-900">
          {t('agents.version.versionDetails')} v{version.versionNumber}
        </h3>
        {!version.isCurrent && (
          <button
            onClick={() => onRollback(version.versionNumber)}
            disabled={rollbackLoading}
            className="px-4 py-2 bg-yellow-500 text-white rounded-md hover:bg-yellow-600 disabled:opacity-50"
          >
            {rollbackLoading ? t('common.loading') : t('agents.version.rollback')}
          </button>
        )}
      </div>

      <div className="grid grid-cols-2 gap-4">
        <DetailItem label={t('agents.name')} value={version.name} />
        <DetailItem label={t('agents.modelName')} value={version.modelName || '-'} />
        <DetailItem
          label={t('agents.temperature')}
          value={version.temperature?.toString() || '-'}
        />
        <DetailItem
          label={t('agents.maxTokens')}
          value={version.maxTokens?.toString() || '-'}
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          {t('agents.description')}
        </label>
        <p className="text-gray-900 bg-gray-50 p-3 rounded-md whitespace-pre-wrap">
          {version.description || '-'}
        </p>
      </div>

      <div>
        <label className="block text-sm font-medium text-gray-700 mb-1">
          {t('agents.systemPrompt')}
        </label>
        <pre className="text-sm text-gray-900 bg-gray-50 p-3 rounded-md overflow-x-auto whitespace-pre-wrap max-h-60">
          {version.systemPrompt || '-'}
        </pre>
      </div>

      <div className="grid grid-cols-2 gap-4">
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            {t('agents.version.functions')}
          </label>
          <p className="text-gray-900">
            {version.functionIds.length > 0
              ? version.functionIds.join(', ')
              : t('agents.version.none')}
          </p>
        </div>
        <div>
          <label className="block text-sm font-medium text-gray-700 mb-1">
            {t('agents.version.knowledgeBases')}
          </label>
          <p className="text-gray-900">
            {version.knowledgeBaseIds.length > 0
              ? version.knowledgeBaseIds.join(', ')
              : t('agents.version.none')}
          </p>
        </div>
      </div>
    </div>
  );
};

interface DetailItemProps {
  label: string;
  value: string;
}

const DetailItem: React.FC<DetailItemProps> = ({ label, value }) => (
  <div>
    <label className="block text-sm font-medium text-gray-700">{label}</label>
    <p className="mt-1 text-gray-900">{value}</p>
  </div>
);

interface VersionComparisonProps {
  version1: AgentVersion | null;
  version2: AgentVersion | null;
  t: (key: string, options?: Record<string, unknown>) => string;
}

const VersionComparison: React.FC<VersionComparisonProps> = ({
  version1,
  version2,
  t,
}) => {
  if (!version1 || !version2) {
    return (
      <div className="text-center text-gray-500 mt-20">
        {t('agents.version.selectTwoVersions')}
      </div>
    );
  }

  const compareField = (label: string, val1: string | null, val2: string | null) => {
    const isDifferent = val1 !== val2;
    return (
      <div className="grid grid-cols-2 gap-4 py-3 border-b border-gray-100">
        <div className={isDifferent ? 'bg-red-50 p-2 rounded' : ''}>
          <label className="block text-xs font-medium text-gray-500 mb-1">
            {label} (v{version1.versionNumber})
          </label>
          <p className="text-sm text-gray-900">{val1 || '-'}</p>
        </div>
        <div className={isDifferent ? 'bg-green-50 p-2 rounded' : ''}>
          <label className="block text-xs font-medium text-gray-500 mb-1">
            {label} (v{version2.versionNumber})
          </label>
          <p className="text-sm text-gray-900">{val2 || '-'}</p>
        </div>
      </div>
    );
  };

  return (
    <div>
      <h3 className="text-lg font-medium text-gray-900 mb-4">
        {t('agents.version.comparing')} v{version1.versionNumber} ↔ v{version2.versionNumber}
      </h3>

      {compareField(t('agents.name'), version1.name, version2.name)}
      {compareField(t('agents.modelName'), version1.modelName, version2.modelName)}
      {compareField(
        t('agents.temperature'),
        version1.temperature?.toString() || null,
        version2.temperature?.toString() || null
      )}
      {compareField(
        t('agents.maxTokens'),
        version1.maxTokens?.toString() || null,
        version2.maxTokens?.toString() || null
      )}
      {compareField(t('agents.description'), version1.description, version2.description)}
      {compareField(t('agents.systemPrompt'), version1.systemPrompt, version2.systemPrompt)}
    </div>
  );
};

export default AgentVersionHistory;
