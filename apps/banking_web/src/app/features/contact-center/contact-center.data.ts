import {
  ChatMessage,
  ContactHistoryEntry,
  CustomerProfile,
  ServiceDisposition,
} from './contact-center.model';

export const CUSTOMER_PROFILE: CustomerProfile = {
  name: 'Jan Wiśniewski',
  segment: 'Retail — Premium',
  loansCount: 2,
  cardsCount: 1,
  riskRating: 'BBB',
  openCases: 1,
};

export const INITIAL_CHAT: ChatMessage[] = [
  { from: 'customer', text: 'Dzień dobry, chciałbym podnieść limit na karcie.', time: '09:12' },
  { from: 'agent', text: 'Dzień dobry, sprawdzam Pana profil. Chwileczkę.', time: '09:12' },
  {
    from: 'agent',
    text: 'Mogę zaproponować podniesienie limitu dziennego do 8000 PLN.',
    time: '09:13',
  },
];

export const DISPOSITIONS: ServiceDisposition[] = [
  {
    id: 'DSP-771',
    typeKey: 'cards.limitDaily',
    status: 'inProgress',
    linkedCase: 'CS-10231',
    date: '2026-10-05',
  },
  {
    id: 'DSP-765',
    typeKey: 'ebanking.unlock',
    status: 'resolved',
    linkedCase: 'CS-10219',
    date: '2026-10-02',
  },
  {
    id: 'DSP-758',
    typeKey: 'loans.decisionMove',
    status: 'new',
    linkedCase: 'CS-10225',
    date: '2026-09-29',
  },
];

export const CONTACT_HISTORY: ContactHistoryEntry[] = [
  {
    id: 'CH-4401',
    channelKey: 'contact.channel.chat',
    date: '2026-10-05 09:12',
    summary: 'Prośba o podniesienie limitu karty',
  },
  {
    id: 'CH-4390',
    channelKey: 'contact.channel.phone',
    date: '2026-09-21 13:40',
    summary: 'Pytanie o harmonogram spłat kredytu',
  },
  {
    id: 'CH-4375',
    channelKey: 'contact.channel.branch',
    date: '2026-08-30 10:05',
    summary: 'Wizyta w oddziale — aktualizacja danych',
  },
];
