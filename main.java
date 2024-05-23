import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class GitHubCrawler {
    private static final String BASE_URL = "https://github.com";
    private static final String REPO_URL = "https://github.com/username/repository";

    public static void main(String[] args) {
        try {
            List<String> fileUrls = crawlRepo(REPO_URL);
            List<String> codeSnippets = extractCode(fileUrls);
            List<String> features = analyzeCode(codeSnippets);
            String newScript = generateScript(features);
            saveScript(newScript, "generated_script.py");
            System.out.println("New script generated and saved as 'generated_script.py'");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<String> crawlRepo(String repoUrl) throws IOException {
        Document doc = Jsoup.connect(repoUrl).get();
        Elements links = doc.select("a[href]");
        List<String> files = new ArrayList<>();
        for (Element link : links) {
            String href = link.attr("href");
            if (href.endsWith(".py")) {
                files.add(BASE_URL + href);
            }
        }
        return files;
    }

    private static List<String> extractCode(List<String> fileUrls) throws IOException {
        List<String> codeSnippets = new ArrayList<>();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            for (String fileUrl : fileUrls) {
                HttpGet request = new HttpGet(fileUrl);
                try (CloseableHttpResponse response = httpClient.execute(request)) {
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        String content = EntityUtils.toString(entity);
                        codeSnippets.add(content);
                    }
                }
            }
        }
        return codeSnippets;
    }

    private static List<String> analyzeCode(List<String> codeSnippets) {
        String allCode = String.join("\n", codeSnippets);
        String[] words = allCode.split("\\W+");
        Map<String, Integer> wordFreq = new HashMap<>();

        for (String word : words) {
            word = word.toLowerCase();
            wordFreq.put(word, wordFreq.getOrDefault(word, 0) + 1);
        }

        return wordFreq.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private static String generateScript(List<String> features) {
        StringBuilder script = new StringBuilder("# Auto-generated script based on analyzed features\n\n");
        script.append("def main():\n");
        for (String feature : features) {
            script.append("    print('Feature: ").append(feature).append("')\n");
        }
        script.append("\nif __name__ == '__main__':\n");
        script.append("    main()\n");
        return script.toString();
    }

    private static void saveScript(String script, String fileName) throws IOException {
        try (FileWriter writer = new FileWriter(fileName)) {
            writer.write(script);
        }
    }
}
