package com.example.controller;

import com.alibaba.fastjson.JSONArray;

import org.json.JSONObject;
import com.example.pojo.CharacterizedData;
import com.example.pojo.DataClear;
import com.example.service.JokerService;
import com.fasterxml.jackson.databind.util.JSONPObject;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author JinShengJie
 * @date 2023-07-03 21:15
 */
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("")
public class JokerController {

    @Autowired
    JokerService jokerService;

    //  获取全部数据，一次性放入缓存
// todo 尝试分页调整数据
    @PostMapping("/jokerBp")
    public List<DataClear> selectAllDataBp(@RequestBody Map<String, Object> params) {
        Integer page = (Integer) params.get("page");
        Integer size = (Integer) params.get("size");
        List<DataClear> dataClearList = jokerService.selectAllDataBp(page, size);
        return dataClearList;
    }

    // todo 将查表号的权限下放给前端，具体操作是搜索模糊查询表号，后端获取具体
//  数据和可以展现相对应的图表，不能是所有数据，可以说数据放在缓存里，但是肯定是根据具体表号来的
    @PostMapping("/featureData")
    public Map<String, List> selectAllFeature(@RequestBody String meterNum) {
        Map<String, List> characterizedDataList = jokerService.selectAllDataFeature(meterNum);
        return characterizedDataList;
    }

    @GetMapping("/ampUp")
    public List<Integer> selectAmpUpData() {
        List<Integer> integerList = jokerService.ampUp();
        return integerList;
    }

    // 特征化数据模糊表号展示
    @GetMapping("/meterNumList")
    public List<String> meterNumList() {
        List<String> meterNumList = jokerService.meterNumList();
        return meterNumList;
    }

    //验证数据模糊表号展示
    @GetMapping("/meterNumList1")
    public List<String> meterNumList1() {
        List<String> meterNumList = jokerService.meterNumList1();
        return meterNumList;
    }

    //      散点图
    @PostMapping("/spreadData")
    public Map<String, List> spreadData(@RequestBody String spreadData) {
        return jokerService.getSpreadData(spreadData);
    }

    @PostMapping("/uploadExcel_1")
    public void uploadExcel_1(@RequestBody String meterNum, HttpServletResponse response) {
        jokerService.uploadExcel_1(meterNum, response);
    }

    @PostMapping("/uploadExcel_2")
    public void uploadExcel_2(@RequestBody String meterNum, HttpServletResponse response) {
        jokerService.uploadExcel_2(meterNum, response);
    }

    @PostMapping("/uploadExcel_3")
    public void uploadExcel_3(@RequestBody String meterNum, HttpServletResponse response) {
        jokerService.uploadExcel_3(meterNum, response);
    }

    @PostMapping("/uploadExcel_4")
    public void uploadExcel_4(@RequestBody String meterNum, HttpServletResponse response) {
        jokerService.uploadExcel_4(meterNum, response);
    }

    @PostMapping("/searchByMeter")
    public List<DataClear> getByMeterNum(@RequestBody String meterNum) {
        List<DataClear> dataClearList = jokerService.getByMeterNum(meterNum);
        return dataClearList;
    }
}
