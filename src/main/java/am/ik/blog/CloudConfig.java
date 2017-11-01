package am.ik.blog;

import javax.sql.DataSource;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.cloud.config.java.AbstractCloudConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Configuration
@Profile("cloud")
public class CloudConfig extends AbstractCloudConfig {

	@Bean
	@ConfigurationProperties(prefix = "spring.datasource.hikari")
	DataSource dataSource(BlogDbCredentials credentials) {
		return DataSourceBuilder.create() //
				.url(credentials.getJdbcUrl() + "&allowMultiQueries=true") //
				.username(credentials.getUsername()) //
				.password(credentials.getPassword()) //
				.driverClassName("org.mariadb.jdbc.Driver") //
				.build();
	}

	@Bean
	ConnectionFactory rabbitConnectionFactory() {
		return connectionFactory().rabbitConnectionFactory();
	}

	@Component
	@ConfigurationProperties(prefix = "vcap.services.blog-db.credentials")
	public static class BlogDbCredentials {
		private String jdbcUrl;
		private String username;
		private String password;

		public String getJdbcUrl() {
			return jdbcUrl;
		}

		public void setJdbcUrl(String jdbcUrl) {
			this.jdbcUrl = jdbcUrl;
		}

		public String getUsername() {
			return username;
		}

		public void setUsername(String username) {
			this.username = username;
		}

		public String getPassword() {
			return password;
		}

		public void setPassword(String password) {
			this.password = password;
		}
	}
}
