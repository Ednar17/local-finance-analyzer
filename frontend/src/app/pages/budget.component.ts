import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { AfterViewInit, Component, ElementRef, OnDestroy, ViewChild, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Chart, registerables } from 'chart.js';

import { ApiService } from '../core/api.service';
import { AskResponse, BudgetSummary } from '../core/models';
import { SourcesComponent } from './sources.component';

Chart.register(...registerables);

@Component({
  selector: 'app-budget',
  standalone: true,
  imports: [CurrencyPipe, DatePipe, DecimalPipe, FormsModule, RouterLink, SourcesComponent],
  template: `
    <h1>Budget personnel</h1>
    @if (summary(); as s) {
      <p class="subtitle">Du {{ s.from | date: 'd MMMM y' }} au {{ s.to | date: 'd MMMM y' }} · {{ s.transactionCount }} opérations</p>

      @if (s.transactionCount === 0) {
        <section class="card">
          <p>Aucune opération sur les 12 derniers mois. <a routerLink="/operations">Importez un relevé CSV</a>
            (un exemple est fourni dans <code>samples/releve_demo.csv</code>).</p>
        </section>
      }

      <div class="grid">
        <div class="card kpi"><div class="label">Revenus</div><div class="value positive">{{ s.income | currency: 'EUR' }}</div></div>
        <div class="card kpi"><div class="label">Dépenses</div><div class="value negative">{{ s.expenses | currency: 'EUR' }}</div></div>
        <div class="card kpi"><div class="label">Solde</div>
          <div class="value" [class.positive]="s.net >= 0" [class.negative]="s.net < 0">{{ s.net | currency: 'EUR' }}</div></div>
        <div class="card kpi"><div class="label">Taux d'épargne</div><div class="value">{{ s.savingsRate | number: '1.1-1' }} %</div></div>
      </div>
    }

    <div class="charts">
      <section class="card"><h2>Revenus et dépenses par mois</h2><canvas #monthChart></canvas></section>
      <section class="card"><h2>Dépenses par catégorie</h2><canvas #categoryChart></canvas></section>
    </div>

    <section class="card">
      <h2>Demander une analyse</h2>
      <textarea [(ngModel)]="question"
        placeholder="Ex. : Où puis-je réduire mes dépenses ? Comment l'inflation et les tensions internationales peuvent-elles peser sur mon budget ?"></textarea>
      <div class="row" style="margin-top: 10px; justify-content: space-between">
        <label class="row"><input type="checkbox" [(ngModel)]="includeNews" /> Tenir compte de l'actualité économique et géopolitique</label>
        <button (click)="ask()" [disabled]="question.length < 3 || asking()">{{ asking() ? 'Analyse en cours…' : 'Analyser' }}</button>
      </div>
      @if (error()) {
        <p class="error">{{ error() }}</p>
      }
      @if (answer(); as a) {
        <div class="answer" style="margin-top: 16px">{{ a.answer }}</div>
        <app-sources [sources]="a.sources" />
      }
    </section>

    @if (summary(); as s) {
      <section class="card">
        <h2>Plus grosses dépenses</h2>
        <table>
          <tbody>
            @for (e of s.largestExpenses; track $index) {
              <tr><td>{{ e.date | date: 'd MMM y' }}</td><td>{{ e.label }}</td><td class="muted">{{ e.category }}</td>
                <td class="amount negative">{{ -e.amount | currency: 'EUR' }}</td></tr>
            }
          </tbody>
        </table>
      </section>
    }
  `,
  styles: `
    .charts { display: grid; grid-template-columns: 2fr 1fr; gap: 20px; }
    @media (max-width: 1000px) { .charts { grid-template-columns: 1fr; } }
  `,
})
export class BudgetComponent implements AfterViewInit, OnDestroy {
  private readonly api = inject(ApiService);

  @ViewChild('monthChart') monthCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('categoryChart') categoryCanvas!: ElementRef<HTMLCanvasElement>;

  summary = signal<BudgetSummary | null>(null);
  answer = signal<AskResponse | null>(null);
  asking = signal(false);
  error = signal('');
  question = '';
  includeNews = true;
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  private charts: Chart<any, number[], string>[] = [];

  ngAfterViewInit(): void {
    this.api.budgetSummary().subscribe((s) => {
      this.summary.set(s);
      this.drawCharts(s);
    });
  }

  ngOnDestroy(): void {
    this.charts.forEach((c) => c.destroy());
  }

  private drawCharts(s: BudgetSummary): void {
    this.charts.forEach((c) => c.destroy());
    this.charts = [
      new Chart(this.monthCanvas.nativeElement, {
        type: 'bar',
        data: {
          labels: s.byMonth.map((m) => m.month),
          datasets: [
            { label: 'Revenus', data: s.byMonth.map((m) => m.income), backgroundColor: '#1f8a5b' },
            { label: 'Dépenses', data: s.byMonth.map((m) => m.expenses), backgroundColor: '#c2413b' },
          ],
        },
        options: { responsive: true, plugins: { legend: { position: 'bottom' } } },
      }),
      new Chart(this.categoryCanvas.nativeElement, {
        type: 'doughnut',
        data: {
          labels: s.byCategory.map((c) => c.label),
          datasets: [{
            data: s.byCategory.map((c) => c.total),
            backgroundColor: ['#2f6fde', '#7b3fc4', '#1f8a5b', '#e0a43a', '#c2413b', '#3aa6b9', '#8a6d3b',
              '#d4579b', '#5c6b80', '#9bb53a', '#4b4fc4', '#b56a3a', '#9aa6b8'],
          }],
        },
        options: { responsive: true, plugins: { legend: { position: 'bottom' } } },
      }),
    ];
  }

  ask(): void {
    this.asking.set(true);
    this.error.set('');
    this.api.askBudget(this.question, this.includeNews).subscribe({
      next: (a) => {
        this.answer.set(a);
        this.asking.set(false);
      },
      error: (e) => {
        this.error.set(e.error?.detail ?? "L'analyse a échoué : le modèle local est-il démarré ?");
        this.asking.set(false);
      },
    });
  }
}
