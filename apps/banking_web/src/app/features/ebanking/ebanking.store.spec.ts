import { TestBed } from '@angular/core/testing';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ApiBaseUrlService } from '../../core/services/api-base-url.service';
import { EBANKING_USERS } from './ebanking.data';
import { EbankingDevice, EbankingUser } from './ebanking.model';
import { EbankingStore } from './ebanking.store';

describe('EbankingStore', () => {
  let store: EbankingStore;
  let httpMock: HttpTestingController;
  let apiBaseUrl: ApiBaseUrlService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    httpMock = TestBed.inject(HttpTestingController);
    apiBaseUrl = TestBed.inject(ApiBaseUrlService);
    store = TestBed.inject(EbankingStore);
  });

  afterEach(() => httpMock.verify());

  it('fetches /ebanking/users from banking_backend and populates the signal', () => {
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/ebanking/users`);
    expect(req.request.method).toBe('GET');
    req.flush(EBANKING_USERS);

    expect(store.users()).toEqual(EBANKING_USERS);
  });

  it('requestUnlockOtp() posts to unlock/request-otp and returns the challenge', async () => {
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/ebanking/users`).flush(EBANKING_USERS);

    const promise = store.requestUnlockOtp('EB-1001');
    const req = httpMock.expectOne(
      `${apiBaseUrl.bankingBackend}/ebanking/EB-1001/unlock/request-otp`,
    );
    expect(req.request.method).toBe('POST');
    req.flush({ otpId: 'otp-1', expiresAt: '2026-10-15T10:05:00Z' });
    const result = await promise;

    expect(result.otpId).toBe('otp-1');
  });

  it('confirmUnlock() posts to unlock/confirm and replaces the user with the backend response', async () => {
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/ebanking/users`).flush(EBANKING_USERS);

    const unlocked: EbankingUser = { ...EBANKING_USERS[0], accessStatus: 'active' };
    const promise = store.confirmUnlock('EB-1001', 'otp-1', '123456');
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/ebanking/EB-1001/unlock/confirm`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ otpId: 'otp-1', code: '123456' });
    req.flush(unlocked);
    const result = await promise;

    expect(result.accessStatus).toBe('active');
    expect(store.users().find((u) => u.id === 'EB-1001')?.accessStatus).toBe('active');
  });

  it('listDevices() gets the device registry proxy', async () => {
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/ebanking/users`).flush(EBANKING_USERS);

    const devices: EbankingDevice[] = [
      {
        deviceId: 'DEV-0002',
        ownerId: 'EB-1001',
        model: 'Samsung Galaxy S23',
        lastSeenAt: '2026-10-14T17:00:00Z',
        trusted: true,
      },
    ];
    const promise = store.listDevices('EB-1001');
    const req = httpMock.expectOne(`${apiBaseUrl.bankingBackend}/ebanking/EB-1001/devices`);
    expect(req.request.method).toBe('GET');
    req.flush(devices);
    const result = await promise;

    expect(result).toEqual(devices);
  });

  it('confirmDeviceReset() posts to devices/{id}/reset/confirm', async () => {
    httpMock.expectOne(`${apiBaseUrl.bankingBackend}/ebanking/users`).flush(EBANKING_USERS);

    const promise = store.confirmDeviceReset('EB-1001', 'DEV-0002', 'otp-2', '654321');
    const req = httpMock.expectOne(
      `${apiBaseUrl.bankingBackend}/ebanking/EB-1001/devices/DEV-0002/reset/confirm`,
    );
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ otpId: 'otp-2', code: '654321' });
    req.flush(null);
    await promise;
  });
});
