package am.ik.blog;

import javax.sql.DataSource;

import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.config.java.AbstractCloudConfig;
import org.springframework.cloud.service.relational.DataSourceConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("cloud")
public class CloudConnectorConfig extends AbstractCloudConfig {
	@Bean
	@ConfigurationProperties(prefix = "spring.datasource.tomcat")
	DataSource dataSource() {
		return connectionFactory().dataSource(new DataSourceConfig(null,
				new DataSourceConfig.ConnectionConfig("allowMultiQueries=true")));
	}

	@Bean
	ConnectionFactory rabbitConnectionFactory() {
		return connectionFactory().rabbitConnectionFactory();
	}
}
