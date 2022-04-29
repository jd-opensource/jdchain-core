package com.jd.blockchain.gateway.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import utils.StringUtils;

@Configuration
@EnableWebSecurity
public class GatewayWebSecurityConfigurer extends WebSecurityConfigurerAdapter {

    @Value("${spring.security.ignored:}")
    String ignored;
    @Value("${spring.security.user.name:}")
    String userName;
    @Value("${spring.security.user.password:}")
    String userPassword;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        if (!StringUtils.isEmpty(userName) && !StringUtils.isEmpty(userPassword)) {
            if (!StringUtils.isEmpty(ignored)) {
                http.authorizeRequests().antMatchers(ignored.split(",")).permitAll().anyRequest().authenticated().and().formLogin().and().httpBasic().and().csrf().disable();
            } else {
                http.authorizeRequests().anyRequest().authenticated().and().formLogin().and().httpBasic().and().csrf().disable();
            }
        } else {
            http.authorizeRequests().anyRequest().permitAll().and().csrf().disable();
        }
    }
}
