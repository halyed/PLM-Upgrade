import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatDividerModule } from '@angular/material/divider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { IntegrationService, ConnectorStatus } from '../../core/services/integration.service';

@Component({
  selector: 'app-integration',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule, MatIconModule, MatButtonModule,
    MatProgressSpinnerModule, MatChipsModule, MatDividerModule, MatSnackBarModule,
  ],
  templateUrl: './integration.component.html',
})
export class IntegrationComponent implements OnInit {
  connectors = signal<ConnectorStatus[]>([]);
  summary = signal<Record<string, number>>({});
  loading = signal(true);

  constructor(
    private integrationService: IntegrationService,
    private snack: MatSnackBar,
  ) {}

  ngOnInit() {
    this.refresh();
  }

  refresh() {
    this.loading.set(true);
    this.integrationService.getConnectors().subscribe({
      next: data => { this.connectors.set(data); this.loading.set(false); },
      error: () => { this.loading.set(false); this.snack.open('Failed to load connectors', 'Close', { duration: 3000 }); },
    });
    this.integrationService.getSummary().subscribe({
      next: data => this.summary.set(data),
    });
  }

  isUp(c: ConnectorStatus): boolean {
    return c.state === 'UP';
  }
}
