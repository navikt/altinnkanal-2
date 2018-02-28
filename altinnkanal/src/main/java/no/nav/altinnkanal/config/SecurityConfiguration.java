package no.nav.altinnkanal.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPBody;
import javax.xml.soap.SOAPFault;
import javax.xml.soap.SOAPMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableWebSecurity
public class SecurityConfiguration {
    @Configuration
    @PropertySource("classpath:application.properties")
    public static class BasicSecurityConfiguration extends WebSecurityConfigurerAdapter {
        @Autowired
        private SoapProperties soapProperties;

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
                    .withUser(soapProperties.getUsername())
                    .password(soapProperties.getPassword())
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
}
