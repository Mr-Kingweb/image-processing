package com.example.pojo;

import lombok.*;

import java.io.Serializable;
import java.util.Date;

/**
 * @author JinShengJie
 * @date 2023-07-03 21:16
 */
@Data
public class DataClear implements Serializable {
    private Date time;//数据接收时间 水台上的表
    private String meterNum; //表号
    private Double diffTofNs;//飞行时间差 （负值比较大，>500以上，错误数据）
    private Double sumTofNs;//飞行时间和 顺逆流 ，恒定 ，影响因素 温度
    private Double meterFlowM3h;// 水台流量计，水速
    private Double standardFlow;// 流量
    private String flowMode;// 流量模式
    private Double tmpIn;// 进入温度
    private Double tmpOut;// 出水温度
    private Double tmpTable; //平均温度
    private Double tofUp; //飞行时间 up->down端时间
    private Double tofDown; // down->up端时间
    private Double pwrUp; // 脉冲宽度比
    private Double pwrDown; //
    private Integer ampUp; // 波形接受幅值
    private Integer ampDown; //
    private Integer flowPoint; //流量点 1L/h
    private Integer tempPoint; // 温度点
}
