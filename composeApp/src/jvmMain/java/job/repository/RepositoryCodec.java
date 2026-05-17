package job.repository;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

final class RepositoryCodec {
    private RepositoryCodec() {}

    static String encodeAttributes(Map<String, String> attributes) {
        if (attributes == null || attributes.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner("&");
        for (Map.Entry<String, String> entry : attributes.entrySet()) {
            String key = URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8);
            String value = URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
            joiner.add(key + "=" + value);
        }
        return joiner.toString();
    }

    static Map<String, String> decodeAttributes(String raw) {
        Map<String, String> result = new HashMap<>();
        if (raw == null || raw.isBlank()) {
            return result;
        }
        String[] pairs = raw.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx <= 0) {
                continue;
            }
            String key = URLDecoder.decode(pair.substring(0, idx), StandardCharsets.UTF_8);
            String value = URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            result.put(key, value);
        }
        return result;
    }
}

