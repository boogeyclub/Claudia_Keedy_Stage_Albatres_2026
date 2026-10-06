import { Component, ElementRef, afterNextRender, inject, input, model, signal, viewChild } from '@angular/core';
import * as L from 'leaflet';
import { TranslationService } from '../../core/i18n/translation.service';
import { NotificationService } from '../../core/notifications/notification.service';

/**
 * Lets a participant drop a GPS pin.
 *
 * Two ways in, because both are needed in the field: tap the map where the place is, or use the
 * browser geolocation when the seller or the buyer is standing there. The selected point is exposed
 * as a two-way model so the surrounding form stays the single source of truth.
 */
@Component({
  selector: 'app-location-picker',
  templateUrl: './location-picker.html',
  styleUrl: './location-picker.css'
})
export class LocationPickerComponent {
  protected readonly i18n = inject(TranslationService);

  /** Selected point; null until the user picks one. */
  readonly latitude = model<number | null>(null);
  readonly longitude = model<number | null>(null);
  /** Optional human name of the point, typed by the user. */
  readonly label = model<string | null>(null);
  /** Initial view: the lot position, the participant's city, or a default view of Cameroon. */
  readonly initialLatitude = input<number | null>(null);
  readonly initialLongitude = input<number | null>(null);
  readonly heightClass = input('h-72');

  protected readonly isLocating = signal(false);

  private readonly mapHost = viewChild.required<ElementRef<HTMLElement>>('mapHost');
  private readonly notifications = inject(NotificationService);
  private map: L.Map | null = null;
  private marker: L.CircleMarker | null = null;

  constructor() {
    afterNextRender(() => this.createMap());
  }

  /** Uses the browser geolocation: the most accurate way to pin "where I am". */
  protected useCurrentPosition(): void {
    if (this.isLocating()) {
      return;
    }
    if (!navigator.geolocation) {
      this.notifications.warning({ key: 'market.location.geolocationUnsupported' });
      return;
    }

    this.isLocating.set(true);
    navigator.geolocation.getCurrentPosition(
      (position) => {
        this.isLocating.set(false);
        this.place(position.coords.latitude, position.coords.longitude);
      },
      () => {
        this.isLocating.set(false);
        this.notifications.warning({ key: 'market.location.geolocationDenied' });
      },
      { enableHighAccuracy: true, timeout: 10000 }
    );
  }

  /** Clears the pin so the form can require a fresh one. */
  protected clear(): void {
    this.latitude.set(null);
    this.longitude.set(null);
    this.label.set(null);
    if (this.map && this.marker) {
      this.map.removeLayer(this.marker);
      this.marker = null;
    }
  }

  private createMap(): void {
    if (this.map) {
      return;
    }

    const startLatitude = this.latitude() ?? this.initialLatitude() ?? 4.0511;
    const startLongitude = this.longitude() ?? this.initialLongitude() ?? 9.7679;
    this.map = L.map(this.mapHost().nativeElement, {
      center: [startLatitude, startLongitude],
      // Wide enough to let the user navigate to the hamlet, close enough to place a gate.
      zoom: this.latitude() === null ? 7 : 15,
      scrollWheelZoom: true
    });

    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap'
    }).addTo(this.map);

    this.map.on('click', (event: L.LeafletMouseEvent) => {
      this.place(event.latlng.lat, event.latlng.lng);
    });

    if (this.latitude() !== null && this.longitude() !== null) {
      this.place(this.latitude() as number, this.longitude() as number);
    }
  }

  private place(latitude: number, longitude: number): void {
    this.latitude.set(rounded(latitude));
    this.longitude.set(rounded(longitude));

    if (!this.map) {
      return;
    }
    if (this.marker) {
      this.marker.setLatLng([latitude, longitude]);
    } else {
      this.marker = L.circleMarker([latitude, longitude], {
        radius: 9,
        color: '#3b2117',
        weight: 3,
        fillColor: '#e0a800',
        fillOpacity: 1
      }).addTo(this.map);
    }
    this.map.setView([latitude, longitude], Math.max(this.map.getZoom(), 14));
  }
}

/** Six decimals is about ten centimetres — far more than any cocoa field needs. */
function rounded(value: number): number {
  return Math.round(value * 1000000) / 1000000;
}
