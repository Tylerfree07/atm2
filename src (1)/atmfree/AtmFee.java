package atmfree;
import java.util.Scanner;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.sql.*;
import java.util.Map;
import java.util.Random;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
//imports


public class AtmFee {
    private static Connection conn;//establish connection values i need two for each database
    private static Connection connTransactions;
    private Scanner inputScanner = new Scanner(System.in);
    private double balance = 0;

    private Map<String, User> users;
    private String accountNumber;
    private Map<String, List<Transaction>> transactions = new HashMap<>();
     HashMap<String, String> rate = new HashMap<>();
     String filePath = "rates.txt";


    public static void initializeDatabase() {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:atm_users.db");
            System.out.println("Connected to users database: atm_users.db");
            
            String createTableSQL = """
                CREATE TABLE IF NOT EXISTS users (
                    accountNumber TEXT PRIMARY KEY,
                    password TEXT NOT NULL,
                    balance REAL NOT NULL,
                    name TEXT NOT NULL,
                    currency TEXT NOT NULL
                )
            """;
            Statement stmt = conn.createStatement();
            stmt.execute(createTableSQL);
            System.out.println("Users table initialized successfully.");
            
        } catch (SQLException e) {
            System.out.println("Error initializing users database.");
            e.printStackTrace();
        }
    }

    public static void initializeDatabaseTransactions() {
        try {
            connTransactions = DriverManager.getConnection("jdbc:sqlite:atm_transaction.db");
            System.out.println("Connected to transactions database: atm_transaction.db");
            
            String createTableSQL = """
                CREATE TABLE IF NOT EXISTS transactions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    accountNumber TEXT NOT NULL,
                    type TEXT NOT NULL,
                    amount REAL NOT NULL,
                    date TEXT NOT NULL
                )
            """;
            Statement stmt = connTransactions.createStatement();
            stmt.execute(createTableSQL);
            System.out.println("Transactions table initialized successfully.");
            
        } catch (SQLException e) {
            System.out.println("Error initializing transactions database.");
            e.printStackTrace();
        }
    }

    public AtmFee() {
        users = new HashMap<>();
        loadUsersFromDatabase();
        loadTransactionsFromDatabase();
    }

    private static class User { // cretes class
        String pinNumber;
        double balance;
        String name;
        String currency;
        User(String pinNumber, double balance, String name, String currency) {
            this.pinNumber = pinNumber;
            this.balance = balance;
            this.name = name;
            this.currency = currency;
        }
    }

    private static class Transaction { // cretes class
        String type;
        double amount;
        String date;

        Transaction(String type, double amount, String date) {
            this.type = type;
            this.amount = amount;
            this.date = date;
        }
    }

    public void loadUsersFromDatabase() {
        try {
            String query = "SELECT * FROM users";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                String accountNumber = rs.getString("accountNumber");
                String pinNumber = rs.getString("password");
                double balance = rs.getDouble("balance");
                String name = rs.getString("name");
                String currency = rs.getString("currency");
                users.put(accountNumber, new User(pinNumber, balance, name, currency));
            }
            
        } catch (SQLException e) {
            System.out.println("Error loading users from database.");
            e.printStackTrace();
        }
    }

    public void saveToDatabase() {
        String sql = """
            INSERT OR REPLACE INTO users (accountNumber, password, balance, name, currency)
            VALUES (?, ?, ?, ?, ?)
        """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            for (Map.Entry<String, User> entry : users.entrySet()) {
                String accountNumber = entry.getKey();
                User user = entry.getValue();
                stmt.setString(1, accountNumber);
                stmt.setString(2, user.pinNumber);
                stmt.setDouble(3, user.balance);
                stmt.setString(4, user.name);
                stmt.setString(5, user.currency);
                stmt.executeUpdate();
            }
            
        } catch (SQLException e) {
            System.out.println("Error saving users to database.");
            e.printStackTrace();
        }
    }

    private void createAccount() {
        Random random = new Random();
        String accountNumber = String.valueOf(10000000 + random.nextInt(90000000));// genertates a random 8 digit code
        System.out.println("Your new account number is: " + accountNumber);

        System.out.print("Enter a 4-digit PIN: ");
        String pinNumber = inputScanner.nextLine();// gets pin

        System.out.print("Enter your name (e.g., John_Doe): ");// gets name
        String name = inputScanner.nextLine();

        try {
            String insertSQL = """
                INSERT INTO users (accountNumber, password, balance, name, currency)
                VALUES (?, ?, ?, ?, ?)
            """;
            PreparedStatement stmt = conn.prepareStatement(insertSQL);
            stmt.setString(1, accountNumber);
            stmt.setString(2, pinNumber);
            stmt.setDouble(3, 0.0);
            stmt.setString(4, name);
            stmt.setString(5, "EUR");//makes base currency EUR cause its one in the txt file
            stmt.executeUpdate();

            users.put(accountNumber, new User(pinNumber, 0.0, name, "EUR"));
            System.out.println("Account created successfully.");
        } catch (SQLException e) {
            System.out.println("Error creating new account.");
            e.printStackTrace();
        }
    }

    public boolean authenticate(String accountNumber, String pinNumber) {
        return users.containsKey(accountNumber) && users.get(accountNumber).pinNumber.equals(pinNumber);
    }

    private void mainMenu() {
        while (true) {
            System.out.println("\nWelcome to the ATM!:");
            System.out.println("1. Log in");
            System.out.println("2. Create Account");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");

            int choice = inputScanner.nextInt();
            inputScanner.nextLine(); // Clear buffer

            switch (choice) {
                case 1 -> logIn();
                case 2 -> createAccount();
                case 3 -> {
                    System.out.println("Thank you for using the ATM. Goodbye!");
                    System.exit(0);
                }
                default -> System.out.println("Invalid option. Try again.");
            }
        }
    }

    private void logIn() {
        System.out.print("Enter your account number: ");
        String accountNum = inputScanner.nextLine();

        System.out.print("Enter your 4-digit PIN: ");
        String pinNum = inputScanner.nextLine();

        if (authenticate(accountNum, pinNum)) {
            accountNumber = accountNum;
            balance = users.get(accountNum).balance;
            
        } else {
            System.out.println("Invalid account number or PIN. Please try again.");
        }
        useraction();

    }

    private void useraction() {
        while (true) {
            System.out.println("1. Account");
            System.out.println("2. Tools");
            System.out.println("3. Settings");
            System.out.println("4. Exit");
            System.out.print("Choose an option: ");
            int choiceacion = inputScanner.nextInt();
            inputScanner.nextLine(); // Clear buffer
            
            switch (choiceacion) {
                case 1 -> userMenu();
                case 2 -> tools();
                case 3 -> settings();
                case 4 -> { return; }
                default -> System.out.println("Invalid option. Try again.");
            }
        }
    }

    private void userMenu() {
        String name = users.get(accountNumber).name;

        System.out.println("\nWelcome back " + name );
        while (true) {

            System.out.println("User Menu:");
            System.out.println("1. View Balance");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Transfer");
            System.out.println("5. Transactions");
            System.out.println("6. Exit");
            System.out.print("Choose an option: ");

            int choice = inputScanner.nextInt();
            inputScanner.nextLine(); // Clear buffer

            switch (choice) {
                case 1 ->{
                    loadExchangeRates();//grabs exchange rates
                    User currentUser = users.get(accountNumber);
                    String currentCurrency = currentUser.currency;//convert the balence to the selected currency in settings
                    double currentRate = Double.parseDouble(rate.get(currentCurrency));
                    double convertedBalance = balance *currentRate;
                    convertedBalance = Math.round(convertedBalance * 100)/100;
                    System.out.println("Your balance is: $" + convertedBalance +" in 3" + currentCurrency);
            }
                case 2 -> {
                    clearTransactions();
                    deposit();
                    saveTransactionsToDatabase();}
                case 3 -> {
                    clearTransactions();
                    withdraw();
                    saveTransactionsToDatabase();}
                case 4 -> {
                    clearTransactions();
                    transfer();
                    saveTransactionsToDatabase();}
                case 5 ->{
                    loadTransactionsFromDatabase(); 
                    transactions();
                    clearTransactions();}
                case 6-> {
                    saveToDatabase();
                   
                    System.out.println("Logging out...");
                    return;
                }
                default -> System.out.println("Invalid option. Try again.");
            }
        }
    }

    private void deposit() {
        System.out.print("Enter deposit amount: $");
        double amount = inputScanner.nextDouble();
        inputScanner.nextLine(); // Clear buffer
        loadExchangeRates();
        User currentUser = users.get(accountNumber);
        String currentCurrency = currentUser.currency;
        double currentRate = Double.parseDouble(rate.get(currentCurrency));
        if (amount > 0) {
            balance += amount;
            users.get(accountNumber).balance = balance;
            double convertedBalance = balance * currentRate;
            convertedBalance = Math.round(convertedBalance * 100.0) / 100.0;
            System.out.println("Deposit successful. Your new balance is $" + convertedBalance + " in " + currentCurrency);
            
            // Get or create transaction list and add new transaction
            List<Transaction> userTransactions = transactions.computeIfAbsent(accountNumber, k -> new ArrayList<>());
            userTransactions.add(new Transaction("Deposit", amount, new Date().toString()));
            
            // Keep only the last 10 transactions
            if (userTransactions.size() > 10) {
                userTransactions = userTransactions.subList(userTransactions.size() - 10, userTransactions.size());
                transactions.put(accountNumber, userTransactions);
            }
        } else {
            System.out.println("Invalid amount. Deposit failed.");
        }
    }
    
    private void withdraw() {
        System.out.print("Enter withdrawal amount: $");
        double amount = inputScanner.nextDouble();
        inputScanner.nextLine(); // Clear buffer
        loadExchangeRates();
        User currentUser = users.get(accountNumber);
        String currentCurrency = currentUser.currency;
        double currentRate = Double.parseDouble(rate.get(currentCurrency));
        if (amount > 0 && amount <= balance) {
            balance = (balance - amount)/currentRate;
            users.get(accountNumber).balance = balance;
            double convertedBalance = balance * currentRate;
            convertedBalance = Math.round(convertedBalance*100)/100;
            System.out.println("Withdrawal successful. Your new balance is $" + convertedBalance+ " in " + currentCurrency);
            
            // Get or create transaction list and add new transaction
            List<Transaction> userTransactions = transactions.computeIfAbsent(accountNumber, k -> new ArrayList<>());
            userTransactions.add(new Transaction("Withdrawal", amount, new Date().toString()));
            
            // Keep only the last 10 transactions
            if (userTransactions.size() > 10) {
                userTransactions = userTransactions.subList(userTransactions.size() - 10, userTransactions.size());
                transactions.put(accountNumber, userTransactions);
            }
        } else if (amount > balance) {
            System.out.println("Insufficient funds. Withdrawal failed.");
        } else {
            System.out.println("Invalid amount. Withdrawal failed.");
        }
    }

    public void loadTransactionsFromDatabase() {
        try {
            String query = "SELECT * FROM transactions";
            Statement stmt = connTransactions.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                String accountNumber = rs.getString("accountNumber");
                String type = rs.getString("type");
                double amount = rs.getDouble("amount");
                String date = rs.getString("date");
                transactions.computeIfAbsent(accountNumber, k -> new ArrayList<>())
                           .add(new Transaction(type, amount, date));
            }
            System.out.println("Transactions loaded from database.");
        } catch (SQLException e) {
            System.out.println("Error loading transactions from database.");
            e.printStackTrace();
        }
    }

    public void saveTransactionsToDatabase() {
        String sql = """
            INSERT OR IGNORE INTO transactions (accountNumber, type, amount, date)
            VALUES (?, ?, ?, ?)
        """;

        try (PreparedStatement stmt = connTransactions.prepareStatement(sql)) {
            Set<String> uniqueTransactions = new HashSet<>(); // Track unique transactions
            for (Map.Entry<String, List<Transaction>> entry : transactions.entrySet()) {
                String accountNumber = entry.getKey();
                List<Transaction> userTransactions = entry.getValue();
                for (Transaction t : userTransactions) {
                    String transactionKey = accountNumber + t.type + t.amount + t.date; // Create a unique key for each transaction
                    if (!uniqueTransactions.contains(transactionKey)) { // Check if the transaction is unique
                        stmt.setString(1, accountNumber);
                        stmt.setString(2, t.type);
                        stmt.setDouble(3, t.amount);
                        stmt.setString(4, t.date);
                        stmt.executeUpdate();
                        uniqueTransactions.add(transactionKey); // Add to the set of unique transactions
                    }
                }
            }
            System.out.println("Transactions saved to database.");

            // Clear the transactions map after successful save
            transactions.clear(); // Clear the HashMap after saving
        } catch (SQLException e) {
            System.out.println("Error saving transactions to database.");
            e.printStackTrace();
        }
    }

    private void transactions() { // prints out transactions for user 
        List<Transaction> userTransactions = transactions.get(accountNumber);
        if (userTransactions != null && !userTransactions.isEmpty()) {
            System.out.println("\nTransaction History:");

            // Get the last 10 transactions
            int startIndex = Math.max(userTransactions.size() - 10, 0);
            List<Transaction> lastTransactions = userTransactions.subList(startIndex, userTransactions.size());

            for (Transaction t : lastTransactions) {
                System.out.println("Type: " + t.type);
                System.out.println("Amount: $" + t.amount);
                System.out.println("Date: " + t.date);
                System.out.println("------------------------");
            }
        } else {
            System.out.println("No transactions found.");
        }
    }

    private void taxEstimator() {// tax esitmator tool
        double[] incomeLimits = {51446, 55867, 90559, 102894, 106732, 111733, 150000, 173205, 220000, 246752};
        double[] taxRates = {0.2005, 0.2415, 0.2965, 0.3148, 0.3389, 0.3791, 0.4341, 0.4497, 0.4829, 0.4985, 0.5353};
    
        System.out.print("What is your yearly income: $");
        double income = inputScanner.nextDouble();
        System.out.print("What did you put into RRSP or FHSA: $");
        double taxFreeAmount = inputScanner.nextDouble();
    
        income -= taxFreeAmount; // Adjust income
        double taxTotal = 0;
        double previousLimit = 0;
    
        for (int i = 0; i < incomeLimits.length; i++) {
            if (income > incomeLimits[i]) {
                taxTotal += (incomeLimits[i] - previousLimit) * taxRates[i];
                previousLimit = incomeLimits[i];
            } else {
                taxTotal += (income - previousLimit) * taxRates[i];
                break;
            }
        }
    
        if (income > incomeLimits[incomeLimits.length - 1]) {
            taxTotal += (income - incomeLimits[incomeLimits.length - 1]) * taxRates[taxRates.length - 1];
        }
    
        System.out.println("You owe $" + Math.round(taxTotal) + " in taxes.");
    }

    private void tools() {
        while (true) {
            System.out.println("1. Tax Estimator");
            System.out.println("2. Exchange Rate");
            System.out.println("3. Exit");
            System.out.print("Choose an option: ");
            
            int tools = inputScanner.nextInt();
            inputScanner.nextLine(); // Clear buffer
            
            try {
                switch (tools) {
                    case 1 -> taxEstimator();
                    case 2 -> exchange();
                    case 3 -> { return; }
                    default -> System.out.println("Invalid option. Try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input. Please enter a number.");
                continue;
            }
        }
    }

    private void settings(){
           
        while(true){
            System.out.println("Welcome to settings");
                    System.out.println("1. Change PinNumber");
                    System.out.println("2. Change Name");
                    System.out.println("3. Currency");
                    System.out.println("4. Exit");
                    Integer settings = inputScanner.nextInt();
                    inputScanner.nextLine();
                    switch(settings){
                        
                        case 1:
                            System.out.println("Enter new pin number:");
                            String Newpin = inputScanner.nextLine();
                            users.get(accountNumber).pinNumber= Newpin;
                            saveToDatabase();
                            System.out.println("New pin saved to file!");
                            break;
                        case 2:
                            System.out.println("Enter new name:");
                            String NewName = inputScanner.nextLine();
                            users.get(accountNumber).name = NewName;
                            saveToDatabase();
                            System.out.println("New name saved to file!");
                            break;
                        case 3:
                            System.out.println("Enter new Currency");
                            String newCurrency = inputScanner.nextLine().toUpperCase();
                            users.get(accountNumber).currency = newCurrency;
                            saveToDatabase();
                            System.out.println("New currency save to file!");
                            break;
                        case 4:
                            return;
                            
                        default:
                            System.out.println(accountNumber);
                            break;
                    }
    }
    }
    private void loadExchangeRates(){
         try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            // Read each line from the file
            while ((line = reader.readLine()) != null) {
                // Split the line into key and value
                String[] parts = line.split(":", 2); // Split the line into two values in between :
                if (parts.length == 2) {
                    String Currency = parts[0].trim();    // Trim whitespace from key
                    String Rate = parts[1].trim(); // Trim whitespace from value
                    
                    rate.put(Currency, Rate);
                } else {
                    System.out.println("Skipping malformed line: " + line);
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
        }
    }
    private void exchange(){
    // Ensure the file exists in your project directory

        loadExchangeRates();
        System.out.println("Welcome to currency exchange!");
        System.out.println("Please insert your current currency(Ex. USD): ");
        String current = inputScanner.nextLine().toUpperCase(); //gets current currency
        
        System.out.println("Please insert the currency you want to exchange to(Ex. CAD): ");
        String exchange = inputScanner.nextLine().toUpperCase(); // gets exchnage currency
        
        System.out.println("Enter Amount: $");
        Double amount = inputScanner.nextDouble();
     
        
        // Retrieve the values from the HashMap
        String currentRateStr = rate.get(current); // Get the rate as a String
        String exchangeRateStr = rate.get(exchange); // Get the exchange rate as a String

        // Convert the String values to double
        double currentRate = Double.parseDouble(currentRateStr);
        double exchangeRate = Double.parseDouble(exchangeRateStr);
        double Total = amount/(currentRate/exchangeRate);
        Total = Total*100;
        Total = Math.round(Total); // convert value into an value like a price
        Total = Total/100;

        System.out.println("Your exchanged amount from " + current +" to " + exchange + " is: $" + Total); //prints out values
    }

    private void transfer(){
       loadExchangeRates();
       User currentUser = users.get(accountNumber); // grbas currentuser account
       String currentCurrency = currentUser.currency; // grabs current users currency setting
       double currentRate = Double.parseDouble(rate.get(currentCurrency)); //grbas rate for the currenyc from rates.txt
        
        System.out.print("Enter the recipient's account number: ");
        String recipientAccount = inputScanner.nextLine();
    
        // Check if recipient exists
        if (!users.containsKey(recipientAccount)) {
            System.out.println("The recipient's account number does not exist.");
            return;
        }
    
        System.out.print("Enter the amount to transfer: $");
        if (inputScanner.hasNextDouble()) {
            double transferAmount = inputScanner.nextDouble();
            inputScanner.nextLine(); // Clear the buffer
            transferAmount = transferAmount/currentRate;
            transferAmount = Math.round(transferAmount *100)/100;
    
            if (transferAmount <= 0) {
                System.out.println("Invalid amount. Transfer amount must be greater than $0.");
                return;
            }
    
            if (transferAmount > balance) {
                System.out.println("Insufficient funds. Transfer cannot be completed.");
                return;
            }
    
            // Deducting from sender's balance
            balance -= transferAmount;
            users.get(accountNumber).balance = balance;
    
            // Add to recipient's balance
            User recipientUser = users.get(recipientAccount); // checks user in database
            recipientUser.balance += transferAmount;
    
            // Save updated balances
            saveToDatabase();
    
            // Record transactions for both accounts
            transaction("Transfer to " + recipientAccount, (transferAmount * currentRate));
            transaction("Transfer from " + accountNumber, (transferAmount * currentRate)); // Record for recipient's transaction
           
            System.out.println("Successfully transferred $" + (transferAmount *currentRate)+ " to account " + recipientAccount);
        } else {
            System.out.println("invalid");
            inputScanner.nextLine(); // Clear invalid input
        }   
        }
        
    
        // Add this method to handle transactions
        private void transaction(String type, double amount) {
            List<Transaction> userTransactions = transactions.computeIfAbsent(accountNumber, k -> new ArrayList<>());

            // Create a unique key for the transaction
            String transactionKey = accountNumber + type + amount + new Date().toString(); // Unique identifier

            // Check if the transaction already exists
            boolean exists = userTransactions.stream()
                .anyMatch(t -> t.type.equals(type) && t.amount == amount && t.date.equals(new Date().toString()));

            if (!exists) {
                userTransactions.add(new Transaction(type, amount, new Date().toString()));
                System.out.println("Transaction added: " + type + " of amount $" + amount);
                
                // Keep only the last 10 transactions
                if (userTransactions.size() > 10) {
                    userTransactions = userTransactions.subList(userTransactions.size() - 10, userTransactions.size());
                    transactions.put(accountNumber, userTransactions);
                }
            } else {
                System.out.println("Transaction already exists: " + type + " of amount $" + amount);
            }
        }

    public void clearTransactions() {
        transactions.clear(); // Clear the HashMap
        System.out.println("All transactions have been cleared.");
    }

    public static void main(String[] args) {
        initializeDatabase();
        initializeDatabaseTransactions(); // intalize bith databases
        AtmFee atm = new AtmFee();
        atm.loadUsersFromDatabase(); // load values from datbase into a hashmap
        atm.loadTransactionsFromDatabase();
        atm.mainMenu(); // opens main menu
        closeDatabase(); //close datbase
    }

    public static void closeDatabase() { // closes database (copied from internet no ides how this works)
        try {
            if (conn != null) {
                conn.close();
                System.out.println("Users database connection closed.");
            }
            if (connTransactions != null) {
                connTransactions.close();
                System.out.println("Transactions database connection closed.");
            }
        } catch (SQLException e) {
            System.out.println("Error closing the database.");
            e.printStackTrace();
        }
    }
}

