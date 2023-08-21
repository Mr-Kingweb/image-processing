package com.example.service;

import com.example.pojo.CharacterizedData;
import com.example.pojo.DataClear;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Map;

/**
 * @author JinShengJie
 * @date 2023-07-04 10:41
 */
public interface JokerService {

    List<DataClear> selectAllDataBp(Integer page,Integer size);

    Map<String,List> selectAllDataFeature(String meterNum);

    List<Integer> ampUp();

    Map<String, List> getSpreadData(String spreadData);
//    导出筛选数据
    void uploadExcel_1(String meterNum,HttpServletResponse response);
//    导出特征话数据
    void uploadExcel_2( String meterNum,HttpServletResponse response);
//    验证数据
    void uploadExcel_3( String meterNum,HttpServletResponse response);
//    误差表数据
    void uploadExcel_4( String meterNum,HttpServletResponse response);
//    根据搜索返回对应数据
    List<DataClear> getByMeterNum(String meterNum);
}
