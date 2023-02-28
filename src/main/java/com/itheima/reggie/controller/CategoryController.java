package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Category;
import com.itheima.reggie.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RequestMapping("/category")
@RestController
public class CategoryController {
    @Autowired
    private CategoryService categoryService;

    @PostMapping
    public R<String> save(@RequestBody Category category){
        log.info("Category:{}", category);
        categoryService.save(category);
        return R.success("新增分类成功");
    }

    /*
    分类信息分页查询 执行流程
    1. 页面发送ajax请求，将分页查询参数(page、pageSize)提交到服务端
    2. 服务端Controller接收页面提交的数据并调用Service查询数据
    3. Service调用Mapper操作数据库，查询分页数据
    4. Controller将查询到的分页数据响应给页面
    5. 页面接收到分页数据并通过ElementUI的Table组件展示到页面上
     */
    @GetMapping("/page")
    public R<Page> page(int page, int pageSize){
        // 分页构造器
        Page<Category> pageInfo = new Page<>(page, pageSize);
        // 条件构造器
        LambdaQueryWrapper<Category> queryWrapper = new LambdaQueryWrapper<>();
        // 添加排序条件，根据sort进行排序
        queryWrapper.orderByAsc(Category::getSort);
        // 进行分页查询
        categoryService.page(pageInfo, queryWrapper);

        return R.success(pageInfo);
    }

    /**
     * 根据id删除分类
     * @param ids
     * @return
     */
    @DeleteMapping
    public R<String> delete(Long ids){
        log.info("删除分类,id为{}",ids);

//        categoryService.removeById(ids);
        // 菜品不能如此直接删除，很多菜品与套餐有关联关系，如果需要删除，需确认
        categoryService.remove(ids);
        return R.success("分类信息删除成功");
    }

    /**
     * 根据id修改分类信息
     * @param category
     * @return
     */
    @PutMapping
    public R<String> update(@RequestBody Category category){
        log.info("修改分类信息");
        categoryService.updateById(category);
        return R.success("修改分类信息成功");
    }

    /**
     * 添加菜品时，获取菜品分类下拉框中的数据
     * 前端页面:/backend/page/food/add.html getCategoryList方法
     * @param category
     * @return
     */
    @GetMapping("/list")
    public R<List<Category>> list(Category category){
        // category中只有type属性是有值的，当然可以用String type来传参，但是用类来接更通用

        // 条件构造器
        LambdaQueryWrapper<Category> lambdaQueryWrapper = new LambdaQueryWrapper<>();
        // 根据type来查询，各类菜品分类/套餐分类
        lambdaQueryWrapper.eq(category.getType() != null,Category::getType, category.getType());
        lambdaQueryWrapper.orderByAsc(Category::getSort);
        lambdaQueryWrapper.orderByDesc(Category::getUpdateTime);
        // 查询
        List<Category> list = categoryService.list(lambdaQueryWrapper);
        return R.success(list);
    }
}