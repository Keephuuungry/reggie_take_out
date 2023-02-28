package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.entity.Category;

public interface CategoryService extends IService<Category> {
    // 自定义方法，因为Iservice和BaseMapper提供的方法不能满足需求
    public void remove(Long id);
}
