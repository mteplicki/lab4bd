import javax.swing.*;
import java.awt.event.*;

public class TableSelector extends JDialog {
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField textField1;
    private JTextField textField2;
    private JTextField textField3;
    private JComboBox comboBox1;
    private JComboBox comboBox2;

    private MyFrame myFrame;

    public TableSelector(MyFrame myFrame) {
        this.myFrame = myFrame;
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
        // add your code here
        String table = (String) comboBox2.getSelectedItem();
        String warunek = textField2.getText();
        String sortowanie = textField1.getText();
        String isASC = (String)comboBox1.getSelectedItem();
        String query = "SELECT * FROM " + table + " ";
        warunek = warunek.trim();
        sortowanie = sortowanie.trim();
        if(!warunek.isBlank()) query = query + "WHERE " + warunek + " ";
        if(!sortowanie.isBlank()) query = query + "ORDER BY " + sortowanie + " " + isASC;
        myFrame.executeSelectQuery(query, table);
        dispose();
    }

    private void onCancel() {
        // add your code here if necessary
        dispose();
    }
}
