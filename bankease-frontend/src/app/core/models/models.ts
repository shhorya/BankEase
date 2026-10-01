export interface Account {
  id: number; accountNumber: string; accountType: string;
  balance: number; currency: string; status: string;
}
export interface Transaction {
  id: number; transactionType: string; amount: number;
  balanceAfter: number; description: string; status: string; createdAt: string;
}
export interface AuthResponse {
  token: string; tokenType: string; userId: number;
  fullName: string; email: string; role: string;
}
