import { Component, OnInit, ViewChild, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatTableModule } from '@angular/material/table';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatSortModule } from '@angular/material/sort';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { HttpErrorResponse } from '@angular/common/http';
import { TransactionService } from '../../../core/services/transaction.service';
import { TransactionResponse } from '../../../core/models/transaction.model';
import { ApiResponse, PagedResponse } from '../../../core/models/api-response.model';

@Component({
  selector: 'app-transaction-history',
  standalone: true,
  imports: [
    CommonModule,
    RouterLink,
    MatTableModule,
    MatPaginatorModule,
    MatSortModule,
    MatInputModule,
    MatFormFieldModule,
    MatIconModule,
    MatButtonModule,
    MatChipsModule
  ],
  templateUrl: './transaction-history.component.html',
  styleUrl: './transaction-history.component.css'
})
export class TransactionHistoryComponent implements OnInit {
  private transactionService = inject(TransactionService);

  displayedColumns: string[] = ['id', 'type', 'amount', 'status', 'createdAt', 'actions'];
  dataSource: TransactionResponse[] = [];
  totalElements = 0;
  pageSize = 10;
  isLoading = true;

  ngOnInit() {
    this.loadTransactions();
  }

  loadTransactions(page: number = 0, size: number = this.pageSize) {
    this.isLoading = true;
    this.transactionService.getAllTransactions(page, size).subscribe({
      next: (res: ApiResponse<PagedResponse<TransactionResponse>>) => {
        if (res.success && res.data) {
          this.dataSource = res.data.content;
          this.totalElements = res.data.totalElements;
        }
        this.isLoading = false;
      },
      error: (err: HttpErrorResponse) => {
        console.error('Failed to load transactions:', err);
        this.isLoading = false;
      }
    });
  }

  onPageChange(event: PageEvent) {
    this.pageSize = event.pageSize;
    this.loadTransactions(event.pageIndex, event.pageSize);
  }

  getStatusColor(status: string): string {
    switch (status) {
      case 'COMPLETED': return 'bg-green-100 text-green-800';
      case 'PENDING': return 'bg-yellow-100 text-yellow-800';
      case 'FAILED': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  getTypeIcon(type: string): string {
    switch (type) {
      case 'DEPOSIT': return 'arrow_downward';
      case 'WITHDRAWAL': return 'arrow_upward';
      case 'TRANSFER': return 'swap_horiz';
      default: return 'receipt';
    }
  }
}
