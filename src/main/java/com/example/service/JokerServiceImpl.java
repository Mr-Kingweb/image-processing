package com.example.service;

import com.example.dao.CharacterizedDao;
import com.example.dao.JokerDao;
import com.example.dao.VerificationDao;
import com.example.pojo.CharacterizedData;
import com.example.pojo.DataClear;
import com.example.pojo.VerificationData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author JinShengJie
 * @date 2023-07-04 10:41
 */
@Service
public class JokerServiceImpl implements JokerService {

    @Autowired
    JokerDao jokerDao;

    @Autowired
    CharacterizedDao characterizedDao;

    @Autowired
    VerificationDao verificationDao;

    @Resource
    private RedisTemplate redisTemplate;

    List<DataClear> dataClearList_1;

    @Override
    public List<DataClear> selectAllDataBp(Integer page, Integer size) {
        // 过滤条数
        Integer offset = (page - 1) * size;
        return jokerDao.getAllDataBp(size, offset);
    }

    // 正态分布筛选出95%数据
    public List<DataClear> normalDistribution(String meterNum) {
        List<DataClear> dataClearList = jokerDao.getAllData(meterNum);

        Map<Integer, List<DataClear>> groupedMap = dataClearList.stream()
                .collect(Collectors.groupingBy(DataClear::getFlowPoint));

        Map<Integer, List<DataClear>> filteredMap = groupedMap.entrySet().stream()
                .map(entry -> {
                    Integer flowPoint = entry.getKey();
                    List<DataClear> dataList = entry.getValue();

                    dataList.sort(Comparator.comparingDouble(DataClear::getDiffTofNs));

                    int lowerIndex = (int) Math.round(dataList.size() * 0.025);
                    int upperIndex = (int) Math.round(dataList.size() * 0.975);

                    List<DataClear> filteredList = dataList.subList(lowerIndex, upperIndex);

                    return Map.entry(flowPoint, filteredList);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldList, newList) -> {
                    List<DataClear> mergedList = new ArrayList<>(oldList);
                    mergedList.addAll(newList);
                    return mergedList;
                }, LinkedHashMap::new));

        return filteredMap.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, List> selectAllDataFeature(String meterNum) {
        JSONObject jsonObject = new JSONObject(meterNum);
        String meterNum_5 = jsonObject.getString("meterNum");
        String[] splitMeterNumData = meterNum_5.split(",");
        String[] splitMeterNumList = splitMeterNumData;
        List<DataClear> list = new ArrayList<>();
        for (String single : splitMeterNumList) {
            list.addAll(normalDistribution(single));
        }
        // tempPoint*meterNum*flowPoint
        Map<String, List> feature = new HashMap();
        List<Double> sumList = new ArrayList<>();
        List<Double> tempTableList = new ArrayList<>();

        Map<Integer, List<DataClear>> tempMap = list.stream().collect(Collectors.groupingBy(DataClear::getTempPoint));
        Map<Integer, List<DataClear>> sortedMap = new TreeMap<>(Comparator.naturalOrder());
        sortedMap.putAll(tempMap);
        DecimalFormat decimalFormat = new DecimalFormat("#.###");
        DecimalFormat decimalFormat_1 = new DecimalFormat("#.#");
        for (Integer temp_1 : sortedMap.keySet()) {
            // meterNum
            Map<Long, List<DataClear>> meterNumMap_1 = sortedMap.get(temp_1).stream()
                    .collect(Collectors.groupingBy(dataClear -> Long.valueOf(dataClear.getMeterNum().trim())));
            Map<Long, List<DataClear>> sortedMap_1 = new TreeMap<>(Comparator.naturalOrder());
            sortedMap_1.putAll(meterNumMap_1);
            for (Long meterNum_1 : sortedMap_1.keySet()) {
                // flowPoint
                Map<Integer, List<DataClear>> flowPointMap = sortedMap_1.get(meterNum_1).stream().collect(Collectors.groupingBy(DataClear::getFlowPoint));
                Map<Integer, List<DataClear>> sortedMap_2 = new TreeMap<>(Comparator.naturalOrder());
                sortedMap_2.putAll(flowPointMap);
                for (Integer flowPoint : sortedMap_2.keySet()) {
                    List<DataClear> dataList = flowPointMap.get(flowPoint);

                    double sumAverage = dataList.stream()
                            .mapToDouble(DataClear::getSumTofNs)
                            .average()
                            .orElse(0.0);
                    sumList.add(Double.valueOf(decimalFormat.format(sumAverage)));

                    double tmpTableAverage = dataList.stream()
                            .filter(Objects::nonNull)
                            .filter(dataClear -> dataClear.getTmpTable() != null)
                            .mapToDouble(DataClear::getTmpTable)
                            .average()
                            .orElse(0.0);
                    tempTableList.add(Double.valueOf(decimalFormat_1.format(tmpTableAverage)));
                }
            }
        }
        // 获取键值对数据(数据筛选机制)
        Map<String, List<DataClear>> map = list.stream() // 按照字段排序
                .peek(dc -> dc.setMeterNum(dc.getMeterNum().trim()))
                .collect(Collectors.toMap(DataClear::getMeterNum,
                        Collections::singletonList, // 创建包含单个元素的列表
                        (oldList, newList) -> {
                            List<DataClear> mergedList = new ArrayList<>(oldList);
                            mergedList.addAll(newList);
                            return mergedList;
                        },
                        LinkedHashMap::new));
        this.dataClearList_1 = list;

        feature.put("sumTofNs", sumList);
        feature.put("tmpTable", tempTableList);
        return feature;
    }

