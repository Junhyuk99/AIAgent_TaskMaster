import { useTranslation } from 'react-i18next';
import { useSidebarStore } from '../../stores/sidebarStore';
import { useAuthStore } from '../../stores/authStore';
import { useNavigate } from 'react-router-dom';
import LanguageSwitcher from '../ui/LanguageSwitcher';

export default function Header() {
  const { t } = useTranslation();
  const { toggle: toggleSidebar } = useSidebarStore();
  const { user, logout, isAuthenticated } = useAuthStore();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <header className="bg-white dark:bg-gray-800 shadow-sm border-b border-gray-200 dark:border-gray-700 fixed top-0 left-0 right-0 z-30">
      <div className="flex items-center justify-between h-16 px-4">
        <div className="flex items-center">
          <button
            onClick={toggleSidebar}
            className="p-2 rounded-md text-gray-500 hover:text-gray-900 dark:text-gray-400 dark:hover:text-white hover:bg-gray-100 dark:hover:bg-gray-700 lg:hidden"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>
          <button
            onClick={toggleSidebar}
            className="p-2 rounded-md text-gray-500 hover:text-gray-900 dark:text-gray-400 dark:hover:text-white hover:bg-gray-100 dark:hover:bg-gray-700 hidden lg:block"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>
          <span className="ml-4 text-xl font-bold text-gray-900 dark:text-white">
            {t('app.name')}
          </span>
        </div>

        <div className="flex items-center space-x-4">
          <LanguageSwitcher />

          {isAuthenticated && user && (
            <div className="flex items-center space-x-3">
              <span className="text-sm text-gray-700 dark:text-gray-300">{user.name}</span>
              <button
                onClick={handleLogout}
                className="px-3 py-1.5 text-sm text-gray-600 dark:text-gray-300 hover:text-gray-900 dark:hover:text-white"
              >
                {t('auth.logout')}
              </button>
            </div>
          )}
        </div>
      </div>
    </header>
  );
}
