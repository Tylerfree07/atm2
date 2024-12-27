
# ATM Fee Management System

The **ATM Fee Management System** is a comprehensive banking system implemented in Java. It provides functionalities like account management, transactions, balance viewing, deposits, withdrawals, currency exchange, tax estimation, and more, using a database for persistent storage.
Main file is in the src(1) folder under the name atmfee.java.

---

## Features

- **User Management**
  - Create new accounts with unique account numbers and PINs.
  - Store user data such as balance, name, and currency in a SQLite database.
  - BASE ACCOUNT NUMBER: 12345678
  - BASE PIN NUMBER:1234 

- **Transactions**
  - Deposit, withdraw, and transfer funds between accounts.
  - Maintain and display transaction history (up to 10 transactions per user).
  - Download transactions as CSV or PDF file.
  - Save transaction history in a SQLite database.

- **Currency Exchange**
  - Convert balances and transactions between multiple currencies.
  - Dynamically load exchange rates from a `rates.txt` file.

- **Tax Estimator**
  - Tax estimator based on income and tax-free contributions.

- **Settings**
  - Update user details: PIN, name,preferred currency, and email.

- **Loan Calculator**
  -Calculate the intrest youll have to pay on loans and what a monthly payment would look like

---

## Technologies Used

- **Programming Language**: Java
- **Database**: SQLite (for user and transaction data)
- **File I/O**: Used for loading exchange rates.
- **API**: used exchnage.api to update `rates.txt` every day 

---

## Prerequisites

1. Java JDK 8 or higher.
2. SQLite library for Java (`sqlite-jdbc`).
3. `rates.txt` file with exchange rates in the format
4. JSON libary
5. ITEXT pdf libary

## Documentation
~ https://docs.google.com/document/d/1hhWKqE6zr4tPKKVbau2W22E8YheD095OtJhAjUPTY7k/edit?usp=sharing


~ I really enjoyed working on this project i hope you enjoy using it