    private Map<Long, List<DataClear>> groupAndSortDataClearByMeterNum(List<DataClear> dataClearList) {
        Map<Long, List<DataClear>> meterNumMap = dataClearList.stream()
                .collect(Collectors.groupingBy(dataClear -> Long.valueOf(dataClear.getMeterNum().trim())));
        Map<Long, List<DataClear>> sortedMap = new TreeMap<>(Comparator.naturalOrder());
        sortedMap.putAll(meterNumMap);
        return sortedMap;
    }

    private Map<Integer, List<DataClear>> groupAndSortDataClearByFlowPoint(List<DataClear> dataClearList) {
        Map<Integer, List<DataClear>> flowPointMap = dataClearList.stream()
                .collect(Collectors.groupingBy(DataClear::getFlowPoint));
        Map<Integer, List<DataClear>> sortedMap = new TreeMap<>(Comparator.naturalOrder());
        sortedMap.putAll(flowPointMap);
        return sortedMap;
    }

    @Override
    public Map<String, List> getSpreadData(String spreadData) {
        JSONObject jsonObject = new JSONObject(spreadData);
        String meterNum = jsonObject.getString("meterNum");
        String[] splitMeterNumData = meterNum.split(",");
        List<String> splitMeterNumList = Arrays.asList(splitMeterNumData);
        System.out.println(meterNum);
        String tempPoint = jsonObject.getString("tempPoint");
        Map<String, List> spreadList = new HashMap();

        List<DataClear> list = this.dataClearList_1;
        List<Double> diffTofNsListBp = new ArrayList<>();
        List<Double> standardFlowListBp = new ArrayList<>();


        Map<Integer, List<DataClear>> tempMap = list.stream().collect(Collectors.groupingBy(DataClear::getTempPoint));
        Map<Integer, List<DataClear>> sortedMap = new TreeMap<>(Comparator.naturalOrder());
        sortedMap.putAll(tempMap);
        for (Integer temp_1 : sortedMap.keySet()) {
            if (temp_1.equals(Integer.valueOf(tempPoint))) {
                // meterNum
                Map<Long, List<DataClear>> meterNumMap_1 = sortedMap.get(temp_1).stream()
                        .collect(Collectors.groupingBy(dataClear -> Long.valueOf(dataClear.getMeterNum().trim())));
                Map<Long, List<DataClear>> sortedMap_1 = new TreeMap<>(Comparator.naturalOrder());
                sortedMap_1.putAll(meterNumMap_1);
                DecimalFormat decimalFormat = new DecimalFormat("#.###");

                for (Integer temp : sortedMap.keySet()) {
                    if (temp.equals(Integer.valueOf(tempPoint))) {
                        Map<Long, List<DataClear>> meterNumMap = groupAndSortDataClearByMeterNum(sortedMap.get(temp));
                        for (Long meterNum_1 : meterNumMap.keySet()) {
                            if (splitMeterNumList.contains(String.valueOf(meterNum_1))) {
                                Map<Integer, List<DataClear>> flowPointMap = groupAndSortDataClearByFlowPoint(meterNumMap.get(meterNum_1));
                                for (Integer flowPoint : flowPointMap.keySet()) {
                                    List<DataClear> dataList = flowPointMap.get(flowPoint);

                                    double diffAverage = dataList.stream()
                                            .mapToDouble(DataClear::getDiffTofNs)
                                            .average()
                                            .orElse(0.0);
                                    diffTofNsListBp.add(Double.valueOf(decimalFormat.format(diffAverage)));

                                    double standardFlowAvg = dataList.stream()
                                            .mapToDouble(DataClear::getStandardFlow)
                                            .average()
                                            .orElse(0.0);
                                    standardFlowListBp.add(Double.valueOf(decimalFormat.format(standardFlowAvg)));
                                }
                            }
                        }
                        spreadList.put("diffTofNs", diffTofNsListBp);
                        spreadList.put("standardFlow", standardFlowListBp);
                    }
                }
            }
        }
        return spreadList;
    }

