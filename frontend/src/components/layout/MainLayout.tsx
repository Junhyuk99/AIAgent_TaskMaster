import { Outlet } from 'react-router-dom';
import Header from './Header';
import Sidebar from './Sidebar';
import { useSidebarStore } from '../../stores/sidebarStore';

export default function MainLayout() {
  const { isOpen } = useSidebarStore();

  return (
    <div className="min-h-screen bg-gray-50 dark:bg-gray-900">
      <Header />
      <Sidebar />
      <main
        className={`pt-16 transition-all duration-300 ${
          isOpen ? 'lg:ml-64' : 'lg:ml-0'
        }`}
      >
        <div className="p-6">
          <Outlet />
        </div>
      </main>
    </div>
  );
}
