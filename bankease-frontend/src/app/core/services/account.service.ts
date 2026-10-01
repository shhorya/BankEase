import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { Account, Transaction } from '../models/models';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private base = `${environment.apiUrl}/accounts`;
  constructor(private http: HttpClient) {}

  getAccounts(): Observable<Account[]> { return this.http.get<Account[]>(this.base); }
  getAccount(accNum: string): Observable<Account> { return this.http.get<Account>(`${this.base}/${accNum}`); }
  getTransactions(accNum: string): Observable<Transaction[]> { return this.http.get<Transaction[]>(`${this.base}/${accNum}/transactions`); }

  transfer(body: { fromAccountNumber: string; toAccountNumber: string; amount: number; description?: string }): Observable<Transaction> {
    return this.http.post<Transaction>(`${this.base}/transfer`, body);
  }
  deposit(body: { accountNumber: string; amount: number; description?: string }): Observable<Transaction> {
    return this.http.post<Transaction>(`${this.base}/deposit`, body);
  }
  withdraw(body: { accountNumber: string; amount: number; description?: string }): Observable<Transaction> {
    return this.http.post<Transaction>(`${this.base}/withdraw`, body);
  }
}
