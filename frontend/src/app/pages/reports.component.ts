import { DatePipe } from '@angular/common';
import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { ApiService } from '../core/api.service';
import { AskResponse, FinancialDocument } from '../core/models';
import { SourcesComponent } from './sources.component';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [DatePipe, FormsModule, SourcesComponent],
  template: `
    <h1>Rapports d'entreprises</h1>
    <p class="subtitle">
      Importez des rapports annuels ou des communiqués de résultats en PDF, puis interrogez-les.
      L'IA peut croiser les chiffres avec la revue de presse diplomatique.
    </p>

    <section class="card">
      <h2>Importer un rapport</h2>
      <div class="row">
        <input type="text" placeholder="Entreprise" [(ngModel)]="company" />
        <input type="number" placeholder="Exercice" [(ngModel)]="fiscalYear" style="width: 110px" />
        <input type="file" accept="application/pdf" (change)="onFile($event)" />
        <button (click)="upload()" [disabled]="!file || !company || uploading()">Importer</button>
      </div>
      @if (uploadError()) {
        <p class="error">{{ uploadError() }}</p>
      }
    </section>

    <section class="card">
      <h2>Documents</h2>
      <table>
        <thead>
          <tr><th></th><th>Entreprise</th><th>Exercice</th><th>Fichier</th><th>État</th><th>Pages</th><th></th></tr>
        </thead>
        <tbody>
          @for (d of documents(); track d.id) {
            <tr>
              <td><input type="checkbox" [checked]="selected.has(d.id)" (change)="toggle(d.id)" [disabled]="d.status !== 'INDEXED'" /></td>
              <td>{{ d.company }}</td>
              <td>{{ d.fiscalYear }}</td>
              <td class="muted">{{ d.filename }}</td>
              <td>
                <span class="badge" [class.ok]="d.status === 'INDEXED'" [class.ko]="d.status === 'FAILED'" [title]="d.errorMessage ?? ''">
                  {{ statusLabel(d) }}
                </span>
              </td>
              <td>{{ d.pageCount ?? '–' }}</td>
              <td><button class="secondary" (click)="remove(d)">Supprimer</button></td>
            </tr>
          } @empty {
            <tr><td colspan="7" class="muted">Aucun rapport importé.</td></tr>
          }
        </tbody>
      </table>
    </section>

    <section class="card">
      <h2>Poser une question</h2>
      <textarea [(ngModel)]="question"
        placeholder="Ex. : Comment évolue la marge opérationnelle et quels risques géopolitiques l'entreprise mentionne-t-elle ?"></textarea>
      <div class="row" style="margin-top: 10px; justify-content: space-between">
        <label class="row"><input type="checkbox" [(ngModel)]="includeNews" /> Croiser avec la revue de presse</label>
        <span class="muted">{{ selected.size ? selected.size + ' document(s) sélectionné(s)' : 'Tous les documents' }}</span>
        <button (click)="ask()" [disabled]="question.length < 3 || asking()">
          {{ asking() ? 'Analyse en cours…' : 'Analyser' }}
        </button>
      </div>
      @if (askError()) {
        <p class="error">{{ askError() }}</p>
      }
      @if (answer(); as a) {
        <div class="answer" style="margin-top: 16px">{{ a.answer }}</div>
        <p class="muted">Modèle : {{ a.model }}</p>
        <app-sources [sources]="a.sources" />
      }
    </section>
  `,
})
export class ReportsComponent implements OnInit, OnDestroy {
  private readonly api = inject(ApiService);
  private poller?: ReturnType<typeof setInterval>;

  documents = signal<FinancialDocument[]>([]);
  answer = signal<AskResponse | null>(null);
  uploading = signal(false);
  asking = signal(false);
  uploadError = signal('');
  askError = signal('');

  selected = new Set<number>();
  company = '';
  fiscalYear = new Date().getFullYear() - 1;
  file: File | null = null;
  question = '';
  includeNews = true;

  ngOnInit(): void {
    this.load();
    // Rafraîchit l'état tant qu'une indexation est en cours
    this.poller = setInterval(() => {
      if (this.documents().some((d) => d.status === 'PENDING')) this.load();
    }, 5000);
  }

  ngOnDestroy(): void {
    clearInterval(this.poller);
  }

  load(): void {
    this.api.documents().subscribe((d) => this.documents.set(d));
  }

  onFile(event: Event): void {
    this.file = (event.target as HTMLInputElement).files?.[0] ?? null;
  }

  upload(): void {
    if (!this.file) return;
    this.uploading.set(true);
    this.uploadError.set('');
    this.api.uploadDocument(this.file, this.company, this.fiscalYear).subscribe({
      next: () => {
        this.uploading.set(false);
        this.load();
      },
      error: (e) => {
        this.uploadError.set(e.error?.detail ?? "L'import a échoué");
        this.uploading.set(false);
      },
    });
  }

  remove(d: FinancialDocument): void {
    if (!confirm(`Supprimer le rapport ${d.company} ${d.fiscalYear} et son index ?`)) return;
    this.selected.delete(d.id);
    this.api.deleteDocument(d.id).subscribe(() => this.load());
  }

  toggle(id: number): void {
    this.selected.has(id) ? this.selected.delete(id) : this.selected.add(id);
  }

  statusLabel(d: FinancialDocument): string {
    return { PENDING: 'Indexation…', INDEXED: 'Indexé', FAILED: 'Échec' }[d.status];
  }

  ask(): void {
    this.asking.set(true);
    this.askError.set('');
    this.api.askReports(this.question, [...this.selected], this.includeNews).subscribe({
      next: (a) => {
        this.answer.set(a);
        this.asking.set(false);
      },
      error: (e) => {
        this.askError.set(e.error?.detail ?? "L'analyse a échoué : le modèle local est-il démarré ?");
        this.asking.set(false);
      },
    });
  }
}