    @Override
    public List<Integer> ampUp() {
        return jokerDao.ampUp();
    }

    @Override
    public void uploadExcel_1(String meterNum, HttpServletResponse response) {
        // 创建工作簿
        Workbook workbook = new XSSFWorkbook();

        // 创建工作表
        Sheet sheet = workbook.createSheet("筛选数据");

        // 创建表头
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("time");
        headerRow.createCell(1).setCellValue("meterNum");
        headerRow.createCell(2).setCellValue("diffTofNs");
        headerRow.createCell(3).setCellValue("sumTofNs");
        headerRow.createCell(4).setCellValue("meterFlowM3h");
        headerRow.createCell(5).setCellValue("standardFlow");
        headerRow.createCell(6).setCellValue("flowMode");
        headerRow.createCell(7).setCellValue("tmpIn");
        headerRow.createCell(8).setCellValue("tmpOut");
        headerRow.createCell(9).setCellValue("tmpTable");
        headerRow.createCell(10).setCellValue("tofUp");
        headerRow.createCell(11).setCellValue("tofDown");
        headerRow.createCell(12).setCellValue("pwrUp");
        headerRow.createCell(13).setCellValue("pwrDown");
        headerRow.createCell(14).setCellValue("flowPoint 1L/h");
        headerRow.createCell(15).setCellValue("tempPoint");
        headerRow.createCell(16).setCellValue("ampUp");
        headerRow.createCell(17).setCellValue("ampDown");
        List<DataClear> list;
        // 从缓存中获取数据
        list = this.dataClearList_1;

//            Set<String> dataClearJsonSet = redisTemplate.opsForSet().members(meterNum);
//            ObjectMapper objectMapper = new ObjectMapper();
//            for (String json : dataClearJsonSet) {
//                try {
//                    DataClear[] dataClearArray = objectMapper.readValue(json, DataClear[].class);
//                    list.addAll(Arrays.asList(dataClearArray));
//                } catch (JsonProcessingException e) {
//                    e.printStackTrace();
//                }
//            }

        // 填充数据
        int rowIndex = 1;
        for (DataClear rowData : list) {
            Row row = sheet.createRow(rowIndex++);
            Date time_1 = rowData.getTime();
            Timestamp timestamp = new Timestamp(time_1.getTime());
            LocalDateTime localDateTime = timestamp.toLocalDateTime();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

            String formattedDateTime = localDateTime.format(formatter);
            row.createCell(0).setCellValue(formattedDateTime);
            row.createCell(1).setCellValue(rowData.getMeterNum());
            row.createCell(2).setCellValue(rowData.getDiffTofNs());
            row.createCell(3).setCellValue(rowData.getSumTofNs());
            row.createCell(4).setCellValue(rowData.getMeterFlowM3h());
            row.createCell(5).setCellValue(rowData.getStandardFlow());
            row.createCell(6).setCellValue(rowData.getFlowMode());
            row.createCell(7).setCellValue(rowData.getTmpIn());
            row.createCell(8).setCellValue(rowData.getTmpOut());
            row.createCell(9).setCellValue(rowData.getTmpTable());
            row.createCell(10).setCellValue(rowData.getTofUp());
            row.createCell(11).setCellValue(rowData.getTofDown());
            row.createCell(12).setCellValue(rowData.getPwrUp());
            row.createCell(13).setCellValue(rowData.getPwrDown());
            row.createCell(14).setCellValue(rowData.getFlowPoint());
            row.createCell(15).setCellValue(rowData.getTempPoint());
            row.createCell(16).setCellValue(rowData.getAmpUp());
            row.createCell(17).setCellValue(rowData.getAmpDown());

        }

        excelReturn("筛选数据.xlsx", workbook, response);
    }

