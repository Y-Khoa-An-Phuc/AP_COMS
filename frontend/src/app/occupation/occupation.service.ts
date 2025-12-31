import { Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';
import { ApiClient } from '../core/api/api-client.service';
import { Occupation, OccupationRequest } from '../model/occupation';

@Injectable({
  providedIn: 'root',
})
export class OccupationService {
  constructor(private api: ApiClient) {}

  list(): Observable<Occupation[]> {
    return this.api.get<any>('/occupations/list').pipe(
      map((response) => {
        const occupations = response?.data || [];
        return occupations.map((occ: any) => ({
          occCode: occ.id?.toString() || '',
          occName: occ.name || '',
          description: occ.description,
        }));
      })
    );
  }

  create(payload: OccupationRequest): Observable<Occupation> {
    return this.api.post<any>('/occupations', payload).pipe(
      map((response) => {
        const data = response?.data || {};
        return {
          occCode: data.id?.toString() || '',
          occName: data.name || '',
          description: data.description || '',
        };
      })
    );
  }

  update(id: string, payload: OccupationRequest): Observable<Occupation> {
    return this.api.put<any>(`/occupations/${id}`, payload).pipe(
      map((response) => {
        const data = response?.data || {};
        return {
          occCode: data.id?.toString() || id,
          occName: data.name || '',
          description: data.description || '',
        };
      })
    );
  }

  delete(id: string): Observable<any> {
    return this.api.delete<any>(`/occupations/${id}`);
  }

  getFilterMetadata(): Observable<any> {
    return this.api.get<any>('/occupations/filter-metadata');
  }

  getFilterValues(endpoint: string): Observable<any> {
    return this.api.get<any>(endpoint);
  }
}
