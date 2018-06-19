package org.zj.winter.core;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by ZhangJun on 2018/6/18.
 */
public class JdkInvocationHandler implements InvocationHandler {
    List<Invoke > before;
    List<Invoke> after;
    Object c;

    public JdkInvocationHandler(List<Invoke> before,List<Invoke> after,Object c){
        this.before=before;
        this.after=after;
        this.c=c;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if(before!=null){
           for(Invoke invo:before){
               invo.getMethod().invoke(invo.getObject());
           }
        }
        //这里第一个参数必须是Object----------------------
        method.invoke(c,args);

        if(after!=null){
            for(Invoke invo:after){
                invo.getMethod().invoke(invo.getObject());
            }
        }
        return proxy;
    }
}
