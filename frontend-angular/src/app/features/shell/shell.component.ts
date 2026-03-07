import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { MatToolbarModule } from '@angular/material/toolbar';
import { MatSidenavModule } from '@angular/material/sidenav';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatInputModule } from '@angular/material/input';
import { MatBadgeModule } from '@angular/material/badge';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [
    CommonModule, RouterModule,
    MatToolbarModule, MatSidenavModule, MatListModule,
    MatIconModule, MatButtonModule, MatTooltipModule,
    MatInputModule, MatBadgeModule,
  ],
  templateUrl: './shell.component.html',
})
export class ShellComponent implements OnInit {
  unreadCount = signal(0);

  constructor(public auth: AuthService, private router: Router, private http: HttpClient) {}

  ngOnInit() {
    this.loadUnreadCount();
    setInterval(() => this.loadUnreadCount(), 30000);
  }

  loadUnreadCount() {
    this.http.get<{ count: number }>('/api/notifications/unread-count').subscribe({
      next: res => this.unreadCount.set(res.count),
      error: () => {},
    });
  }

  onSearch(query: string) {
    if (query.trim()) {
      this.router.navigate(['/search'], { queryParams: { q: query.trim() } });
    }
  }
}
