import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ChangeRequestService } from '../../../core/services/change-request.service';
import { ChangeRequest, ChangeRequestRequest, CrStatus } from '../../../core/models/change-request.model';

@Component({
  selector: 'app-cr-list',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule,
    MatTableModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSnackBarModule, MatCardModule, MatProgressSpinnerModule,
  ],
  templateUrl: './cr-list.component.html',
})
export class CrListComponent implements OnInit {
  crs = signal<ChangeRequest[]>([]);
  loading = signal(false);
  showForm = signal(false);
  editingCr = signal<ChangeRequest | null>(null);
  displayedColumns = ['title', 'status', 'createdAt', 'actions'];
  statuses: CrStatus[] = ['OPEN', 'IN_REVIEW', 'APPROVED', 'REJECTED', 'CLOSED'];

  form = this.fb.group({
    title: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    status: ['OPEN' as CrStatus],
  });

  constructor(private crService: ChangeRequestService, private fb: FormBuilder, private snack: MatSnackBar) {}

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.crService.getAll().subscribe({
      next: crs => { this.crs.set(crs); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  openCreate() {
    this.editingCr.set(null);
    this.form.reset({ status: 'OPEN' });
    this.showForm.set(true);
  }

  openEdit(cr: ChangeRequest) {
    this.editingCr.set(cr);
    this.form.patchValue(cr);
    this.showForm.set(true);
  }

  save() {
    if (this.form.invalid) return;
    const req = this.form.value as ChangeRequestRequest;
    const editing = this.editingCr();
    const obs = editing ? this.crService.update(editing.id, req) : this.crService.create(req);
    obs.subscribe({
      next: () => { this.load(); this.showForm.set(false); this.snack.open('Saved', '', { duration: 2000 }); },
      error: (e) => this.snack.open(e.error?.message ?? 'Error', 'Close', { duration: 3000 }),
    });
  }

  updateStatus(cr: ChangeRequest, status: CrStatus) {
    this.crService.updateStatus(cr.id, status).subscribe({
      next: updated => this.crs.update(list => list.map(c => c.id === updated.id ? updated : c)),
    });
  }

  delete(cr: ChangeRequest) {
    if (!confirm(`Delete "${cr.title}"?`)) return;
    this.crService.delete(cr.id).subscribe({
      next: () => { this.load(); this.snack.open('Deleted', '', { duration: 2000 }); },
    });
  }
}
