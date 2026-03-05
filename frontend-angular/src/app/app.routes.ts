import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then(m => m.LoginComponent),
  },
  {
    path: 'register',
    loadComponent: () =>
      import('./features/auth/register/register.component').then(m => m.RegisterComponent),
  },
  {
    path: '',
    loadComponent: () =>
      import('./features/shell/shell.component').then(m => m.ShellComponent),
    canActivate: [authGuard],
    children: [
      { path: '', redirectTo: 'items', pathMatch: 'full' },
      {
        path: 'items',
        loadComponent: () =>
          import('./features/items/item-list/item-list.component').then(m => m.ItemListComponent),
      },
      {
        path: 'items/:id',
        loadComponent: () =>
          import('./features/items/item-detail/item-detail.component').then(m => m.ItemDetailComponent),
      },
      {
        path: 'change-requests',
        loadComponent: () =>
          import('./features/change-requests/cr-list/cr-list.component').then(m => m.CrListComponent),
      },
      {
        path: 'viewer/:documentId',
        loadComponent: () =>
          import('./features/viewer/viewer.component').then(m => m.ViewerComponent),
      },
      {
        path: 'audit-log',
        loadComponent: () =>
          import('./features/audit-log/audit-log.component').then(m => m.AuditLogComponent),
      },
    ],
  },
  { path: '**', redirectTo: 'items' },
];
