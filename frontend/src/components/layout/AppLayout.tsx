import { Outlet } from 'react-router-dom';
import { Sidebar } from './Sidebar';

export const AppLayout = () => (
  <div className="page">
    <Sidebar />
    <main className="page-content">
      <Outlet />
    </main>
  </div>
);
