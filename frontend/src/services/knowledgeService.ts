import api from './api';

export type ChunkingStrategy = 'FIXED_SIZE' | 'SENTENCE' | 'PARAGRAPH';
export type DocumentStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export interface KnowledgeBase {
  id: number;
  name: string;
  description: string | null;
  collectionName: string | null;
  chunkSize: number;
  chunkOverlap: number;
  chunkingStrategy: ChunkingStrategy;
  isActive: boolean;
  documentCount: number;
  documents?: Document[];
  createdAt: string;
  updatedAt: string;
}

export interface Document {
  id: number;
  fileName: string;
  fileSize: number;
  contentType: string;
  status: DocumentStatus;
  chunkCount: number | null;
  progress: number | null;
  processingStage: string | null;
  errorMessage: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface KnowledgeBaseCreateRequest {
  name: string;
  description?: string;
  chunkSize?: number;
  chunkOverlap?: number;
  chunkingStrategy?: ChunkingStrategy;
}

export type KnowledgeBaseUpdateRequest = KnowledgeBaseCreateRequest;

export interface SearchRequest {
  query: string;
  topK?: number;
  metadataFilter?: Record<string, unknown>;
}

export interface SearchMatch {
  chunkId: string;
  content: string;
  documentName: string | null;
  documentId: number | null;
  score: number;
  metadata: Record<string, unknown>;
}

export interface SearchResult {
  query: string;
  matches: SearchMatch[];
  searchTimeMs: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const knowledgeService = {
  // Knowledge Base endpoints
  getAll: async (): Promise<KnowledgeBase[]> => {
    const response = await api.get<KnowledgeBase[]>('/knowledge-bases');
    return response.data;
  },

  getPaged: async (page = 0, size = 10): Promise<PageResponse<KnowledgeBase>> => {
    const response = await api.get<PageResponse<KnowledgeBase>>('/knowledge-bases/paged', {
      params: { page, size },
    });
    return response.data;
  },

  getActive: async (): Promise<KnowledgeBase[]> => {
    const response = await api.get<KnowledgeBase[]>('/knowledge-bases/active');
    return response.data;
  },

  search: async (query: string): Promise<KnowledgeBase[]> => {
    const response = await api.get<KnowledgeBase[]>('/knowledge-bases/search', {
      params: { query },
    });
    return response.data;
  },

  getById: async (id: number): Promise<KnowledgeBase> => {
    const response = await api.get<KnowledgeBase>(`/knowledge-bases/${id}`);
    return response.data;
  },

  create: async (data: KnowledgeBaseCreateRequest): Promise<KnowledgeBase> => {
    const response = await api.post<KnowledgeBase>('/knowledge-bases', data);
    return response.data;
  },

  update: async (id: number, data: KnowledgeBaseUpdateRequest): Promise<KnowledgeBase> => {
    const response = await api.put<KnowledgeBase>(`/knowledge-bases/${id}`, data);
    return response.data;
  },

  delete: async (id: number): Promise<void> => {
    await api.delete(`/knowledge-bases/${id}`);
  },

  toggleActive: async (id: number): Promise<KnowledgeBase> => {
    const response = await api.patch<KnowledgeBase>(`/knowledge-bases/${id}/toggle-active`);
    return response.data;
  },

  reindex: async (id: number): Promise<void> => {
    await api.post(`/knowledge-bases/${id}/reindex`);
  },

  searchDocuments: async (id: number, data: SearchRequest): Promise<SearchResult> => {
    const response = await api.post<SearchResult>(`/knowledge-bases/${id}/search`, data);
    return response.data;
  },

  // Document endpoints
  getDocuments: async (kbId: number): Promise<Document[]> => {
    const response = await api.get<Document[]>(`/knowledge-bases/${kbId}/documents`);
    return response.data;
  },

  getDocument: async (kbId: number, docId: number): Promise<Document> => {
    const response = await api.get<Document>(`/knowledge-bases/${kbId}/documents/${docId}`);
    return response.data;
  },

  uploadDocument: async (
    kbId: number,
    file: File,
    onProgress?: (progress: number) => void
  ): Promise<Document> => {
    const formData = new FormData();
    formData.append('file', file);

    const response = await api.post<Document>(`/knowledge-bases/${kbId}/documents`, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress: (progressEvent) => {
        if (progressEvent.total && onProgress) {
          const progress = Math.round((progressEvent.loaded * 100) / progressEvent.total);
          onProgress(progress);
        }
      },
    });
    return response.data;
  },

  deleteDocument: async (kbId: number, docId: number): Promise<void> => {
    await api.delete(`/knowledge-bases/${kbId}/documents/${docId}`);
  },

  retryDocumentProcessing: async (kbId: number, docId: number): Promise<Document> => {
    const response = await api.post<Document>(`/knowledge-bases/${kbId}/documents/${docId}/retry`);
    return response.data;
  },

  getDocumentProgress: async (
    kbId: number,
    docId: number
  ): Promise<{
    documentId: number;
    fileName: string;
    status: DocumentStatus;
    progress: number;
    processingStage: string;
    chunkCount: number;
    errorMessage: string;
  }> => {
    const response = await api.get(`/knowledge-bases/${kbId}/documents/${docId}/progress`);
    return response.data;
  },
};

export default knowledgeService;
