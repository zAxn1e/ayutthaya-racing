package core.data;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.sql.SQLException;
import java.util.List;

public final class LeaderboardDialog extends JDialog {
    private final DefaultTableModel model;

    private LeaderboardDialog(JFrame owner) {
        super(owner, "Leaderboard", true);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(640, 500);
        setResizable(false);
        setLocationRelativeTo(owner);

        JPanel root = new JPanel(new BorderLayout(10, 10));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        root.setBackground(new Color(18, 24, 38));

        JLabel title = new JLabel("Leaderboard", SwingConstants.CENTER);
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
        title.setForeground(new Color(240, 223, 140));
        root.add(title, BorderLayout.NORTH);

        model = new DefaultTableModel(new Object[]{"Rank", "Player", "Score", "Saved At"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        JTable table = new JTable(model);
        table.setRowHeight(28);
        table.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        table.getTableHeader().setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        table.getTableHeader().setBackground(new Color(37, 46, 71));
        table.getTableHeader().setForeground(new Color(230, 236, 250));
        table.setBackground(new Color(30, 39, 58));
        table.setForeground(new Color(236, 240, 250));
        table.setGridColor(new Color(52, 63, 95));

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(600, 360));
        root.add(scrollPane, BorderLayout.CENTER);

        JButton close = new JButton("Back");
        close.addActionListener(e -> dispose());
        JPanel footer = new JPanel();
        footer.setOpaque(false);
        footer.add(close);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
        reloadScores();
    }

    private void reloadScores() {
        model.setRowCount(0);
        try {
            List<AppDatabase.ScoreEntry> scores = AppDatabase.getTopScores(20);
            int rank = 1;
            for (AppDatabase.ScoreEntry score : scores) {
                model.addRow(new Object[]{
                        rank++,
                        score.username(),
                        score.score(),
                        score.createdAt()
                });
            }
        } catch (SQLException ex) {
            model.addRow(new Object[]{"-", "Database error", "-", ex.getMessage()});
        }
    }

    public static void open(JFrame owner) {
        LeaderboardDialog dialog = new LeaderboardDialog(owner);
        dialog.setVisible(true);
    }
}
