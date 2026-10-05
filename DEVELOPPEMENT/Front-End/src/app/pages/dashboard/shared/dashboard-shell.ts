import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { DashboardHeaderComponent } from '../../../shared/dashboard-header/dashboard-header';

@Component({
  selector: 'app-dashboard-shell',
  imports: [RouterOutlet, DashboardHeaderComponent],
  templateUrl: './dashboard-shell.html',
  styleUrl: './dashboard-shell.css'
})
export class DashboardShellComponent {}
