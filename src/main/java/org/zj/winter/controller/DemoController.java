package org.zj.winter.controller;

import org.zj.winter.annotation.Autofired;
import org.zj.winter.annotation.Controller;
import org.zj.winter.annotation.RequestMapping;
import org.zj.winter.service.ChildService;
import org.zj.winter.service.IDemoService;

import java.io.IOException;

/**
 * Created by ZhangJun on 2018/6/16.
 */
@Controller
@RequestMapping("/demo")
public class DemoController {

    @Autofired
    public IDemoService demoService;

    @Autofired
    public ChildService childService;
    @RequestMapping("/test")
    public void test() throws IOException {

        childService.hh();

        demoService.print();

        //demoService.getInfo("你懂得啊");
        //response.getWriter().write(demoService.getInfo(name));
    }

    @RequestMapping("/ss")
    public String index(){
        System.out.println("主页");
        return "index";
    }

}
