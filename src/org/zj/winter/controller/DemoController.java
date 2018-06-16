package org.zj.winter.controller;

import org.zj.winter.annotation.Autofired;
import org.zj.winter.annotation.Controller;
import org.zj.winter.annotation.RequestMapping;
import org.zj.winter.annotation.RequestParam;
import org.zj.winter.service.DemoService;
import org.zj.winter.service.IDemoService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by ZhangJun on 2018/6/16.
 */
@Controller
@RequestMapping("/demo")
public class DemoController {

    @Autofired
    public IDemoService demoService;
    @RequestMapping("/test")
    public void test() throws IOException {

        System.out.println(demoService+"        demoservice");

        System.out.println(demoService.getInfo("我是张君哦"));

        System.out.println("我走了");

        //response.getWriter().write(demoService.getInfo(name));
    }

    @RequestMapping("/ss")
    public String index(){
        System.out.println("主页");
        return "index";
    }

}
