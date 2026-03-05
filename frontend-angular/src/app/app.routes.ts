import { Routes } from '@angular/router';
import { AuthGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  {
    path: '',
    loadComponent: () =>
      import('./features/shell/shell.component').then(m => m.ShellComponent),
    canActivate: [AuthGuard],
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
  { path: '**', redirectTo: '' },
];
