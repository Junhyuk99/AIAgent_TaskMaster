import { useMemo, useState } from 'react';
import type { Parameter } from './ParameterBuilder';

interface JsonSchemaPreviewProps {
  functionName: string;
  description: string;
  parameters: Parameter[];
  returnType: string;
}

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

export default function JsonSchemaPreview({
  functionName,
  description,
  parameters,
  returnType,
}: JsonSchemaPreviewProps) {
  const [copied, setCopied] = useState(false);

  const schema = useMemo(() => {
    return generateJsonSchema(functionName, description, parameters, returnType);
  }, [functionName, description, parameters, returnType]);

  const schemaString = useMemo(() => {
    return JSON.stringify(schema, null, 2);
  }, [schema]);

  const handleCopy = async () => {
    try {
      await navigator.clipboard.writeText(schemaString);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch (err) {
      console.error('Failed to copy:', err);
    }
  };

  return (
    <div className="h-full flex flex-col">
      <div className="flex items-center justify-between mb-2">
        <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300">
          JSON Schema Preview
        </h4>
        <button
          type="button"
          onClick={handleCopy}
          className="inline-flex items-center px-2 py-1 text-xs font-medium text-gray-600 hover:text-gray-900 dark:text-gray-400 dark:hover:text-gray-200 border border-gray-300 dark:border-gray-600 rounded hover:bg-gray-50 dark:hover:bg-gray-700"
        >
          {copied ? (
            <>
              <svg className="w-3.5 h-3.5 mr-1 text-green-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
              </svg>
              Copied!
            </>
          ) : (
            <>
              <svg className="w-3.5 h-3.5 mr-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M8 16H6a2 2 0 01-2-2V6a2 2 0 012-2h8a2 2 0 012 2v2m-6 12h8a2 2 0 002-2v-8a2 2 0 00-2-2h-8a2 2 0 00-2 2v8a2 2 0 002 2z" />
              </svg>
              Copy
            </>
          )}
        </button>
      </div>
      <div className="flex-1 bg-gray-900 rounded-lg overflow-hidden">
        <pre className="h-full overflow-auto p-4 text-xs">
          <code className="text-gray-100">
            {schemaString.split('\n').map((line, index) => {
              // Simple syntax highlighting
              let highlightedLine = line;

              // Highlight keys (before :)
              highlightedLine = highlightedLine.replace(
                /("[\w]+"):/g,
                '<span class="text-purple-400">$1</span>:'
              );

              // Highlight string values
              highlightedLine = highlightedLine.replace(
                /: (".*?")/g,
                ': <span class="text-green-400">$1</span>'
              );

              // Highlight brackets and braces
              highlightedLine = highlightedLine.replace(
                /([{}\[\]])/g,
                '<span class="text-yellow-400">$1</span>'
              );

              return (
                <div key={index} className="flex">
                  <span className="select-none text-gray-600 w-8 text-right mr-4">
                    {index + 1}
                  </span>
                  <span dangerouslySetInnerHTML={{ __html: highlightedLine }} />
                </div>
              );
            })}
          </code>
        </pre>
      </div>
    </div>
  );
}
