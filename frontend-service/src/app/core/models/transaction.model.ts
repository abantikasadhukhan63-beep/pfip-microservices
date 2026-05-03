export interface TransactionResponse {
  id: number;
  userId: number;
  type: string;
  amount: number;
  currency: string;
  description: string;
  status: string;
  referenceNumber: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTransactionRequest {
  type: string;
  amount: number;
  currency: string;
  description: string;
}

export interface TransactionSummary {
  totalDeposits: number;
  totalWithdrawals: number;
  totalTransfers: number;
  netBalance: number;
}
