import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.*;

public class TableAdder extends JDialog {
    private final JTable table1 = new JTable();
    private final String[] columns;
    private final String tableName;
    private final MyFrame frame;

    public TableAdder(String[] columns, String tableName, MyFrame frame) {
        this.tableName = tableName;
        this.columns = columns;
        TableModel tableModel;
        this.frame = frame;
        JPanel contentPane = new JPanel();
        setContentPane(contentPane);
        setModal(true);
        tableModel = new DefaultTableModel(new String[1][columns.length], columns);
        table1.setModel(tableModel);
        JScrollPane scrollPane1 = new JScrollPane(table1);
        setSize(new Dimension(640,130));
        setPreferredSize(new Dimension(640,150));
        JButton buttonOK = new JButton("Ok");
        getRootPane().setDefaultButton(buttonOK);
        buttonOK.addActionListener(e -> onOK());
        JButton buttonCancel = new JButton("Cancel");
        buttonCancel.addActionListener(e -> onCancel());
        // call onCancel() when cross is clicked
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                onCancel();
            }
        });
        contentPane.setLayout(new BorderLayout());
        JPanel buttonPane = new JPanel();
        buttonPane.setLayout(new GridLayout(1,4));
        buttonPane.add(new JLabel());
        buttonPane.add(new JLabel());
        buttonPane.add(buttonOK);
        buttonPane.add(buttonCancel);
        contentPane.add(buttonPane, BorderLayout.SOUTH);
        contentPane.add(scrollPane1, BorderLayout.CENTER);
        table1.doLayout();
        table1.setRowHeight(30);
        // call onCancel() on ESCAPE
        contentPane.registerKeyboardAction(e -> onCancel(), KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT);

    }

    private void onOK() {
        // add your code here
        StringBuilder queryBuilder = new StringBuilder("INSERT INTO " + tableName + "( ");
        for(String column : columns){
            queryBuilder.append(column).append(", ");
        }
        String query = queryBuilder.toString();
        query = query.substring(0,query.length() - 2);
        StringBuilder queryBuilder1 = new StringBuilder(query + " ) VALUES ( ");
        for(int i = 0; i< table1.getColumnCount(); i++){
            queryBuilder1.append(table1.getModel().getValueAt(0, i)).append(", ");
        }
        query = queryBuilder1.toString();
        query = query.substring(0,query.length() - 2);
        query = query + " )";

        frame.executeManipulationQuery(query);
        frame.executeSelectQuery("SELECT * FROM " + tableName, tableName);
        dispose();

    }

    private void onCancel() {
        dispose();
    }
}