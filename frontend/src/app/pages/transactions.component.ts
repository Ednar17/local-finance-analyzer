import { CurrencyPipe, DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ApiService } from '../core/api.service';
import { CategoryOption, ImportReport, Transaction } from '../core/models';

@Component({
  selector: 'app-transactions',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, FormsModule],
  template: `
    <h1>Opérations bancaires</h1>
    <p class="subtitle">
      Import d'un relevé CSV (12 derniers mois affichés). Les opérations sont d'abord classées par règles,
      puis le modèle local s'occupe de celles qu'il reste. Vous pouvez corriger chaque catégorie.
    </p>

    <section class="card">
      <div class="row">
        <input type="file" accept=".csv,text/csv" (change)="onFile($event)" />
        <button (click)="import()" [disabled]="!file || importing()">{{ importing() ? 'Import en cours…' : 'Importer' }}</button>
      </div>
      @if (report(); as r) {
        <p>
          {{ r.imported }} opérations importées sur {{ r.read }} lues · {{ r.duplicates }} doublons ignorés ·
          {{ r.categorizedByRules }} classées par règles · {{ r.categorizedByAi }} par l'IA
        </p>
        @for (w of r.warnings; track $index) {
          <p class="muted">{{ w }}</p>
        }
      }
      @if (error()) {
        <p class="error">{{ error() }}</p>
      }
    </section>

    <section class="card">
      <div class="row" style="justify-content: space-between; margin-bottom: 10px">
        <h2 style="margin: 0">{{ filtered().length }} opérations</h2>
        <div class="row">
          <input type="text" placeholder="Filtrer par libellé" [ngModel]="filter()" (ngModelChange)="filter.set($event)" />
          <select [ngModel]="categoryFilter()" (ngModelChange)="categoryFilter.set($event)">
            <option value="">Toutes les catégories</option>
            @for (c of categories(); track c.code) {
              <option [value]="c.code">{{ c.label }}</option>
            }
          </select>
        </div>
      </div>
      <table>
        <thead><tr><th>Date</th><th>Libellé</th><th>Catégorie</th><th>Classée par</th><th class="amount">Montant</th></tr></thead>
        <tbody>
          @for (t of filtered(); track t.id) {
            <tr>
              <td>{{ t.date | date: 'dd/MM/yyyy' }}</td>
              <td>{{ t.label }}</td>
              <td>
                <select [ngModel]="t.category" (ngModelChange)="changeCategory(t, $event)">
                  @for (c of categories(); track c.code) {
                    <option [value]="c.code">{{ c.label }}</option>
                  }
                </select>
              </td>
              <td><span class="badge">{{ sourceLabel[t.categorizedBy] }}</span></td>
              <td class="amount" [class.positive]="t.amount > 0" [class.negative]="t.amount < 0">{{ t.amount | currency: 'EUR' }}</td>
            </tr>
          } @empty {
            <tr><td colspan="5" class="muted">Aucune opération.</td></tr>
          }
        </tbody>
      </table>
    </section>
  `,
})
export class TransactionsComponent implements OnInit {
  private readonly api = inject(ApiService);

  transactions = signal<Transaction[]>([]);
  categories = signal<CategoryOption[]>([]);
  report = signal<ImportReport | null>(null);
  importing = signal(false);
  error = signal('');
  filter = signal('');
  categoryFilter = signal('');
  file: File | null = null;

  readonly sourceLabel = { RULE: 'Règle', AI: 'IA locale', USER: 'Manuel' };

  filtered = computed(() => {
    const f = this.filter().toLowerCase();
    const c = this.categoryFilter();
    return this.transactions().filter((t) => (!f || t.label.toLowerCase().includes(f)) && (!c || t.category === c));
  });

  ngOnInit(): void {
    this.api.categories().subscribe((c) => this.categories.set(c));
    this.load();
  }

  load(): void {
    this.api.transactions().subscribe((t) => this.transactions.set(t));
  }

  onFile(event: Event): void {
    this.file = (event.target as HTMLInputElement).files?.[0] ?? null;
  }

  import(): void {
    if (!this.file) return;
    this.importing.set(true);
    this.error.set('');
    this.api.importTransactions(this.file).subscribe({
      next: (r) => {
        this.report.set(r);
        this.importing.set(false);
        this.load();
      },
      error: (e) => {
        this.error.set(e.error?.detail ?? "L'import a échoué");
        this.importing.set(false);
      },
    });
  }

  changeCategory(t: Transaction, category: string): void {
    this.api.updateCategory(t.id, category).subscribe((updated) =>
      this.transactions.update((list) => list.map((x) => (x.id === updated.id ? updated : x))),
    );
  }
}
