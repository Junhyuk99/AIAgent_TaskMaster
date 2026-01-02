import api from './api';

export type ImplementationType = 'HTTP_API' | 'CODE' | 'TEMPLATE';

export interface FunctionParameter {
  name: string;
  type: 'string' | 'number' | 'boolean' | 'object' | 'array';
  required: boolean;
  description?: string;
}

export interface HttpApiConfig {
  url: string;
  method: 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';
  headers?: Record<string, string>;
  bodyTemplate?: string;
}

export interface CodeConfig {
  language: 'javascript' | 'python';
  code: string;
}

export interface TemplateConfig {
  templateId: string;
  templateName: string;
}

export interface Function {
  id: number;
  name: string;
  description: string | null;
  parametersSchema: string | null;
  returnType: string | null;
  implementationType: ImplementationType;
  implementationConfig: string | null;
  isActive: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface FunctionCreateRequest {
  name: string;
  description?: string;
  parametersSchema?: string;
  returnType?: string;
  implementationType: ImplementationType;
  implementationConfig?: string;
}

export type FunctionUpdateRequest = FunctionCreateRequest;

export interface FunctionTestRequest {
  parameters: Record<string, unknown>;
}

export interface FunctionTestResponse {
  success: boolean;
  result: unknown;
  error: string | null;
  executionTimeMs: number;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

const functionService = {
  getAll: async (): Promise<Function[]> => {
    const response = await api.get<Function[]>('/functions');
    return response.data;
  },

  getPaged: async (page = 0, size = 10): Promise<PageResponse<Function>> => {
    const response = await api.get<PageResponse<Function>>('/functions/paged', {
      params: { page, size },
    });
    return response.data;
  },

  getActive: async (): Promise<Function[]> => {
    const response = await api.get<Function[]>('/functions/active');
    return response.data;
  },

  search: async (query: string): Promise<Function[]> => {
    const response = await api.get<Function[]>('/functions/search', {
      params: { query },
    });
    return response.data;
  },

  getById: async (id: number): Promise<Function> => {
    const response = await api.get<Function>(`/functions/${id}`);
    return response.data;
  },

  create: async (data: FunctionCreateRequest): Promise<Function> => {
    const response = await api.post<Function>('/functions', data);
    return response.data;
  },

  update: async (id: number, data: FunctionUpdateRequest): Promise<Function> => {
    const response = await api.put<Function>(`/functions/${id}`, data);
    return response.data;
  },

  delete: async (id: number): Promise<void> => {
    await api.delete(`/functions/${id}`);
  },

  toggleActive: async (id: number): Promise<Function> => {
    const response = await api.patch<Function>(`/functions/${id}/toggle-active`);
    return response.data;
  },

  test: async (id: number, data: FunctionTestRequest): Promise<FunctionTestResponse> => {
    const response = await api.post<FunctionTestResponse>(`/functions/${id}/test`, data);
    return response.data;
  },
};

export default functionService;
