import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;

public class MyFrame extends JFrame{
    private final String[][] manipulationPrivileges = {
            {"rodzaj_taboru", "zajezdnia", "tabor", "naprawa", "kurs", "linie_autobusowe", "pracownik"},
            {},
            {"naprawa", "kurs"},
    };
    public static final String[] databaseTables = {"rodzaj_taboru", "zajezdnia", "tabor", "naprawa", "kurs", "linie_autobusowe", "pracownik"};
    public static final int ADMINISTRATOR = 0;
    public static final int KIEROWCA = 1;
    public static final int ZARZĄDCA = 2;
    private final int role;
    private final MyJTable table = new MyJTable();
    private MyDefaultTableModel data;
    private final Connection con;
    private final JPopupMenu jPopupMenu = new JPopupMenu();


    /**
     * Konstrukor klasy MyFrame
     * @param connection połączenie z bazą danych
     * @param role determinuje, jakie uprawnienia będzie miał
     */
    MyFrame(Connection connection, int role){
        this.con = connection;
        this.role = role;
        setTitle("Lab4");
        setLayout(new BorderLayout());
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JMenuItem showTables = new JMenuItem();
        showTables.setText("Tablice");
        showTables.addActionListener(event -> {
            TableSelector dialog = new TableSelector(this);
            dialog.pack();
            dialog.setVisible(true);
                } );
        JMenuItem insertSelect = new JMenuItem();
        insertSelect.addActionListener(event -> {
            JTextField query = new JTextField();
            Object[] message = {
                    "Kwerenda:", query,
            };
            JOptionPane.showConfirmDialog(this, message, "Wykonaj selecta", JOptionPane.OK_CANCEL_OPTION);
            if(query.getText().startsWith("SELECT")) executeSelectQuery(query.getText(), null);
            else executeManipulationQuery(query.getText());
        });
        insertSelect.setText("Wykonaj selecta");
        JMenu showTable = new JMenu();
        showTable.add(showTables);
        showTable.add(insertSelect);
        showTable.setText("Pokaż");
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(showTable);

        JMenuItem executeUsuńTabor = new JMenuItem("Usuń przestarzały tabor");
        executeUsuńTabor.addActionListener(event -> executeManipulationQuery("CALL usuwanieAutomatyczne()"));
        JMenu executeProcedure = new JMenu();
        if(role == MyFrame.ADMINISTRATOR) executeProcedure.add(executeUsuńTabor);
        JMenuItem executeDodajNaprawa = new JMenuItem("Zgloś naprawę");
        executeDodajNaprawa.addActionListener(event -> {
            JTextField date = new JTextField();
            JTextField id_taboru = new JTextField();
            Object[] message = {
                    "Data:", date,
                    "Id_taboru:", id_taboru
            };
            int option = JOptionPane.showConfirmDialog(this, message, "Dodaj naprawę", JOptionPane.OK_CANCEL_OPTION);
            if(option == JOptionPane.OK_OPTION) executeManipulationQuery("CALL naprawa('" + date.getText() + "', " + id_taboru.getText() + ")");
        });
        executeProcedure.add(executeDodajNaprawa);
        JMenuItem executeDodajKursy = new JMenuItem("Dodaj kursy");
        executeDodajKursy.addActionListener(event ->{
            JTextField date = new JTextField();
            Object[] message = {
                    "Data:", date,
            };
            int option = JOptionPane.showConfirmDialog(this, message, "Podaj datę", JOptionPane.OK_CANCEL_OPTION);
            if(option == JOptionPane.OK_OPTION) executeManipulationQuery("CALL generowanieKursów('" + date.getText() + "')");
        });
        executeProcedure.add(executeDodajKursy);
        executeProcedure.setText("Wykonaj");
        if(role != MyFrame.KIEROWCA) menuBar.add(executeProcedure);
        setJMenuBar(menuBar);

        Action action = new AbstractAction("Usuń") {
            @Override
            public void actionPerformed(ActionEvent e) {
                JPopupMenu parent = (JPopupMenu) SwingUtilities.getAncestorOfClass(
                        JPopupMenu.class, (Component) e.getSource());
                JTable invoker = (JTable) parent.getInvoker();
                Point p = (Point) invoker.getClientProperty("popupTriggerLocation");
                String id = table.getValueAt(invoker.rowAtPoint(p),0).toString();
                String query = "DELETE FROM " + data.getTableName() + " WHERE " + data.getColumnName(0) + " = '" + id + "'";
                executeManipulationQuery(query);
                executeSelectQuery("SELECT * FROM " + data.getTableName(), data.getTableName());
            }
        };
        jPopupMenu.add(action);

        JMenuItem insertRow = new JMenuItem("Wstaw");
        insertRow.addActionListener(e -> {
            String[] sender = Arrays.copyOfRange(data.getColumnNames(), 1, data.getColumnNames().length);
            TableAdder dialog = new TableAdder(sender, data.getTableName(), this);
            //dialog.pack();
            dialog.setVisible(true);
        });
        jPopupMenu.add(insertRow);


        table.getTableHeader().setReorderingAllowed(false);
        add(new JScrollPane(table), BorderLayout.CENTER);

        executeSelectQuery("SELECT * FROM " + databaseTables[2], databaseTables[2]);
        pack();

        setVisible(true);

    }

