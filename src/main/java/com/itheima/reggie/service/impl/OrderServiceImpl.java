package com.itheima.reggie.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.itheima.reggie.common.BaseContext;
import com.itheima.reggie.common.CustomException;
import com.itheima.reggie.entity.*;
import com.itheima.reggie.mapper.OrderMapper;
import com.itheima.reggie.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Orders> implements OrderService {
    @Autowired
    private ShoppingCartService shoppingCartService;

    @Autowired
    private UserService userService;

    @Autowired
    private AddressBookService addressBookService;

    @Autowired
    private OrderDetailService orderDetailService;
    /**
     * 用户下单
     * @param orders
     */
    @Transactional
    @Override
    public void submit(Orders orders) {
        // 前端只提交了三项，addressBookId\payMethod\remark 没有传购物车数据、具体地址等信息，因为可以根据用户id查的到
        // 1. 获取当前用户id
        Long userId = BaseContext.getCurrentId();


        // 2. 查询当前用户的购物车数据
        LambdaQueryWrapper<ShoppingCart> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ShoppingCart::getUserId, userId);
        List<ShoppingCart> shoppingCarts = shoppingCartService.list(wrapper);

        if(shoppingCarts == null || shoppingCarts.size() == 0){
            throw new CustomException("购物车为空，不能下单");
        }


        // 3. 向订单表orders插入数据 （1条数据）
        // 3.1 查询用户数据
        User user = userService.getById(userId);
        // 3.2 查询地址数据
        Long addressBookId = orders.getAddressBookId();
        AddressBook addressBook = addressBookService.getById(addressBookId);
        if(addressBook == null){
            throw new CustomException("用户地址信息有误，不能下单");
        }

        // 3.3 向订单表插入数据
        long orderId = IdWorker.getId(); // 订单号
        AtomicInteger amount = new AtomicInteger(0); // 金额,原子操作

        // 设置订单号number

        orders.setNumber(String.valueOf(orderId));
        // 设置订单时间
        orders.setOrderTime(LocalDateTime.now());
        // 设置订单付款时间
        orders.setCheckoutTime(LocalDateTime.now());
        // 设置订单状态 2——待派送
        orders.setStatus(2);
        // 设置订单金额（遍历购物车，计算总金额）
        List<OrderDetail> orderDetails = shoppingCarts.stream().map((item) -> {
            // 遍历过程中，1. 计算总金额 2. 计算orderDetails
            OrderDetail orderDetail = new OrderDetail();
            orderDetail.setOrderId(orderId);
            orderDetail.setNumber(item.getNumber());
            orderDetail.setDishFlavor(item.getDishFlavor());
            orderDetail.setDishId(item.getDishId());
            orderDetail.setSetmealId(item.getSetmealId());
            orderDetail.setName(item.getName());
            orderDetail.setImage(item.getImage());
            orderDetail.setAmount(item.getAmount());
            // amount累加
            amount.addAndGet(item.getAmount().multiply(new BigDecimal(item.getNumber())).intValue());
            return orderDetail;
        }).collect(Collectors.toList());

        orders.setAmount(new BigDecimal(amount.get()));
        // 设置用户id
        orders.setUserId(userId);
        // 设置用户名
        orders.setUserName(user.getName());
        // 设置收货人
        orders.setConsignee(addressBook.getConsignee());
        // 设置联系电话
        orders.setPhone(addressBook.getPhone());
        // 设置地址
        orders.setAddress((addressBook.getProvinceName() == null ? "" : addressBook.getProvinceName())
                + (addressBook.getCityName() == null ? "" : addressBook.getCityName())
                + ((addressBook.getDistrictName() == null) ? "" : addressBook.getDistrictName())
                + ((addressBook.getDetail() == null) ? "" : addressBook.getDetail()));

        this.save(orders);


        // 4. 向订单明细表order_detail插入数据（有多少菜 插入多少数据）
        orderDetailService.saveBatch(orderDetails);


        // 5. 清空购物车shopping_cart数据
        shoppingCartService.remove(wrapper);
    }
}
