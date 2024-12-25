package atmfree;
import java.util.Scanner;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.util.Map;
import java.util.Random;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import org.json.JSONObject;
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
     private String API_KEY = "3d3ad73d8e9fda40a5af4915"; // Replace with your API Key
     private String BASE_URL = "https://v6.exchangerate-api.com/v6/" + API_KEY + "/latest/USD";
    public static void initializeDatabase() {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:atmfree.db");
            String createTableSQL = """
                CREATE TABLE IF NOT EXISTS users (
                    accountNumber TEXT PRIMARY KEY,
                    password TEXT NOT NULL,
                    balance REAL NOT NULL,
                    name TEXT NOT NULL,
                    currency TEXT NOT NULL
                    email TEXT NOT NULL
                )
            """;
            Statement stmt = conn.createStatement();
            stmt.execute(createTableSQL);
        } catch (SQLException e) {
            System.out.println("Error initializing users database.");
            e.printStackTrace();
        }
    }

    public static void initializeDatabaseTransactions() {
        try {
            connTransactions = DriverManager.getConnection("jdbc:sqlite:atm_transaction.db");
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
           
            
        } catch (SQLException e) {
            System.out.println("Error initializing transactions database.");
            e.printStackTrace();
        }
    }

    public AtmFee() {
        users = new HashMap<>();
        loadUsersFromDatabase();
        
    }

    private static class User { // cretes class
        String pinNumber;
        double balance;
        String name;
        String currency;
        String email;
        User(String pinNumber, double balance, String name, String currency, String email) {
            this.pinNumber = pinNumber;
            this.balance = balance;
            this.name = name;
            this.currency = currency;
            this.email = email;
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
         try{
            String query = "SELECT * FROM users";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(query);

            while (rs.next()) {
                String accountNumber = rs.getString("accountNumber");
                String pinNumber = rs.getString("password");
                double balance = rs.getDouble("balance");
                String name = rs.getString("name");
                String currency = rs.getString("currency");
                String email = rs.getString("email");
                users.put(accountNumber, new User(pinNumber, balance, name, currency, email));
            }
            
        } catch (SQLException e) {
            System.out.println("Error loading users from database.");
            e.printStackTrace();
        }
    }

    public void saveToDatabase() {
        String sql = """
            INSERT OR REPLACE INTO users (accountNumber, password, balance, name, currency, email)
            VALUES (?, ?, ?, ?, ?, ?)
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
                stmt.setString(6,user.email);
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

        System.out.print("Enter your email: ");// gets email
        String email = inputScanner.nextLine();

        try {
            String insertSQL = """
                INSERT INTO users (accountNumber, password, balance, name, currency, email)
                VALUES (?, ?, ?, ?, ?, ?)
            """;
            PreparedStatement stmt = conn.prepareStatement(insertSQL);
            stmt.setString(1, accountNumber);
            stmt.setString(2, pinNumber);
            stmt.setDouble(3, 0.0);
            stmt.setString(4, name);
            stmt.setString(5, "USD");
            stmt.setString(6, email);//makes base currency EUR cause its one in the txt file
            stmt.executeUpdate();

            users.put(accountNumber, new User(pinNumber, 0.0, name, "USD", email));
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
            System.out.println("\n------------------------");
            System.out.println("Welcome to the ATM!:");
            System.out.println("1. Log in");
            System.out.println("2. Create Account");
            System.out.println("3. Exit");
            System.out.println("------------------------");
            System.out.print("Choose an option: ");

            int choice = inputScanner.nextInt();
            inputScanner.nextLine(); // Clear buffer

            switch (choice) {
                case 1 -> logIn();
                case 2 -> createAccount();
                case 3 -> {
                    System.out.println("------------------------");
                    System.out.println("Thank you for using the ATM. Goodbye!");
                    System.out.println("------------------------");
                    System.exit(0);
                }
                default -> System.out.println("Invalid option. Try again.");
            }
        }
    }

    private void logIn() {
       int Attempts = 3;
       for ( int i = 0; i <Attempts ; i++ ) {
        System.out.println("------------------------");
        System.out.print("Enter your account number: ");
        String accountNum = inputScanner.nextLine();
        System.out.println("------------------------");
        System.out.print("Enter your 4-digit PIN: ");
        String pinNum = inputScanner.nextLine();

        if (authenticate(accountNum, pinNum)) {
            accountNumber = accountNum;
            balance = users.get(accountNum).balance;
            useraction();
            return;
        } else {
            System.out.println("Invalid account number or PIN. Please try again.");
            System.out.println("Attempts left: " + (Attempts - 1 - i));
        }
    }
    System.out.println("you have run out of trys please try again later!!!!");
    try {
        System.out.println("Locking out for 30 seconds...");
        Thread.sleep(30000); // Lockout delay (30 seconds)
    } catch (InterruptedException e) {
        System.out.println("Error during lockout delay.");
    }
    return;
    }
    private void useraction() {
        while (true) {
            System.out.println("------------------------");
            System.out.println("1. Account");
            System.out.println("2. Tools");
            System.out.println("3. Settings");
            System.out.println("4. Exit");
            System.out.println("------------------------");
            System.out.print("Choose an option: ");
            int choiceacion = inputScanner.nextInt();
            inputScanner.nextLine(); // Clear buffer
            
            switch (choiceacion) {
                case 1 -> userMenu();
                case 2 -> tools();
                case 3 -> settings();
                case 4 -> { System.out.println("------------------------");
                            System.out.println("Logging out...");
                            return; }
                default -> System.out.println("Invalid option. Try again.");
            }
        }
    }

private void estamentmenu() {
while (true) {
    System.out.println("------------------------");
    System.out.println("Estament Menu:");
    System.out.println("1. View transactions");
    System.out.println("2. Export to PDF file");
    System.out.println("3. Export to CSV file");
    System.out.println("4. Clear transaction history");
    System.out.println("5. Exit");
    System.out.println("------------------------");
    System.out.print("Choose an option: ");
    int choice = inputScanner.nextInt();
    inputScanner.nextLine(); // Clear buffer
    
    switch (choice) {
                case 1 -> {loadTransactionsFromDatabase(); 
                            transactions();
                            clearTransactions();}
                case 2 -> {clearTransactions();
                            loadTransactionsFromDatabase();
                            exportTransactionsToPDF();
                            clearTransactions();}
                case 3 -> {clearTransactions();
                            loadTransactionsFromDatabase();
                            exportTransactionsToCSV();
                            clearTransactions();}
                case 4 ->  {System.out.println("------------------------"); 
                            System.out.println("Are you sure you want to clear transaction history? (y/n)");
                            System.out.println("------------------------");
                            System.out.print(":");
                            String answer = inputScanner.nextLine();
                            if (answer.equalsIgnoreCase("y")) {
                                    clearEstaments(accountNumber);
                                    System.out.println("Transaction history cleared.");
                                    }
                            else if (answer.equalsIgnoreCase("n")) {return;}
                            else { System.out.println("Invalid answer. Please try again.");}}
                case 5 -> { return; }
                default -> System.out.println("Invalid option. Try again.");
            }
        }
    
}



    private void userMenu() {
        String name = users.get(accountNumber).name;
        System.out.println("------------------------");
        System.out.println("\nWelcome back " + name );
        while (true) {
            System.out.println("------------------------");
            System.out.println("User Menu:");
            System.out.println("1. View Balance");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Transfer");
            System.out.println("5. Estament's");
            System.out.println("6. Exit");
            System.out.println("------------------------");
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
                    System.out.println("------------------------");
                    System.out.println("Your balance is: $" + convertedBalance +" in " + currentCurrency);
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
                case 5 ->estamentmenu();
                case 6-> {
                    saveToDatabase();
                    return;
                }
                default -> System.out.println("Invalid option. Try again.");
            }
        }
    }

    private void deposit() {
        System.out.println("------------------------");
        System.out.print("Enter deposit amount: $");
        double amount = inputScanner.nextDouble();
        inputScanner.nextLine(); // Clear buffer
        loadExchangeRates();
        User currentUser = users.get(accountNumber);
        String currentCurrency = currentUser.currency;
        double currentRate = Double.parseDouble(rate.get(currentCurrency));
        if (amount > 0) {
            balance = balance + amount/currentRate;
            users.get(accountNumber).balance = balance;
            double convertedBalance = balance * currentRate;
            convertedBalance = Math.round(convertedBalance * 100.0) / 100.0;
           System.out.println("------------------------");
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
            System.out.println("------------------------");
            System.out.println("Invalid amount. Deposit failed.");
        }
    }
    
    private void withdraw() {
        System.out.println("------------------------");
        System.out.print("Enter withdrawal amount: $");
        double amount = inputScanner.nextDouble();
        inputScanner.nextLine(); // Clear buffer
        loadExchangeRates();
        User currentUser = users.get(accountNumber);
        String currentCurrency = currentUser.currency;
        double currentRate = Double.parseDouble(rate.get(currentCurrency));
        if (amount > 0 && amount <= balance) {
            balance = balance - amount/currentRate;
            users.get(accountNumber).balance = balance;
            double convertedBalance = balance * currentRate;
            convertedBalance = Math.round(convertedBalance*100)/100;
            System.out.println("------------------------");
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
            System.out.println("------------------------");
            System.out.println("Insufficient funds. Withdrawal failed.");
        } else {
            System.out.println("------------------------");
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
        System.out.println("------------------------");
        System.out.print("What is your yearly income: $");
        double income = inputScanner.nextDouble();
        System.out.println("------------------------");
        System.out.print("What did you put into RRSP or FHSA: $");
        double taxFreeAmount = inputScanner.nextDouble();
        System.out.println("------------------------");
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
            System.out.println("------------------------");
            System.out.println("1. Tax Estimator");
            System.out.println("2. Exchange Rate");
            System.out.println("3. Loan Calculator");
            System.out.println("4. Exit");
            System.out.println("------------------------");
            System.out.print("Choose an option: ");
            
            int tools = inputScanner.nextInt();
            inputScanner.nextLine(); // Clear buffer
            
            try {
                switch (tools) {
                    case 1 -> taxEstimator();
                    case 2 -> exchange();
                    case 3 -> calcLoan();
                    case 4 -> { return; }
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
                    System.out.println("------------------------");
                    System.out.println("Welcome to settings");
                    System.out.println("1. Change PinNumber");
                    System.out.println("2. Change Name");
                    System.out.println("3. Change Currency");
                    System.out.println("4. Change Email Address");
                    System.out.println("5. Delete Account");
                    System.out.println("6. Exit");
                    System.out.println("------------------------");
                    System.out.print("Choose an option: ");
                    Integer settings = inputScanner.nextInt();
                    inputScanner.nextLine();
                    switch(settings){
                        
                        case 1:
                            System.out.println("------------------------");
                            System.out.print("Enter new pin number:");
                            String Newpin = inputScanner.nextLine();
                            users.get(accountNumber).pinNumber= Newpin;
                            saveToDatabase();
                            System.out.println("------------------------");
                            System.out.println("New pin saved to file!");
                            break;
                        case 2:
                        System.out.println("------------------------");    
                        System.out.print("Enter new name:");
                            String NewName = inputScanner.nextLine();
                            users.get(accountNumber).name = NewName;
                            saveToDatabase();
                            System.out.println("------------------------");
                            System.out.println("New name saved to file!");
                            break;
                        case 3:
                        System.out.println("------------------------");    
                        System.out.print("Enter new Currency:");
                            String newCurrency = inputScanner.nextLine().toUpperCase();
                            users.get(accountNumber).currency = newCurrency;
                            saveToDatabase();
                            System.out.println("------------------------");
                            System.out.println("New currency save to file!");
                            break;
                        case 4: {
                            System.out.println("------------------------");
                            System.out.print("Enter new email address:");
                            String newEmail = inputScanner.nextLine();
                            users.get(accountNumber).email = newEmail;
                            saveToDatabase();
                            System.out.println("------------------------");
                            System.out.println("New email saved to file!");
                        }
                            case 5:{ System.out.println("------------------------");
                            System.out.println("Are you sure you want to delete your acount!(y/n)");
                            System.out.println("------------------------");
                            System.out.print("Choice:");
                            String confirm = inputScanner.next();
                            if(confirm.equalsIgnoreCase("y")){deleteUser(accountNumber);}
                            mainMenu();
                        }
                            case 6:
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


   private void transfer() {
    loadExchangeRates();

    User currentUser = users.get(accountNumber); // Sender's account
    String currentCurrency = currentUser.currency;
    double currentRate = Double.parseDouble(rate.get(currentCurrency));
    System.out.println("------------------------");
    System.out.print("Enter the recipient's account number: ");
    String recipientAccount = inputScanner.nextLine();

    // Validate recipient
    if (!users.containsKey(recipientAccount)) {
        System.out.println("The recipient's account number does not exist.");
        return;
    }

    User recipientUser = users.get(recipientAccount);
    String recipientCurrency = recipientUser.currency;
    double recipientRate = Double.parseDouble(rate.get(recipientCurrency));
    System.out.println("------------------------");
    System.out.print("Enter the amount to transfer: $");
    if (inputScanner.hasNextDouble()) {
        double transferAmount = inputScanner.nextDouble();
        inputScanner.nextLine(); // Clear the buffer

        double amountInSenderCurrency = transferAmount / currentRate;
        amountInSenderCurrency = Math.round(amountInSenderCurrency * 100) / 100.0;

        if (amountInSenderCurrency <= 0) {
            System.out.println("------------------------");
            System.out.println("Invalid amount. Transfer amount must be greater than $0.");
            return;
        }

        if (amountInSenderCurrency > balance) {
            System.out.println("------------------------");
            System.out.println("Insufficient funds. Transfer cannot be completed.");
            return;
        }

        // Deduct from sender
        balance -= amountInSenderCurrency;
        currentUser.balance = balance;

        // Add to recipient
        double amountInRecipientCurrency = transferAmount * recipientRate;
        recipientUser.balance += amountInRecipientCurrency;

        // Save updated balances
        saveToDatabase();

        // Record transactions
        transaction(accountNumber, "Transfer to " + recipientAccount, transferAmount);
        transaction(recipientAccount, "Transfer from " + accountNumber, transferAmount);
        System.out.println("------------------------");
        System.out.printf(
            "Successfully transferred $%.2f to account %s.%n",
            transferAmount, recipientAccount
        );
    } else {
        System.out.println("Invalid input. Please enter a valid amount.");
        inputScanner.nextLine(); // Clear invalid input
    }
}
        // Add this method to handle transactions
       private void transaction(String accountNumber, String type, double amount) {
    if (accountNumber == null || type == null || amount <= 0) {
        System.out.println("Invalid transaction details. Please check the input.");
        return;
    }

    // Fetch or initialize transaction history for the account
    List<Transaction> userTransactions = transactions.computeIfAbsent(accountNumber, k -> new ArrayList<>());

    // Get the current date in a consistent format
    String currentDate = new java.text.SimpleDateFormat("yyyy-MM-dd").format(new Date());

    // Check if the transaction already exists
    boolean exists = userTransactions.stream()
            .anyMatch(t -> t.type.equals(type) && t.amount == amount && t.date.equals(currentDate));

    if (!exists) {
        // Add the transaction
        userTransactions.add(new Transaction(type, amount, currentDate));
        

        // Retain only the last 10 transactions
        if (userTransactions.size() > 10) {
            userTransactions = userTransactions.subList(userTransactions.size() - 10, userTransactions.size());
            transactions.put(accountNumber, userTransactions); // Update map reference
        }
    } else {
        System.out.println("Transaction already exists: " + type + " of amount $" + amount);
    }
}

    public void clearTransactions() {
        transactions.clear(); // Clear the HashMap
    }

    public static void main(String[] args) {
        initializeDatabase();
        initializeDatabaseTransactions(); // intalize bith databases
        AtmFee atm = new AtmFee();
        atm.loadUsersFromDatabase(); // load values from datbase into a hashmap
        atm.updateRatesFileOnceADay();
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
private void exportTransactionsToCSV() {
    try {
        List<Transaction> userTransactions = transactions.get(accountNumber);
        if (userTransactions == null || userTransactions.isEmpty()) {
            System.out.println("------------------------");
            System.out.println("No transactions to export.");
            return;
        }

        String csvFilePath = "transactions_" + accountNumber + ".csv";
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(csvFilePath))) {
            writer.write("Type,Amount,Date");
            writer.newLine();
            for (Transaction t : userTransactions) {
                writer.write(t.type + "," + t.amount + "," + t.date);
                writer.newLine();
            }
        }
        System.out.println("------------------------");
        System.out.println("Transactions exported to: " + csvFilePath);
    } catch (IOException e) {
        System.out.println("Error exporting transactions to CSV.");
        e.printStackTrace();
    }
}
private void exportTransactionsToPDF() {
    List<Transaction> userTransactions = transactions.get(accountNumber);
    if (userTransactions == null || userTransactions.isEmpty()) {
       System.out.println("------------------------");
        System.out.println("No transactions to export.");
        return;
    }
    String pdfFilePath = "transactions_" + accountNumber + ".pdf";
    Document document = new Document();
    try {
        PdfWriter.getInstance(document, new FileOutputStream(pdfFilePath));
        document.open();
            String imagePath = "logo.png"; // Path to your image file
            Image img = Image.getInstance(imagePath);
            // Resize the image
            img.scaleToFit(100, 50); // Adjust width and height as needed
            // Set image position (top-right corner)
            float x = document.getPageSize().getWidth() - img.getScaledWidth() - 10; // 10 units padding
            float y = document.getPageSize().getHeight() - img.getScaledHeight() - 10; // 10 units padding
            img.setAbsolutePosition(x, y);
            // Add the image
            document.add(img);

        document.add(new Paragraph("Transaction Report for Account: " + accountNumber));
        document.add(new Paragraph(" ")); // Empty line for spacing

        PdfPTable table = new PdfPTable(3); // 3 columns: Type, Amount, Date
        table.addCell("Type");
        table.addCell("Amount");
        table.addCell("Date");

        for (Transaction t : userTransactions) {
            table.addCell(t.type);
            table.addCell(String.valueOf(t.amount));
            table.addCell(t.date);
        }
        document.add(table);
        System.out.println("------------------------");
        System.out.println("Transactions exported to: " + pdfFilePath);
    } catch (DocumentException | IOException e) {
        System.out.println("Error exporting transactions to PDF.");
        e.printStackTrace();
    } finally {
        document.close();
    }
}
private void clearEstaments(String accountNumber) {
    String deleteTransactionsQuery = "DELETE FROM transactions WHERE accountNumber = ?";
    try (PreparedStatement deleteStmt = connTransactions.prepareStatement(deleteTransactionsQuery)) {
        // Set the account number in the query
        deleteStmt.setString(1, accountNumber);
        // Execute the deletion
        int rowsDeleted = deleteStmt.executeUpdate();
        System.out.println(rowsDeleted + " transaction(s) deleted for account: " + accountNumber);
    } catch (SQLException e) {
        System.out.println("Error clearing transactions for account: " + accountNumber);
        e.printStackTrace();
    }
    }

private void deleteUser(String accountNumber) {
    try {
        // Remove from database
        String deleteUserQuery = "DELETE FROM users WHERE accountNumber = ?";
        try (PreparedStatement stmt = conn.prepareStatement(deleteUserQuery)) {
            stmt.setString(1, accountNumber);
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                System.out.println("Failed to delete the user from the database.");
                return;
            }
        }
        // Remove from transactions database
        clearEstaments(accountNumber);
        // Remove from memory
        users.remove(accountNumber);
    } catch (SQLException e) {
        System.out.println("Error deleting the user.");
        e.printStackTrace();
    }
    }

    private void calcLoan(){
        System.out.println("------------------------");
        System.out.print("Enter the loan amount: $");
        double loanAmount = inputScanner.nextDouble();
        inputScanner.nextLine(); // Clear the buffer
        System.out.println("------------------------");
        System.out.print("Enter the amount you pay the loan back in years: ");
        int loanTerm = inputScanner.nextInt();
        inputScanner.nextLine(); // Clear the buffer
        System.out.println("------------------------");
        System.out.print("Enter the annual interest rate (in decimal form)Ex. 0.07: ");
        double annualInterestRate = inputScanner.nextDouble();
        inputScanner.nextLine();
        double total = loanAmount * (1 + (annualInterestRate * loanTerm));
        total = Math.round(total*100)/100; // Round to two decimal places
        System.out.println("------------------------");
        System.out.println("You will owe $" + total + " after " + loanTerm + " years.");
        double monthlyPayment = total / (loanTerm * 12);
        monthlyPayment = Math.round(monthlyPayment*100)/100;
        System.out.println("------------------------");
        System.out.println("Your monthly payment will be $" + monthlyPayment);
    }

private void updateRatesFileOnceADay() {
    try {
        // Check if today's date matches the last update date in the file
        if (isFileUpToDate()) {
            
            return; // Exit if rates are already updated today
        }
        // Fetch new rates from API
        Map<String, String> updatedRates = fetchExchangeRatesFromAPI();
        // Write new rates and today's date to rates.txt
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write("LAST_UPDATE:" + LocalDate.now()); // Write today's date first
            writer.newLine();

            for (Map.Entry<String, String> entry : updatedRates.entrySet()) {
                writer.write(entry.getKey() + ":" + entry.getValue()); // Currency and rate
                writer.newLine();
            }
        }
        System.out.println("Rates file successfully updated with new API data.");
    } catch (Exception e) {
        System.out.println("Failed to update rates file: " + e.getMessage());
        e.printStackTrace();
    }
}

// Function to check if rates.txt is up to date
private boolean isFileUpToDate() {
    try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
        String line = reader.readLine(); // Read the first line
        if (line != null && line.startsWith("LAST_UPDATE:")) {
            String lastUpdate = line.split(":")[1];
            return lastUpdate.equals(LocalDate.now().toString()); // Compare dates
        }
    } catch (IOException e) {
        System.out.println("Rates file not found or unreadable. It will be created.");
    }
    return false; // Default to false if file doesn't exist or is invalid
}

// Function to fetch exchange rates from the API
private Map<String, String> fetchExchangeRatesFromAPI() throws Exception {
    Map<String, String> newRates = new HashMap<>();

    // API call
    URL url = new URL(BASE_URL);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    conn.setRequestMethod("GET");
    int responseCode = conn.getResponseCode();
    if (responseCode == HttpURLConnection.HTTP_OK) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        // Parse JSON response
        JSONObject jsonResponse = new JSONObject(response.toString());
        JSONObject ratesJSON = jsonResponse.getJSONObject("conversion_rates");
        // Store rates in the Map
        for (String currency : ratesJSON.keySet()) {
            newRates.put(currency, String.valueOf(ratesJSON.getDouble(currency)));
        }
    } else {
        throw new Exception("API returned non-OK response: " + responseCode);
    }
    return newRates;
}
private void exchange(){
    // Ensure the file exists in your project directory

        loadExchangeRates();
        System.out.println("------------------------");
        System.out.println("Welcome to currency exchange!");
        System.out.println("Please insert your current currency(Ex. USD): ");
        String current = inputScanner.nextLine().toUpperCase(); //gets current currency
        System.out.println("------------------------");
        System.out.println("Please insert the currency you want to exchange to(Ex. CAD): ");
        String exchange = inputScanner.nextLine().toUpperCase(); // gets exchnage currency
        System.out.println("------------------------");
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
        System.out.println("------------------------");
        System.out.println("Your exchanged amount from " + current +" to " + exchange + " is: $" + Total); //prints out values
    }

}