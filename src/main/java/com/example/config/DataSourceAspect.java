//package com.example.config;
//
//import org.aspectj.lang.annotation.*;
//import org.springframework.core.annotation.Order;
//import org.springframework.stereotype.Component;
//
///**
// * @author JJ_un
// * @date 2023/09/08
// */
//@Aspect
//@Component
//@Order(-1)
//public class DataSourceAspect {
//
//    /**
//     * @param ds
//     */
//    @Before("@annotation(ds)") // 拦截带有 @DS 注解的方法
//    public void setDataSource(DS ds) {
//        String dataSourceName = ds.value();
//        // 根据 dataSourceName 来切换数据源
//        // 这里可以使用您的动态数据源切换框架来实现数据源切换逻辑
//    }
//}
