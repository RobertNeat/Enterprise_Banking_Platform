import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { MessageService } from 'primeng/api';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { EBANKING_USERS } from './ebanking.data';
import { EbankingPage } from './ebanking.page';

describe('EbankingPage', () => {
  let httpMock: HttpTestingController;
  let apiBaseUrl: ApiBaseUrlService;

  function createPage(): EbankingPage {
    TestBed.configureTestingModule({
      imports: [EbankingPage],
      providers: [provideHttpClient(), provideHttpClientTesting(), MessageService],
    });
    httpMock = TestBed.inject(HttpTestingController);
    apiBaseUrl = TestBed.inject(ApiBaseUrlService);
    const fixture = TestBed.createComponent(EbankingPage);
    fixture.detectChanges();
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/ebanking/users`).flush(EBANKING_USERS);
    return fixture.componentInstance;
  }

  afterEach(() => httpMock.verify());

  it('unlock() on an active user is a no-op (UX guard before any HTTP call)', () => {
    const page = createPage();
    const activeUser = EBANKING_USERS.find((u) => u.id === 'EB-1002')!;

    page.unlock(activeUser);

    expect(page['otpDialogOpen']()).toBe(false);
  });

  it('unlock() on a locked user opens the OTP dialog at step 1', () => {
    const page = createPage();
    const lockedUser = EBANKING_USERS.find((u) => u.id === 'EB-1001')!;

    page.unlock(lockedUser);

    expect(page['otpDialogOpen']()).toBe(true);
    expect(page['otpStep']()).toBe(1);
    expect(page['otpPurpose']()).toBe('unlock');
  });

  it('sendOtp() requests the challenge and advances to step 2', async () => {
    const page = createPage();
    const lockedUser = EBANKING_USERS.find((u) => u.id === 'EB-1001')!;
    page.unlock(lockedUser);

    const promise = page.sendOtp();
    const req = httpMock.expectOne(
      `${apiBaseUrl.bankingBackend}/ebanking/EB-1001/unlock/request-otp`,
    );
    expect(req.request.method).toBe('POST');
    req.flush({ otpId: 'otp-1', expiresAt: '2026-10-15T10:05:00Z' });
    await promise;

    expect(page['otpStep']()).toBe(2);
    expect(page['otpId']()).toBe('otp-1');
  });

  it('confirmOtp() submits the code and closes the dialog on success', async () => {
    const page = createPage();
    const lockedUser = EBANKING_USERS.find((u) => u.id === 'EB-1001')!;
    page.unlock(lockedUser);
    const sendPromise = page.sendOtp();
    httpMock
      .expectOne(`${apiBaseUrl.bankingBackend}/ebanking/EB-1001/unlock/request-otp`)
      .flush({ otpId: 'otp-1', expiresAt: '2026-10-15T10:05:00Z' });
    await sendPromise;
    page['otpCode'].set('123456');

    const confirmPromise = page.confirmOtp();
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/ebanking/EB-1001/unlock/confirm`);
    expect(req.request.body).toEqual({ otpId: 'otp-1', code: '123456' });
    req.flush({ ...lockedUser, accessStatus: 'active' });
    await confirmPromise;

    expect(page['otpDialogOpen']()).toBe(false);
    expect(page['users']().find((u) => u.id === 'EB-1001')?.accessStatus).toBe('active');
  });

  it('confirmOtp() shows an inline error and keeps the dialog open on a wrong code', async () => {
    const page = createPage();
    const lockedUser = EBANKING_USERS.find((u) => u.id === 'EB-1001')!;
    page.unlock(lockedUser);
    const sendPromise = page.sendOtp();
    httpMock
      .expectOne(`${apiBaseUrl.bankingBackend}/ebanking/EB-1001/unlock/request-otp`)
      .flush({ otpId: 'otp-1', expiresAt: '2026-10-15T10:05:00Z' });
    await sendPromise;
    page['otpCode'].set('000000');

    const confirmPromise = page.confirmOtp();
    httpMock
      .expectOne(`${apiBaseUrl.bankingBackend}/ebanking/EB-1001/unlock/confirm`)
      .flush({ message: 'forbidden' }, { status: 403, statusText: 'Forbidden' });
    await confirmPromise;

    expect(page['otpDialogOpen']()).toBe(true);
    expect(page['otpError']()).toBe(true);
  });

  it('resetDevice() fetches devices and opens the dialog for device-reset purpose', async () => {
    const page = createPage();
    const mobileUser = EBANKING_USERS.find((u) => u.id === 'EB-1001')!;

    const promise = page.resetDevice(mobileUser);
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/ebanking/EB-1001/devices`);
    req.flush([
      {
        deviceId: 'DEV-0002',
        ownerId: 'EB-1001',
        model: 'Samsung Galaxy S23',
        lastSeenAt: '2026-10-14T17:00:00Z',
        trusted: true,
      },
    ]);
    await promise;

    expect(page['otpDialogOpen']()).toBe(true);
    expect(page['otpPurpose']()).toBe('resetDevice');
    expect(page['otpSelectedDeviceId']()).toBe('DEV-0002');
  });
});
