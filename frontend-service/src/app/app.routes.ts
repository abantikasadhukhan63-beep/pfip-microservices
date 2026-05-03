import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
  { 
    path: 'login', 
    loadComponent: () => import('./features/auth/login/login.component').then(m => m.LoginComponent) 
  },
  { 
    path: 'register', 
    loadComponent: () => import('./features/auth/register/register.component').then(m => m.RegisterComponent) 
  },
  {
    path: '',
    loadComponent: () => import('./layout/main-layout/main-layout.component').then(m => m.MainLayoutComponent),
    canActivate: [authGuard],
    children: [
      { 
        path: 'dashboard', 
        loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent) 
      },
      { 
        path: 'profile', 
        loadComponent: () => import('./features/profile/profile.component').then(m => m.ProfileComponent) 
      },
      { 
        path: 'transactions', 
        loadComponent: () => import('./features/transactions/transaction-history/transaction-history.component').then(m => m.TransactionHistoryComponent) 
      },
      { 
        path: 'transactions/:id', 
        loadComponent: () => import('./features/transactions/transaction-detail/transaction-detail.component').then(m => m.TransactionDetailComponent) 
      }
    ]
  },
  { path: '**', redirectTo: 'dashboard' }
];
