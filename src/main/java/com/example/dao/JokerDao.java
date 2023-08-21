package com.example.dao;

import com.example.pojo.CharacterizedData;
import com.example.pojo.DataClear;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import javax.xml.crypto.Data;
import java.util.List;

/**
 * @author JinShengJie
 * @date 2023-07-04 10:39
 */
@Mapper
@Repository
public interface JokerDao {
    // 获取全部数据
    List<DataClear> getAllData(@Param("meterNum") String meterNum);
    // 获取原始全部数据
    List<DataClear> getAllDataOrigin(@Param("meterNum") String meterNum);

    List<DataClear> getAllDataBp(@Param("pageSize")Integer pageSize,@Param("offset")Integer offset);

    List<Integer> ampUp();

    Integer increaseDatabase(@Param("timeNow") String timeNow);

    Integer insertData(@Param("timeNow")String timeNow,CharacterizedData characterizedData);

    Integer ifHasBaseNum(@Param("TableName") String tableName);
}
