import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatDialogModule, MatDialog } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatCardModule } from '@angular/material/card';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { ItemService } from '../../../core/services/item.service';
import { Item, ItemRequest, LifecycleState } from '../../../core/models/item.model';

@Component({
  selector: 'app-item-list',
  standalone: true,
  imports: [
    CommonModule, RouterModule, ReactiveFormsModule,
    MatTableModule, MatButtonModule, MatIconModule, MatDialogModule,
    MatFormFieldModule, MatInputModule, MatSelectModule,
    MatSnackBarModule, MatCardModule, MatProgressSpinnerModule,
  ],
  templateUrl: './item-list.component.html',
})
export class ItemListComponent implements OnInit {
  items = signal<Item[]>([]);
  loading = signal(false);
  displayedColumns = ['itemNumber', 'name', 'lifecycleState', 'updatedAt', 'actions'];
  showForm = signal(false);
  editingItem = signal<Item | null>(null);

  lifecycleStates: LifecycleState[] = ['DRAFT', 'IN_REVIEW', 'RELEASED', 'OBSOLETE'];

  form = this.fb.group({
    itemNumber: ['', [Validators.required, Validators.maxLength(50)]],
    name: ['', [Validators.required, Validators.maxLength(255)]],
    description: [''],
    lifecycleState: ['DRAFT' as LifecycleState],
  });

  constructor(
    private itemService: ItemService,
    private fb: FormBuilder,
    private snack: MatSnackBar,
  ) {}

  ngOnInit() { this.load(); }

  load() {
    this.loading.set(true);
    this.itemService.getAll().subscribe({
      next: items => { this.items.set(items); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  openCreate() {
    this.editingItem.set(null);
    this.form.reset({ lifecycleState: 'DRAFT' });
    this.showForm.set(true);
  }

  openEdit(item: Item) {
    this.editingItem.set(item);
    this.form.patchValue(item);
    this.showForm.set(true);
  }

  save() {
    if (this.form.invalid) return;
    const req = this.form.value as ItemRequest;
    const editing = this.editingItem();
    const obs = editing
      ? this.itemService.update(editing.id, req)
      : this.itemService.create(req);

    obs.subscribe({
      next: () => { this.load(); this.showForm.set(false); this.snack.open('Saved', '', { duration: 2000 }); },
      error: (e) => this.snack.open(e.error?.message ?? 'Error', 'Close', { duration: 3000 }),
    });
  }

  delete(item: Item) {
    if (!confirm(`Delete "${item.name}"?`)) return;
    this.itemService.delete(item.id).subscribe({
      next: () => { this.load(); this.snack.open('Deleted', '', { duration: 2000 }); },
      error: (e) => this.snack.open(e.error?.message ?? 'Error', 'Close', { duration: 3000 }),
    });
  }
}
