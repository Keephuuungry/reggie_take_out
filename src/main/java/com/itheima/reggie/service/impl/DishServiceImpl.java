package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.mapper.DishMapper;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DishServiceImpl extends ServiceImpl<DishMapper, Dish> implements DishService {
    @Autowired
    private DishFlavorService dishFlavorService;
    /**
     * 新增菜品，同时保存对应的口味数据
     * @param dishDto
     */
    @Transactional
    @Override
    public void saveWithFlavor(DishDto dishDto) {
        // 保存菜品的基本信息到菜品表dish
        // mp自动生成的id 先保存到dish的实体中，再进行数据库的插入，这一步可以获取id
        // 注意，此时Dish实体中的id和DishFlavor实体中的dishId是一个东西
        this.save(dishDto);

        // 保存菜品口味数据到菜品口味表 dish_flavor
        // 虽然dishDto封装了DishFlavor类和Dish类，但是前端传过来的dishDto中，并没有DishFlavor类中的dishId字段
        // 所以不能用简单的 dishFlavorService.saveBatch(dishDto.getFlavors());

        // dishDto没有getId()方法，继承父类Dish的getId()方法
        Long dishId = dishDto.getId();

        // 菜品口味
        List<DishFlavor> flavors = dishDto.getFlavors();
//        if(flavors != null){
//            // dishId是当前所有口味flavors的对应的菜品id
//            for(int i = 0; i < flavors.size(); i++){
//                flavors.get(i).setDishId(dishId);
//            }
//            dishFlavorService.saveBatch(flavors);
//        }
        flavors = flavors.stream().map((item) ->{
            item.setDishId(dishDto.getId());
            return item;
        }).collect(Collectors.toList());
        dishFlavorService.saveBatch(flavors);
    }

    // 根据id 查询菜品信息和对应的口味信息
    @Override
    public DishDto getByIdWithFlavor(Long id) {
        // 1. 查询菜品的基本信息 查dish
        Dish dish = this.getById(id);

        // 用DishDto对象来整合菜品基本信息
        DishDto dishDto = new DishDto();
        BeanUtils.copyProperties(dish, dishDto);

        // 2. 查询口味的基本信息 查dish_flavor
        LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(DishFlavor::getDishId, id);
        List<DishFlavor> flavors = dishFlavorService.list(lambdaQueryWrapper);

        // 用DishDto对象来整合口味基本信息
        dishDto.setFlavors(flavors);

        return dishDto;
    }

    // 更新菜品信息，同时更新对应的口味信息
    @Override
    @Transactional
    public void updateWithFlavor(DishDto dishDto) {
        // 1. 更新dish表基本信息
        // 方法的参数名是Dish类型的，这里传了个子类进去，多态
        this.updateById(dishDto);
        // 2. 清理dish_flavor表中当前菜品的口味数据——delete操作
        LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(DishFlavor::getDishId, dishDto.getId());
        dishFlavorService.remove(lambdaQueryWrapper);
        // 3. 添加dish_flavor表中当前菜品的口味数据——insert操作
        // 前端传过来的dishDto数据中，DishFlavor类的数据里dishId属性缺失，传过来的只有口味名称name和口味数据value
        List<DishFlavor> flavors = dishDto.getFlavors();
        flavors = flavors.stream().map((item) ->{
            item.setDishId(dishDto.getId());
            return item;
        }).collect(Collectors.toList());
        dishFlavorService.saveBatch(flavors);
    }
}