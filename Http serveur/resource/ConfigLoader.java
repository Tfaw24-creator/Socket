

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ConfigLoader {
    private final Map<String, String> config = new HashMap<>();

    public ConfigLoader(String filePath) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    config.put(parts[0].trim(), parts[1].trim());
                }
            }
        }
    }

    public String get(String key) {
        return config.get(key);
    }

    public int getInt(String key) {
        return Integer.parseInt(config.get(key));
    }
}