    @Override
    public void uploadExcel_2(String meterNum, HttpServletResponse response) {

        // 创建工作簿
        Workbook workbook = new XSSFWorkbook();

        // 创建工作表
        Sheet sheet = workbook.createSheet("特征化数据");

        // 创建表头
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Number");
        headerRow.createCell(1).setCellValue("use");
        headerRow.createCell(2).setCellValue("SP");
        headerRow.createCell(3).setCellValue("weight");
        headerRow.createCell(4).setCellValue("AVGdiffTof");
        headerRow.createCell(5).setCellValue("AVGsumTof");
        headerRow.createCell(6).setCellValue("标准流量(*K)");
        headerRow.createCell(7).setCellValue("温度");
        headerRow.createCell(8).setCellValue("温度点");
        headerRow.createCell(9).setCellValue("数据处理条数");
        headerRow.createCell(10).setCellValue("过滤前数据");
        // ...
        List<DataClear> list;
        List<DataClear> listOrigin;
// 从缓存中获取数据
        list = this.dataClearList_1;
        listOrigin = jokerDao.getAllDataOrigin(meterNum);
        int rowIndex = 1;
        CharacterizedData characterizedData = new CharacterizedData();
//        创建数据表
        LocalDate currentDate = LocalDate.now();
        StringBuilder stringBuilder = new StringBuilder();
        String formattedDate = currentDate.format(DateTimeFormatter.ofPattern("yyMMdd"));
        stringBuilder.append("characterized_data");
        stringBuilder.append(formattedDate);
        String joker_king = stringBuilder.toString();
        // todo 目前是首先判断今日表是否创建，无=>创建表，下面插入数据；有=>不做任何操作；
        int if_success;
        int if_success_1 = 0;
        if_success = jokerDao.ifHasBaseNum(joker_king);
        if (if_success == 1) {
            System.out.println("已创建过该数据表");
        } else {
            if_success_1 = jokerDao.increaseDatabase(joker_king);
        }
        if (if_success_1 == 1) {

        } else {
            System.out.println("创建失败");
        }
        // tempPoint*meterNum*flowPoint
        Map<Integer, List<DataClear>> tempMap = list.stream().collect(Collectors.groupingBy(DataClear::getTempPoint));
        // todo 考虑把下面的多个增强循环改成普通循环，不需要对数据进行任何处理，获取数据就行,前提条件是一个完整的部分数据不会都被清除掉
        // todo 实现安全token存放 转储页面
        Map<Integer, List<DataClear>> tempMapOrigin = listOrigin.stream().collect(Collectors.groupingBy(DataClear::getTempPoint));
        Map<Integer, List<DataClear>> sortedMap = new TreeMap<>(Comparator.naturalOrder());
        Map<Integer, List<DataClear>> sortedMapOrigin = new TreeMap<>(Comparator.naturalOrder());
        sortedMap.putAll(tempMap);
        sortedMapOrigin.putAll(tempMapOrigin);
        Set<Integer> keys1 = sortedMap.keySet();
        Set<Integer> keys2 = sortedMapOrigin.keySet();
        Iterator<Integer> iterator1 = keys1.iterator();
        Iterator<Integer> iterator2 = keys2.iterator();
        while (iterator1.hasNext()) {
            Integer temp_1 = iterator1.next();
            Integer temp_2 = iterator2.next();
            // meterNum
            Map<Long, List<DataClear>> meterNumMap_1 = sortedMap.get(temp_1).stream()
                    .collect(Collectors.groupingBy(dataClear -> Long.valueOf(dataClear.getMeterNum().trim())));
            Map<Long, List<DataClear>> meterNumMap_2 = sortedMapOrigin.get(temp_2).stream()
                    .collect(Collectors.groupingBy(dataClear -> Long.valueOf(dataClear.getMeterNum().trim())));
            Map<Long, List<DataClear>> sortedMap_1 = new TreeMap<>(Comparator.naturalOrder());
            Map<Long, List<DataClear>> sortedMapOrigin_1 = new TreeMap<>(Comparator.naturalOrder());
            sortedMap_1.putAll(meterNumMap_1);
            sortedMapOrigin_1.putAll(meterNumMap_2);
            Set<Long> keys3 = sortedMap_1.keySet();
            Set<Long> keys4 = sortedMapOrigin_1.keySet();
            Iterator<Long> iterator3 = keys3.iterator();
            Iterator<Long> iterator4 = keys4.iterator();
            while (iterator3.hasNext()) {
                Long meterNum_1 = iterator3.next();
                Long meterNum_2 = iterator4.next();
                // flowPoint
                Map<Integer, List<DataClear>> flowPointMap = sortedMap_1.get(meterNum_1).stream().collect(Collectors.groupingBy(DataClear::getFlowPoint));
                Map<Integer, List<DataClear>> flowPointMapOrigin = sortedMapOrigin_1.get(meterNum_2).stream().collect(Collectors.groupingBy(DataClear::getFlowPoint));
                Map<Integer, List<DataClear>> sortedMap_2 = new TreeMap<>(Comparator.naturalOrder());
                Map<Integer, List<DataClear>> sortedMap_3 = new TreeMap<>(Comparator.naturalOrder());
                sortedMap_2.putAll(flowPointMap);
                sortedMap_3.putAll(flowPointMapOrigin);
                Set<Integer> keys5 = sortedMap_2.keySet();
                Set<Integer> keys6 = sortedMap_3.keySet();
                Iterator<Integer> iterator5 = keys5.iterator();
                Iterator<Integer> iterator6 = keys6.iterator();
                while (iterator5.hasNext()) {
                    Integer flowPoint_1 = iterator5.next();
                    Integer flowPoint_2 = iterator6.next();
                    List<DataClear> list1 = flowPointMap.get(flowPoint_1);
                    Row row = sheet.createRow(rowIndex++);
                    //序号  0
                    row.createCell(0).setCellValue(rowIndex - 1);
                    //use
                    row.createCell(1).setCellValue(0);
                    //  SP
                    int lastDigit = Math.toIntExact(meterNum_1 % 10);
                    row.createCell(2).setCellValue("SP" + lastDigit);
//                    row.createCell(2).setCellValue("SP" + (Integer.valueOf(sp) - 48));
                    //weight
                    row.createCell(3).setCellValue(0);
                    // 格式化结果，精确到小数点后三位
                    DecimalFormat decimalFormat = new DecimalFormat("#.###");
                    OptionalDouble diffAverage = list1.stream()
                            .mapToDouble(DataClear::getDiffTofNs)
                            .average();
                    //平均difTof(精准度)
                    String diffAverage_1 = "";
                    if (diffAverage.isPresent()) {
                        // 进行转换或处理
                        diffAverage_1 = decimalFormat.format(diffAverage.getAsDouble());
                        row.createCell(4).setCellValue(diffAverage_1);
                    } else {
                        // 处理字段值为null的情况
                        row.createCell(4).setCellValue("0");
                    }
                    //流量
                    OptionalDouble standardFlowAvg = list1.stream()
                            .mapToDouble(DataClear::getStandardFlow)
                            .average();
                    String standardFlowAverage_1 = decimalFormat.format(standardFlowAvg.getAsDouble());
                    row.createCell(6).setCellValue(standardFlowAverage_1);
                    //平均sumTof
                    OptionalDouble sumAverage = list1.stream()
                            .mapToDouble(DataClear::getSumTofNs)
                            .average();
                    //平均sumTof(精准度)
                    String sumAverage_1 = "";
                    if (sumAverage.isPresent()) {
                        // 进行转换或处理
                        sumAverage_1 = decimalFormat.format(sumAverage.getAsDouble());
                        row.createCell(5).setCellValue(sumAverage_1);
                    } else {
                        // 处理字段值为null的情况
                        row.createCell(5).setCellValue("0");
                    }

                    //平均tmpIn
                    //平均tmpOut
                    OptionalDouble tmpInAverage_1 = list1.stream()
                            .mapToDouble(DataClear::getTmpIn)
                            .average();
                    OptionalDouble tmpOutAverage_1 = list1.stream()
                            .mapToDouble(DataClear::getTmpOut)
                            .average();
                    DecimalFormat decimalFormat_1 = new DecimalFormat("#.#");
                    String tmpAverage = decimalFormat_1.format((tmpInAverage_1.getAsDouble() + tmpOutAverage_1.getAsDouble()) / 2);
                    row.createCell(7).setCellValue(tmpAverage);
                    row.createCell(8).setCellValue(temp_1);
                    row.createCell(9).setCellValue(sortedMap_2.get(flowPoint_1).size());
                    row.createCell(10).setCellValue(sortedMap_3.get(flowPoint_2).size());
                    // 不是线程安全的
                    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
                    String dateString = format.format(list1.get(0).getTime());
                    characterizedData.setTime(dateString);
                    characterizedData.setMeterNum(String.valueOf(meterNum_1));
                    characterizedData.setSp("SP" + lastDigit);
                    characterizedData.setAvgDiffTofNs(diffAverage_1);
                    characterizedData.setAvgSumTofNs(sumAverage_1);
                    characterizedData.setAvgStandardFlow(standardFlowAverage_1);
                    characterizedData.setAvgTemp(tmpAverage);
                    characterizedData.setTempPoint(temp_1);
                    characterizedData.setStripData(sortedMap_2.get(flowPoint_1).size());
                    // 数据库插入特征化数据
                    int result;
                    if (if_success == 1) {

                    } else {
//                        按照当前时间戳进行排序
                        result = jokerDao.insertData(joker_king, characterizedData);
                        if (result == 1) {

                        } else {
                            System.out.println("数据出错辣");
                        }
                    }
                }
            }
        }
        excelReturn("特征化数据.xlsx", workbook, response);
    }

