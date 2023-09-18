package com.example;


import com.alibaba.druid.spring.boot.autoconfigure.DruidDataSourceAutoConfigure;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * @author JJ_un
 * @date 2023/09/08
 */
@SpringBootApplication
//@EnableAutoConfiguration
//@ComponentScan(excludeFilters={@ComponentScan.Filter(type= FilterType.ASSIGNABLE_TYPE, value= DruidDataSourceAutoConfigure.class)})
//(exclude={DataSourceAutoConfiguration.class})
@MapperScan("com.example.dao")
public class NoobApplication {

    public static void main(String[] args) {
        SpringApplication.run(NoobApplication.class, args);
    }

}
