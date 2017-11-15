package no.nav.altinnkanal.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@EnableGlobalMethodSecurity(prePostEnabled = true)
@EnableWebSecurity
@Configuration
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    @Override
    protected void configure(HttpSecurity http) throws Exception {
    SavedRequestAwareAuthenticationSuccessHandler successHandler = new SavedRequestAwareAuthenticationSuccessHandler();
    successHandler.setUseReferer(true);
    http.authorizeRequests()
                .antMatchers("/configuration/**", "/configuration").permitAll().and()
                .formLogin()
                .loginProcessingUrl("/configuration/login")
                .loginPage("/configuration/login")
                .successHandler(successHandler)
                .defaultSuccessUrl("/configuration")
                .permitAll()
                .and()
                .csrf().ignoringAntMatchers("/altinnkanal/**").and()
                .logout().logoutUrl("/configuration/logout").logoutSuccessUrl("/configuration").permitAll();
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth, LdapConfiguration config) throws Exception {
        auth.ldapAuthentication()
                .userSearchBase(config.userBaseDn)
                .userSearchFilter("cn={0}")
                .groupSearchBase(config.groupBaseDn)
                .groupSearchFilter("Member={0}")
                .contextSource()
                .url(config.url)
                .managerDn(config.managerDn)
                .managerPassword(config.managerPassword);
    }

    @Component
    @ConfigurationProperties("ldap")
    public static class LdapConfiguration {
        private String managerDn;
        private String managerPassword;
        private String userBaseDn;
        private String groupBaseDn;
        private String url;

        public String getManagerDn() {
            return managerDn;
        }

        public String getManagerPassword() {
            return managerPassword;
        }

        public String getUserBaseDn() {
            return userBaseDn;
        }

        public String getGroupBaseDn() {
            return groupBaseDn;
        }

        public String getUrl() {
            return url;
        }

        public void setManagerDn(String managerDn) {
            this.managerDn = managerDn;
        }

        public void setManagerPassword(String managerPassword) {
            this.managerPassword = managerPassword;
        }

        public void setUserBaseDn(String userBaseDn) {
            this.userBaseDn = userBaseDn;
        }

        public void setGroupBaseDn(String groupBaseDn) {
            this.groupBaseDn = groupBaseDn;
        }

        public void setUrl(String url) {
            this.url = url;
        }
    }
}
