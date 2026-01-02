import { useEffect, useState } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { useAuthStore } from '../stores/authStore';
import { useTranslation } from 'react-i18next';

export default function OAuth2CallbackPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const { setTokenFromOAuth } = useAuthStore();
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const token = searchParams.get('token');
    const errorParam = searchParams.get('error');

    if (errorParam) {
      setError(errorParam);
      setTimeout(() => {
        navigate('/login');
      }, 3000);
      return;
    }

    if (token) {
      setTokenFromOAuth(token);
      navigate('/dashboard');
    } else {
      setError('No token received');
      setTimeout(() => {
        navigate('/login');
      }, 3000);
    }
  }, [searchParams, navigate, setTokenFromOAuth]);

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50 dark:bg-gray-900">
      <div className="max-w-md w-full space-y-8 p-8">
        {error ? (
          <div className="text-center">
            <div className="text-red-500 text-5xl mb-4">!</div>
            <h2 className="text-xl font-semibold text-gray-900 dark:text-white mb-2">
              {t('auth.loginFailed', 'Login Failed')}
            </h2>
            <p className="text-gray-600 dark:text-gray-400 mb-4">{error}</p>
            <p className="text-sm text-gray-500 dark:text-gray-500">
              {t('auth.redirectingToLogin', 'Redirecting to login page...')}
            </p>
          </div>
        ) : (
          <div className="text-center">
            <svg
              className="animate-spin mx-auto h-12 w-12 text-primary-600"
              xmlns="http://www.w3.org/2000/svg"
              fill="none"
              viewBox="0 0 24 24"
            >
              <circle
                className="opacity-25"
                cx="12"
                cy="12"
                r="10"
                stroke="currentColor"
                strokeWidth="4"
              ></circle>
              <path
                className="opacity-75"
                fill="currentColor"
                d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"
              ></path>
            </svg>
            <h2 className="mt-4 text-xl font-semibold text-gray-900 dark:text-white">
              {t('auth.authenticating', 'Authenticating...')}
            </h2>
            <p className="mt-2 text-gray-600 dark:text-gray-400">
              {t('auth.pleaseWait', 'Please wait while we complete your login.')}
            </p>
          </div>
        )}
      </div>
    </div>
  );
}
