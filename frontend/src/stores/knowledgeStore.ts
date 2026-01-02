import { create } from 'zustand';
import knowledgeService from '../services/knowledgeService';
import type {
  KnowledgeBase,
  KnowledgeBaseCreateRequest,
  KnowledgeBaseUpdateRequest,
  Document,
  SearchResult,
  SearchRequest,
} from '../services/knowledgeService';

interface KnowledgeState {
  knowledgeBases: KnowledgeBase[];
  selectedKnowledgeBase: KnowledgeBase | null;
  documents: Document[];
  searchResult: SearchResult | null;
  isLoading: boolean;
  isUploading: boolean;
  uploadProgress: number;
  isSearching: boolean;
  error: string | null;
  searchQuery: string;

  // Actions
  fetchKnowledgeBases: () => Promise<void>;
  fetchKnowledgeBase: (id: number) => Promise<void>;
  createKnowledgeBase: (data: KnowledgeBaseCreateRequest) => Promise<KnowledgeBase>;
  updateKnowledgeBase: (id: number, data: KnowledgeBaseUpdateRequest) => Promise<KnowledgeBase>;
  deleteKnowledgeBase: (id: number) => Promise<void>;
  toggleKnowledgeBaseActive: (id: number) => Promise<void>;
  reindexKnowledgeBase: (id: number) => Promise<void>;

  // Document actions
  fetchDocuments: (kbId: number) => Promise<void>;
  uploadDocument: (kbId: number, file: File) => Promise<Document>;
  deleteDocument: (kbId: number, docId: number) => Promise<void>;
  retryDocumentProcessing: (kbId: number, docId: number) => Promise<void>;
  refreshDocument: (kbId: number, docId: number) => Promise<void>;

  // Search
  searchDocuments: (kbId: number, request: SearchRequest) => Promise<void>;

  setSearchQuery: (query: string) => void;
  clearError: () => void;
  clearSelectedKnowledgeBase: () => void;
  clearSearchResult: () => void;
}

