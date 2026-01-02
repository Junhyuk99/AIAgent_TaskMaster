import { create } from 'zustand';
import functionService from '../services/functionService';
import type {
  Function,
  FunctionCreateRequest,
  FunctionUpdateRequest,
  FunctionTestRequest,
  FunctionTestResponse,
} from '../services/functionService';

interface FunctionState {
  functions: Function[];
  selectedFunction: Function | null;
  isLoading: boolean;
  error: string | null;
  searchQuery: string;
  testResult: FunctionTestResponse | null;
  isTesting: boolean;

  // Actions
  fetchFunctions: () => Promise<void>;
  fetchFunction: (id: number) => Promise<void>;
  createFunction: (data: FunctionCreateRequest) => Promise<Function>;
  updateFunction: (id: number, data: FunctionUpdateRequest) => Promise<Function>;
  deleteFunction: (id: number) => Promise<void>;
  toggleFunctionActive: (id: number) => Promise<void>;
  testFunction: (id: number, data: FunctionTestRequest) => Promise<FunctionTestResponse>;
  setSearchQuery: (query: string) => void;
  clearError: () => void;
  clearSelectedFunction: () => void;
  clearTestResult: () => void;
}

export const useFunctionStore = create<FunctionState>((set) => ({
  functions: [],
  selectedFunction: null,
  isLoading: false,
  error: null,
  searchQuery: '',
  testResult: null,
  isTesting: false,

  fetchFunctions: async () => {
    set({ isLoading: true, error: null });
    try {
      const functions = await functionService.getAll();
      set({ functions, isLoading: false });
    } catch (err: unknown) {
      const errorMessage =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to fetch functions';
      set({ error: errorMessage, isLoading: false });
    }
  },

  fetchFunction: async (id: number) => {
    set({ isLoading: true, error: null });
    try {
      const func = await functionService.getById(id);
      set({ selectedFunction: func, isLoading: false });
    } catch (err: unknown) {
      const errorMessage =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to fetch function';
      set({ error: errorMessage, isLoading: false });
    }
  },

  createFunction: async (data: FunctionCreateRequest) => {
    set({ isLoading: true, error: null });
    try {
      const newFunction = await functionService.create(data);
      set((state) => ({
        functions: [...state.functions, newFunction],
        isLoading: false,
      }));
      return newFunction;
    } catch (err: unknown) {
      const errorMessage =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to create function';
      set({ error: errorMessage, isLoading: false });
      throw err;
    }
  },

  updateFunction: async (id: number, data: FunctionUpdateRequest) => {
    set({ isLoading: true, error: null });
    try {
      const updatedFunction = await functionService.update(id, data);
      set((state) => ({
        functions: state.functions.map((f) => (f.id === id ? updatedFunction : f)),
        selectedFunction: state.selectedFunction?.id === id ? updatedFunction : state.selectedFunction,
        isLoading: false,
      }));
      return updatedFunction;
    } catch (err: unknown) {
      const errorMessage =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to update function';
      set({ error: errorMessage, isLoading: false });
      throw err;
    }
  },

  deleteFunction: async (id: number) => {
    set({ isLoading: true, error: null });
    try {
      await functionService.delete(id);
      set((state) => ({
        functions: state.functions.filter((f) => f.id !== id),
        selectedFunction: state.selectedFunction?.id === id ? null : state.selectedFunction,
        isLoading: false,
      }));
    } catch (err: unknown) {
      const errorMessage =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to delete function';
      set({ error: errorMessage, isLoading: false });
      throw err;
    }
  },

  toggleFunctionActive: async (id: number) => {
    try {
      const updatedFunction = await functionService.toggleActive(id);
      set((state) => ({
        functions: state.functions.map((f) => (f.id === id ? updatedFunction : f)),
        selectedFunction: state.selectedFunction?.id === id ? updatedFunction : state.selectedFunction,
      }));
    } catch (err: unknown) {
      const errorMessage =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to toggle function status';
      set({ error: errorMessage });
      throw err;
    }
  },

  testFunction: async (id: number, data: FunctionTestRequest) => {
    set({ isTesting: true, testResult: null, error: null });
    try {
      const result = await functionService.test(id, data);
      set({ testResult: result, isTesting: false });
      return result;
    } catch (err: unknown) {
      const errorMessage =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to test function';
      set({ error: errorMessage, isTesting: false });
      throw err;
    }
  },

  setSearchQuery: (query) => set({ searchQuery: query }),
  clearError: () => set({ error: null }),
  clearSelectedFunction: () => set({ selectedFunction: null }),
  clearTestResult: () => set({ testResult: null }),
}));

// Selector for filtered functions
export const useFilteredFunctions = () => {
  const { functions, searchQuery } = useFunctionStore();
  if (!searchQuery) return functions;
  const query = searchQuery.toLowerCase();
  return functions.filter(
    (func) =>
      func.name.toLowerCase().includes(query) ||
      func.description?.toLowerCase().includes(query)
  );
};

// Helper to parse parameters from schema
export const parseParametersFromSchema = (schema: string | null): { name: string; type: string; required: boolean; description?: string }[] => {
  if (!schema) return [];
  try {
    const parsed = JSON.parse(schema);
    if (parsed.type !== 'object' || !parsed.properties) return [];

    const required = parsed.required || [];
    return Object.entries(parsed.properties).map(([name, prop]: [string, unknown]) => {
      const typedProp = prop as { type?: string; description?: string };
      return {
        name,
        type: typedProp.type || 'string',
        required: required.includes(name),
        description: typedProp.description,
      };
    });
  } catch {
    return [];
  }
};

// Helper to count parameters
export const countParameters = (schema: string | null): number => {
  return parseParametersFromSchema(schema).length;
};
