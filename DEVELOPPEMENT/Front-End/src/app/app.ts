import { Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NotificationOutletComponent } from './shared/notification-outlet/notification-outlet';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, NotificationOutletComponent],
  templateUrl: './app.html',
  styleUrl: './app.css'
})
export class App {}
