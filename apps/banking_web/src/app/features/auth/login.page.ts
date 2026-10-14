import { ChangeDetectionStrategy, Component, inject, signal, viewChild } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { ButtonModule } from 'primeng/button';
import { InputTextModule } from 'primeng/inputtext';
import { Popover, PopoverModule } from 'primeng/popover';
import { PasswordModule } from 'primeng/password';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { AuthService } from '../../core/services/auth.service';

/** Konto demo: identyfikator/hasło/rola do szybkiego logowania bez wpisywania danych ręcznie. */
export interface DemoAccount {
  id: string;
  employeeId: string;
  password: string;
  role: string;
}

/**
 * Lista kont demo — aplikacja nie ma prawdziwego backendu auth, więc to jedyny
 * sposób na przetestowanie różnych ról bez ręcznego wpisywania danych logowania.
 */
const DEMO_ACCOUNTS: DemoAccount[] = [
  { id: 'admin', employeeId: 'PRC-00001', password: 'Admin12345', role: 'Administrator systemu' },
  { id: 'loans', employeeId: 'PRC-10234', password: 'Loans12345', role: 'Doradca kredytowy' },
  { id: 'risk', employeeId: 'PRC-20456', password: 'Risk123456', role: 'Analityk ryzyka' },
  { id: 'collections', employeeId: 'PRC-30678', password: 'Collect1234', role: 'Windykator' },
  {
    id: 'contact',
    employeeId: 'PRC-40890',
    password: 'Contact1234',
    role: 'Konsultant kanału kontaktu',
  },
  { id: 'compliance', employeeId: 'PRC-50123', password: 'Compliance1', role: 'Compliance / AML' },
];

@Component({
  selector: 'app-login-page',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ButtonModule,
    InputTextModule,
    PopoverModule,
    PasswordModule,
    TranslatePipe,
  ],
  templateUrl: './login.page.html',
  styleUrl: './login.page.css',
})
export class LoginPage {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly auth = inject(AuthService);

  protected readonly demoAccountsPopover = viewChild<Popover>('demoAccountsPopover');

  protected readonly form = this.fb.nonNullable.group({
    employeeId: ['', [Validators.required, Validators.pattern(/^PRC-\d{5}$/)]],
    password: ['', [Validators.required, Validators.minLength(8)]],
  });

  protected readonly demoAccounts = DEMO_ACCOUNTS;
  protected readonly submitting = signal(false);
  protected readonly errorKey = signal<string | null>(null);

  async onSubmit(): Promise<void> {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.errorKey.set(null);
    this.submitting.set(true);
    try {
      await this.auth.login(this.form.getRawValue());
      await this.router.navigate(['/dashboard']);
    } catch (error) {
      this.errorKey.set(this.resolveErrorKey(error));
    } finally {
      this.submitting.set(false);
    }
  }

  private resolveErrorKey(error: unknown): string {
    if (error instanceof HttpErrorResponse) {
      if (error.status === 401) return 'auth.login.error.invalidCredentials';
      if (error.status === 403) return 'auth.login.error.pendingApproval';
      if (error.status === 0 || error.status >= 500) return 'auth.login.error.unavailable';
    }
    return 'auth.login.error.generic';
  }

  selectDemoAccount(account: DemoAccount, event: Event): void {
    this.form.setValue({ employeeId: account.employeeId, password: account.password });
    this.form.markAllAsTouched();
    this.demoAccountsPopover()?.hide();
    event.stopPropagation();
  }
}
