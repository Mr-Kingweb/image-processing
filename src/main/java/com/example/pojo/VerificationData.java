package com.example.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * @author JinShengJie
 * @date 2023-07-13 13:28
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VerificationData implements Serializable {
    private Date time;//数据接收时间 水台上的表
    private String meterNum; //表号
    private Double waterMeterFlowM3h;
    private Double standardFlowM3h;// 流量
    private Double instantaneousFlowM3h;
    private Double tempBiao;// 进入温度
    private Double tempTable; //平均温度
    private String wrong;
    private Integer ampUp; // 波形接受幅值
    private Integer ampDown; //
    private Double diffTofNs;//飞行时间差 （负值比较大，>500以上，错误数据）
    private Double sumTofNs;//飞行时间和 顺逆流 ，恒定 ，影响因素 温度
    private Double pwrUp; // 脉冲宽度比
    private Double pwrDown; //
    private Double accruedFlow; //
    private Double shuiTaiAccruedFlow; //
    private Double error;// 流量模式
    private Double errorStandard;// 流量模式
    private Integer flowPoint; //流量点 1L/h
    private Integer tempPoint; // 温度点
}
