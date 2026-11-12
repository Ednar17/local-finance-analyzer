import { Component } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    <div class="shell">
      <aside class="sidebar">
        <div class="brand">
          <span class="logo">◆</span>
          <div>
            <strong>Finance Analyzer</strong>
            <small>IA locale, aucune donnée ne sort</small>
          </div>
        </div>
        <nav>
          <a routerLink="/presse" routerLinkActive="active">Revue de presse</a>
          <a routerLink="/rapports" routerLinkActive="active">Rapports d'entreprises</a>
          <a routerLink="/budget" routerLinkActive="active">Budget</a>
          <a routerLink="/operations" routerLinkActive="active">Opérations</a>
        </nav>
        <p class="disclaimer">
          Analyses informatives générées par un modèle local. Elles ne constituent pas un conseil en investissement.
        </p>
      </aside>
      <main class="content">
        <router-outlet />
      </main>
    </div>
  `,
  styleUrl: './app.component.scss',
})
export class AppComponent {}
