import React, { FormEvent, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { CalendarDays, LogIn, LogOut, Plane, Search, ShieldCheck, Star, UserPlus } from 'lucide-react';
import './styles.css';

type AuthResponse = {
  userId: string;
  email: string;
  accessToken: string;
  refreshToken: string;
  expiresAt: string;
};

type FlightOffer = {
  id: string;
  providerCode: string;
  airline: string;
  flightNumber: string;
  price: number;
  currency: string;
  departureAt: string;
  arrivalAt: string;
  transfersCount: number;
  bookingUrl: string;
};

type SearchResponse = {
  searchRequestId: string;
  offers: FlightOffer[];
};

type SearchHistory = {
  id: string;
  originIata: string;
  destinationIata: string;
  departureDate: string;
  passengers: number;
  createdAt: string;
};

const today = new Date().toISOString().slice(0, 10);

function readSavedAuth(): AuthResponse | null {
  const saved = localStorage.getItem('auth');
  if (!saved) {
    return null;
  }
  try {
    return JSON.parse(saved) as AuthResponse;
  } catch {
    localStorage.removeItem('auth');
    return null;
  }
}

function App() {
  const [auth, setAuth] = useState<AuthResponse | null>(readSavedAuth);
  const [mode, setMode] = useState<'login' | 'register'>('login');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [firstName, setFirstName] = useState('');
  const [lastName, setLastName] = useState('');
  const [origin, setOrigin] = useState('MOW');
  const [destination, setDestination] = useState('AER');
  const [departureDate, setDepartureDate] = useState(today);
  const [passengers, setPassengers] = useState(1);
  const [offers, setOffers] = useState<FlightOffer[]>([]);
  const [history, setHistory] = useState<SearchHistory[]>([]);
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);

  const apiHeaders = useMemo<Record<string, string>>(() => {
    const headers: Record<string, string> = {};
    if (auth) {
      headers.Authorization = `Bearer ${auth.accessToken}`;
    }
    return headers;
  }, [auth]);

  async function request<T>(url: string, options: RequestInit = {}): Promise<T> {
    const response = await fetch(url, {
      ...options,
      headers: {
        'Content-Type': 'application/json',
        ...(options.headers ?? {}),
      },
    });
    const payload = await response.json().catch(() => ({}));
    if (!response.ok) {
      throw new Error(payload.error ?? 'Ошибка запроса');
    }
    return payload;
  }

  async function submitAuth(event: FormEvent) {
    event.preventDefault();
    setMessage('');
    setLoading(true);
    try {
      const payload =
        mode === 'login'
          ? { email, password }
          : { email, password, firstName, lastName };
      const result = await request<AuthResponse>(`/api/auth/${mode === 'login' ? 'login' : 'register'}`, {
        method: 'POST',
        body: JSON.stringify(payload),
      });
      setAuth(result);
      localStorage.setItem('auth', JSON.stringify(result));
      setMessage('');
      await loadHistory(result.accessToken);
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Ошибка авторизации');
    } finally {
      setLoading(false);
    }
  }

  async function loadHistory(token = auth?.accessToken) {
    if (!token) {
      return;
    }
    const result = await request<SearchHistory[]>('/api/search/history', {
      headers: { Authorization: `Bearer ${token}` },
    });
    setHistory(result);
  }

  async function submitSearch(event: FormEvent) {
    event.preventDefault();
    if (!auth) {
      setMessage('Сначала выполните вход');
      return;
    }
    setMessage('');
    setLoading(true);
    const params = new URLSearchParams({
      origin,
      destination,
      departureDate,
      passengers: String(passengers),
    });
    try {
      const result = await request<SearchResponse>(`/api/search/flights?${params.toString()}`, {
        headers: apiHeaders,
      });
      setOffers(result.offers);
      await loadHistory();
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Ошибка поиска');
    } finally {
      setLoading(false);
    }
  }

  async function saveFavorite(offerId: string) {
    try {
      await request(`/api/search/favorites?flightOfferId=${offerId}`, {
        method: 'POST',
        headers: apiHeaders,
      });
      setMessage('Предложение добавлено в избранное');
    } catch (error) {
      setMessage(error instanceof Error ? error.message : 'Ошибка сохранения');
    }
  }

  async function logout() {
    if (auth) {
      await fetch('/api/auth/logout', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken: auth.refreshToken }),
      }).catch(() => undefined);
    }
    setAuth(null);
    setOffers([]);
    setHistory([]);
    localStorage.removeItem('auth');
  }

  if (!auth) {
    return (
      <main className="auth-screen">
        <section className="auth-panel">
          <div className="brand-mark">
            <Plane size={30} />
          </div>
          <h1>Поиск авиабилетов</h1>
          <p className="auth-subtitle">Войдите или создайте аккаунт, чтобы искать билеты и сохранять историю запросов.</p>

          <div className="segmented">
            <button className={mode === 'login' ? 'active' : ''} onClick={() => setMode('login')}>
              <LogIn size={18} /> Вход
            </button>
            <button className={mode === 'register' ? 'active' : ''} onClick={() => setMode('register')}>
              <UserPlus size={18} /> Регистрация
            </button>
          </div>

          <form className="auth-form" onSubmit={submitAuth}>
            <label>
              Email
              <input value={email} onChange={(event) => setEmail(event.target.value)} placeholder="name@example.com" />
            </label>
            <label>
              Пароль
              <input type="password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="Минимум 8 символов" />
            </label>
            {mode === 'register' && (
              <div className="two-columns">
                <label>
                  Имя
                  <input value={firstName} onChange={(event) => setFirstName(event.target.value)} />
                </label>
                <label>
                  Фамилия
                  <input value={lastName} onChange={(event) => setLastName(event.target.value)} />
                </label>
              </div>
            )}
            <button className="primary-button" disabled={loading}>
              {loading ? 'Подождите...' : mode === 'login' ? 'Войти' : 'Создать аккаунт'}
            </button>
          </form>
          {message && <p className="error-box">{message}</p>}
        </section>
      </main>
    );
  }

  return (
    <main className="app-shell">
      <header className="topbar">
        <div className="topbar-inner">
          <div className="app-title">
            <div className="small-mark">
              <Plane size={22} />
            </div>
            <div>
              <h1>Поиск авиабилетов</h1>
              <p>Микросервисное приложение: Gateway, Auth, Search, PostgreSQL, Kafka</p>
            </div>
          </div>
          <div className="user-area">
            <span>{auth.email}</span>
            <button onClick={logout}>
              <LogOut size={17} /> Выйти
            </button>
          </div>
        </div>
      </header>

      <section className="summary-band">
        <div>
          <ShieldCheck size={22} />
          <span>JWT проверяется на API Gateway</span>
        </div>
        <div>
          <CalendarDays size={22} />
          <span>История поисков хранится в PostgreSQL</span>
        </div>
        <div>
          <Star size={22} />
          <span>Предложения можно сохранять в избранное</span>
        </div>
      </section>

      <div className="workspace">
        <section className="search-panel">
          <div className="section-heading">
            <Search size={21} />
            <h2>Параметры поиска</h2>
          </div>
          <form onSubmit={submitSearch} className="search-form">
            <label>
              Откуда
              <input value={origin} maxLength={3} onChange={(event) => setOrigin(event.target.value.toUpperCase())} />
            </label>
            <label>
              Куда
              <input value={destination} maxLength={3} onChange={(event) => setDestination(event.target.value.toUpperCase())} />
            </label>
            <label>
              Дата вылета
              <input type="date" value={departureDate} onChange={(event) => setDepartureDate(event.target.value)} />
            </label>
            <label>
              Пассажиры
              <input type="number" min={1} max={9} value={passengers} onChange={(event) => setPassengers(Number(event.target.value))} />
            </label>
            <button className="primary-button" disabled={loading}>{loading ? 'Поиск...' : 'Найти билеты'}</button>
          </form>
          {message && <p className="notice-box">{message}</p>}
        </section>

        <section className="results-panel">
          <div className="section-heading">
            <Plane size={21} />
            <h2>Найденные предложения</h2>
          </div>
          {offers.length === 0 ? (
            <div className="empty-state">
              Выберите направление и дату, затем запустите поиск. Для примера доступны коды MOW, AER и LED.
            </div>
          ) : (
            <div className="offers-list">
              {offers.map((offer) => (
                <article key={offer.id} className="offer-card">
                  <div>
                    <p className="muted">{offer.providerCode}</p>
                    <h3>{offer.airline} {offer.flightNumber}</h3>
                    <p>{new Date(offer.departureAt).toLocaleString('ru-RU')} {'->'} {new Date(offer.arrivalAt).toLocaleString('ru-RU')}</p>
                    <p>Пересадки: {offer.transfersCount}</p>
                  </div>
                  <div className="offer-price">
                    <strong>{offer.price.toLocaleString('ru-RU')} {offer.currency}</strong>
                    <button onClick={() => saveFavorite(offer.id)}>
                      <Star size={17} /> В избранное
                    </button>
                  </div>
                </article>
              ))}
            </div>
          )}
        </section>

        <aside className="history-panel">
          <h2>История поиска</h2>
          {history.length === 0 ? (
            <p className="muted">История появится после первого поиска.</p>
          ) : (
            history.map((item) => (
              <div className="history-item" key={item.id}>
                <strong>{item.originIata} {'->'} {item.destinationIata}</strong>
                <span>{item.departureDate}, пассажиров: {item.passengers}</span>
              </div>
            ))
          )}
        </aside>
      </div>
    </main>
  );
}

createRoot(document.getElementById('root')!).render(<App />);
