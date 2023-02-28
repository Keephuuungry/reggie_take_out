package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.SetmealDto;
import com.itheima.reggie.entity.Setmeal;
import com.itheima.reggie.entity.SetmealDish;
import com.itheima.reggie.mapper.SetmealMapper;
import com.itheima.reggie.service.SetmealDishService;
import com.itheima.reggie.service.SetmealService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SetmealServiceImpl extends ServiceImpl<SetmealMapper, Setmeal> implements SetmealService {
    @Autowired
    private SetmealDishService setmealDishService;
    // 将套餐的基本信息和关联的菜品信息保存
    @Override
    @Transactional // 操作两张表，加事务注解
    public void saveWithDish(SetmealDto setmealDto) {
        // 保存套餐的基本信息，操作setmeal，执行insert操作
        this.save(setmealDto); // setmealDto继承setmeal，多态

        // 保存套餐和菜品的关联关系，操作setmeal_dish，执行insert操作
        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        // 在保存setmealDishes之前，注意到前端传过来的一个个SetmealDish对象中，setmealId是没有值的，需要进行处理
        setmealDishes.stream().map((item) -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());
        setmealDishService.saveBatch(setmealDishes);
    }


    @Transactional
    @Override
    public void removeWithDish(List<Long> ids) {
        // 1. 查询套餐状态，只有停售的套餐才能删除，如果套餐正在售卖中，不能删除
        // select count(*) from setmeal where id in (1,2,3) and status = 1
        LambdaQueryWrapper<Setmeal> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.in(Setmeal::getId, ids);
        queryWrapper.eq(Setmeal::getStatus, 1);
        int count = this.count(queryWrapper);
        // 2. 如果不能删除，抛出一个业务异常
        if(count > 0){
            throw new CustomException("套餐正在售卖中，请停售后再删除");
        }
        // 3. 如果可以删除，删除套餐表中的数据 setmeal表
        this.removeByIds(ids);
        // 4. 删除关系表中的数据 setmeal_dish表
        // 这里的ids是套餐的id，为setmealDish中的setmeal_id
        LambdaQueryWrapper<SetmealDish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(SetmealDish::getSetmealId, ids);
        setmealDishService.remove(lambdaQueryWrapper);
    }

    // 修改套餐时，回显页面数据
    @Override
    public SetmealDto getWithDish(Long id) {
        SetmealDto setmealDto = new SetmealDto();

        // 将套餐基本信息复制进setmealDto对象中
        Setmeal setmeal = this.getById(id);
        BeanUtils.copyProperties(setmeal, setmealDto);
        // 将套餐关联的菜品信息装入setmealDto对象中
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, id);
        List<SetmealDish> list = setmealDishService.list(queryWrapper);
        setmealDto.setSetmealDishes(list);

        return setmealDto;
    }

    // 修改套餐时，保存修改的套餐信息
    @Override
    public void updateWithDish(SetmealDto setmealDto) {
        // 更新setmeal表中的数据
        this.updateById(setmealDto);

        // 更新setmeal_dish表中的数据 (删除原有数据，添加新的数据)
        LambdaQueryWrapper<SetmealDish> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(SetmealDish::getSetmealId, setmealDto.getId());
        setmealDishService.remove(queryWrapper);

        List<SetmealDish> setmealDishes = setmealDto.getSetmealDishes();
        setmealDishes.stream().map((item) -> {
            item.setSetmealId(setmealDto.getId());
            return item;
        }).collect(Collectors.toList());
        setmealDishService.saveBatch(setmealDto.getSetmealDishes());
    }
}
