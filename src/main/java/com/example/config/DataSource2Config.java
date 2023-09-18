//package com.example.config;
//
//
//import com.zaxxer.hikari.util.DriverDataSource;
//import org.springframework.boot.context.properties.ConfigurationProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.jdbc.datasource.DriverManagerDataSource;
//
//import javax.sql.DataSource;
//
///**
// * @illustrate 数据源2配置
// * @author JJ_un
// * @date 2023/09/08
// */
//@Configuration
//public class DataSource2Config {
//
//
//    @Bean(name = "dataSource2")
//    @ConfigurationProperties(prefix = "spring.datasource.datasource2")
//    public DataSource dataSource2() {
//        return new DriverManagerDataSource();
//    }
//}
