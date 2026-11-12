import { DatePipe, DecimalPipe } from '@angular/common';
import { Component, Input } from '@angular/core';

import { Source } from '../core/models';

/** Liste des extraits cités par l'IA : passages de rapports et articles de presse. */
@Component({
  selector: 'app-sources',
  standalone: true,
  imports: [DatePipe, DecimalPipe],
  template: `
    @if (sources.length) {
      <div class="sources">
        <span class="muted">Sources utilisées</span>
        @for (s of sources; track $index) {
          <div class="source" [class.news]="s.type === 'news'" [class.report]="s.type === 'report'">
            @if (s.type === 'report') {
              <strong>[R{{ reportIndex(s) }}] {{ s.company }} {{ s.fiscalYear }}, page {{ s.page }}</strong>
            } @else {
              <strong>[N{{ newsIndex(s) }}] <a [href]="s.url" target="_blank" rel="noopener">{{ s.title }}</a></strong>
              <span> · {{ s.publishedAt | date: 'd MMM y' }}</span>
            }
            <span> · pertinence {{ s.score | number: '1.2-2' }}</span>
            <div>{{ s.excerpt }}</div>
          </div>
        }
      </div>
    }
  `,
})
export class SourcesComponent {
  @Input({ required: true }) sources: Source[] = [];

  reportIndex(s: Source): number {
    return this.sources.filter((x) => x.type === 'report').indexOf(s) + 1;
  }

  newsIndex(s: Source): number {
    return this.sources.filter((x) => x.type === 'news').indexOf(s) + 1;
  }
}
