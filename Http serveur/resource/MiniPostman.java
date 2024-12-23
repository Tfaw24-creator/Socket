import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class MiniPostman {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(MiniPostman::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Mini Postman");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(1000, 700);

        JPanel panel = new JPanel(new BorderLayout());

        // Barre d'adresse et méthode HTTP
        JPanel topPanel = new JPanel(new FlowLayout());
        JTextField urlField = new JTextField(50);
        String[] methods = {"GET", "DELETE"};
        JComboBox<String> methodBox = new JComboBox<>(methods);
        JButton sendButton = new JButton("Envoyer");

        topPanel.add(new JLabel("URL:"));
        topPanel.add(urlField);
        topPanel.add(methodBox);
        topPanel.add(sendButton);

        // Interface à onglets
        JTabbedPane tabbedPane = new JTabbedPane();

        // Onglet pour les en-têtes de réponse
        JTextArea responseHeadersArea = new JTextArea();
        responseHeadersArea.setBorder(BorderFactory.createTitledBorder("En-têtes de la Réponse"));
        responseHeadersArea.setEditable(false);
        JScrollPane headersScrollPane = new JScrollPane(responseHeadersArea);
        tabbedPane.addTab("En-têtes de Réponse", headersScrollPane);

        // Onglet pour le corps de la réponse
        JTextArea responseArea = new JTextArea();
        responseArea.setBorder(BorderFactory.createTitledBorder("Corps de la Réponse"));
        responseArea.setEditable(false);
        JScrollPane responseScrollPane = new JScrollPane(responseArea);
        tabbedPane.addTab("Corps de la Réponse", responseScrollPane);

        // Onglet pour l'aperçu
        JEditorPane previewPane = new JEditorPane();
        previewPane.setContentType("text/html");
        previewPane.setEditable(false);
        JScrollPane previewScrollPane = new JScrollPane(previewPane);
        previewScrollPane.setBorder(BorderFactory.createTitledBorder("Aperçu"));
        tabbedPane.addTab("Aperçu", previewScrollPane);

        // Ajouter les composants
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(tabbedPane, BorderLayout.CENTER);
        frame.add(panel);
        frame.setVisible(true);

        // Action pour le bouton "Envoyer"
        sendButton.addActionListener((ActionEvent e) -> {
            String url = urlField.getText();
            String method = (String) methodBox.getSelectedItem();

            try {
                HttpResponse response = sendHttpRequest(url, method);
                responseHeadersArea.setText(response.headers);
                responseArea.setText(response.body);

                // Afficher l'aperçu uniquement pour GET avec du contenu HTML
                if ("GET".equals(method) && isHtmlContent(response.headers)) {
                    previewPane.setText(response.body);
                } else {
                    previewPane.setText("<html><body><h3>Aperçu indisponible pour cette requête.</h3></body></html>");
                }
            } catch (Exception ex) {
                responseArea.setText("Erreur : " + ex.getMessage());
                previewPane.setText("<html><body><h3>Erreur lors de l'exécution de la requête.</h3></body></html>");
            }
        });
    }

    private static HttpResponse sendHttpRequest(String urlString, String method) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod(method);

        // Aucune gestion de corps de requête pour GET ou DELETE
        if (method.equals("DELETE")) {
            connection.setDoOutput(true);
        }

        int status = connection.getResponseCode();
        InputStream inputStream = (status < 400) ? connection.getInputStream() : connection.getErrorStream();

        StringBuilder headers = new StringBuilder();
        connection.getHeaderFields().forEach((key, value) -> {
            headers.append(key).append(": ").append(String.join(", ", value)).append("\n");
        });

        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line).append("\n");
            }
        }

        return new HttpResponse(headers.toString(), response.toString());
    }

    private static boolean isHtmlContent(String headers) {
        return headers.toLowerCase().contains("content-type: text/html");
    }

    private static class HttpResponse {
        String headers;
        String body;

        HttpResponse(String headers, String body) {
            this.headers = headers;
            this.body = body;
        }
    }
}
