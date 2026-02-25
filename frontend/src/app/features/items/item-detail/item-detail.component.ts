import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatDividerModule } from '@angular/material/divider';
import { MatChipsModule } from '@angular/material/chips';
import { MatTooltipModule } from '@angular/material/tooltip';
import { ItemService } from '../../../core/services/item.service';
import { RevisionService } from '../../../core/services/revision.service';
import { BomService } from '../../../core/services/bom.service';
import { DocumentService } from '../../../core/services/document.service';
import { Item, LifecycleState } from '../../../core/models/item.model';
import { Revision, RevisionStatus } from '../../../core/models/revision.model';
import { BomLink } from '../../../core/models/bom.model';
import { Document } from '../../../core/models/document.model';

@Component({
  selector: 'app-item-detail',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, RouterModule,
    MatCardModule, MatButtonModule, MatIconModule, MatTableModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatExpansionModule,
    MatSnackBarModule, MatProgressSpinnerModule, MatDividerModule, MatChipsModule,
    MatTooltipModule,
  ],
  templateUrl: './item-detail.component.html',
})
export class ItemDetailComponent implements OnInit {
  item = signal<Item | null>(null);
  revisions = signal<Revision[]>([]);
  selectedRevision = signal<Revision | null>(null);
  bomChildren = signal<BomLink[]>([]);
  documents = signal<Document[]>([]);
  loading = signal(true);

  revisionStatuses: RevisionStatus[] = ['IN_WORK', 'IN_REVIEW', 'RELEASED', 'OBSOLETE'];
  lifecycleStates: LifecycleState[] = ['DRAFT', 'IN_REVIEW', 'RELEASED', 'OBSOLETE'];

  revCols = ['revisionCode', 'status', 'createdAt', 'actions'];
  bomCols = ['childItemNumber', 'childRevisionCode', 'quantity', 'actions'];
  docCols = ['fileName', 'fileType', 'uploadedAt', 'actions'];

  bomForm = this.fb.group({
    childRevisionId: ['', Validators.required],
    quantity: [1, [Validators.required, Validators.min(0.0001)]],
  });

  constructor(
    private route: ActivatedRoute,
    private itemService: ItemService,
    private revisionService: RevisionService,
    private bomService: BomService,
    private documentService: DocumentService,
    private fb: FormBuilder,
    private snack: MatSnackBar,
  ) {}

  ngOnInit() {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.itemService.getById(id).subscribe(item => {
      this.item.set(item);
      this.loading.set(false);
    });
    this.revisionService.getByItem(id).subscribe(revs => {
      this.revisions.set(revs);
      if (revs.length > 0) this.selectRevision(revs[revs.length - 1]);
    });
  }

  selectRevision(rev: Revision) {
    this.selectedRevision.set(rev);
    this.bomService.getChildren(rev.id).subscribe(bom => this.bomChildren.set(bom));
    this.documentService.getByRevision(rev.id).subscribe(docs => this.documents.set(docs));
  }

  nextRevision() {
    const item = this.item();
    if (!item) return;
    this.revisionService.nextRevision(item.id).subscribe({
      next: rev => {
        this.revisions.update(list => [...list, rev]);
        this.selectRevision(rev);
        this.snack.open(`Revision ${rev.revisionCode} created`, '', { duration: 2000 });
      },
      error: (e) => this.snack.open(e.error?.message ?? 'Error', 'Close', { duration: 3000 }),
    });
  }

  updateRevStatus(rev: Revision, status: RevisionStatus) {
    this.revisionService.updateStatus(rev.id, status).subscribe({
      next: updated => {
        this.revisions.update(list => list.map(r => r.id === updated.id ? updated : r));
        if (this.selectedRevision()?.id === updated.id) this.selectedRevision.set(updated);
        this.snack.open('Status updated', '', { duration: 2000 });
      },
    });
  }

  addBomChild() {
    const rev = this.selectedRevision();
    if (!rev || this.bomForm.invalid) return;
    const { childRevisionId, quantity } = this.bomForm.value;
    this.bomService.addChild(rev.id, { childRevisionId: Number(childRevisionId), quantity: Number(quantity) }).subscribe({
      next: link => {
        this.bomChildren.update(list => [...list, link]);
        this.bomForm.reset({ quantity: 1 });
        this.snack.open('BOM child added', '', { duration: 2000 });
      },
      error: (e) => this.snack.open(e.error?.message ?? 'Error', 'Close', { duration: 3000 }),
    });
  }

  removeBomChild(link: BomLink) {
    const rev = this.selectedRevision();
    if (!rev) return;
    this.bomService.removeChild(rev.id, link.childRevisionId).subscribe({
      next: () => this.bomChildren.update(list => list.filter(l => l.id !== link.id)),
    });
  }

  uploadFile(event: Event) {
    const rev = this.selectedRevision();
    if (!rev) return;
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;
    this.documentService.upload(rev.id, file).subscribe({
      next: doc => {
        this.documents.update(list => [...list, doc]);
        this.snack.open('File uploaded', '', { duration: 2000 });
      },
      error: (e) => this.snack.open(e.error?.message ?? 'Upload failed', 'Close', { duration: 3000 }),
    });
  }

  deleteDoc(doc: Document) {
    this.documentService.delete(doc.id).subscribe({
      next: () => this.documents.update(list => list.filter(d => d.id !== doc.id)),
    });
  }

  isViewable(fileType: string): boolean {
    return ['GLTF', 'GLB', 'STEP', 'STP'].includes((fileType || '').toUpperCase());
  }

  transitionLifecycle(state: LifecycleState) {
    const item = this.item();
    if (!item) return;
    this.itemService.transitionLifecycle(item.id, state).subscribe({
      next: updated => { this.item.set(updated); this.snack.open(`State -> ${state}`, '', { duration: 2000 }); },
      error: (e) => this.snack.open(e.error?.message ?? 'Error', 'Close', { duration: 3000 }),
    });
  }
}
