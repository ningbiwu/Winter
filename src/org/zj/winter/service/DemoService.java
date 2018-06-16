package org.zj.winter.service;

import org.zj.winter.annotation.Autofired;
import org.zj.winter.annotation.Service;

/**
 * Created by ZhangJun on 2018/6/16.
 */
@Service
public class DemoService implements IDemoService {

    public DemoService(){
        System.out.println(" 人家被初始化了哦 我是demoservice-------------右边是我儿子--"+childService+"----------");
    }

    @Autofired
    ChildService childService;
    @Override
    public String getInfo(String name) {
        childService.hh();
        return "你好啊"+name;
    }
}
