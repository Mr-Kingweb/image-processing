package com.example.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.pojo.CharacterizedData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

/**
 * @author JinShengJie
 * @date 2023-07-12 13:26
 */
@Mapper
@Repository
public interface CharacterizedDao extends BaseMapper<CharacterizedData> {

}
