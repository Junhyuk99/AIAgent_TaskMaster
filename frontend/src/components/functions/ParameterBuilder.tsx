import { useState } from 'react';

export interface Parameter {
  id: string;
  name: string;
  type: 'string' | 'number' | 'boolean' | 'object' | 'array';
  required: boolean;
  description: string;
}

interface ParameterBuilderProps {
  parameters: Parameter[];
  onChange: (parameters: Parameter[]) => void;
}

const parameterTypes = [
  { value: 'string', label: 'String' },
  { value: 'number', label: 'Number' },
  { value: 'boolean', label: 'Boolean' },
  { value: 'object', label: 'Object' },
  { value: 'array', label: 'Array' },
];

export default function ParameterBuilder({ parameters, onChange }: ParameterBuilderProps) {
  const [errors, setErrors] = useState<Record<string, string>>({});

  const generateId = () => `param_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;

  const validateParameterName = (name: string, index: number): string | null => {
    if (!name.trim()) {
      return 'Parameter name is required';
    }
    if (!/^[a-zA-Z][a-zA-Z0-9_]*$/.test(name)) {
      return 'Name must start with a letter and contain only letters, numbers, and underscores';
    }
    const duplicateIndex = parameters.findIndex((p, i) => i !== index && p.name === name);
    if (duplicateIndex !== -1) {
      return 'Duplicate parameter name';
    }
    return null;
  };

  const handleAddParameter = () => {
    const newParameter: Parameter = {
      id: generateId(),
      name: '',
      type: 'string',
      required: false,
      description: '',
    };
    onChange([...parameters, newParameter]);
  };

  const handleRemoveParameter = (index: number) => {
    const newParameters = parameters.filter((_, i) => i !== index);
    onChange(newParameters);

    // Clean up errors for removed parameter
    const newErrors = { ...errors };
    delete newErrors[parameters[index].id];
    setErrors(newErrors);
  };

  const handleParameterChange = (index: number, field: keyof Parameter, value: string | boolean) => {
    const newParameters = [...parameters];
    newParameters[index] = { ...newParameters[index], [field]: value };
    onChange(newParameters);

    // Validate name field
    if (field === 'name') {
      const error = validateParameterName(value as string, index);
      setErrors((prev) => ({
        ...prev,
        [newParameters[index].id]: error || '',
      }));
    }
  };

  const moveParameter = (index: number, direction: 'up' | 'down') => {
    if (
      (direction === 'up' && index === 0) ||
      (direction === 'down' && index === parameters.length - 1)
    ) {
      return;
    }

    const newParameters = [...parameters];
    const targetIndex = direction === 'up' ? index - 1 : index + 1;
    [newParameters[index], newParameters[targetIndex]] = [newParameters[targetIndex], newParameters[index]];
    onChange(newParameters);
  };

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between">
        <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300">Parameters</h4>
        <button
          type="button"
          onClick={handleAddParameter}
          className="inline-flex items-center px-3 py-1.5 text-sm font-medium text-primary-600 hover:text-primary-700 dark:text-primary-400 dark:hover:text-primary-300"
        >
          <svg className="w-4 h-4 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          Add Parameter
        </button>
      </div>

      {parameters.length === 0 ? (
        <div className="text-center py-6 bg-gray-50 dark:bg-gray-700/50 rounded-lg">
          <p className="text-sm text-gray-500 dark:text-gray-400">
            No parameters defined. Click "Add Parameter" to add one.
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {parameters.map((param, index) => (
            <div
              key={param.id}
              className="bg-gray-50 dark:bg-gray-700/50 rounded-lg p-4 border border-gray-200 dark:border-gray-600"
            >
              <div className="flex items-start gap-4">
                {/* Reorder buttons */}
                <div className="flex flex-col gap-1 pt-1">
                  <button
                    type="button"
                    onClick={() => moveParameter(index, 'up')}
                    disabled={index === 0}
                    className="p-1 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 disabled:opacity-30 disabled:cursor-not-allowed"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 15l7-7 7 7" />
                    </svg>
                  </button>
                  <button
                    type="button"
                    onClick={() => moveParameter(index, 'down')}
                    disabled={index === parameters.length - 1}
                    className="p-1 text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 disabled:opacity-30 disabled:cursor-not-allowed"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                    </svg>
                  </button>
                </div>

                {/* Parameter fields */}
                <div className="flex-1 grid grid-cols-1 md:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">
                      Name <span className="text-red-500">*</span>
                    </label>
                    <input
                      type="text"
                      value={param.name}
                      onChange={(e) => handleParameterChange(index, 'name', e.target.value)}
                      placeholder="parameterName"
                      className={`block w-full rounded-md border ${
                        errors[param.id] ? 'border-red-300 dark:border-red-600' : 'border-gray-300 dark:border-gray-600'
                      } px-3 py-1.5 text-sm text-gray-900 dark:text-white dark:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-primary-500`}
                    />
                    {errors[param.id] && (
                      <p className="mt-1 text-xs text-red-600 dark:text-red-400">{errors[param.id]}</p>
                    )}
                  </div>

                  <div>
                    <label className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">
                      Type
                    </label>
                    <select
                      value={param.type}
                      onChange={(e) => handleParameterChange(index, 'type', e.target.value)}
                      className="block w-full rounded-md border border-gray-300 dark:border-gray-600 px-3 py-1.5 text-sm text-gray-900 dark:text-white dark:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-primary-500"
                    >
                      {parameterTypes.map((type) => (
                        <option key={type.value} value={type.value}>
                          {type.label}
                        </option>
                      ))}
                    </select>
                  </div>

                  <div className="md:col-span-2">
                    <label className="block text-xs font-medium text-gray-600 dark:text-gray-400 mb-1">
                      Description
                    </label>
                    <input
                      type="text"
                      value={param.description}
                      onChange={(e) => handleParameterChange(index, 'description', e.target.value)}
                      placeholder="Describe what this parameter is for..."
                      className="block w-full rounded-md border border-gray-300 dark:border-gray-600 px-3 py-1.5 text-sm text-gray-900 dark:text-white dark:bg-gray-700 focus:outline-none focus:ring-2 focus:ring-primary-500"
                    />
                  </div>

                  <div className="flex items-center">
                    <input
                      type="checkbox"
                      id={`required-${param.id}`}
                      checked={param.required}
                      onChange={(e) => handleParameterChange(index, 'required', e.target.checked)}
                      className="h-4 w-4 rounded border-gray-300 text-primary-600 focus:ring-primary-500"
                    />
                    <label
                      htmlFor={`required-${param.id}`}
                      className="ml-2 text-sm text-gray-700 dark:text-gray-300"
                    >
                      Required
                    </label>
                  </div>
                </div>

                {/* Delete button */}
                <button
                  type="button"
                  onClick={() => handleRemoveParameter(index)}
                  className="p-1.5 text-gray-400 hover:text-red-600 dark:hover:text-red-400"
                  title="Remove parameter"
                >
                  <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                  </svg>
                </button>
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