    @Override
    public void uploadExcel_3(String meterNum, HttpServletResponse response) {
        // 创建工作簿
        Workbook workbook = new XSSFWorkbook();

        // 创建工作表
        Sheet sheet = workbook.createSheet("验证数据");

        // 创建表头
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("time");
        headerRow.createCell(1).setCellValue("meterNum");
        headerRow.createCell(2).setCellValue("waterMeterFlowM3h");
        headerRow.createCell(3).setCellValue("standardFlowM3h");
        headerRow.createCell(4).setCellValue("instantaneousFlowM3h");
        headerRow.createCell(5).setCellValue("tempBiao");
        headerRow.createCell(6).setCellValue("tempTable");
        headerRow.createCell(7).setCellValue("wrong");
        headerRow.createCell(8).setCellValue("ampUp");
        headerRow.createCell(9).setCellValue("ampDown");
        headerRow.createCell(10).setCellValue("diffTofNs");
        headerRow.createCell(11).setCellValue("sumTofNs");
        headerRow.createCell(12).setCellValue("pwrUp");
        headerRow.createCell(13).setCellValue("pwrDown");
        headerRow.createCell(14).setCellValue("accruedFlow");
        headerRow.createCell(15).setCellValue("shuiTaiAccruedFlow");
        headerRow.createCell(16).setCellValue("error");
        headerRow.createCell(17).setCellValue("errorStandard");
        headerRow.createCell(18).setCellValue("flowPoint");
        headerRow.createCell(19).setCellValue("tempPoint");
        List<VerificationData> list;
        list = verificationDao.getAllDataVerification(meterNum);
        // 正态分布筛选出95%数据
        List<VerificationData> filteredList = dataAlgorithm(list);
        // 填充数据
        int rowIndex = 1;
        for (VerificationData verificationData : filteredList) {
            Row row = sheet.createRow(rowIndex++);
            Date time_1 = verificationData.getTime();
            Timestamp timestamp = new Timestamp(time_1.getTime());
            LocalDateTime localDateTime = timestamp.toLocalDateTime();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

            String formattedDateTime = localDateTime.format(formatter);
            row.createCell(0).setCellValue(formattedDateTime);
            row.createCell(1).setCellValue(verificationData.getMeterNum());
            row.createCell(2).setCellValue(verificationData.getWaterMeterFlowM3h());
            row.createCell(3).setCellValue(verificationData.getStandardFlowM3h());
            row.createCell(4).setCellValue(verificationData.getInstantaneousFlowM3h());
            row.createCell(5).setCellValue(verificationData.getTempBiao());
            row.createCell(6).setCellValue(verificationData.getTempTable());
            row.createCell(7).setCellValue(verificationData.getWrong());
            row.createCell(8).setCellValue(verificationData.getAmpUp());
            row.createCell(9).setCellValue(verificationData.getAmpDown());
            row.createCell(10).setCellValue(verificationData.getDiffTofNs());
            row.createCell(11).setCellValue(verificationData.getSumTofNs());
            row.createCell(12).setCellValue(verificationData.getPwrUp());
            row.createCell(13).setCellValue(verificationData.getPwrDown());
            row.createCell(14).setCellValue(verificationData.getAccruedFlow());
            row.createCell(15).setCellValue(verificationData.getShuiTaiAccruedFlow());
            row.createCell(16).setCellValue(verificationData.getError());
            row.createCell(17).setCellValue(verificationData.getErrorStandard());
            row.createCell(18).setCellValue(verificationData.getFlowPoint());
            row.createCell(19).setCellValue(verificationData.getTempPoint());

        }
        excelReturn("验证数据.xlsx", workbook, response);
    }

