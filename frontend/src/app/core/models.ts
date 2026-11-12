export type DocumentStatus = 'PENDING' | 'INDEXED' | 'FAILED';

export interface FinancialDocument {
  id: number;
  company: string;
  fiscalYear: number;
  filename: string;
  status: DocumentStatus;
  pageCount: number | null;
  chunkCount: number | null;
  errorMessage: string | null;
  createdAt: string;
}

export interface Source {
  type: 'report' | 'news';
  documentId?: number | null;
  company?: string | null;
  fiscalYear?: number | null;
  page?: number | null;
  articleId?: number | null;
  title?: string | null;
  url?: string | null;
  publishedAt?: string | null;
  excerpt: string;
  score: number;
}

export interface AskResponse {
  answer: string;
  sources: Source[];
  model: string;
}

export interface Transaction {
  id: number;
  date: string;
  label: string;
  amount: number;
  category: string;
  categoryLabel: string;
  categorizedBy: 'RULE' | 'AI' | 'USER';
}

export interface CategoryOption {
  code: string;
  label: string;
}

export interface ImportReport {
  read: number;
  imported: number;
  duplicates: number;
  categorizedByRules: number;
  categorizedByAi: number;
  warnings: string[];
}

export interface BudgetSummary {
  from: string;
  to: string;
  income: number;
  expenses: number;
  net: number;
  savingsRate: number;
  byCategory: { category: string; label: string; total: number; share: number }[];
  byMonth: { month: string; income: number; expenses: number; net: number }[];
  largestExpenses: { date: string; label: string; amount: number; category: string }[];
  transactionCount: number;
}

export interface NewsArticle {
  id: number;
  source: string;
  category: string;
  priority: number;
  region: string;
  language: string;
  title: string;
  summary: string;
  url: string;
  publishedAt: string;
}

export interface NewsDigest {
  id: number;
  periodStart: string;
  periodEnd: string;
  articleCount: number;
  content: string;
  model: string;
  createdAt: string;
}
