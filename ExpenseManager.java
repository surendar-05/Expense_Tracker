import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;
import java.text.NumberFormat;
import java.util.Locale;

public class ExpenseManager {
    private JFrame frame;
    private JTextField salaryField;
    private JTextField expenseField;
    private JTextField expenseDateField;
    private JTextField expenseNameField;
    private JButton salaryButton;
    private JButton addButton;
    private JButton deleteButton;
    private JButton editButton;
    private DefaultListModel<String> expenseListModel;
    private JList<String> expenseList;
    private JLabel remainingLabel;
    private double monthlySalary = 0.0;
    private double totalExpenses = 0.0;

    private static final String DB_URL = "jdbc:mysql://localhost:3306/Expense_Manager";
    private static final String USER = "root";
    private static final String PASS = "";

    public ExpenseManager() {
        frame = new JFrame("Expense Manager");
        frame.setLayout(new BorderLayout());
        frame.setSize(600, 400);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        expenseListModel = new DefaultListModel<>();
        expenseList = new JList<>(expenseListModel);
        JScrollPane expenseScrollPane = new JScrollPane(expenseList);
        frame.add(expenseScrollPane, BorderLayout.CENTER);

        JPanel expenseInputPanel = new JPanel();
        salaryButton = new JButton("Enter Salary");
        salaryField = new JTextField(10);
        expenseDateField = new JTextField(10);
        expenseNameField = new JTextField(10);
        expenseField = new JTextField(10);

        addButton = new JButton("Add Expense");
        deleteButton = new JButton("Delete Expense");
        editButton = new JButton("Edit Expense");

        remainingLabel = new JLabel("Remaining Balance: $0.00");

        salaryButton.addActionListener(e -> {
            String salaryText = salaryField.getText();
            if (!salaryText.isEmpty()) {
                try {
                    monthlySalary = Double.parseDouble(salaryText);
                    totalExpenses = 0.0; 
                    loadExpensesFromDB();
                    updateRemainingLabel();
                    salaryField.setText("");
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Invalid salary amount!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        addButton.addActionListener(e -> {
            String expenseDate = expenseDateField.getText();
            String expenseName = expenseNameField.getText();
            String expenseText = expenseField.getText();

            if (!expenseText.isEmpty() && !expenseName.isEmpty() && !expenseDate.isEmpty()) {
                try {
                    double expenseAmount = Double.parseDouble(expenseText);
                    if (expenseAmount <= monthlySalary - totalExpenses) {
                        addExpenseToDB(expenseDate, expenseName, expenseAmount);
                        expenseListModel.addElement(
                                expenseDate + " : " + expenseName + " : $" + String.format("%.2f", expenseAmount));
                        totalExpenses += expenseAmount;
                        updateRemainingLabel();
                        expenseField.setText("");
                        expenseNameField.setText("");
                        expenseDateField.setText("");
                    } else {
                        JOptionPane.showMessageDialog(frame, "Expense exceeds remaining balance!", "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(frame, "Invalid expense amount!", "Error", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        deleteButton.addActionListener(e -> {
            int selectedIndex = expenseList.getSelectedIndex();
            if (selectedIndex != -1) {
                String selectedExpense = expenseListModel.getElementAt(selectedIndex);
                String[] parts = selectedExpense.split(": \\$");
                if (parts.length == 2) {
                    String expenseAmountStr = parts[1].trim();
                    try {
                        double expenseAmount = Double.parseDouble(expenseAmountStr);
                        expenseListModel.remove(selectedIndex);
                        deleteExpenseFromDB(selectedExpense);
                        totalExpenses -= expenseAmount;
                        updateRemainingLabel();
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(frame, "Invalid expense amount!", "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        editButton.addActionListener(e -> {
            int selectedIndex = expenseList.getSelectedIndex();
            if (selectedIndex != -1) {
                String selectedExpense = expenseListModel.getElementAt(selectedIndex);
                String[] parts = selectedExpense.split(": \\$");
                if (parts.length == 2) {
                    String expenseDate = parts[0].split(" : ")[0].trim();
                    String expenseName = parts[0].split(" : ")[1].trim();
                    String expenseAmountStr = parts[1].trim();
                    String newExpenseName = JOptionPane.showInputDialog(frame, "Enter new expense name:", expenseName);
                    String newExpenseDate = JOptionPane.showInputDialog(frame, "Enter new expense date:", expenseDate);
                    String newExpenseAmountStr = JOptionPane.showInputDialog(frame, "Enter new expense amount:",
                            expenseAmountStr);
                    try {
                        double newExpenseAmount = Double.parseDouble(newExpenseAmountStr);
                        double originalExpenseAmount = Double.parseDouble(expenseAmountStr);
                        updateExpenseInDB(selectedExpense, newExpenseDate, newExpenseName, newExpenseAmount);
                        expenseListModel.set(selectedIndex, newExpenseDate + " : " + newExpenseName + " : $"
                                + String.format("%.2f", newExpenseAmount));
                        totalExpenses += (newExpenseAmount - originalExpenseAmount);
                        updateRemainingLabel();
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(frame, "Invalid expense amount!", "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }
        });

        expenseInputPanel.add(new JLabel("Enter Monthly Salary: $"));
        expenseInputPanel.add(salaryField);
        expenseInputPanel.add(salaryButton);
        expenseInputPanel.add(new JLabel("Enter Expense Name:"));
        expenseInputPanel.add(expenseNameField);
        expenseInputPanel.add(new JLabel("Enter Expense Date:"));
        expenseInputPanel.add(expenseDateField);
        expenseInputPanel.add(new JLabel("Enter Expense: $"));
        expenseInputPanel.add(expenseField);
        expenseInputPanel.add(addButton);
        expenseInputPanel.add(deleteButton);
        expenseInputPanel.add(editButton);

        frame.add(expenseInputPanel, BorderLayout.SOUTH);
        frame.add(remainingLabel, BorderLayout.NORTH);

        frame.setVisible(true);

        loadExpensesFromDB();
    }

    private void addExpenseToDB(String date, String name, double amount) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                PreparedStatement pstmt = conn
                        .prepareStatement("INSERT INTO expenses (date, name, amount) VALUES (?, ?, ?)")) {
            pstmt.setString(1, date);
            pstmt.setString(2, name);
            pstmt.setDouble(3, amount);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteExpenseFromDB(String expense) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                PreparedStatement pstmt = conn
                        .prepareStatement("DELETE FROM expenses WHERE CONCAT(date, ' : ', name, ' : $', amount) = ?")) {
            pstmt.setString(1, expense);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateExpenseInDB(String originalExpense, String newDate, String newName, double newAmount) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                PreparedStatement pstmt = conn.prepareStatement(
                        "UPDATE expenses SET date = ?, name = ?, amount = ? WHERE CONCAT(date, ' : ', name, ' : $', amount) = ?")) {
            pstmt.setString(1, newDate);
            pstmt.setString(2, newName);
            pstmt.setDouble(3, newAmount);
            pstmt.setString(4, originalExpense);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadExpensesFromDB() {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT date, name, amount FROM expenses")) {
            totalExpenses = 0.0;
            expenseListModel.clear();
            while (rs.next()) {
                String date = rs.getString("date");
                String name = rs.getString("name");
                double amount = rs.getDouble("amount");
                expenseListModel.addElement(date + " : " + name + " : $" + String.format("%.2f", amount));
                totalExpenses += amount;
            }
            updateRemainingLabel();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateRemainingLabel() {
        double remainingBalance = monthlySalary - totalExpenses;
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(Locale.US);
        remainingLabel.setText("Remaining Balance: " + currencyFormat.format(remainingBalance));
        
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ExpenseManager::new);
    }
}
