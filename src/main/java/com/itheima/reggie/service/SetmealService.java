package com.itheima.reggie.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;

import java.util.List;

public interface SetmealService extends IService<Setmeal> {
    // 将套餐的基本信息和关联的菜品信息保存
    public void saveWithDish(SetmealDto setmealDto);

    // 删除套餐，同时需要删除套餐和菜品的关联数据
    public void removeWithDish(List<Long> ids);

    // 修改套餐时，回显页面数据
    public SetmealDto getWithDish(Long id);

    // 修改套餐时，保存修改的套餐信息
    public void updateWithDish(SetmealDto setmealDto);
}
