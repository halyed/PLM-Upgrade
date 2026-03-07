import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { MatListModule } from '@angular/material/list';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

interface Notification {
  id: number;
  type: string;
  title: string;
  message: string;
  entityType: string;
  entityId: string;
  read: boolean;
  createdAt: string;
}

interface Page<T> { content: T[]; totalElements: number; }

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule, MatListModule, MatIconModule, MatButtonModule, MatProgressSpinnerModule],
  templateUrl: './notifications.component.html',
})
export class NotificationsComponent implements OnInit {
  notifications = signal<Notification[]>([]);
  loading = signal(false);

  constructor(private http: HttpClient) {}

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.http.get<Page<Notification>>('/api/notifications?size=50').subscribe({
      next: p => { this.notifications.set(p.content); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  markRead(n: Notification) {
    if (n.read) return;
    this.http.put(`/api/notifications/${n.id}/read`, {}).subscribe(() => {
      n.read = true;
      this.notifications.update(list => [...list]);
    });
  }

  markAllRead() {
    this.http.put('/api/notifications/read-all', {}).subscribe(() => {
      this.notifications.update(list => list.map(n => ({ ...n, read: true })));
    });
  }

  typeIcon(type: string): string {
    if (type.includes('CREATED')) return 'add_circle';
    if (type.includes('DELETED')) return 'delete';
    if (type.includes('RELEASED')) return 'check_circle';
    if (type.includes('REJECTED')) return 'cancel';
    if (type.includes('LIFECYCLE')) return 'swap_horiz';
    return 'notifications';
  }
}
