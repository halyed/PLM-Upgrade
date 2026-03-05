import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatTableModule } from '@angular/material/table';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatChipsModule } from '@angular/material/chips';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';
import { SearchService, ItemDocument } from '../../core/services/search.service';

@Component({
  selector: 'app-search',
  standalone: true,
  imports: [
    CommonModule, ReactiveFormsModule, RouterModule,
    MatTableModule, MatFormFieldModule, MatInputModule, MatSelectModule,
    MatButtonModule, MatIconModule, MatProgressSpinnerModule, MatChipsModule,
  ],
  templateUrl: './search.component.html',
})
export class SearchComponent implements OnInit {
  queryCtrl = new FormControl('');
  stateFilter = new FormControl('');
  results = signal<ItemDocument[]>([]);
  loading = signal(false);
  searched = signal(false);
  reindexing = signal(false);

  columns = ['itemNumber', 'name', 'lifecycleState', 'description', 'actions'];
  lifecycleStates = ['DRAFT', 'IN_REVIEW', 'RELEASED', 'OBSOLETE'];

  constructor(
    private searchService: SearchService,
    private http: HttpClient,
    private route: ActivatedRoute,
    private router: Router,
  ) {}

  ngOnInit() {
    this.route.queryParamMap.subscribe(params => {
      const q = params.get('q') ?? '';
      this.queryCtrl.setValue(q, { emitEvent: false });
      if (q) this.runSearch(q);
    });

    this.queryCtrl.valueChanges.pipe(debounceTime(400), distinctUntilChanged()).subscribe(q => {
      this.router.navigate([], { queryParams: { q }, replaceUrl: true });
    });

    this.stateFilter.valueChanges.subscribe(state => {
      if (state) this.runStateFilter(state);
      else {
        const q = this.queryCtrl.value ?? '';
        if (q) this.runSearch(q);
      }
    });
  }

  runSearch(q: string) {
    this.loading.set(true);
    this.searched.set(true);
    this.searchService.searchItems(q).subscribe({
      next: results => { this.results.set(results); this.loading.set(false); },
      error: () => { this.results.set([]); this.loading.set(false); },
    });
  }

  runStateFilter(state: string) {
    this.loading.set(true);
    this.searched.set(true);
    this.searchService.filterByState(state).subscribe({
      next: results => { this.results.set(results); this.loading.set(false); },
      error: () => { this.results.set([]); this.loading.set(false); },
    });
  }

  goToItem(doc: ItemDocument) {
    this.router.navigate(['/items', doc.id]);
  }

  reindex() {
    this.reindexing.set(true);
    this.http.post<{indexed: number}>('/api/search/reindex', {}).subscribe({
      next: res => {
        this.reindexing.set(false);
        const q = this.queryCtrl.value ?? '';
        if (q) this.runSearch(q); else this.runSearch('');
      },
      error: () => this.reindexing.set(false),
    });
  }
}
