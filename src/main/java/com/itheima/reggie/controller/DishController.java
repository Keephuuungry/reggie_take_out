package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.dto.DishDto;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.entity.Dish;
import com.itheima.reggie.entity.DishFlavor;
import com.itheima.reggie.service.CategoryService;
import com.itheima.reggie.service.DishFlavorService;
import com.itheima.reggie.service.DishService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequestMapping("/dish")
@RestController
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private DishFlavorService dishFlavorService;

    @Autowired
    private CategoryService categoryService;

    /**
     * 新增菜品
     * 新增菜品 交互过程
     *     1. 页面(backend/page/food/add.html)发送ajax请求，请求服务端获取菜品分类数据并展示到下拉框中
     *     2. 页面发送请求进行图片上传，请求服务端将图片保存到服务器
     *     3. 页面发送请求进行图片下载，将上传的图片进行回显
     *     4. 点击保存按钮， 发送ajax请求，将菜品相关数据以json形式提交到服务端
     * @param dishDto
     * @return
     */
    @PostMapping
    public R<String> save(@RequestBody DishDto dishDto){
        // 注意这里使用了dishDto类，来封装页面发过来的数据
        log.info(dishDto.toString());
        // dishService中自定义了saveWithFlavor方法
        dishService.saveWithFlavor(dishDto);
        return R.success("新增菜品成功");
    }


    /**
     * 菜品信息分页查询
     * @param page
     * @param pageSize
     * @param name
     * @return
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name){
        // 构造分页构造器对象
        Page<Dish> pageInfo = new Page<>(page, pageSize);
        Page<DishDto> dishDtoPage = new Page<>(page, pageSize);
        // 条件构造器
        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        // 添加过滤条件
        lambdaQueryWrapper.like(name != null, Dish::getName, name);
        // 添加排序条件
        lambdaQueryWrapper.orderByDesc(Dish::getUpdateTime);
        // 执行分页查询
        dishService.page(pageInfo, lambdaQueryWrapper);

        // 此时，pageInfo里面的一条条Dish记录中，categoryId菜品分类是Long型数据，而页面要的是分类名称
        // 解决办法：使用DishDto类，Dish表分页查询后，将pageInfo对象拷贝到dishDtoPage对象中
        // pageInfo除了records属性都要拷贝，records属性需要处理一下
        BeanUtils.copyProperties(pageInfo, dishDtoPage, "records");

        List<Dish> records = pageInfo.getRecords();
        List<DishDto> list = records.stream().map((item) -> {
            DishDto dishDto = new DishDto();

            BeanUtils.copyProperties(item, dishDto);

            Long categoryId = item.getCategoryId(); // 分类id
            // 根据categoryid查询分类对象
            Category category = categoryService.getById(categoryId);

            if(category != null){
                String categoryName = category.getName();
                dishDto.setCategoryName(categoryName);
            }
            return dishDto;
        }).collect(Collectors.toList());

        dishDtoPage.setRecords(list);

        return R.success(dishDtoPage);
    }

    /**
     * 数据根据id查询菜品信息和对应的口味信息 用于修改菜品时 回显
     * @param id
     * @return
     */
    @GetMapping("/{id}")
    public R<DishDto> get(@PathVariable Long id){
        //不仅要回显dish的基本属性，而且要回显DishFlavor菜品的口味，所以用DishDto
        DishDto dishDto = dishService.getByIdWithFlavor(id);
        return R.success(dishDto);
    }


    /**
     * 修改菜品的信息——提交
     * @param dishDto
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody DishDto dishDto){
        // 注意这里使用了dishDto类，来封装页面发过来的数据
        log.info(dishDto.toString());
        // dishService中自定义了saveWithFlavor方法
        dishService.updateWithFlavor(dishDto);
        return R.success("新增菜品成功");
    }

    /**
     * 删除菜品
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<String> delete(Long[] ids){
        log.info("删除id为{}的菜品数据",ids);
        // 删除dish_flavor表中关联的口味
        List<Long> list = Arrays.asList(ids);
        LambdaQueryWrapper<DishFlavor> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.in(DishFlavor::getDishId, list);
        dishFlavorService.remove(lambdaQueryWrapper);

        // 删除dish表中的菜品
        dishService.removeByIds(list);
        return R.success("菜品删除成功");
    }


    /**
     * 改变Dish菜品的销售状态(改变和批量改变)
     * @param status
     * @param ids
     * @return
     */
    @PostMapping("/status/{status}")
    public R<String> updateStatus(@PathVariable Integer status, Long[] ids){
        log.info("ids:{}", ids);
        List<Dish> dishes = dishService.listByIds(Arrays.asList(ids));
        for(int i = 0; i < dishes.size(); i++){
            Dish dish = dishes.get(i);
            dish.setStatus(status);
            dishService.updateById(dish);
        }
        return R.success("菜品销售状态改变成功");
    }

    @GetMapping("/list")
    public R<List<Dish>> list(Dish dish){
        // 虽然页面传过来的参数只有categoryid，用Dish类来接收更通用一点

        LambdaQueryWrapper<Dish> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        lambdaQueryWrapper.eq(dish.getCategoryId() != null, Dish::getCategoryId, dish.getCategoryId());
        // 查询起售状态的菜品
        lambdaQueryWrapper.eq(Dish::getStatus, 1);
        lambdaQueryWrapper.orderByAsc(Dish::getSort);
        lambdaQueryWrapper.orderByDesc(Dish::getUpdateTime);
        List<Dish> list = dishService.list(lambdaQueryWrapper);
        return R.success(list);
    }
}
