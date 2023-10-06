import io.github.cdimascio.dotenv.Dotenv;

public class App {
    static Dotenv dotenv = null;

    public static void main(String[] args) {
        dotenv = Dotenv.configure()
                .directory("assets")
                .filename(".env")
                .load();

        MainWindow mainWindow = new MainWindow();
        mainWindow.setVisible(true);
    }
}