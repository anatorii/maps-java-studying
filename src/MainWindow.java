import javax.swing.*;
import java.awt.*;

public class MainWindow extends JFrame {
    private static int width = 1000;
    private static int height = 700;
    private JPanel panel;
    private JTextField textField1;
    private JButton поискButton;
    private JTextArea textArea2;
    private JList list1;
    private JSlider slider1;
    private JRadioButton схемаRadioButton;
    private JRadioButton спутникRadioButton;
    private JRadioButton пробкиRadioButton;
    private JPanel panelUp;
    private JPanel panelDown;
    private JButton backButton;
    private JButton nextButton;
    private JLabel titleLabel;
    public int curIndex = 0;
    private Timer timer;

    public MainWindow() {
        super("Карты");
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        this.setSize(MainWindow.width, MainWindow.height);
        this.setLocation(d.width / 2 - MainWindow.width / 2, d.height / 2 - MainWindow.height / 2);
        this.getContentPane().add(panel);

    }
}
