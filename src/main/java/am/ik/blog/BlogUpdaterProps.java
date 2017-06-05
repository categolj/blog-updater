package am.ik.blog;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@ConfigurationProperties(prefix = "blog-updater")
@Data
@Component
public class BlogUpdaterProps {
	private Map<String, String> githubToken;
}
