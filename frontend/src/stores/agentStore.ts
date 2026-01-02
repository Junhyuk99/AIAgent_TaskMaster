import { create } from 'zustand';
import agentService from '../services/agentService';
import type { Agent, AgentCreateRequest, AgentUpdateRequest } from '../services/agentService';

interface AgentState {
  agents: Agent[];
  selectedAgent: Agent | null;
  isLoading: boolean;
  error: string | null;
  viewMode: 'grid' | 'list';
  searchQuery: string;

  // Actions
  fetchAgents: () => Promise<void>;
  fetchAgent: (id: number) => Promise<void>;
  createAgent: (data: AgentCreateRequest) => Promise<Agent>;
  updateAgent: (id: number, data: AgentUpdateRequest) => Promise<Agent>;
  deleteAgent: (id: number) => Promise<void>;
  duplicateAgent: (id: number) => Promise<Agent>;
  toggleAgentActive: (id: number) => Promise<void>;
  setViewMode: (mode: 'grid' | 'list') => void;
  setSearchQuery: (query: string) => void;
  clearError: () => void;
  clearSelectedAgent: () => void;
}

export const useAgentStore = create<AgentState>((set) => ({
  agents: [],
  selectedAgent: null,
  isLoading: false,
  error: null,
  viewMode: 'grid',
  searchQuery: '',

  fetchAgents: async () => {
    set({ isLoading: true, error: null });
    try {
      const agents = await agentService.getAll();
      set({ agents, isLoading: false });
    } catch (err: unknown) {
      const errorMessage =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to fetch agents';
      set({ error: errorMessage, isLoading: false });
    }
  },

  fetchAgent: async (id: number) => {
    set({ isLoading: true, error: null });
    try {
      const agent = await agentService.getById(id);
      set({ selectedAgent: agent, isLoading: false });
    } catch (err: unknown) {
      const errorMessage =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to fetch agent';
      set({ error: errorMessage, isLoading: false });
    }
  },

  createAgent: async (data: AgentCreateRequest) => {
    set({ isLoading: true, error: null });
    try {
      const newAgent = await agentService.create(data);
      set((state) => ({
        agents: [...state.agents, newAgent],
        isLoading: false,
      }));
      return newAgent;
    } catch (err: unknown) {
      const errorMessage =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to create agent';
      set({ error: errorMessage, isLoading: false });
      throw err;
    }
  },

  updateAgent: async (id: number, data: AgentUpdateRequest) => {
    set({ isLoading: true, error: null });
    try {
      const updatedAgent = await agentService.update(id, data);
      set((state) => ({
        agents: state.agents.map((a) => (a.id === id ? updatedAgent : a)),
        selectedAgent: state.selectedAgent?.id === id ? updatedAgent : state.selectedAgent,
        isLoading: false,
      }));
      return updatedAgent;
    } catch (err: unknown) {
      const errorMessage =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to update agent';
      set({ error: errorMessage, isLoading: false });
      throw err;
    }
  },

  deleteAgent: async (id: number) => {
    set({ isLoading: true, error: null });
    try {
      await agentService.delete(id);
      set((state) => ({
        agents: state.agents.filter((a) => a.id !== id),
        selectedAgent: state.selectedAgent?.id === id ? null : state.selectedAgent,
        isLoading: false,
      }));
    } catch (err: unknown) {
      const errorMessage =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to delete agent';
      set({ error: errorMessage, isLoading: false });
      throw err;
    }
  },

  duplicateAgent: async (id: number) => {
    set({ isLoading: true, error: null });
    try {
      const duplicatedAgent = await agentService.duplicate(id);
      set((state) => ({
        agents: [...state.agents, duplicatedAgent],
        isLoading: false,
      }));
      return duplicatedAgent;
    } catch (err: unknown) {
      const errorMessage =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to duplicate agent';
      set({ error: errorMessage, isLoading: false });
      throw err;
    }
  },

  toggleAgentActive: async (id: number) => {
    try {
      const updatedAgent = await agentService.toggleActive(id);
      set((state) => ({
        agents: state.agents.map((a) => (a.id === id ? updatedAgent : a)),
        selectedAgent: state.selectedAgent?.id === id ? updatedAgent : state.selectedAgent,
      }));
    } catch (err: unknown) {
      const errorMessage =
        (err as { response?: { data?: { message?: string } } })?.response?.data?.message ||
        'Failed to toggle agent status';
      set({ error: errorMessage });
      throw err;
    }
  },

  setViewMode: (mode) => set({ viewMode: mode }),
  setSearchQuery: (query) => set({ searchQuery: query }),
  clearError: () => set({ error: null }),
  clearSelectedAgent: () => set({ selectedAgent: null }),
}));

// Selector for filtered agents
export const useFilteredAgents = () => {
  const { agents, searchQuery } = useAgentStore();
  if (!searchQuery) return agents;
  const query = searchQuery.toLowerCase();
  return agents.filter(
    (agent) =>
      agent.name.toLowerCase().includes(query) ||
      agent.description?.toLowerCase().includes(query)
  );
};