    public void executeManipulationQuery(String query){
        try {
            PreparedStatement ps = con.prepareStatement(query);
            ps.executeUpdate();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Metoda wykonuje zapytanie typu select, a następnie wywołuje metody {@link #updateTable(ResultSet, String)}
     * @param query
     * @param tableName
     */
    public void executeSelectQuery(String query, String tableName){
        try {
            PreparedStatement ps = con.prepareStatement(query, ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = ps.executeQuery();
            updateTable(rs, tableName);
            setPrivileges(tableName);
        } catch (SQLException | IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     *
     * @param rs
     * @param tableName
     * @throws IllegalArgumentException
     * @throws SQLException
     */
    private void updateTable(ResultSet rs, String tableName) throws IllegalArgumentException, SQLException{
        if(rs == null) throw new IllegalArgumentException("updateTable: Argument nie może być nullem");
        String[] columns = getColumns(rs);
        String[][] resultTable = printQuery(rs);
        data = new MyDefaultTableModel(resultTable,columns, tableName);
        data.addTableModelListener(e -> {
            System.out.println("cd");
            if(table.getSelectedRow() >= 0){
                String id = table.getValueAt(table.getSelectedRow(),0).toString();
                String id_column = data.getColumnName(0);
                String column = data.getColumnName(table.getSelectedColumn());
                String newValue = table.getValueAt(table.getSelectedRow(), table.getSelectedColumn()).toString();
                String query = "UPDATE " + data.getTableName() + " SET " + column + "= " + '"' + newValue + '"' + " WHERE " + id_column + "=" + id;
                executeManipulationQuery(query);
                executeSelectQuery("SELECT * FROM " + data.getTableName(), data.getTableName());
            }
        });
        if(tableName != null) setTitle("Lab4 - " + tableName);
        table.setModel(data);
        table.addNonEditableColumns(0);
    }

    /**
     * Metoda pobierająca z {@link ResultSet} dane o kolumnach w tym zapytaniu
     * @param rs ResultSet, który będzie analizowany
     * @return listę kolumn w tym zapytaniu
     * @throws SQLException
     */
    private String[] getColumns(ResultSet rs) throws SQLException{
        String[] columns;
        ResultSetMetaData rsmd = rs.getMetaData();
        int columnCount = rsmd.getColumnCount();
        columns = new String[columnCount];
        for(int i=1; i<=columnCount; i++){
            columns[i-1] = rsmd.getColumnName(i);
        }
        return columns;
    }

    /**
     * Metoda pobierająca z {@link ResultSet} dane o wierszach w tym zapytaniu
     * @param rs ResultSet, który będzie analizowany
     * @return listę rekordów w tym zapytaniu
     * @throws SQLException
     */
    private String[][] printQuery(ResultSet rs) throws SQLException{
        String[][] table;
        ResultSetMetaData rsmd = rs.getMetaData();
        int cols = rsmd.getColumnCount();
        rs.last();
        int rows = rs.getRow();
        rs.beforeFirst();
        table = new String[rows][cols];
        for(int i = 1; i<= rows; i++){
            rs.next();
            for(int j = 1; j<=cols; j++){
                table[i-1][j-1] = rs.getString(j);
            }
        }
        return table;
    }

    /**
     * Metoda sprawdzająca, czy dany użyytkownik
     * @param tableName
     */
    public void setPrivileges(String tableName){
        table.deleteNonEditableColumns();
        ArrayList<String> list = new ArrayList<>(Arrays.asList(manipulationPrivileges[role]));
        if(list.contains(tableName)){
            table.setComponentPopupMenu(jPopupMenu);
            table.addNonEditableColumns(0);
        }
        else{
            table.setComponentPopupMenu(null);
            for(int i = 0; i < table.getModel().getColumnCount(); i++){
                table.addNonEditableColumns(i);
            }
        }
    }

    public static void main(String[] args){
        try {
            DriverManager.registerDriver(new com.mysql.cj.jdbc.Driver());
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:3306/lab4", "default_administrator", "mateusz726");
            new MyFrame(connection, MyFrame.ADMINISTRATOR);
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(null, e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }

    }
}

/**
 * Rozszerzenie klasy {@link JTable}, definiuje możliwość
 * edytowanie komórek
 */
class MyJTable extends JTable{
    private final ArrayList<Integer> nonEditableColumns = new ArrayList<>();

    @Override
    public boolean isCellEditable(int row, int column){
        return !nonEditableColumns.contains(column);
    }

    public void addNonEditableColumns(int column){
        nonEditableColumns.add(column);
    }

    public void deleteNonEditableColumns(){
        nonEditableColumns.clear();
    }

    @Override
    public Point getPopupLocation(MouseEvent event) {
        setPopupTriggerLocation(event);
        return super.getPopupLocation(event);
    }

    protected void setPopupTriggerLocation(MouseEvent event) {
        putClientProperty("popupTriggerLocation",
                event != null ? event.getPoint() : null);
    }
}

/**
 * Rozszenie klasy {@link DefaultTableModel}, przechowuje informacje o nazwie tablicy
 */
class MyDefaultTableModel extends DefaultTableModel{
    private final String table;

    public String getTableName() {
        return table;
    }

    public String[] getColumnNames() {
        String[] columnNames = new String[this.getColumnCount()];
        for (int i = 0, columnCount = this.getColumnCount(); i < columnCount; i++) {
            columnNames[i] = this.getColumnName(i);
        }
        return columnNames;
    }

    MyDefaultTableModel(Object[][] data, Object[] columnNames, String tableName){
        super(data, columnNames);
        this.table = tableName;
    }
}
