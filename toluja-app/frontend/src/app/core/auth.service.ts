import { Injectable, computed, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { map, tap } from 'rxjs';
import { AuthUser, LoginResponse } from './models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenKey = 'toluja_jwt';
  private readonly userKey = 'toluja_user';

  private logged = signal<boolean>(!!localStorage.getItem(this.tokenKey));
  private userSignal = signal<AuthUser | null>(this.readStoredUser());

  readonly user = computed(() => this.userSignal());

  constructor(private http: HttpClient) {}

  login(username: string, password: string) {
    return this.http.post<LoginResponse>('/api/auth/login', { username, password }).pipe(
      tap((response) => this.setSession(response.token, response.user)),
      map(() => void 0)
    );
  }

  setSession(token: string, user: AuthUser): void {
    localStorage.setItem(this.tokenKey, token);
    localStorage.setItem(this.userKey, JSON.stringify(user));
    this.userSignal.set(user);
    this.logged.set(true);
  }

  getToken(): string | null {
    return localStorage.getItem(this.tokenKey);
  }

  getUser(): AuthUser | null {
    return this.userSignal();
  }

  isAdmin(): boolean {
    return this.userSignal()?.role === 'ADMIN';
  }

  isLoggedIn(): boolean {
    return this.logged();
  }

  logout(): void {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
    this.userSignal.set(null);
    this.logged.set(false);
  }

  private readStoredUser(): AuthUser | null {
    const raw = localStorage.getItem(this.userKey);
    if (!raw) return null;
    try {
      return JSON.parse(raw) as AuthUser;
    } catch {
      return null;
    }
  }
}
