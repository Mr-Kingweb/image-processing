package com.example.dao;

import com.example.pojo.VerificationData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author JinShengJie
 * @date 2023-07-13 13:41
 */
@Mapper
@Repository
public interface VerificationDao {
    List<VerificationData> getAllDataVerification(@Param("meterNum")String meterNum);
    List<VerificationData> getAllErrorData(@Param("meterNum")String meterNum);
    List<VerificationData> getAllErrorDataOrigin(@Param("meterNum") String meterNum);
}
