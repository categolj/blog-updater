package am.ik.blog;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@ConfigurationProperties(prefix = "blog-updater")
@Data
@Component
public class BlogUpdaterProps {
	private String githubToken;
}
