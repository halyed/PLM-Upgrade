import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface ConnectorStatus {
  id: string;
  name: string;
  state: string;
  detail: string | null;
}

@Injectable({ providedIn: 'root' })
export class IntegrationService {
  constructor(private http: HttpClient) {}

  getConnectors(): Observable<ConnectorStatus[]> {
    return this.http.get<ConnectorStatus[]>('/api/integration/connectors');
  }

  getSummary(): Observable<Record<string, number>> {
    return this.http.get<Record<string, number>>('/api/integration/connectors/summary');
  }
}
