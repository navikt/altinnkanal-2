package no.nav.altinnkanal.config;

import org.apache.cxf.binding.soap.SoapFault;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.*;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableWebSecurity
public class SecurityConfiguration {
    @Configuration
    @Order(1)
    @PropertySource("classpath:application.properties")
    public static class BasicSecurityConfiguration extends WebSecurityConfigurerAdapter {
        @Value("${soap.auth.username}")
        private String username;
        @Value("${soap.auth.password}")
        private String password;

        @Autowired
        private CustomBasicAuthEntryPoint authEntryPoint;

        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.antMatcher("/webservices/**")
                .authorizeRequests().anyRequest().fullyAuthenticated()
                    .and()
                .csrf().disable()
                .httpBasic()
                .authenticationEntryPoint(authEntryPoint)
                    .and()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        }

        @Override
        public void configure(AuthenticationManagerBuilder auth) throws Exception {
            auth.inMemoryAuthentication()
                    .withUser(username)
                    .password(password)
                    .roles("USER");
        }

        @Component
        public class CustomBasicAuthEntryPoint extends BasicAuthenticationEntryPoint {
            @Override
            public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authEx)
                    throws IOException {
                response.addHeader("WWW-Authenticate", "Basic realm=\"" + getRealmName() + "\"");
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("text/xml");
                PrintWriter writer = response.getWriter();
                String result = null;
                try {
                    SOAPMessage message = MessageFactory.newInstance().createMessage();
                    SOAPBody body = message.getSOAPBody();
                    SOAPFault fault = body.addFault();
                    fault.setFaultCode("SOAP-ENV:Client");
                    fault.setFaultString("HTTP 401 Unauthorized.");
                    ByteArrayOutputStream outStream = new ByteArrayOutputStream();
                    message.writeTo(outStream);
                    result = new String(outStream.toByteArray(), StandardCharsets.UTF_8);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                writer.println(result);
            }

            @Override
            public void afterPropertiesSet() throws Exception {
                setRealmName("Altinnkanal");
                super.afterPropertiesSet();
            }
        }

    }

    @Configuration
    @Order(2)
    public static class LdapSecurityConfiguration extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
            successHandler.setUseReferer(true);
            http.authorizeRequests()
                    .antMatchers("/configuration/**", "/configuration").permitAll()
                        .and()
                    .formLogin()
                        .loginProcessingUrl("/configuration/login")
                        .loginPage("/configuration/login")
                        .successHandler(successHandler)
                        .defaultSuccessUrl("/configuration")
                        .permitAll()
                        .and()
                    .csrf().ignoringAntMatchers("/webservices/**")
                        .and()
                    .logout()
                        .logoutUrl("/configuration/logout")
                        .logoutSuccessUrl("/configuration")
                        .permitAll();
        }

        @Override
        public void configure(WebSecurity web) throws Exception {
            web.ignoring().antMatchers("/static/**");
        }

        @Autowired
        public void configureGlobal(AuthenticationManagerBuilder auth, LdapConfiguration config) throws Exception {
            auth.ldapAuthentication()
                    .userSearchBase(config.userBasedn)
                    .userSearchFilter("cn={0}")
                    .groupSearchBase("ou=AccountGroups, ou=Groups," + config.userBasedn)
                    .groupSearchFilter("Member={0}")
                    .contextSource()
                    .url(config.url)
                    .managerDn(config.username)
                    .managerPassword(config.password);
        }

        @Component
        @ConfigurationProperties("ldap")
        public static class LdapConfiguration {
            private String username;
            private String password;
            private String userBasedn;
            private String url;

            public String getUsername() {
                return username;
            }

            public String getPassword() {
                return password;
            }

            public String getUserBasedn() {
                return userBasedn;
            }

            public String getUrl() {
                return url;
            }

            public void setUsername(String username) {
                this.username = username;
            }

            public void setPassword(String password) {
                this.password = password;
            }

            public void setUserBasedn(String userBasedn) {
                this.userBasedn = userBasedn;
            }

            public void setUrl(String url) {
                this.url = url;
            }
        }
    }
}
