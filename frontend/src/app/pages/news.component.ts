import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { marked } from 'marked';

import { ApiService } from '../core/api.service';
import { NewsArticle, NewsDigest } from '../core/models';

@Component({
  selector: 'app-news',
  standalone: true,
  imports: [DatePipe, FormsModule],
  template: `
    <h1>Revue de presse mondiale</h1>
    <p class="subtitle">
      Revues diplomatiques et think tanks en priorité, puis presse économique et banques centrales.
      Historique glissant de {{ historyDays() }} jours ({{ articlesInWindow() }} articles indexés).
    </p>

    <section class="card">
      <div class="row" style="justify-content: space-between">
        <h2>Synthèse de l'IA</h2>
        <div class="row">
          <select [(ngModel)]="digestHours">
            <option [ngValue]="24">Dernières 24 h</option>
            <option [ngValue]="72">3 derniers jours</option>
            <option [ngValue]="168">7 derniers jours</option>
          </select>
          <button (click)="generateDigest()" [disabled]="generating()">
            {{ generating() ? 'Rédaction en cours…' : 'Générer la synthèse' }}
          </button>
        </div>
      </div>
      @if (digest(); as d) {
        <p class="muted">
          {{ d.createdAt | date: 'd MMMM y, HH:mm' }} · {{ d.articleCount }} articles · modèle {{ d.model }}
        </p>
        <div class="markdown" [innerHTML]="digestHtml()"></div>
      } @else {
        <p class="muted">Aucune synthèse pour l'instant. Lancez une collecte puis générez la synthèse.</p>
      }
      @if (error()) {
        <p class="error">{{ error() }}</p>
      }
    </section>

    <section class="card">
      <div class="row" style="justify-content: space-between">
        <h2>Articles</h2>
        <div class="row">
          <select [(ngModel)]="category" (ngModelChange)="loadArticles()">
            <option value="">Toutes les sources</option>
            <option value="diplomatie">Revues diplomatiques</option>
            <option value="international">International</option>
            <option value="economie">Économie</option>
            <option value="banque_centrale">Banques centrales</option>
          </select>
          <select [(ngModel)]="days" (ngModelChange)="loadArticles()">
            <option [ngValue]="7">7 jours</option>
            <option [ngValue]="30">30 jours</option>
            <option [ngValue]="90">3 mois</option>
            <option [ngValue]="365">1 an</option>
          </select>
          <input type="text" placeholder="Rechercher dans les titres" [(ngModel)]="query" (keyup.enter)="loadArticles()" />
          <button class="secondary" (click)="refresh()" [disabled]="refreshing()">
            {{ refreshing() ? 'Collecte lancée' : 'Collecter maintenant' }}
          </button>
        </div>
      </div>

      @for (a of articles(); track a.id) {
        <article class="article">
          <div class="meta">
            <span class="badge" [class.diplo]="a.priority === 1">{{ a.source }}</span>
            <span class="muted">{{ a.publishedAt | date: 'd MMM y' }} · {{ a.region }}</span>
          </div>
          <a [href]="a.url" target="_blank" rel="noopener" class="title">{{ a.title }}</a>
          @if (a.summary) {
            <p>{{ a.summary.length > 280 ? a.summary.slice(0, 280) + '…' : a.summary }}</p>
          }
        </article>
      } @empty {
        <p class="muted">Aucun article sur cette période.</p>
      }
    </section>
  `,
  styles: `
    .article { padding: 12px 0; border-bottom: 1px solid var(--line); }
    .article p { margin: 6px 0 0; color: var(--muted); font-size: 14px; }
    .meta { display: flex; gap: 10px; align-items: center; margin-bottom: 4px; }
    .title { font-weight: 600; color: var(--text); text-decoration: none; }
    .title:hover { color: var(--accent); }
  `,
})
export class NewsComponent implements OnInit {
  private readonly api = inject(ApiService);
  private readonly sanitizer = inject(DomSanitizer);

  articles = signal<NewsArticle[]>([]);
  digest = signal<NewsDigest | null>(null);
  historyDays = signal(365);
  articlesInWindow = signal(0);
  generating = signal(false);
  refreshing = signal(false);
  error = signal('');

  category = '';
  days = 7;
  query = '';
  digestHours = 24;

  digestHtml = computed<SafeHtml>(() => {
    const d = this.digest();
    return d ? this.sanitizer.bypassSecurityTrustHtml(marked.parse(d.content, { async: false }) as string) : '';
  });

  ngOnInit(): void {
    this.loadArticles();
    this.api.latestDigest().subscribe({ next: (d) => this.digest.set(d), error: () => this.digest.set(null) });
    this.loadStats();
  }

  loadStats(): void {
    this.api.newsStats().subscribe((s) => {
      this.historyDays.set(s.historyDays);
      this.articlesInWindow.set(s.articlesInWindow);
    });
  }

  loadArticles(): void {
    this.api.news({ category: this.category, q: this.query, days: this.days }).subscribe((a) => this.articles.set(a));
  }

  refresh(): void {
    this.refreshing.set(true);
    this.api.refreshNews().subscribe({
      next: () => setTimeout(() => {
        this.refreshing.set(false);
        this.loadArticles();
        this.loadStats();
      }, 15000),
      error: () => this.refreshing.set(false),
    });
  }

  generateDigest(): void {
    this.generating.set(true);
    this.error.set('');
    this.api.generateDigest(this.digestHours).subscribe({
      next: () => this.api.latestDigest().subscribe((d) => {
        this.digest.set(d);
        this.generating.set(false);
      }),
      error: (e) => {
        this.error.set(e.error?.detail ?? 'La synthèse a échoué : le modèle local est-il démarré ?');
        this.generating.set(false);
      },
    });
  }
}
