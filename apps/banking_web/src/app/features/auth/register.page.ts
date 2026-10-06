import { Component, inject } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  ValidatorFn,
  Validators,
} from '@angular/forms';
import { ButtonModule } from 'primeng/button';
import { CheckboxModule } from 'primeng/checkbox';
import { DatePickerModule } from 'primeng/datepicker';
import { InputTextModule } from 'primeng/inputtext';
import { MultiSelectModule } from 'primeng/multiselect';
import { PasswordModule } from 'primeng/password';
import { SelectModule } from 'primeng/select';
import { TranslatePipe } from '../../core/services/translate.pipe';
import { NotificationService } from '../../core/services/notification.service';

/** Cross-field validator: confirmPassword must equal password. */
const passwordsMatchValidator: ValidatorFn = (group: AbstractControl): ValidationErrors | null => {
  const password = group.get('password')?.value;
  const confirmPassword = group.get('confirmPassword')?.value;
  if (!password || !confirmPassword) return null;
  return password === confirmPassword ? null : { passwordMismatch: true };
};

@Component({
  selector: 'app-register-page',
  standalone: true,
  imports: [
    ReactiveFormsModule,
    RouterLink,
    ButtonModule,
    CheckboxModule,
    DatePickerModule,
    InputTextModule,
    MultiSelectModule,
    PasswordModule,
    SelectModule,
    TranslatePipe,
  ],
  templateUrl: './register.page.html',
  styleUrl: './register.page.css',
})
export class RegisterPage {
  private readonly fb = inject(FormBuilder);
  private readonly router = inject(Router);
  private readonly notify = inject(NotificationService);

  /** Czas wyświetlenia toastu po rejestracji (ms) — użytkownik musi zdążyć przeczytać informację o zatwierdzeniu przez administratora. */
  private static readonly REGISTER_TOAST_LIFE_MS = 5000;

  protected readonly departments = [
    'IT',
    'Ryzyko Kredytowe',
    'Obsługa Klienta',
    'Compliance',
    'Operacje',
  ];

  protected readonly businessLineOptions = [
    { key: 'retail', labelKey: 'auth.register.businessLine.retail' },
    { key: 'corporate', labelKey: 'auth.register.businessLine.corporate' },
    { key: 'risk', labelKey: 'auth.register.businessLine.risk' },
    { key: 'collections', labelKey: 'auth.register.businessLine.collections' },
    { key: 'compliance', labelKey: 'auth.register.businessLine.compliance' },
    { key: 'it', labelKey: 'auth.register.businessLine.it' },
  ];

  protected readonly form = this.fb.group({
    personal: this.fb.group({
      firstName: this.fb.control('', { validators: [Validators.required] }),
      lastName: this.fb.control('', { validators: [Validators.required] }),
      email: this.fb.control('', { validators: [Validators.required, Validators.email] }),
      phone: this.fb.control('', { validators: [Validators.required] }),
    }),
    employment: this.fb.group({
      employeeId: this.fb.control('', {
        validators: [Validators.required, Validators.pattern(/^PRC-\d{5}$/)],
      }),
      department: this.fb.control<string | null>(null, { validators: [Validators.required] }),
      branch: this.fb.control('', { validators: [Validators.required] }),
      position: this.fb.control('', { validators: [Validators.required] }),
      supervisor: this.fb.control('', { validators: [Validators.required] }),
      startDate: this.fb.control<Date | null>(null, { validators: [Validators.required] }),
    }),
    industry: this.fb.group({
      businessLines: this.fb.control<string[]>([], {
        validators: [Validators.required, Validators.minLength(1)],
      }),
      certifications: this.fb.control(''),
      licenseNumber: this.fb.control(''),
    }),
    account: this.fb.group(
      {
        password: this.fb.control('', {
          validators: [Validators.required, Validators.minLength(8)],
        }),
        confirmPassword: this.fb.control('', { validators: [Validators.required] }),
        acceptPolicy: this.fb.control(false, { validators: [Validators.requiredTrue] }),
      },
      { validators: [passwordsMatchValidator] },
    ),
  });

  protected get personalGroup() {
    return this.form.controls.personal;
  }

  protected get employmentGroup() {
    return this.form.controls.employment;
  }

  protected get industryGroup() {
    return this.form.controls.industry;
  }

  protected get accountGroup() {
    return this.form.controls.account;
  }

  onSubmit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.notify.success('toast.registerSuccess', undefined, RegisterPage.REGISTER_TOAST_LIFE_MS);
    this.router.navigate(['/login']);
  }
}
