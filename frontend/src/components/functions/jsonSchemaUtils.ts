import type { Parameter } from './ParameterBuilder';

export function generateJsonSchema(
  functionName: string,
  description: string,
  parameters: Parameter[],
  returnType: string
): object {
  const properties: Record<string, object> = {};
  const required: string[] = [];

  parameters.forEach((param) => {
    if (param.name) {
      properties[param.name] = {
        type: param.type,
        ...(param.description && { description: param.description }),
      };
      if (param.required) {
        required.push(param.name);
      }
    }
  });

  return {
    name: functionName || 'unnamed_function',
    description: description || '',
    parameters: {
      type: 'object',
      properties,
      ...(required.length > 0 && { required }),
    },
    ...(returnType && { returns: { type: returnType } }),
  };
}
