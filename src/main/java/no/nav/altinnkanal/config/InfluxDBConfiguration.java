package no.nav.altinnkanal.config;

import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Configuration
public class InfluxDBConfiguration {
    @Bean
    InfluxDB influxDB(Configuration configuration) {
        Configuration.Connection connection = configuration.getConnection();
        InfluxDB influxDB = InfluxDBFactory.connect(connection.getUrl(), connection.getUsername(), connection.getPassword());
        influxDB.enableBatch(2000, 100, TimeUnit.MILLISECONDS);

        // TODO: Retention policy
        return influxDB;
    }

    @Component
    @ConfigurationProperties(value = "influxdb")
    public static class Configuration {
        private Connection connection;

        public static class Connection {
            private String url;
            private String username;
            private String password;

            public String getUrl() {
                return url;
            }

            public void setUrl(String url) {
                this.url = url;
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

        public Connection getConnection() {
            return connection;
        }

        public void setConnection(Connection connection) {
            this.connection = connection;
        }
    }
}
