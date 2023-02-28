package com.itheima.reggie.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.itheima.reggie.common.R;
import com.itheima.reggie.entity.Employee;
import com.itheima.reggie.service.EmployeeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.jni.Local;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.stereotype.Controller;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;

@Slf4j
@RestController
@RequestMapping("/employee")
public class EmployeeController {

    @Autowired
    private EmployeeService employeeService;

    /**
     * 员工登录
     * @param employee
     * @return
     */
    @PostMapping("/login")
    public R<Employee> login(HttpServletRequest request, @RequestBody Employee employee){
        // 前端传过来的数据是json格式的，需要用RequestBody来接收。
        // json包括username和password，与Employee的两个属性相对应

        /* 员工登录，处理逻辑：
            1. 将页面提交的密码password进行md5加密处理
            2. 根据页面提交的用户名username查询数据库
            3. 如果没有查询到则返回登录失败结果
            4. 密码比对，如果不一致则返回登录失败结果
            5. 查看员工状态，如果为已禁用状态，则返回员工已禁用结果
            6. 登录成功，将员工id存入session并返回登录成功结果
         */

        // 1. 将页面提交的密码password进行md5加密处理
        String password = employee.getPassword();
        // DigestUtils工具类
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        // 2. 根据页面提交的用户名username查询数据库
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Employee::getUsername, employee.getUsername());
        // 数据库中对username做了唯一性约束，所以可以使用getOne
        Employee emp = employeeService.getOne(queryWrapper);

        // 3. 如果没有查询到则返回登录失败结果
        if(emp == null){
            return R.error("登录失败");
        }

        // 4. 密码比对，如果不一致则返回登录失败结果
        if(!emp.getPassword().equals(password)){
            return R.error("登录失败");
        }

        // 5. 查看员工状态，如果为已禁用状态，则返回员工已禁用结果
        if(emp.getStatus() == 0){
            return R.error("账号已禁用");
        }

