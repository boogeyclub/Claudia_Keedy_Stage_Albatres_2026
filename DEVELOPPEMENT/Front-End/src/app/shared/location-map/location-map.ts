import { Component, ElementRef, afterNextRender, effect, inject, input, viewChild } from '@angular/core';
import * as L from 'leaflet';
import { TranslationService } from '../../core/i18n/translation.service';

/**
 * Read-only map showing one GPS point: the exact position of a lot, or the meeting point of a visit.
 *
 * The pin is drawn as a vector marker so no image asset has to be downloaded, and the tiles come from
 * the public OpenStreetMap service. The component also offers the two links a participant actually
 * needs on a phone: open the point in a map application, or start a route to it.
 */
@Component({
  selector: 'app-location-map',
  templateUrl: './location-map.html',
  styleUrl: './location-map.css'
})
export class LocationMapComponent {
  protected readonly i18n = inject(TranslationService);

  readonly latitude = input.required<number>();
  readonly longitude = input.required<number>();
  readonly label = input<string | null>(null);
  /** Tailwind height class of the map area, so a screen can choose how much room the map takes. */
  readonly heightClass = input('h-64');

  private readonly mapHost = viewChild.required<ElementRef<HTMLElement>>('mapHost');
  private map: L.Map | null = null;
  private marker: L.CircleMarker | null = null;

  constructor() {
    afterNextRender(() => {
      this.createMap();
    });

    // The pin can move (a visit point was re-proposed): the effect keeps the map in sync.
    effect(() => {
      const latitude = this.latitude();
      const longitude = this.longitude();
      if (this.map) {
        this.placeMarker(latitude, longitude);
      }
    });
  }

  /** OpenStreetMap page of the point, opened in a new tab. */
  protected openStreetMapUrl(): string {
    const latitude = this.latitude();
    const longitude = this.longitude();
    return `https://www.openstreetmap.org/?mlat=${latitude}&mlon=${longitude}#map=17/${latitude}/${longitude}`;
  }

  /** Turn-by-turn route to the point, handled by the visitor's own map application. */
  protected directionsUrl(): string {
    return `https://www.google.com/maps/dir/?api=1&destination=${this.latitude()},${this.longitude()}`;
  }

  private createMap(): void {
    if (this.map) {
      return;
    }

    const latitude = this.latitude();
    const longitude = this.longitude();
    this.map = L.map(this.mapHost().nativeElement, {
      center: [latitude, longitude],
      zoom: 15,
      scrollWheelZoom: false
    });

    L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
      maxZoom: 19,
      attribution: '&copy; OpenStreetMap'
    }).addTo(this.map);

    this.placeMarker(latitude, longitude);
  }

  private placeMarker(latitude: number, longitude: number): void {
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
