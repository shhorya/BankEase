import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { Observable } from 'rxjs';
import { AccountService } from '../../core/services/account.service';
import { AuthService } from '../../core/services/auth.service';
import { Account, Transaction } from '../../core/models/models';
import { environment } from '../../../environments/environment';
import { ICN } from '../../core/icons';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  nav = [
    { id: 'dashboard', label: 'Dashboard', icon: 'home' },
    { id: 'accounts', label: 'Accounts', icon: 'wallet' },
    { id: 'transfer', label: 'Transfer', icon: 'transfer' },
    { id: 'transactions', label: 'Transaction history', icon: 'file' },
    { id: 'bills', label: 'Pay bills', icon: 'file' },
    { id: 'loans', label: 'Loans', icon: 'wallet' },
    { id: 'invest', label: 'Invest', icon: 'trend' },
    { id: 'activity', label: 'Activity log', icon: 'clock' }
  ];
  modes = ['Transfer', 'Deposit', 'Withdraw'];
  filters = [{ v: 'ALL', l: 'All' }, { v: 'DEPOSIT', l: 'Deposits' }, { v: 'WITHDRAWAL', l: 'Withdrawals' }, { v: 'TRANSFER', l: 'Transfers' }, { v: 'BILL', l: 'Bills' }];
  ranges = [{ d: 365, l: '1 year' }, { d: 180, l: '6 month' }, { d: 90, l: '3 month' }, { d: 30, l: '1 month' }];

  page = 'dashboard'; collapsed = false; q = ''; filter = 'ALL'; range = 365;
  accounts: Account[] = []; transactions: Transaction[] = []; selTx: Transaction | null = null;
  billers: any[] = []; loans: any[] = []; pending: any[] = []; investments: any[] = []; audit: any[] = [];
  selAcc = ''; toAcc = ''; desc = '';
  amount: number | null = null; billerId: number | null = null; tenure = 12; invType = 'FD';
  totalBalance = 0; loading = true; error = ''; toasts: string[] = [];
  drawer = false; mode = 'Transfer'; step = 'form'; drawErr = '';
  today = new Date().toLocaleDateString('en-IN', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });

  private api = environment.apiUrl;
  private cache: Record<string, SafeHtml> = {};

  constructor(private accountService: AccountService, public auth: AuthService,
              private http: HttpClient, private sanitizer: DomSanitizer) {}

  ic(n: string): SafeHtml { return this.cache[n] ??= this.sanitizer.bypassSecurityTrustHtml(ICN[n] ?? ''); }

  get isAdmin() { return this.auth.currentUser()?.role === 'ADMIN'; }
  get role() { return this.isAdmin ? 'Administrator' : 'Customer'; }
  get firstName() { return (this.auth.currentUser()?.fullName || '').split(' ')[0]; }
  get greet() { const h = new Date().getHours(); return h < 12 ? 'Good morning' : h < 17 ? 'Good afternoon' : 'Good evening'; }
  get curAcc() { return this.accounts.find(a => a.accountNumber === this.selAcc); }
  get curBal() { return this.curAcc?.balance ?? 0; }
  get newBal() { const a = Number(this.amount) || 0; return this.mode === 'Deposit' ? this.curBal + a : this.curBal - a; }

  get shown() {
    const q = this.q.toLowerCase();
    return this.transactions.filter(t =>
      (this.filter === 'ALL' || t.transactionType.startsWith(this.filter)) &&
      (!q || (t.description || t.transactionType).toLowerCase().includes(q)));
  }
  get list() { return this.page === 'dashboard' ? this.shown.slice(0, 4) : this.shown; }

  get monthTotal() {
    const n = new Date();
    return this.transactions.filter(t => { const d = new Date(t.createdAt); return d.getMonth() === n.getMonth() && d.getFullYear() === n.getFullYear(); })
      .reduce((s, t) => s + t.amount, 0);
  }
  get pendingLoans() { return this.loans.filter(l => l.status === 'PENDING'); }
  get pendingSum() { return this.pendingLoans.reduce((s, l) => s + Number(l.principal), 0); }
  get spent() { return this.transactions.filter(t => !this.isCredit(t.transactionType)).reduce((s, t) => s + t.amount, 0); }

  get chart() {
    const lim = Date.now() - this.range * 864e5;
    const v = this.transactions.filter(t => +new Date(t.createdAt) >= lim).map(t => t.balanceAfter).reverse();
    const vals = v.length > 1 ? v : [this.curBal, this.curBal];
    const mn = Math.min(...vals), span = (Math.max(...vals) - mn) || 1;
    const pts = vals.map((x, i) => `${(i * 600 / (vals.length - 1)).toFixed(1)},${(140 - (x - mn) / span * 110).toFixed(1)}`);
    const line = 'M' + pts.join(' L');
    return { line, area: line + ' L600,160 L0,160 Z' };
  }

  ngOnInit() {
    this.http.get<any[]>(`${this.api}/billers`).subscribe(b => this.billers = b);
    this.refresh();
  }

  refresh() {
    this.loadAccounts(); this.loadLoans(); this.loadInvestments(); this.loadAudit();
    if (this.isAdmin) this.loadPending();
  }
  loadAccounts() {
    this.accountService.getAccounts().subscribe({
      next: (accs) => {
        this.accounts = accs;
        this.totalBalance = accs.reduce((s, a) => s + a.balance, 0);
        if (!this.selAcc && accs.length) this.selAcc = accs[0].accountNumber;
        if (this.selAcc) this.loadTxns(this.selAcc);
        this.loading = false;
      },
      error: () => { this.error = 'Could not load accounts'; this.loading = false; }
    });
  }
  loadTxns(n: string) {
    this.accountService.getTransactions(n).subscribe({
      next: (t) => { this.transactions = t; this.selTx = t[0] ?? null; },
      error: () => this.error = 'Could not load transactions'
    });
  }
  loadLoans() { this.http.get<any[]>(`${this.api}/loans`).subscribe(l => this.loans = l); }
  loadPending() { this.http.get<any[]>(`${this.api}/loans/pending`).subscribe(l => this.pending = l); }
  loadInvestments() { this.http.get<any[]>(`${this.api}/investments`).subscribe(i => this.investments = i); }
  loadAudit() { this.http.get<any[]>(`${this.api}/audit`).subscribe(a => this.audit = a); }

  go(p: string) { this.page = p; this.error = ''; this.amount = null; this.q = ''; }
  navTo(id: string) { id === 'transfer' ? this.openDrawer('Transfer') : this.go(id); }
  pick(a: Account) { this.selAcc = a.accountNumber; this.loadTxns(a.accountNumber); }
  toast(m: string) { this.toasts.push(m); setTimeout(() => this.toasts.shift(), 2600); }
  toggleTheme() {
    const h = document.documentElement;
    h.setAttribute('data-theme', h.getAttribute('data-theme') === 'dark' ? 'light' : 'dark');
  }

  // ---- drawer ----
  openDrawer(m: string) {
    this.mode = m; this.step = 'form'; this.drawErr = ''; this.amount = null; this.desc = ''; this.toAcc = ''; this.drawer = true;
  }
  closeDrawer() { this.drawer = false; }
  review() {
    const a = Number(this.amount) || 0; this.drawErr = '';
    if (a <= 0) { this.drawErr = 'Enter an amount greater than zero'; return; }
    if (this.mode !== 'Deposit' && a > this.curBal) { this.drawErr = `Insufficient balance in account ${this.selAcc}`; return; }
    if (this.mode === 'Transfer' && !this.toAcc.trim()) { this.drawErr = 'Enter the recipient account number'; return; }
    this.step = 'review';
  }
  submit() {
    const amount = Number(this.amount), description = this.desc || this.mode;
    const req = this.mode === 'Transfer'
      ? this.accountService.transfer({ fromAccountNumber: this.selAcc, toAccountNumber: this.toAcc.trim(), amount, description })
      : this.mode === 'Deposit'
        ? this.accountService.deposit({ accountNumber: this.selAcc, amount, description })
        : this.accountService.withdraw({ accountNumber: this.selAcc, amount, description });
    req.subscribe({
      next: () => {
        this.step = 'done'; this.refresh();
        setTimeout(() => { this.closeDrawer(); this.toast('Transaction completed successfully'); }, 1500);
      },
      error: (e) => { this.step = 'form'; this.drawErr = this.errText(e); }
    });
  }

  // ---- other actions ----
  private run(req: Observable<unknown>, ok: string) {
    this.error = '';
    req.subscribe({
      next: () => { this.toast(ok); this.amount = null; this.refresh(); },
      error: (e) => this.error = this.errText(e)
    });
  }
  private errText(e: any): string {
    const b = e?.error;
    if (b?.message) return b.message;
    if (b && typeof b === 'object') return Object.values(b).join(', ');
    return 'Something went wrong';
  }
  payBill() { this.run(this.http.post(`${this.api}/bills/pay`, { accountNumber: this.selAcc, billerId: this.billerId, amount: Number(this.amount) }), 'Bill paid'); }
  applyLoan() { this.run(this.http.post(`${this.api}/loans/apply`, { accountNumber: this.selAcc, principal: Number(this.amount), tenureMonths: Number(this.tenure) }), 'Loan application submitted'); }
  approve(id: number) { this.run(this.http.post(`${this.api}/loans/${id}/approve`, {}), 'Loan approved'); }
  invest() { this.run(this.http.post(`${this.api}/investments`, { accountNumber: this.selAcc, type: this.invType, amount: Number(this.amount) }), 'Investment successful'); }

  isCredit(t: string) { return t === 'DEPOSIT' || t === 'TRANSFER_IN'; }
  fmt(n: number) { return '₹' + Number(n).toLocaleString('en-IN', { minimumFractionDigits: 2, maximumFractionDigits: 2 }); }
  whole(n: number) { return Math.floor(n).toLocaleString('en-IN'); }
  cents(n: number) { return n.toFixed(2).split('.')[1]; }
}
