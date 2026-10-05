import { Component } from '@angular/core';
import { ShellPage } from './core/layout/shell.page';

@Component({
  selector: 'app-root',
  imports: [ShellPage],
  template: '<app-shell />',
})
export class App {}
