# SmartStay Hotel Booking System

Hamdhan Mohamed Ameen | Kingston EUK00361749 | ESOFT E179021

Java 17 CLI coursework for CI6115 Programming III Patterns and Algorithms.
No third-party application libraries, database server or build tool is required.

## Run in VS Code

1. Install a **JDK 17 or newer**, not just a runtime. Install VS Code's Extension Pack for Java.
2. Extract the ZIP and open the **SmartStay** folder in VS Code.
3. Open Terminal > New Terminal. Confirm `java -version` and `javac -version`.
4. On Windows PowerShell run `./run.ps1`. On macOS/Linux run `bash run.sh`.
5. Select Administrator, add rooms, then select Guest and register a fictional guest.
6. Search rooms by type and dates, then book using a displayed room ID.
7. Record the generated booking ID to modify, cancel or invoice the reservation.

Manual compilation from the SmartStay folder:

```sh
javac -d out src/smartstay/*.java
java -cp out smartstay.EUK00361749_Cli
```

On Windows use `javac -d out src\smartstay\*.java` in Command Prompt, or use run.ps1.
In VS Code you can also open EUK00361749_Cli.java and select Run above main.
The working directory must be the SmartStay project root for the default data location.

## Tests

Windows: `./test.ps1`. macOS/Linux: `bash test.sh`.
Tests use a fixed clock at 5 November 2026, 10:00 Asia/Colombo, so expected boundaries remain reproducible.
The included run passed 49 cases. Re-running writes docs/evidence/test-results.tsv.
Tests create disposable temporary files and do not touch your application's data.

## Data and workflow

The first launch starts empty. Production clock is the actual current time in Asia/Colombo.
`data/smartstay.db` stores room, guest and booking records after every successful mutation.
An OS file lock prevents two CLI processes from writing the same data path simultaneously.
A temporary snapshot replaces the prior file; an unsupported atomic move falls back to normal replacement.
Back up the data folder manually. Base64 encoding is delimiter protection, **not encryption**.
A damaged/unknown file stops startup instead of silently overwriting it.
Invoices exported through menu 9 are saved under data/invoices.

The administrator and guest menus are workflow demonstrations, not authenticated accounts.
Use fictional data. The application does not process electronic payments or issue actual bank refunds.
At booking, YES records receipt of the advance; cancellation records a refund due.

## Assumptions

- Included capacities: Standard 2, Deluxe 2, Suite 3, Family Suite 4; at most two additional guests.
- Bed type describes the room; package capacity determines pricing. No adult/child distinction.
- Extra guests are charged for each night. All monetary amounts are LKR.
- Five or more nights earn 12% off the base room charge only. Tax applies to discounted base plus extras.
- The full LKR 10,000 advance is collected per booking. An overpayment becomes a refund, never negative payable.
- Eligible cancellation returns the full advance. Cancelled bookings remain in history.
- Book at least two **calendar days** ahead. Check-in is 14:00 Asia/Colombo.
- Changes and cancellation are allowed exactly 24 hours before check-in, blocked after that instant.
- A changed stay must also satisfy the two-day advance rule.
- Date intervals are [check-in, check-out); adjacent stays are allowed.
- Manual RESERVED and UNDER_MAINTENANCE block all dates; confirmed bookings reserve only their own dates.
- Update room/type/bed/status only when no current or future confirmed stay uses it.
- Rooms with any booking history cannot be deleted. Retain them with a manual unavailable status.
- Rates and tax percentages are fixed by package, as prescribed in the assignment.
- Registration checks the format of a supplied identity string, not government identity authenticity.
- One local installation is one inventory; branches, authentication, networking and payment processing are outside scope.

## Project contents

- src/smartstay: 12 application Java files with the Kingston ID prefix.
- test/smartstay: dependency-free regression runner.
- docs: editable PlantUML diagrams, PNG exports and detailed class-member inventory.
- docs/evidence: real execution transcripts, their rendered screenshots, regression results and exported invoice.
- GITHUB_SETUP.md: upload and sharing steps for your own GitHub account.

Report and PDF are supplied alongside the ZIP. Put your actual GitHub URL into section 5 of the Word report after upload and re-export the PDF. No repository URL has been invented.

## Evidence and limitations

CLI transcripts were captured from actual Java processes using a pseudo-terminal. The PNG evidence is a browser screenshot of those captured transcripts or exact source text, not a screenshot of VS Code. The original text files are included for audit. The report makes no claim of a VS Code execution in this environment. Run locally in VS Code to confirm your installed JDK and, if required by your lecturer, capture additional IDE screenshots.

File storage is suitable for this single-user coursework, not a production hotel platform. It lacks encryption, authentication, payment settlement, a tamper-evident audit log, full corruption recovery and multi-user database transactions.
