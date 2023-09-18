//package com.example.config;
//
//
//import org.apache.ibatis.session.SqlSessionFactory;
//import org.mybatis.spring.SqlSessionFactoryBean;
//import org.mybatis.spring.SqlSessionTemplate;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
//
//import javax.sql.DataSource;
//
///**
// * @author JJ_un
// * @date 2023/09/08
// */
//@Configuration
//public class MybatisConfig {
//
//    @Bean
//    public SqlSessionFactory sqlSessionFactory(@Qualifier("dataSource1")DataSource dataSource1) throws Exception{
//        SqlSessionFactoryBean sqlSessionFactoryBean = new SqlSessionFactoryBean();
//        sqlSessionFactoryBean.setDataSource(dataSource1);
//        sqlSessionFactoryBean.setMapperLocations(new PathMatchingResourcePatternResolver().getResource("/mapper/*.xml"));
//        return sqlSessionFactoryBean.getObject();
//    }
//
//
//    public SqlSessionTemplate sqlSessionTemplate(@Qualifier("sqlSessionFactory")SqlSessionFactory sqlSessionFactory) {
//        return new SqlSessionTemplate(sqlSessionFactory);
//
//    }
//}
