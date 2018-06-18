package org.zj.winter.service;

import org.zj.winter.annotation.Service;

/**
 * Created by ZhangJun on 2018/6/16.
 */
@Service
public class ChildService {

    public ChildService(){
        System.out.println("人家被初始化了     我是childservice");
    }

    public void hh(){
        System.out.println("呵呵");
    }
}