    private List<VerificationData> dataAlgorithm(List<VerificationData> list) {
        Map<Integer, List<VerificationData>> groupedMap = list.stream()
                .collect(Collectors.groupingBy(VerificationData::getFlowPoint));

        Map<Integer, List<VerificationData>> filteredMap = groupedMap.entrySet().stream()
                .map(entry -> {
                    Integer flowPoint = entry.getKey();
                    List<VerificationData> dataList = entry.getValue();

                    dataList.sort(Comparator.comparingDouble(VerificationData::getDiffTofNs));

                    int lowerIndex = (int) Math.round(dataList.size() * 0.025);
                    int upperIndex = (int) Math.round(dataList.size() * 0.975);

                    List<VerificationData> filteredList = dataList.subList(lowerIndex, upperIndex);

                    return Map.entry(flowPoint, filteredList);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (oldList, newList) -> {
                    List<VerificationData> mergedList = new ArrayList<>(oldList);
                    mergedList.addAll(newList);
                    return mergedList;
                }, LinkedHashMap::new));

        return filteredMap.values().stream()
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    @Override
    public void uploadExcel_4(String meterNum, HttpServletResponse response) {
        // 创建工作簿
        Workbook workbook = new XSSFWorkbook();

        // 创建工作表
        Sheet sheet = workbook.createSheet("误差表数据");

        // 创建表头
        Row headerRow = sheet.createRow(0);

        headerRow.createCell(0).setCellValue("SP");
        headerRow.createCell(1).setCellValue("tempPoint");
        headerRow.createCell(2).setCellValue("flowPoint");
        headerRow.createCell(3).setCellValue("diffTofNs");
        headerRow.createCell(4).setCellValue("waterMeterFlowM3h");
        headerRow.createCell(5).setCellValue("standardFlowM3h");
        headerRow.createCell(6).setCellValue("errorStandard");
        headerRow.createCell(7).setCellValue("sumTofNs");
        headerRow.createCell(8).setCellValue("tempBiao");
        headerRow.createCell(9).setCellValue("tempTable");
        headerRow.createCell(10).setCellValue("过滤后条数");
        headerRow.createCell(11).setCellValue("过滤前条数");

        List<VerificationData> list = verificationDao.getAllErrorData(meterNum);
        List<VerificationData> listOrigin = verificationDao.getAllErrorDataOrigin(meterNum);
        // 填充数据
        // 正态分布筛选出95%数据
        List<VerificationData> filteredList = dataAlgorithm(list);
        int rowIndex = 1;
        // tempPoint*meterNum*flowPoint
        Map<Integer, List<VerificationData>> tempMap = filteredList.stream().collect(Collectors.groupingBy(VerificationData::getTempPoint));
        Map<Integer, List<VerificationData>> tempMapOrigin = listOrigin.stream().collect(Collectors.groupingBy(VerificationData::getTempPoint));
        Map<Integer, List<VerificationData>> sortedMap = new TreeMap<>(Comparator.naturalOrder());
        Map<Integer, List<VerificationData>> sortedMapOrigin = new TreeMap<>(Comparator.naturalOrder());
        sortedMap.putAll(tempMap);
        sortedMapOrigin.putAll(tempMapOrigin);
        Set<Integer> keys1 = sortedMap.keySet();
        Set<Integer> keys2 = sortedMapOrigin.keySet();
        Iterator<Integer> iterator1 = keys1.iterator();
        Iterator<Integer> iterator2 = keys2.iterator();
        while (iterator1.hasNext()) {
            Integer temp_1 = iterator1.next();
            Integer temp_2 = iterator2.next();
            // meterNum
            Map<Long, List<VerificationData>> meterNumMap_1 = sortedMap.get(temp_1).stream()
                    .collect(Collectors.groupingBy(VerificationData -> Long.valueOf(VerificationData.getMeterNum().trim())));
            Map<Long, List<VerificationData>> meterNumMap_2origin = sortedMapOrigin.get(temp_2).stream()
                    .collect(Collectors.groupingBy(VerificationData -> Long.valueOf(VerificationData.getMeterNum().trim())));
            Map<Long, List<VerificationData>> sortedMap_1 = new TreeMap<>(Comparator.naturalOrder());
            Map<Long, List<VerificationData>> sortedMap_2origin = new TreeMap<>(Comparator.naturalOrder());
            sortedMap_1.putAll(meterNumMap_1);
            sortedMap_2origin.putAll(meterNumMap_2origin);
            Set<Long> keys3 = sortedMap_1.keySet();
            Set<Long> keys4 = sortedMap_2origin.keySet();
            Iterator<Long> iterator3 = keys3.iterator();
            Iterator<Long> iterator4 = keys4.iterator();
            while (iterator3.hasNext()) {
                Long meterNum_1 = iterator3.next();
                Long meterNum_2 = iterator4.next();
                // flowPoint
                Map<Integer, List<VerificationData>> flowPointMap = sortedMap_1.get(meterNum_1).stream().collect(Collectors.groupingBy(VerificationData::getFlowPoint));
                Map<Integer, List<VerificationData>> flowPointMaporigin = sortedMap_2origin.get(meterNum_2).stream().collect(Collectors.groupingBy(VerificationData::getFlowPoint));
                Map<Integer, List<VerificationData>> sortedMap_2 = new TreeMap<>(Comparator.naturalOrder());
                Map<Integer, List<VerificationData>> sortedMap_3origin = new TreeMap<>(Comparator.naturalOrder());
                sortedMap_2.putAll(flowPointMap);
                sortedMap_3origin.putAll(flowPointMaporigin);
                Set<Integer> keys5 = sortedMap_2.keySet();
                Set<Integer> keys6 = sortedMap_3origin.keySet();
                Iterator<Integer> iterator5 = keys5.iterator();
                Iterator<Integer> iterator6 = keys6.iterator();
                while (iterator5.hasNext()) {
                    Integer flowPoint_1 = iterator5.next();
                    Integer flowPoint_2 = iterator6.next();
                    List<VerificationData> list1 = flowPointMap.get(flowPoint_1);
                    Row row = sheet.createRow(rowIndex++);
                    //  SP
                    int lastDigit = Math.toIntExact(meterNum_1 % 10);
                    row.createCell(0).setCellValue("SP" + lastDigit);

                    row.createCell(1).setCellValue(temp_1);
                    row.createCell(2).setCellValue(flowPoint_1);
                    // 格式化结果，精确到小数点后三位
                    DecimalFormat decimalFormat = new DecimalFormat("#.###");
                    OptionalDouble diffAverage = list1.stream()
                            .mapToDouble(VerificationData::getDiffTofNs)
                            .average();
                    //平均difTof(精准度)
                    String diffAverage_1;
                    if (diffAverage.isPresent()) {
                        // 进行转换或处理
                        diffAverage_1 = decimalFormat.format(diffAverage.getAsDouble());
                        row.createCell(3).setCellValue(Double.parseDouble(diffAverage_1));
                    } else {
                        // 处理字段值为null的情况
                        row.createCell(3).setCellValue("0");
                    }
                    //平均sumTof
                    OptionalDouble sumAverage = list1.stream()
                            .mapToDouble(VerificationData::getSumTofNs)
                            .average();

                    //waterMeterFlowM3h
                    OptionalDouble waterMeterFlowM3hAvg = list1.stream()
                            .mapToDouble(VerificationData::getWaterMeterFlowM3h)
                            .average();
                    String waterMeterFlowM3hAvg_1 = decimalFormat.format(waterMeterFlowM3hAvg.getAsDouble() * 1000);
                    row.createCell(4).setCellValue(Double.parseDouble(waterMeterFlowM3hAvg_1));
                    //流量
                    OptionalDouble standardFlowAvg = list1.stream()
                            .mapToDouble(VerificationData::getStandardFlowM3h)
                            .average();
                    String standardFlowAverage_1 = decimalFormat.format(standardFlowAvg.getAsDouble());
                    row.createCell(5).setCellValue(Double.parseDouble(standardFlowAverage_1));
                    //错误流量值
                    OptionalDouble errorStandardAvg = list1.stream()
                            .mapToDouble(VerificationData::getErrorStandard)
                            .average();
                    String errorStandardAvg_1 = decimalFormat.format(errorStandardAvg.getAsDouble());
                    Cell cell = row.createCell(6);
                    cell.setCellValue(Double.parseDouble(errorStandardAvg_1));
                    if (Math.abs(Double.parseDouble(errorStandardAvg_1)) >= 2 && Math.abs(Double.parseDouble(errorStandardAvg_1)) < 5) {
                        // Create a new cell style with a green fill color
                        CellStyle style = workbook.createCellStyle();
                        style.setFillForegroundColor(IndexedColors.GREEN.getIndex());
                        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                        // Apply the cell style to the cell
                        cell.setCellStyle(style);
                    } else if (Math.abs(Double.parseDouble(errorStandardAvg_1)) >= 5) {
                        CellStyle style = workbook.createCellStyle();
                        style.setFillForegroundColor(IndexedColors.RED.getIndex());
                        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                        // Apply the cell style to the cell
                        cell.setCellStyle(style);
                    }
                    //平均sumTof(精准度)
                    String sumAverage_1;
                    if (sumAverage.isPresent()) {
                        // 进行转换或处理
                        sumAverage_1 = decimalFormat.format(sumAverage.getAsDouble());
                        row.createCell(7).setCellValue(Double.parseDouble(sumAverage_1));
                    } else {
                        // 处理字段值为null的情况
                        row.createCell(7).setCellValue("0");
                    }
                    DecimalFormat decimalFormat_1 = new DecimalFormat("#.#");
                    //平均tmp_biao
                    OptionalDouble tmpBiaoAverage_1 = list1.stream()
                            .mapToDouble(VerificationData::getTempBiao)
                            .average();
                    String tmpBiaoAverage = decimalFormat_1.format(tmpBiaoAverage_1.getAsDouble());
                    row.createCell(8).setCellValue(Double.parseDouble(tmpBiaoAverage));
                    //平均tmp
                    OptionalDouble tmpTableAverage_1 = list1.stream()
                            .mapToDouble(VerificationData::getTempTable)
                            .average();

                    String tmpAverage = decimalFormat_1.format(tmpTableAverage_1.getAsDouble());
                    row.createCell(9).setCellValue(Double.parseDouble(tmpAverage));
                    row.createCell(10).setCellValue(sortedMap_2.get(flowPoint_1).size());
                    row.createCell(11).setCellValue(sortedMap_3origin.get(flowPoint_2).size());
                }
            }
        }
        excelReturn("误差表数据.xlsx", workbook, response);
    }

    private void excelReturn(String filename, Workbook workbook, HttpServletResponse response) {
        String encodedFilename;
        encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8);

        // 设置响应头信息
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encodedFilename + ".xlsx\"");

        // 将工作簿写入响应流并关闭工作环境
        try {
            workbook.write(response.getOutputStream());
            workbook.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public List<DataClear> getByMeterNum(String meterNum) {
        // 防止数据为空,空搜索展现全部数据
        if (meterNum == null || meterNum.equals("")) {
            return new ArrayList<>();
        }
//        筛选到符合数据
        List<DataClear> filteredList = normalDistribution(meterNum);
        this.dataClearList_1 = filteredList;
        return filteredList;

    }
}
