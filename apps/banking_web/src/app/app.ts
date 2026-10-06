import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ToastModule } from 'primeng/toast';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, ToastModule],
  template: `
    <p-toast
      id="app-toast"
      position="bottom-right"
      [pt]="{ closeButton: { id: 'app-toast-close-btn' } }"
    />
    <router-outlet />
  `,
})
export class App {}
