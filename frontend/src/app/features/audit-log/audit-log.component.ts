import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { MatTableModule } from '@angular/material/table';
import { MatCardModule } from '@angular/material/card';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatIconModule } from '@angular/material/icon';

interface AuditLog {
  id: number;
  username: string;
  action: string;
  entityType: string;
  entityId: number;
  details: string;
  createdAt: string;
}

interface Page<T> {
  content: T[];
  totalElements: number;
  size: number;
  number: number;
}

@Component({
  selector: 'app-audit-log',
  standalone: true,
  imports: [
    CommonModule,
    MatTableModule, MatCardModule, MatPaginatorModule,
    MatProgressSpinnerModule, MatIconModule,
  ],
  templateUrl: './audit-log.component.html',
})
export class AuditLogComponent implements OnInit {
  logs = signal<AuditLog[]>([]);
  totalElements = signal(0);
  pageIndex = signal(0);
  pageSize = signal(50);
  loading = signal(false);

  displayedColumns = ['createdAt', 'username', 'action', 'entityType', 'entityId', 'details'];

  constructor(private http: HttpClient) {}

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.http.get<Page<AuditLog>>(`/api/audit-log?page=${this.pageIndex()}&size=${this.pageSize()}`).subscribe({
      next: page => {
        this.logs.set(page.content);
        this.totalElements.set(page.totalElements);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
  }

  onPage(event: PageEvent) {
    this.pageIndex.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.load();
  }
}
