import org.json.JSONArray;
import org.json.JSONObject;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class MainWindow extends JFrame {
    private static int width = 1100;
    private static int height = 700;
    private JPanel panel;
    private JTextField search;
    private JButton searchButton;
    private JTextArea textArea;
    private JList<String> list;
    private JSlider slider;
    private JRadioButton schemaRadioButton;
    private JRadioButton satelliteRadioButton;
    private JPanel mapPanel;
    private JCheckBox jamsCheckBox;
    private boolean showJamsLayout = false;
    private JSONArray geodata = null;
    private Image images[][];
    private JRadioButton curRadio;
    public Image curImage = null;
    public int curIndex = -1;
    public int curSliderValue = 10;
    private String curSearch = "";

    public MainWindow() {
        super("Карты");
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);

        Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
        this.setSize(MainWindow.width, MainWindow.height);
        this.setLocation(d.width / 2 - MainWindow.width / 2, d.height / 2 - MainWindow.height / 2);
        this.getContentPane().add(panel);

        slider.setValue(curSliderValue);
        search.setText(curSearch);
        schemaRadioButton.setSelected(true);
        curRadio = getSelectedRadio();

        searchButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    searchButton_actionPerformed(e);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        slider.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                try {
                    slider_stateChanged(e);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        schemaRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    radio_actionPerformed(e);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        satelliteRadioButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    radio_actionPerformed(e);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        jamsCheckBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    jams_actionPerformed(e);
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
        list.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                ListSelectionModel lsm = ((JList<String>) e.getSource()).getSelectionModel();
                System.out.println("valueChanged: " + lsm.getMinSelectionIndex());

                curIndex = lsm.getMinSelectionIndex();
                try {
                    listSelected();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
            }
        });
    }

    private void jams_actionPerformed(ActionEvent e) throws IOException {
        showJamsLayout = ((JCheckBox) e.getSource()).isSelected();
        loadImage();
        mapPanel.repaint();
    }

    private void radio_actionPerformed(ActionEvent e) throws IOException {
        curRadio = getSelectedRadio();
        loadImage();
        mapPanel.repaint();
    }

    private void slider_stateChanged(ChangeEvent e) throws IOException {
        curSliderValue = slider.getValue();
        loadImage();
        mapPanel.repaint();
    }

    private void listSelected() throws IOException {
        showObjectInfo();
        loadImage();
        mapPanel.repaint();
    }

    private void showObjectInfo() {
        if (curIndex == -1) {
            return;
        }
        JSONObject obj = ((JSONObject) geodata.get(curIndex));
        JSONObject props = obj.getJSONObject("properties");
        HashMap<String, String> info = new HashMap<>();

        info.put("название", props.getString("name"));

        if (props.has("description")) {
            info.put("описание", props.getString("description"));
        }

        if (props.has("CompanyMetaData")) {
            JSONObject metadata = props.getJSONObject("CompanyMetaData");
            info.put("адрес", metadata.getString("address"));
            if (metadata.has("Hours")) {
                info.put("часы работы", metadata.getJSONObject("Hours").getString("text"));
            }
            if (metadata.has("Phones")) {
                String phones = "";
                for (Object phone : metadata.getJSONArray("Phones")) {
                    phones += !phones.equals("") ? ", " : "";
                    phones += ((JSONObject) phone).getString("formatted");
                }
                info.put("телефоны", phones);
            }
        } else if (props.has("GeocoderMetaData")) {
            JSONObject metadata = props.getJSONObject("GeocoderMetaData");
            info.put("адрес", metadata.getString("text"));
            info.put("вид объекта", metadata.getString("kind"));
        }

        StringBuilder text = new StringBuilder();
        for (String key : info.keySet()) {
            text.append(key).append(": ").append(info.get(key)).append("\n");
        }

        textArea.setText(text.toString());
    }

    private JSONArray doSearch() throws IOException {
        String apikey = "apikey=" + App.dotenv.get("API_KEY_BIZ");
        String geocode = "&text=" + encodeValue(curSearch);
        String uri = "https://search-maps.yandex.ru/v1?" + apikey + "&lang=ru_RU&format=json" + geocode;

        String response = getResponse(uri);

        return new JSONObject(response).getJSONArray("features");
    }

    private static String getResponse(String uri) throws IOException {
        System.out.println(uri);
        HttpURLConnection connection = (HttpURLConnection) (new URL(uri)).openConnection();
        connection.setRequestProperty("Content-Type", "application/json");

        InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8);
        BufferedReader bufferedReader = new BufferedReader(reader);
        return bufferedReader.lines().collect(Collectors.joining(""));
    }

    private static String encodeValue(String value) {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException(ex.getCause());
        }
    }

    private JRadioButton getSelectedRadio() {
        JRadioButton radio;
        if (satelliteRadioButton.isSelected()) {
            radio = satelliteRadioButton;
        } else {
            radio = schemaRadioButton;
        }
        return radio;
    }

    public void searchButton_actionPerformed(ActionEvent e) throws IOException {
        clearImages();
        curIndex = -1;
        curImage = null;
        curSliderValue = 10;
        curRadio = schemaRadioButton;
        curSearch = search.getText();

        slider.setValue(curSliderValue);
        mapPanel.repaint();
        textArea.setText("");

        if (!curSearch.equals("")) {
            geodata = doSearch();
            fillList();
        }
    }

    public void fillList() {
        String bizText = "";
        DefaultListModel<String> model = new DefaultListModel<String>();
        for (Object obj : geodata) {
            JSONObject feature = (JSONObject) obj;
            JSONObject properties = feature.getJSONObject("properties");
            bizText = properties.has("CompanyMetaData")
                    ? properties.getString("name") + " (организация)"
                    : properties.getString("name");
            model.add(model.getSize(),properties.getString("name") + " " + bizText);
        }
        list.setModel(model);
    }

    private void clearImages() {
        images = null;
    }

    private void initImagesCache() {
        if (geodata != null) {
            images = new Image[geodata.length()][21];
            for (int i = 0; i < geodata.length(); i++) {
                for (int j = 0; j < 21; j++) {
                    images[i][j] = null;
                }
            }
        }
    }

    public void loadImage() throws IOException {
        System.out.println("loadImages: " + curIndex);
        if (curIndex == -1) {
            return;
        }
        if (images == null) {
            initImagesCache();
        }
        if (images[curIndex][curSliderValue] == null) {
            JSONArray coords = ((JSONObject) geodata.get(curIndex)).getJSONObject("geometry").getJSONArray("coordinates");
            String lat = String.valueOf(coords.get(0));
            String lon = String.valueOf(coords.get(1));
            String point = "&ll="+lat+","+lon;
            String pt = "&pt="+lat+","+lon+",flag";
            String apikey = App.dotenv.get("API_KEY");
            String zString = "&z=" + curSliderValue;
            String uri = "https://static-maps.yandex.ru/v1?size=650,450" + "&apikey=" + apikey + point + zString + pt;
            URL url = new URL(uri);
            images[curIndex][curSliderValue] = ImageIO.read(url);
        }
        curImage = images[curIndex][curSliderValue];
        System.out.println("image loaded: " + curIndex + " " + curSliderValue);
    }

    private void createUIComponents() {
        mapPanel = new MapPanel();
        mapPanel.setLayout(new BorderLayout());
    }
}

class MapPanel extends JPanel {
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        MainWindow frame = (MainWindow) SwingUtilities.getWindowAncestor(this);
        g.clearRect(0, 0, getSize().width, getSize().height);
        if (frame.curImage != null) {
            g.drawImage(frame.curImage, (getSize().width - 650) / 2, (getSize().height - 450) / 2, null);
        }
    }
}
