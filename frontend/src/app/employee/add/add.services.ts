import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ApiClient } from '../../core/api/api-client.service';
import { Occupation } from '../../model/occupation';

@Injectable({
  providedIn: 'root'
})
export class AddEmployeeService {
  constructor(private api: ApiClient) {}

  loadOccupations(): Observable<Occupation[]> {
    // TODO: Replace mock fallback when backend stabilizes
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
}
