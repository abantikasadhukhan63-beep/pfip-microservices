import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { UserDto } from '../models/user.model';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private http = inject(HttpClient);
  private apiUrl = `${environment.apiUrl}/users`;

  getCurrentUser(): Observable<ApiResponse<UserDto>> {
    return this.http.get<ApiResponse<UserDto>>(`${this.apiUrl}/me`);
  }

  getUserById(id: number): Observable<ApiResponse<UserDto>> {
    return this.http.get<ApiResponse<UserDto>>(`${this.apiUrl}/${id}`);
  }
}
