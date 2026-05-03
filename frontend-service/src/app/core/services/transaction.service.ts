import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { ApiResponse, PagedResponse } from '../models/api-response.model';
import { TransactionResponse, CreateTransactionRequest, TransactionSummary } from '../models/transaction.model';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class TransactionService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/transactions`;

  createTransaction(request: CreateTransactionRequest): Observable<ApiResponse<TransactionResponse>> {
    return this.http.post<ApiResponse<TransactionResponse>>(this.apiUrl, request);
  }

  getAllTransactions(page: number = 0, size: number = 20, sortBy: string = 'createdAt', direction: string = 'desc'): Observable<ApiResponse<PagedResponse<TransactionResponse>>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString())
      .set('sortBy', sortBy)
      .set('direction', direction);
    return this.http.get<ApiResponse<PagedResponse<TransactionResponse>>>(this.apiUrl, { params });
  }

  getTransactionById(id: number): Observable<ApiResponse<TransactionResponse>> {
    return this.http.get<ApiResponse<TransactionResponse>>(`${this.apiUrl}/${id}`);
  }

  getTransactionsByType(type: string, page: number = 0, size: number = 20): Observable<ApiResponse<PagedResponse<TransactionResponse>>> {
    const params = new HttpParams()
      .set('page', page.toString())
      .set('size', size.toString());
    return this.http.get<ApiResponse<PagedResponse<TransactionResponse>>>(`${this.apiUrl}/type/${type}`, { params });
  }

  getTransactionSummary(): Observable<ApiResponse<TransactionSummary>> {
    return this.http.get<ApiResponse<TransactionSummary>>(`${this.apiUrl}/summary`);
  }
}
