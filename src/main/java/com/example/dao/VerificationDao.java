package com.example.dao;

import com.baomidou.dynamic.datasource.annotation.DS;
import com.example.pojo.VerificationData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * @author JinShengJie
 * @date 2023-07-13 13:41
 */
@Repository
@Mapper
public interface VerificationDao {
    /**
     * @param meterNum
     * @return {@link List}<{@link VerificationData}>
     */
    List<VerificationData> getAllDataVerification(@Param("meterNum") String meterNum);

    /**
     * @param meterNum
     * @return {@link List}<{@link VerificationData}>
     */
    List<VerificationData> getAllErrorData(@Param("meterNum") String meterNum);

    /**
     * @param meterNum
     * @return {@link List}<{@link VerificationData}>
     */
    List<VerificationData> getAllErrorDataOrigin(@Param("meterNum") String meterNum);

    /**
     * @return {@link List}<{@link String}>
     */
    List<String> meterNumList();
}
