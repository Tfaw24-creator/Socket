import java.io.*;
import java.net.*;
import java.util.*;
import java.nio.file.*;


public class ServeurHttp {

    private static  int PORT ;
    private static  String DOCUMENT_ROOT ;
    
    public static void main(String[] args) {
        try{
            ConfigLoader conf = new ConfigLoader("config.txt");

            PORT = conf.getInt("port");
            DOCUMENT_ROOT = conf.get("document_root");
             }
             catch(IOException e){
                e.printStackTrace();
             }
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            System.out.println("Server is listening on port " + PORT);
            while (true) {
                Socket socket = serverSocket.accept();
                handleClient(socket);
            }
        } catch (IOException ex) {
            System.err.println("Server error: " + ex.getMessage());
        }
    }

    private static void handleClient(Socket socket) {
        try (InputStream input = socket.getInputStream();
             OutputStream rawOut = socket.getOutputStream();
             PrintWriter out = new PrintWriter(rawOut, true)) {
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(input));
            String requestLine = reader.readLine();
            if (requestLine == null || requestLine.isEmpty()) {
                return;
            }
            
            String[] tokens = requestLine.split(" ");
            if (tokens.length < 3) {
                send400(rawOut);
                return;
            }
            
            String method = tokens[0];
            String fullPath = URLDecoder.decode(tokens[1], "UTF-8");
            String path = fullPath.split("\\?")[0];
            
            if (!method.equals("GET") && !method.equals("POST") && !method.equals("DELETE")) {
                send400(rawOut);
                return;
            }
            
            File file = new File(DOCUMENT_ROOT, path);
            String normalizedPath = file.getCanonicalPath();
            
            // Vérification de sécurité pour éviter le path traversal
            if (!normalizedPath.startsWith(new File(DOCUMENT_ROOT).getCanonicalPath())) {
                send403(rawOut);
                return;
            }
            
            if ("DELETE".equals(method)) {
                handleDeleteRequest(file, rawOut);
            } else if ("POST".equals(method)) {
                handlePostRequest(file, reader, rawOut);
            } else if ("GET".equals(method)) {
                if (file.isDirectory()) {
                    listDirectory(file, rawOut);
                } else if (!file.exists()) {
                    send404(rawOut);
                } else if (path.endsWith(".php")) {
                    executePhpFile(file, rawOut, fullPath.contains("?") ? fullPath.split("\\?", 2)[1] : "");
                } else if (path.endsWith(".json")){
                     sendFile(file, rawOut);
                }else {
                    sendFile(file, rawOut);
                }
            }
        } catch (IOException ex) {
            System.err.println("Client error: " + ex.getMessage());
        }
    }

    private static void handleDeleteRequest(File file, OutputStream rawOut) throws IOException {
        try (PrintWriter out = new PrintWriter(rawOut, true)) {
            // Vérifications de sécurité
            if (!file.exists()) {
                send404(rawOut);
                return;
            }

            if (file.isDirectory()) {
                send403(rawOut, "Cannot delete directories");
                return;
            }

            // Vérifier les permissions
            if (!file.canWrite()) {
                send403(rawOut, "No permission to delete this file");
                return;
            }

            try {
                // Utiliser Files.delete() pour une meilleure gestion des erreurs
                Files.delete(file.toPath());
                
                // Envoyer la réponse de succès
                out.println("HTTP/1.1 200 OK");
                out.println("Content-Type: text/html; charset=UTF-8");
                out.println();
                out.println("<!DOCTYPE html>");
                out.println("<html><body>");
                out.println("<h1>File successfully deleted</h1>");
                out.println("<p>The file '" + file.getName() + "' has been deleted.</p>");
                out.println("</body></html>");
                
            } catch (SecurityException e) {
                send403(rawOut, "Security error: " + e.getMessage());
            } catch (IOException e) {
                // Log l'erreur pour le débogage
                System.err.println("Error deleting file: " + e.getMessage());
                send500(rawOut, "Could not delete file: " + e.getMessage());
            }
        }
    }

    private static void send403(OutputStream rawOut, String message) throws IOException {
        try (PrintWriter out = new PrintWriter(rawOut, true)) {
            out.println("HTTP/1.1 403 Forbidden");
            out.println("Content-Type: text/html; charset=UTF-8");
            out.println();
            out.println("<!DOCTYPE html>");
            out.println("<html><body>");
            out.println("<h1>403 Forbidden</h1>");
            out.println("<p>" + message + "</p>");
            out.println("</body></html>");
        }
    }

    private static void send500(OutputStream rawOut, String message) throws IOException {
        try (PrintWriter out = new PrintWriter(rawOut, true)) {
            out.println("HTTP/1.1 500 Internal Server Error");
            out.println("Content-Type: text/html; charset=UTF-8");
            out.println();
            out.println("<!DOCTYPE html>");
            out.println("<html><body>");
            out.println("<h1>500 Internal Server Error</h1>");
            out.println("<p>" + message + "</p>");
            out.println("</body></html>");
        }
    }

    // [Le reste des méthodes reste identique...]
    
    private static void handlePostRequest(File file, BufferedReader reader, OutputStream rawOut) throws IOException {
        String line;
        int contentLength = 0;
        String contentType = "application/x-www-form-urlencoded";

        while (!(line = reader.readLine()).isEmpty()) {
            if (line.toLowerCase().startsWith("content-length:")) {
                contentLength = Integer.parseInt(line.split(":")[1].trim());
            } else if (line.toLowerCase().startsWith("content-type:")) {
                contentType = line.split(":")[1].trim();
            }
        }

        char[] body = new char[contentLength];
        reader.read(body, 0, contentLength);
        String requestBody = new String(body);

        if (file.exists() && file.getName().endsWith(".php")) {
            executePhpFileForPost(file, rawOut, requestBody, contentLength, contentType);
        } else {
            send404(rawOut);
        }
    }

    private static void executePhpFileForPost(File file, OutputStream rawOut, String requestBody, 
                                            int contentLength, String contentType) throws IOException {
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("REQUEST_METHOD", "POST");
        env.put("SCRIPT_FILENAME", file.getAbsolutePath());
        env.put("CONTENT_LENGTH", String.valueOf(contentLength));
        env.put("CONTENT_TYPE", contentType);
        env.put("REDIRECT_STATUS", "200");

        ProcessBuilder pb = new ProcessBuilder("php-cgi");
        pb.environment().putAll(env);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedWriter processInput = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
            processInput.write(requestBody);
            processInput.flush();
        }

        try (BufferedReader processOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
             PrintWriter out = new PrintWriter(rawOut, true)) {

            boolean headersSent = false;

            String line;
            while ((line = processOutput.readLine()) != null) {
                if (!headersSent && line.trim().isEmpty()) {
                    out.println("HTTP/1.1 200 OK");
                    out.println("Content-Type: text/html; charset=UTF-8");
                    out.println();
                    headersSent = true;
                } else if (headersSent) {
                    out.println(line);
                }
            }

            if (!headersSent) {
                System.err.println("Erreur : Aucun en-tête PHP valide retourné.");
                send500(rawOut);
            }
        }
    }

    private static void executePhpFile(File file, OutputStream rawOut, String queryString) throws IOException {
        Map<String, String> env = new HashMap<>(System.getenv());
        env.put("QUERY_STRING", queryString);
        env.put("REQUEST_METHOD", "GET");
        env.put("SCRIPT_FILENAME", file.getAbsolutePath());
        env.put("REDIRECT_STATUS", "200");

        ProcessBuilder pb = new ProcessBuilder("php-cgi");
        pb.environment().putAll(env);
        pb.redirectErrorStream(true);

        Process process = pb.start();

        try (BufferedReader processOutput = new BufferedReader(new InputStreamReader(process.getInputStream()));
             PrintWriter out = new PrintWriter(rawOut, true)) {

            boolean headersSent = false;

            String line;
            while ((line = processOutput.readLine()) != null) {
                if (!headersSent && line.trim().isEmpty()) {
                    out.println("HTTP/1.1 200 OK");
                    out.println("Content-Type: text/html; charset=UTF-8");
                    out.println();
                    headersSent = true;
                } else if (headersSent) {
                    out.println(line);
                }
            }

            if (!headersSent) {
                System.err.println("Erreur : Aucun en-tête PHP valide retourné.");
                send500(rawOut);
            }
        }
    }

    private static void listDirectory(File directory, OutputStream rawOut) throws IOException {
        try (PrintWriter out = new PrintWriter(rawOut, true)) {
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: text/html; charset=UTF-8");
            out.println();
            out.println("<!DOCTYPE html>");
            out.println("<html><body>");
            out.println("<h1>Index of " + directory.getName() + "</h1>");
            out.println("<ul>");

            File[] files = directory.listFiles();
            if (files != null) {
                Arrays.sort(files);
                for (File file : files) {
                    String link = file.getName() + (file.isDirectory() ? "/" : "");
                    out.println("<li><a href=\"" + link + "\">" + link + "</a></li>");
                }
            }

            out.println("</ul>");
            out.println("</body></html>");
        }
    }
    
    private static void sendJsonFile(File file, OutputStream rawOut) throws IOException {
    try {
        // Lire le contenu du fichier JSON
        String jsonContent = readJsonFile(file);
        
        // Envoyer le contenu JSON
        try (PrintWriter out = new PrintWriter(rawOut, true)) {
            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: application/json; charset=UTF-8");
            out.println("Content-Length: " + file.length());
            out.println();
            out.print(jsonContent);
            out.flush();
        }
    } catch (IOException e) {
        System.err.println("Error reading JSON file: " + e.getMessage());
        send500(rawOut);
    }
}