        // 6. 登录成功，将员工id存入session并返回登录成功结果
        HttpSession session = request.getSession();
        session.setAttribute("employee", emp.getId());
        // 若登录成功，浏览器端/backend/page/login/login.html会将emp以userInfo的key-value形式缓存
        return R.success(emp);
    }

    /**
     * 员工退出
     * 用户点击页面中退出按钮，发送请求，请求地址为/employee/logout，请求方式为POST
     */
    @PostMapping("/logout")
    public R<String> logout(HttpServletRequest request){
        /*
            员工退出登录，处理逻辑
            1. 清理Session中存放的employee信息
         */
        request.getSession().removeAttribute("employee");
        // 若用户退出成功，浏览器端/backend/index.html会将userInfo缓存清理
        return R.success("退出成功");
    }


    /**
     * 新增员工
     * @param employee
     * @return
     */
    @PostMapping()
    public R<String> save(HttpServletRequest request, @RequestBody Employee employee){
        // @RequestBody注解用于接收前端发过来的json数据
        /*
        1. 页面发送ajax请求，将新增员工页面中输入的数据以json的形式提交到服务器
        2. 服务器Controller接收页面提交的数据
        3. Service调用Mapper操作数据库，保存数据
        */

        log.info("员工信息:{}", employee.toString());
        // 由于前端用于新建用户信息的form没有密码，在这里给每一个新建的用户一个初始密码
        // 密码需要用md5加密
        employee.setPassword(DigestUtils.md5DigestAsHex("123456".getBytes()));
        // status虽然没有在新建时要求输入，但是数据库中有默认值1
        // 其他没有默认值的属性就需要默认值
        // 多个数据库公共字段，mybatisplus公共字段自动填充!!!
        // 公共字段自动填充步骤：
        // 1. 在实体类属性上加入@TableField注解，指定自动填充的策略
        // 2. 按照框架要求编写元数据对象处理器，在此类中统一为公共字段赋值，此类需要实现MetaObjectHandler接口
//        employee.setCreateTime(LocalDateTime.now());
//        employee.setUpdateTime(LocalDateTime.now());
        // 当前登录用户id
//        Long empId = (Long) request.getSession().getAttribute("employee");
//        employee.setCreateUser(empId);
//        employee.setUpdateUser(empId);

        /*
            employeeService.save(employee)时，由于username字段的唯一性约束，可能会报异常，数据添加不成功
            此时需要我们进行异常捕获，通常有两种处理方式
            1. 在Controller方法中加入try、catch进行异常捕获
            2. 使用异常处理器进行全局捕获（本项目采用的方法）
         */
        employeeService.save(employee);

        return R.success("新增员工成功");
    }

    /**
     *
     * @param page
     * @param pageSize
     * @param name
     * @return
     */

    @GetMapping("/page")
    public R<Page> page(int page, int pageSize, String name){
        // Page为MP中的类，与/backend/page/member/list.html中返回的数据 所拥有的属性相配合
        // page,pageSize为前端提交过来的参数，name为查询功能时才有的key

         /*
         员工信息分页查询 执行过程
         1. 页面发送ajax请求，将分页查询参数（page、pageSize、name）提交到服务端
         2. 服务端Controller接收页面提交的数据并调用Service查询数据
         3. Service调用Mapper操作数据库，查询分页数据
         4. Controller将查询到的分页数据响应给页面
         5. 页面接收到分页数据并通过ElementUI的Table组件展示到页面上

         以下几种情况会执行分页查询
         1. 页面初始化时
         2. 用户点击查询按钮时
         3. 在page栏切换页面时
         */

        log.info("page={}, pageSize={}, name={}", page, pageSize, name);

        // 构造分页构造器
        Page pageInfo = new Page(page, pageSize);
        // 构造条件构造器
        LambdaQueryWrapper<Employee> queryWrapper = new LambdaQueryWrapper<>();
        // 添加过滤条件
        queryWrapper.like(StringUtils.isNotBlank(name), Employee::getName, name);
        // 添加排序条件
        queryWrapper.orderByDesc(Employee::getUpdateTime);
        // 执行查询，方法内部会将返回值封装到page
        employeeService.page(pageInfo, queryWrapper);

        return R.success(pageInfo);
    }

    /*
    禁用/启用员工账号：
        在员工管理列表页面，可以对某个员工账号进行启用或禁用操作。禁用的员工不能登录系统，启用后的员工可以正常登录。
        需要注意，只有管理员（admin用户）可以对其他普通用户进行启用、禁用操作，所以普通用户登录系统后启用禁用按钮不显示。（前端页面已实现 /backend/page/member/list.html）
        管理员登录系统可以对所有员工账号进行禁用、启用操作，如果某个员工账号状态为正常，则按钮显示为"禁用"，如果员工账号状态为已禁用，则按钮显示为"启用"
     */
    // update:禁用/启用员工账号;编辑员工信息
    @PutMapping
    public R<String> update(HttpServletRequest request, @RequestBody Employee employee){
        // 传过来的数据是json格式，需要@RequestBody
        log.info(employee.toString());

        long id = Thread.currentThread().getId();
        log.info("线程id为{}", id);
        // 需要将更新时间和更新者 传入
//        employee.setUpdateTime(LocalDateTime.now());
//        employee.setUpdateUser((Long) request.getSession().getAttribute("employee"));
        /*
        禁用/启用员工账号：
            通过观察，控制台输出的SQL发现页面传递过来的员工id的值和数据库中的id值不一致，导致无法更改员工的status属性
            分页查询时服务端响应给页面的数据中id的值是19位数字，类型为long，页面中js处理long型数字只能精确到前16位，所以最终通过ajax请求提交给服务端的id不准确
            如何解决？服务器给页面响应json数据时，将long型数据统一转为String字符串
            具体实现步骤：
            1. 提供对象转换器JacksonObjectMapper，基于Jackson进行java对象到json数据的转换
            2. 在WebMvcConfig配置类中扩展Spring mvc的消息转换器，在此消息转换器中使用提供的对象转换器进行Java对象到json数据的转换

         编辑员工信息执行流程：
            1. 点击编辑按钮时，页面跳转到add.html，并在url中携带参数（员工id）
            2. 在add.html页面获取url中的参数（员工id）
            3. 发送ajax请求，请求服务端，同时提交员工id参数
            4. 服务端接收请求，根据员工id查询员工信息，将员工信息以json形式响应给页面
            5. 页面接收服务端响应的json数据，通过VUE的数据绑定进行员工的信息回显
            6. 点击保存按钮，发送ajax请求，将页面中的员工信息以json方式提交到服务端
            7. 服务端接收员工信息，并进行处理，完成后给页面响应
            8. 页面接收到服务端响应信息后进行相应处理
         */


        employeeService.updateById(employee);

        return R.success("员工信息修改成功");
    }

    @GetMapping("/{id}")
    public R<Employee> getById(@PathVariable Long id){
        log.info("根据id查询员工信息");
        Employee employee = employeeService.getById(id);
        if(employee != null){
            return R.success(employee);
        }
        return R.error("没有查询到员工信息");
    }

}
