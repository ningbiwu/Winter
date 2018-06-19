package org.zj.winter.core;

import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Created by ZhangJun on 2018/6/18.
 */
public class CGlibMethodInterceptor implements MethodInterceptor {
    List<Invoke> before;
    List<Invoke> after;
    public CGlibMethodInterceptor(List<Invoke> before,List<Invoke> after){
        this.before=before;
        this.after=after;
    }

    public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {

        if(before!=null){
            for(Invoke invo:before){
                invo.getMethod().invoke(invo.getObject());
            }
        }
        methodProxy.invokeSuper(o,objects);
        if(after!=null){
            for(Invoke invo:after){
                invo.getMethod().invoke(invo.getObject());
            }
        }
        return o;
    }
}
