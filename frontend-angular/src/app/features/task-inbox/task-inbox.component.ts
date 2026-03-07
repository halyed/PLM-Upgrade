import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { WorkflowService, PendingTask } from '../../core/services/workflow.service';

@Component({
  selector: 'app-task-inbox',
  standalone: true,
  imports: [
    CommonModule, FormsModule,
    MatTableModule, MatButtonModule, MatIconModule,
    MatProgressSpinnerModule, MatChipsModule, MatDialogModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
  ],
  templateUrl: './task-inbox.component.html',
})
export class TaskInboxComponent implements OnInit {
  tasks = signal<PendingTask[]>([]);
  loading = signal(false);
  columns = ['taskType', 'itemName', 'revisionCode', 'submittedBy', 'createdAt', 'actions'];

  // Inline approval form state
  activeJobKey: number | null = null;
  decision = 'APPROVED';
  comment = '';
  submitting = signal(false);

  constructor(private workflowService: WorkflowService, private http: HttpClient) {}

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.workflowService.getPendingTasks().subscribe({
      next: tasks => { this.tasks.set(tasks); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  openForm(jobKey: number) {
    this.activeJobKey = jobKey;
    this.decision = 'APPROVED';
    this.comment = '';
  }

  cancelForm() { this.activeJobKey = null; }

  submit() {
    if (!this.activeJobKey) return;
    this.submitting.set(true);
    this.workflowService.completeTask(this.activeJobKey, this.decision, this.comment).subscribe({
      next: () => {
        this.submitting.set(false);
        this.activeJobKey = null;
        this.load();
      },
      error: () => this.submitting.set(false),
    });
  }
}
