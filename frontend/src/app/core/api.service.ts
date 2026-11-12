import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';

import {
  AskResponse,
  BudgetSummary,
  CategoryOption,
  FinancialDocument,
  ImportReport,
  NewsArticle,
  NewsDigest,
  Transaction,
} from './models';

@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api';

  // Rapports d'entreprises
  documents(): Observable<FinancialDocument[]> {
    return this.http.get<FinancialDocument[]>(`${this.base}/documents`);
  }

  uploadDocument(file: File, company: string, fiscalYear: number): Observable<FinancialDocument> {
    const form = new FormData();
    form.append('file', file);
    form.append('company', company);
    form.append('fiscalYear', String(fiscalYear));
    return this.http.post<FinancialDocument>(`${this.base}/documents`, form);
  }

  deleteDocument(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/documents/${id}`);
  }

  askReports(question: string, documentIds: number[], includeNews: boolean): Observable<AskResponse> {
    return this.http.post<AskResponse>(`${this.base}/reports/ask`, { question, documentIds, includeNews });
  }

  // Budget personnel
  importTransactions(file: File): Observable<ImportReport> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ImportReport>(`${this.base}/transactions/import`, form);
  }

  transactions(): Observable<Transaction[]> {
    return this.http.get<Transaction[]>(`${this.base}/transactions`);
  }

  categories(): Observable<CategoryOption[]> {
    return this.http.get<CategoryOption[]>(`${this.base}/transactions/categories`);
  }

  updateCategory(id: number, category: string): Observable<Transaction> {
    return this.http.patch<Transaction>(`${this.base}/transactions/${id}/category`, { category });
  }

  budgetSummary(): Observable<BudgetSummary> {
    return this.http.get<BudgetSummary>(`${this.base}/budget/summary`);
  }

  askBudget(question: string, includeNews: boolean): Observable<AskResponse> {
    return this.http.post<AskResponse>(`${this.base}/budget/ask`, { question, includeNews });
  }

  // Revue de presse
  news(filters: { category?: string; q?: string; days?: number }): Observable<NewsArticle[]> {
    let params = new HttpParams().set('limit', 80).set('days', filters.days ?? 7);
    if (filters.category) params = params.set('category', filters.category);
    if (filters.q) params = params.set('q', filters.q);
    return this.http.get<NewsArticle[]>(`${this.base}/news`, { params });
  }

  newsStats(): Observable<{ historyDays: number; articlesInWindow: number }> {
    return this.http.get<{ historyDays: number; articlesInWindow: number }>(`${this.base}/news/stats`);
  }

  refreshNews(): Observable<unknown> {
    return this.http.post(`${this.base}/news/refresh`, {});
  }

  latestDigest(): Observable<NewsDigest | null> {
    return this.http.get<NewsDigest | null>(`${this.base}/news/digest/latest`);
  }

  generateDigest(hours: number): Observable<unknown> {
    return this.http.post(`${this.base}/news/digest`, {}, { params: { hours } });
  }
}
