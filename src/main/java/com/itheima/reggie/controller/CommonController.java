package com.itheima.reggie.controller;

import com.itheima.reggie.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.UUID;

/**
 * 文件上传和下载
 */
@Slf4j
@RestController
@RequestMapping("/common")
public class CommonController {
    // 导入application.yml配置文件中自定义的文件上传根目录
    @Value("${reggie.path}")
    private String basePath;
    /**
     * 对应的前端页面为/backend/page/demo/upload.html
     * 文件上传 三个必要条件 1. post 2. enctype="multipart/form-data 3. type=file
     * 虽然前端是一个文件上传的框框，但是底层还是用form表单的提交方式
     * @return
     */
    @PostMapping("/upload")
    public R<String> upload(MultipartFile file){
        // MultipartFile类型的参数名不能随意，必须与表单提交type=file标签的name属性一致，此处为file
        // file为一个存在c盘的临时文件，需要转存到指定位置，否则本次请求结束后临时文件会删除
        log.info(file.toString());

        // 获取原先的文件名以及文件后缀
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));

        // 使用UUID重新生成文件名，防止文件名称重复造成文件覆盖
        String fileName = UUID.randomUUID().toString() + suffix;

        // 创建目录对象
        File dir = new File(basePath);
        // 转存之前判断当前根目录是否存在
        if(!dir.exists()){
            // 目录不存在，创建一个目录
            dir.mkdirs();
        }

        try {
            // 转存临时文件,路径为根目录+UUID随机生成文件名
            file.transferTo(new File(basePath + fileName));
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 返回图片的文件名称，方便数据库存储
        return R.success(fileName);
    }

    /**
     * 文件下载
     * @param name
     * @param response
     */
    @GetMapping("/download")
    public void download(String name, HttpServletResponse response){
        try {
            // 输入流，通过输入流读取文件内容
            FileInputStream fileInputStream = new FileInputStream(new File(basePath + name));

            // 输出流，通过输出流将文件写会浏览器，在浏览器展示图片
            ServletOutputStream outputStream = response.getOutputStream();
            response.setContentType("image/jpeg");

            int len = 0;
            byte[] bytes = new byte[1024];
            while( (len = fileInputStream.read(bytes)) != -1){
                outputStream.write(bytes,0, len);
                // 刷新
                outputStream.flush();
            }

            // 关闭资源
            fileInputStream.close();
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }
}