export const useKnowledgeStore = create<KnowledgeState>((set) => ({
  knowledgeBases: [],
  selectedKnowledgeBase: null,
  documents: [],
  searchResult: null,
  isLoading: false,
  isUploading: false,
  uploadProgress: 0,
  isSearching: false,
  error: null,
  searchQuery: '',

  fetchKnowledgeBases: async () => {
    set({ isLoading: true, error: null });
    try {
      const knowledgeBases = await knowledgeService.getAll();
      set({ knowledgeBases, isLoading: false });
    } catch (err: unknown) {
      const errorMessage =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to fetch knowledge bases';
      set({ error: errorMessage, isLoading: false });
    }
  },

  fetchKnowledgeBase: async (id: number) => {
    set({ isLoading: true, error: null });
    try {
      const kb = await knowledgeService.getById(id);
      set({ selectedKnowledgeBase: kb, documents: kb.documents || [], isLoading: false });
    } catch (err: unknown) {
      const errorMessage =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to fetch knowledge base';
      set({ error: errorMessage, isLoading: false });
    }
  },

  createKnowledgeBase: async (data: KnowledgeBaseCreateRequest) => {
    set({ isLoading: true, error: null });
    try {
      const newKb = await knowledgeService.create(data);
      set((state) => ({
        knowledgeBases: [...state.knowledgeBases, newKb],
        isLoading: false,
      }));
      return newKb;
    } catch (err: unknown) {
      const errorMessage =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to create knowledge base';
      set({ error: errorMessage, isLoading: false });
      throw err;
    }
  },

  updateKnowledgeBase: async (id: number, data: KnowledgeBaseUpdateRequest) => {
    set({ isLoading: true, error: null });
    try {
      const updatedKb = await knowledgeService.update(id, data);
      set((state) => ({
        knowledgeBases: state.knowledgeBases.map((kb) => (kb.id === id ? updatedKb : kb)),
        selectedKnowledgeBase: state.selectedKnowledgeBase?.id === id ? updatedKb : state.selectedKnowledgeBase,
        isLoading: false,
      }));
      return updatedKb;
    } catch (err: unknown) {
      const errorMessage =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to update knowledge base';
      set({ error: errorMessage, isLoading: false });
      throw err;
    }
  },

  deleteKnowledgeBase: async (id: number) => {
    set({ isLoading: true, error: null });
    try {
      await knowledgeService.delete(id);
      set((state) => ({
        knowledgeBases: state.knowledgeBases.filter((kb) => kb.id !== id),
        selectedKnowledgeBase: state.selectedKnowledgeBase?.id === id ? null : state.selectedKnowledgeBase,
        isLoading: false,
      }));
    } catch (err: unknown) {
      const errorMessage =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to delete knowledge base';
      set({ error: errorMessage, isLoading: false });
      throw err;
    }
  },

  toggleKnowledgeBaseActive: async (id: number) => {
    try {
      const updatedKb = await knowledgeService.toggleActive(id);
      set((state) => ({
        knowledgeBases: state.knowledgeBases.map((kb) => (kb.id === id ? updatedKb : kb)),
        selectedKnowledgeBase: state.selectedKnowledgeBase?.id === id ? updatedKb : state.selectedKnowledgeBase,
      }));
    } catch (err: unknown) {
      const errorMessage =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to toggle knowledge base status';
      set({ error: errorMessage });
      throw err;
    }
  },

  reindexKnowledgeBase: async (id: number) => {
    try {
      await knowledgeService.reindex(id);
    } catch (err: unknown) {
      const errorMessage =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to start reindexing';
      set({ error: errorMessage });
      throw err;
    }
  },

  fetchDocuments: async (kbId: number) => {
    set({ isLoading: true, error: null });
    try {
      const documents = await knowledgeService.getDocuments(kbId);
      set({ documents, isLoading: false });
    } catch (err: unknown) {
      const errorMessage =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to fetch documents';
      set({ error: errorMessage, isLoading: false });
    }
  },

  uploadDocument: async (kbId: number, file: File) => {
    set({ isUploading: true, uploadProgress: 0, error: null });
    try {
      const doc = await knowledgeService.uploadDocument(kbId, file, (progress) => {
        set({ uploadProgress: progress });
      });
      set((state) => ({
        documents: [...state.documents, doc],
        isUploading: false,
        uploadProgress: 0,
      }));
      return doc;
    } catch (err: unknown) {
      const errorMessage =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to upload document';
      set({ error: errorMessage, isUploading: false, uploadProgress: 0 });
      throw err;
    }
  },

  deleteDocument: async (kbId: number, docId: number) => {
    set({ isLoading: true, error: null });
    try {
      await knowledgeService.deleteDocument(kbId, docId);
      set((state) => ({
        documents: state.documents.filter((doc) => doc.id !== docId),
        isLoading: false,
      }));
    } catch (err: unknown) {
      const errorMessage =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to delete document';
      set({ error: errorMessage, isLoading: false });
      throw err;
    }
  },

  retryDocumentProcessing: async (kbId: number, docId: number) => {
    try {
      const doc = await knowledgeService.retryDocumentProcessing(kbId, docId);
      set((state) => ({
        documents: state.documents.map((d) => (d.id === docId ? doc : d)),
      }));
    } catch (err: unknown) {
      const errorMessage =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to retry processing';
      set({ error: errorMessage });
      throw err;
    }
  },

  refreshDocument: async (kbId: number, docId: number) => {
    try {
      const doc = await knowledgeService.getDocument(kbId, docId);
      set((state) => ({
        documents: state.documents.map((d) => (d.id === docId ? doc : d)),
      }));
    } catch (err: unknown) {
      // Silently fail for refresh
      console.error('Failed to refresh document', err);
    }
  },

  searchDocuments: async (kbId: number, request: SearchRequest) => {
    set({ isSearching: true, error: null });
    try {
      const result = await knowledgeService.searchDocuments(kbId, request);
      set({ searchResult: result, isSearching: false });
    } catch (err: unknown) {
      const errorMessage =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to search documents';
      set({ error: errorMessage, isSearching: false });
    }
  },

  setSearchQuery: (query) => set({ searchQuery: query }),
  clearError: () => set({ error: null }),
  clearSelectedKnowledgeBase: () => set({ selectedKnowledgeBase: null, documents: [] }),
  clearSearchResult: () => set({ searchResult: null }),
}));

// Selector for filtered knowledge bases
export const useFilteredKnowledgeBases = () => {
  const { knowledgeBases, searchQuery } = useKnowledgeStore();
  if (!searchQuery) return knowledgeBases;
  const query = searchQuery.toLowerCase();
  return knowledgeBases.filter(
    (kb) =>
      kb.name.toLowerCase().includes(query) ||
      kb.description?.toLowerCase().includes(query)
  );
};
