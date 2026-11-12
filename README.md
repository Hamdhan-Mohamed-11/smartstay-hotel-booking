# SmartStay Hotel Booking System

Hamdhan Mohamed Ameen | Kingston University ID EUK00361749 | ESOFT E179021
CI6115 Programming III – Patterns and Algorithms, Coursework 1

A Java 17+ command-line application for room administration, guest registration, reservations
and detailed invoices. No external libraries, database or build tool are needed.

## Run

1. Install a JDK 17 or newer and the VS Code *Extension Pack for Java*.
2. Open the `SmartStay` folder in VS Code.
3. Windows PowerShell: `./run.ps1` &nbsp;|&nbsp; macOS/Linux: `bash run.sh`
   (or open `EUK00361749_Cli.java` and click **Run** above `main`).

Manual build from the project folder:

```sh
javac -d out src/smartstay/*.java
java -cp out smartstay.EUK00361749_Cli
```

**Administrator login (demo account):** username `admin`, password `admin123`.

Typical walkthrough: log in as administrator and add rooms → back to main menu → Guest services →
register → make a reservation (choose type, guests, dates, room, review price, confirm) → note the
booking ID (e.g. `BK0001`) → view/export invoice, search, modify or cancel.

## Tests

Windows: `./test.ps1` &nbsp;|&nbsp; macOS/Linux: `bash test.sh`

The regression runner executes 59 test cases (normal, boundary and invalid inputs) with a fixed
clock of 5 Nov 2026 10:00 Asia/Colombo, and writes the results table to
`docs/evidence/test-results.tsv`.

## Design summary

| Concept | Where |
|---|---|
| Inheritance / abstraction | `Person` (abstract) → `Guest`, `Admin`; `DiscountPolicy` (abstract) → `LongStayDiscount` |
| Polymorphism | `getRole()` / `getIdentifier()` / `toString()` on `Person`; `getDiscountRate()` on the policy |
| Encapsulation | private fields, immutable objects, store returns copies of its collections |
| Singleton pattern | `HotelSystem.getInstance()` – one system per data file |
| Builder pattern | `Booking.BookingBuilder` |
| Strategy pattern | `DiscountPolicy` injected into `Service` and `Invoice` |
| Data structures | `LinkedHashMap` for O(1) lookup by ID, `ArrayList` for results |
| Algorithms | merge sort (`BookingSorter`), linear search, interval-overlap availability check, 5-step billing |

The class diagram source is `docs/class-diagram.puml` (render with the VS Code PlantUML extension
or plantuml.com).

## Assumptions

- Included guests: Standard 2, Deluxe 2, Suite 3, Family Suite 4; at most two extra guests.
- Extra guest charge is per extra guest **per night**.
- The 12% long-stay discount (5+ nights) applies to the base room charge only; tax is charged on
  base + extras − discount.
- The LKR 10,000 advance is deducted from every active booking; if it exceeds the bill the
  difference is shown as a refund. Cancelling an eligible booking refunds the full advance.
- "2 days in advance" means check-in is at least two calendar days after today.
- Check-in time is 14:00 Asia/Colombo; changes/cancellation are allowed up to exactly 24 hours before.
- Stays are [check-in, check-out), so back-to-back bookings on the same room are allowed.
- Room status Reserved / Under Maintenance blocks all new bookings; confirmed bookings block only their own dates.
- Rooms with booking history cannot be deleted (set them to Under Maintenance instead).
- Payment is recorded, not processed electronically.

## Data

Records are saved to `data/smartstay.db` after every change; exported invoices go to
`data/invoices/`. The `data/` folder is git-ignored. Use fictional details only.
