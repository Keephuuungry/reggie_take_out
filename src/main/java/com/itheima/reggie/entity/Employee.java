package com.itheima.reggie.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 员工实体类，对应数据库中的employee表
 */
@Data
public class Employee implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;

    // 数据库中对该字段有唯一性约束
    private String username;

    private String name;

    private String password;

    private String phone;

    private String sex;

    private String idNumber;

    // status默认值为1
    private Integer status;

    // mybatisplus公共字段自动填充
    // 在插入时自动填充字段
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    // 在插入和更新时自动填充字段
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableField(fill = FieldFill.INSERT)
    private Long createUser;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updateUser;

}