private static String readJsonFile(File file) throws IOException {
    return new String(Files.readAllBytes(file.toPath()));
}



    private static void sendFile(File file, OutputStream rawOut) throws IOException {
        try (BufferedInputStream fileIn = new BufferedInputStream(new FileInputStream(file));
             PrintWriter out = new PrintWriter(rawOut, true)) {

            out.println("HTTP/1.1 200 OK");
            out.println("Content-Type: " + guessContentType(file.getName()));
            out.println("Content-Length: " + file.length());
            out.println();

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fileIn.read(buffer)) != -1) {
                rawOut.write(buffer, 0, bytesRead);
            }
        }
    }

    private static void send404(OutputStream rawOut) throws IOException {
        try (PrintWriter out = new PrintWriter(rawOut, true)) {
            out.println("HTTP/1.1 404 Not Found");
            out.println("Content-Type: text/html; charset=UTF-8");
            out.println();
            out.println("<!DOCTYPE html>");
            out.println("<html><body>");
            out.println("<h1>404 Not Found</h1>");
            out.println("<p>The requested resource could not be found on this server.</p>");
            out.println("</body></html>");
        }
    }

    private static void send400(OutputStream rawOut) throws IOException {
        try (PrintWriter out = new PrintWriter(rawOut, true)) {
            out.println("HTTP/1.1 400 Bad Request");
            out.println("Content-Type: text/html; charset=UTF-8");
            out.println();
            out.println("<!DOCTYPE html>");
            out.println("<html><body>");
            out.println("<h1>400 Bad Request</h1>");
            out.println("<p>Your browser sent a request that this server could not understand.</p>");
            out.println("</body></html>");
        }
    }

    private static void send500(OutputStream rawOut) throws IOException {
        try (PrintWriter out = new PrintWriter(rawOut, true)) {
            out.println("HTTP/1.1 500 Internal Server Error");
            out.println("Content-Type: text/html; charset=UTF-8");
            out.println();
            out.println("<!DOCTYPE html>");
            out.println("<html><body>");
            out.println("<h1>500 Internal Server Error</h1>");
            out.println("<p>The server encountered an internal error and was unable to complete your request.</p>");
            out.println("</body></html>");
        }
    }

    private static void send403(OutputStream rawOut) throws IOException {
        try (PrintWriter out = new PrintWriter(rawOut, true)) {
            out.println("HTTP/1.1 403 Forbidden");
            out.println("Content-Type: text/html; charset=UTF-8");
            out.println();
            out.println("<!DOCTYPE html>");
            out.println("<html><body>");
            out.println("<h1>403 Forbidden</h1>");
            out.println("<p>You don't have permission to access this resource.</p>");
            out.println("</body></html>");
        }
    }

    private static String guessContentType(String fileName) {
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
            return "text/html";
        } else if (fileName.endsWith(".css")) {
            return "text/css";
        } else if (fileName.endsWith(".js")) {
            return "application/javascript";
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.endsWith(".png")) {
            return "image/png";
        } else if (fileName.endsWith(".gif")) {
            return "image/gif";
        } else if (fileName.endsWith(".json")) {
            return "application/json";
        } else {
            return "application/octet-stream";
        }
    }
}