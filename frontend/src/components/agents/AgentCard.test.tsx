import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent } from '../../test/test-utils';
import AgentCard from './AgentCard';
import type { Agent } from '../../services/agentService';

describe('AgentCard', () => {
  const mockAgent: Agent = {
    id: 1,
    name: 'Test Agent',
    slug: 'test-agent',
    description: 'A test agent for testing purposes',
    systemPrompt: null,
    llmServerId: null,
    llmServerName: null,
    modelName: 'gpt-4',
    temperature: 0.7,
    maxTokens: 1000,
    isActive: true,
    functions: [{ id: 1, name: 'func1' }, { id: 2, name: 'func2' }],
    knowledgeBases: [{ id: 1, name: 'kb1' }],
    createdAt: '2024-01-01T00:00:00Z',
    updatedAt: '2024-01-01T00:00:00Z',
  };

  const mockHandlers = {
    onDelete: vi.fn(),
    onDuplicate: vi.fn(),
    onToggleActive: vi.fn(),
    onVersionHistory: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders agent name', () => {
    render(<AgentCard agent={mockAgent} {...mockHandlers} />);
    expect(screen.getByText('Test Agent')).toBeInTheDocument();
  });

  it('renders agent description', () => {
    render(<AgentCard agent={mockAgent} {...mockHandlers} />);
    expect(screen.getByText('A test agent for testing purposes')).toBeInTheDocument();
  });

  it('renders model name', () => {
    render(<AgentCard agent={mockAgent} {...mockHandlers} />);
    expect(screen.getByText('gpt-4')).toBeInTheDocument();
  });

  it('shows Active badge when agent is active', () => {
    render(<AgentCard agent={mockAgent} {...mockHandlers} />);
    // In test environment, i18n returns the key directly
    expect(screen.getByText('common.active')).toBeInTheDocument();
  });

  it('shows Inactive badge when agent is not active', () => {
    const inactiveAgent = { ...mockAgent, isActive: false };
    render(<AgentCard agent={inactiveAgent} {...mockHandlers} />);
    // In test environment, i18n returns the key directly
    expect(screen.getByText('common.inactive')).toBeInTheDocument();
  });

  it('displays function count badge', () => {
    render(<AgentCard agent={mockAgent} {...mockHandlers} />);
    expect(screen.getByText(/2 functions/)).toBeInTheDocument();
  });

  it('displays knowledge base count badge', () => {
    render(<AgentCard agent={mockAgent} {...mockHandlers} />);
    expect(screen.getByText(/1 KB/)).toBeInTheDocument();
  });

  it('calls onToggleActive when status button is clicked', () => {
    render(<AgentCard agent={mockAgent} {...mockHandlers} />);
    // In test environment, i18n returns the key directly
    fireEvent.click(screen.getByText('common.active'));
    expect(mockHandlers.onToggleActive).toHaveBeenCalledWith(1);
  });

  it('calls onDelete when delete button is clicked', () => {
    render(<AgentCard agent={mockAgent} {...mockHandlers} />);
    // In test environment, i18n returns the key directly
    const deleteButton = screen.getByTitle('agents.deleteAgent');
    fireEvent.click(deleteButton);
    expect(mockHandlers.onDelete).toHaveBeenCalledWith(1);
  });

  it('calls onDuplicate when duplicate button is clicked', () => {
    render(<AgentCard agent={mockAgent} {...mockHandlers} />);
    // In test environment, i18n returns the key directly
    const duplicateButton = screen.getByTitle('agents.duplicateAgent');
    fireEvent.click(duplicateButton);
    expect(mockHandlers.onDuplicate).toHaveBeenCalledWith(1);
  });

  it('has edit link with correct URL', () => {
    render(<AgentCard agent={mockAgent} {...mockHandlers} />);
    // In test environment, i18n returns the key directly
    const editLink = screen.getByText('agents.agentSettings');
    expect(editLink).toHaveAttribute('href', '/agents/1/edit');
  });
});
