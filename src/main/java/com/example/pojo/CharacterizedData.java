package com.example.pojo;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.activerecord.Model;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * @author JinShengJie
 * @date 2023-07-12 13:20
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class CharacterizedData extends Model<CharacterizedData> implements Serializable {
    private String time;
    private String meterNum;
    private String sp;
    private String avgDiffTofNs;
    private String avgSumTofNs;
    private String avgStandardFlow;
    private String avgTemp;
    private Integer tempPoint;
    private Integer stripData;
}
