import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AccountService } from '../../core/services/account.service';
import { AuthService } from '../../core/services/auth.service';
import { Account, Transaction } from '../../core/models/models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  accounts: Account[] = [];
  transactions: Transaction[] = [];
  totalBalance = 0;
  loading = true;
  error = '';

  constructor(private accountService: AccountService, public auth: AuthService) {}

  ngOnInit() {
    this.accountService.getAccounts().subscribe({
      next: (accs) => {
        this.accounts = accs;
        this.totalBalance = accs.reduce((s, a) => s + a.balance, 0);
        if (accs.length) this.loadTxns(accs[0].accountNumber);
        this.loading = false;
      },
      error: () => { this.error = 'Could not load accounts'; this.loading = false; }
    });
  }

  loadTxns(accNum: string) {
    this.accountService.getTransactions(accNum).subscribe({
      next: (t) => this.transactions = t,
      error: () => this.error = 'Could not load transactions'
    });
  }

  isCredit(type: string) { return type === 'DEPOSIT' || type === 'TRANSFER_IN'; }
  fmt(n: number) { return '₹' + n.toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
  logout() { this.auth.logout(); }
}