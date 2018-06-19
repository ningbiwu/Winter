package org.zj.winter.core;

import java.lang.reflect.Method;

/**
 * Created by ZhangJun on 2018/6/18.
 */
class Invoke{
    Object object;
    Method method;

    public Invoke() {
    }

    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        this.object = object;
    }

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Invoke(Object object, Method method) {
        this.object = object;
        this.method = method;
    }

    @Override
    public String toString() {
        return "Invoke{" +
                "object=" + object +
                ", method=" + method +
                '}';
    }
}
