import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

interface Summary {
  totalItems: number;
  itemsByState: Record<string, number>;
  totalChangeRequests: number;
  changeRequestsByStatus: Record<string, number>;
}

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [CommonModule, MatCardModule, MatButtonModule, MatIconModule, MatProgressSpinnerModule],
  templateUrl: './reports.component.html',
})
export class ReportsComponent implements OnInit {
  summary = signal<Summary | null>(null);
  loading = signal(false);

  constructor(private http: HttpClient) {}

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.http.get<Summary>('/api/reports/summary').subscribe({
      next: s => { this.summary.set(s); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  exportItems() {
    window.open('/api/reports/items/export?format=csv', '_blank');
  }

  exportBom(revisionId: string) {
    if (revisionId.trim()) {
      window.open('/api/reports/bom/' + revisionId.trim() + '/export', '_blank');
    }
  }

  entries(obj: Record<string, number>): [string, number][] {
    return Object.entries(obj);
  }
}
