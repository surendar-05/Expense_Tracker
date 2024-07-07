import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Create_DB {
    static final String DB_URL = "jdbc:mysql://localhost:3306/Expense_Manager";
    static final String USER = "root";
    static final String PASS = "";

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stat = conn.createStatement()) {
            String sql = "CREATE TABLE IF NOT EXISTS expenses (" +
                    "id INT AUTO_INCREMENT PRIMARY KEY," +
                    "date VARCHAR(255) NOT NULL," +
                    "name VARCHAR(255) NOT NULL," +
                    "amount DOUBLE NOT NULL)";
            stat.executeUpdate(sql);
            conn.close();
            System.out.println("Table created successfully (if it didn't already exist).");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
