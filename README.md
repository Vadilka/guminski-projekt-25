# TCP Chat Server – Concurrency Project

Projekt zrealizowany w ramach przedmiotu **Programowanie współbieżne**.

Celem projektu było zaprojektowanie i zaimplementowanie aplikacji wykorzystującej
współbieżność w praktycznym scenariuszu typu klient–serwer. Program umożliwia
równoczesną obsługę wielu użytkowników oraz prezentuje zastosowanie mechanizmów
synchronizacji w środowisku wielowątkowym.

---

## Opis projektu
Aplikacja składa się z wielowątkowego serwera TCP oraz prostego klienta konsolowego.
Każde połączenie klienta jest obsługiwane w osobnym wątku, co pozwala na równoległą
komunikację wielu użytkowników bez wzajemnego blokowania połączeń.

Serwer wykorzystuje dodatkowe wątki techniczne odpowiedzialne za monitorowanie
stanu aplikacji oraz asynchroniczne logowanie zdarzeń, co poprawia czytelność
architektury oraz stabilność działania programu.

---

## Funkcjonalności
- obsługa wielu klientów jednocześnie (TCP)
- komendy dostępne po stronie klienta:
  - `/help` – wyświetlenie listy dostępnych komend
  - `/nick <name>` – ustawienie nicku użytkownika
  - `/rename <name>` – zmiana nicku
  - `/who` – lista użytkowników online
  - `/all <message>` – wiadomość do wszystkich użytkowników
  - `/msg <user> <msg>` – wiadomość prywatna
  - `/stats` – statystyki serwera (liczba klientów, liczba wiadomości, uptime)
  - `/quit` – rozłączenie z serwerem
- asynchroniczne logowanie zdarzeń do pliku
- monitoring stanu serwera wykonywany cyklicznie co 10 sekund

---

## Architektura wątków
Projekt wykorzystuje kilka typów wątków, z których każdy pełni określoną rolę:

- **Wątek główny serwera**  
  Odpowiada za nasłuchiwanie połączeń TCP oraz tworzenie nowych wątków klientów.

- **Wątki klientów (`ClientHandler`)**  
  Każdy klient jest obsługiwany w osobnym wątku odpowiedzialnym za komunikację
  z użytkownikiem oraz interpretację komend.

- **Wątek logowania (`LoggerWorker`)**  
  Odpowiada za zapis zdarzeń do pliku logów przy użyciu bezpiecznej kolejki
  `BlockingQueue`.

- **Wątek monitorujący (`MonitorWorker`)**  
  Co 10 sekund prezentuje aktualny stan serwera, w tym liczbę klientów online,
  liczbę obsłużonych wiadomości oraz czas działania serwera (uptime).

---

## Synchronizacja i bezpieczeństwo współbieżne
W projekcie zastosowano następujące mechanizmy synchronizacji:

- `ConcurrentHashMap` – bezpieczne przechowywanie listy aktywnych klientów
- `BlockingQueue` – kolejka do asynchronicznego logowania zdarzeń
- `AtomicLong` – licznik obsłużonych wiadomości
- `synchronized` – bezpieczna zmiana nicku użytkownika

Zastosowanie powyższych mechanizmów pozwala uniknąć problemów takich jak
race condition oraz zapewnia stabilne działanie aplikacji w środowisku
wielowątkowym.

---

## Technologie
- Java 21+
- TCP sockets
- Wielowątkowość (`Thread`, `Runnable`)
- Kolekcje współbieżne (`ConcurrentHashMap`, `BlockingQueue`)
- Mechanizmy atomowe (`AtomicLong`)

---

## Uruchomienie projektu

### Kompilacja
```bash
javac -encoding UTF-8 -d out src/*.java
```
### Uruchomienie serwera
```bash
java -cp out Server 5555
```
### Uruchomienie klienta
```bash
java -cp out Client 127.0.0.1 5555
```

---

### Przykładowy scenariusz testowy

1. Uruchomić serwer.
2. Uruchomić kilku klientów w osobnych terminalach.
3. Ustawić nick za pomocą /nick.
4. Wysłać wiadomość globalną (/all).
5. Wysłać wiadomość prywatną (/msg).
6. Sprawdzić statystyki serwera (/stats).
7. Zakończyć połączenie komendą /quit.

---

### Autor

```bash
Vladyslav Kosolap / 121266 
```
