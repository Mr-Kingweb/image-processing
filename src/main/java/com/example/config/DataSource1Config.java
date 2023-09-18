//package com.example.config;
//
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.jdbc.datasource.DriverManagerDataSource;
//
//import javax.sql.DataSource;
//
///**
// * @illustrate 数据源1配置
// * @author JJ_un
// * @date 2023/09/08
// */
//@Configuration
//public class DataSource1Config {
//
//    @Bean(name="dataSource1")
//    @ConfigurationProperties(prefix = "spring.datasource.datasource1")
//    public DataSource dataSource1() {
//        return new DriverManagerDataSource();
//    }
//}
