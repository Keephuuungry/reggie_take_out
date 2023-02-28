package com.itheima.reggie.dto;

import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import lombok.Data;
import java.util.ArrayList;
import java.util.List;

/**
 * 用于封装页面提交的数据（传过来的属性和实体类的属性并不一一对应，需要专门新建dto来进行数据交互）
 */
@Data
public class DishDto extends Dish {

    private List<DishFlavor> flavors = new ArrayList<>();

    // categoryName用于分页查询
    private String categoryName;

    private Integer copies;
}
