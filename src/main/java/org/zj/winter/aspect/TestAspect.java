package org.zj.winter.aspect;

import org.zj.winter.annotation.After;
import org.zj.winter.annotation.Aspect;
import org.zj.winter.annotation.Befoer;
import org.zj.winter.annotation.Condition;

/**
 * Created by ZhangJun on 2018/6/18.
 */

/**
 * 扫描带有Aspect注解的类
 * 获得condition
 * 获得所有加了after/before注解的方法
 * 保存到容器里
 * Map<String,Invoke>map
 */
@Aspect
@Condition("")
public class TestAspect {
    @After
    public void after(){
        System.out.println("之后");
    }
    @Befoer
    public void before(){
        System.out.println("之前");
    }
}
