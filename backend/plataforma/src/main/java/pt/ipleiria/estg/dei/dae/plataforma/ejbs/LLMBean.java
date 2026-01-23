package pt.ipleiria.estg.dei.dae.plataforma.ejbs;

import jakarta.ejb.Stateless;
import java.io.IOException;
import java.net.Proxy;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@Stateless
public class LLMBean {

    private static final String OLLAMA_URL = "http://ollama:11434/api/generate";
    private static final String MODEL = System.getenv("LLM_MODEL") != null ? System.getenv("LLM_MODEL") : "tinyllama";

    public String generateSummary(String textContent) throws IOException {
        String cleanText = textContent.replace("\"", "'").replace("\n", " ");
        if (cleanText.length() > 6000) cleanText = cleanText.substring(0, 6000);

        String prompt = "Quero que cries um resumo consiso em português de portugal, de no máximo 250 palavras sobre o texto que te estou a enviar: " + cleanText;
        String jsonBody = String.format("{\"model\": \"%s\", \"prompt\": \"%s\", \"stream\": false}", MODEL, prompt);

        System.out.println(">>>A TENTAR LIGAR AO OLLAMA: " + OLLAMA_URL);
        URL url = new URL(OLLAMA_URL);

        HttpURLConnection conn = (HttpURLConnection) url.openConnection(Proxy.NO_PROXY);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonBody.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        int code = conn.getResponseCode();
        if (code == 200) {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder response = new StringBuilder();
                String responseLine;
                while ((responseLine = br.readLine()) != null) {
                    response.append(responseLine.trim());
                }
                return extractResponseFromJson(response.toString());
            }
        } else {
            throw new IOException("Erro Ollama: HTTP " + code);
        }
    }

    private String extractResponseFromJson(String json) {
        try {
            int startIndex = json.indexOf("\"response\":\"") + 12;
            int endIndex = json.indexOf("\",\"done\"");
            if (startIndex > 11 && endIndex > startIndex) {
                String raw = json.substring(startIndex, endIndex);
                return raw.replace("\\n", "\n").replace("\\\"", "\"");
            }
        } catch (Exception e) {
            return "Erro ao processar resposta do LLM.";
        }
        return "Resumo indisponível.";
    }
}