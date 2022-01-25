import javax.swing.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Arrays;

/**
 * Klasa odpowiadająca za logowanie się do aplikacji bazodanowej
 */
public class LoginPage extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField textField1;
    private JPasswordField passwordField1;
    private final String url;

    /**
     * Konstruktor ekranu logowanie
     * @param url adres url bazy danych, do której chcemy się podłączyć
     */
    public LoginPage(String url) {
        this.url = url;
        setTitle("Login");
        setContentPane(contentPane);
        setModal(true);
        getRootPane().setDefaultButton(buttonOK);

        buttonOK.addActionListener(e -> onOK());

        buttonCancel.addActionListener(e -> onCancel());

        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });

        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);
    }

    private void onOK() {
        String user = textField1.getText();
        char[] password = passwordField1.getPassword();
        try {
            DriverManager.registerDriver(new com.mysql.cj.jdbc.Driver());
            Connection connection = DriverManager.getConnection(url, user, String.valueOf(password));
            Arrays.fill(password, '0');
            PreparedStatement stmt = connection.prepareStatement("SELECT CURRENT_ROLE()");
            ResultSet rs = stmt.executeQuery();
            rs.next();
            String role = rs.getString(1);
            switch (role) {
                case "`administrator`@`%`" -> {
                    new MyFrame(connection, MyFrame.ADMINISTRATOR);
                    dispose();
                }
                case "`zarządca`@`%`" -> {
                    new MyFrame(connection, MyFrame.ZARZĄDCA);
                    dispose();
                }
                case "`kierowca`@`%`", default -> {
                    new MyFrame(connection, MyFrame.KIEROWCA);
                    dispose();
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }

    public static void main(String[] args) {
        LoginPage dialog = new LoginPage("jdbc:mysql://localhost:3306/lab4");
        dialog.pack();
        dialog.setVisible(true);
    }
}
