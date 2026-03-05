import { Injectable, OnDestroy } from '@angular/core';
import { Client, IMessage } from '@stomp/stompjs';
import { Subject } from 'rxjs';

export interface ConversionEvent {
  documentId: number;
  status: string;
  gltfPath?: string;
}

@Injectable({ providedIn: 'root' })
export class WebSocketService implements OnDestroy {
  private client: Client;
  private conversionEvents$ = new Subject<ConversionEvent>();

  conversionUpdates$ = this.conversionEvents$.asObservable();

  constructor() {
    const wsUrl = window.location.protocol === 'https:'
      ? `wss://${window.location.host}/ws`
      : `ws://${window.location.host}/ws`;

    this.client = new Client({
      brokerURL: wsUrl,
      reconnectDelay: 5000,
    });

    this.client.onConnect = () => {
      this.client.subscribe('/topic/conversions', (msg: IMessage) => {
        try {
          this.conversionEvents$.next(JSON.parse(msg.body));
        } catch { /* ignore parse errors */ }
      });
    };

    this.client.activate();
  }

  ngOnDestroy() {
    this.client.deactivate();
  }
}
