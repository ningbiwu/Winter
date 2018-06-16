package org.zj.winter.core;

import com.sun.xml.internal.ws.api.model.wsdl.WSDLBoundOperation;
import javafx.beans.value.ObservableObjectValue;
import org.omg.CORBA.ARG_OUT;
import org.zj.winter.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by ZhangJun on 2018/6/16.
 */
public class DispatcherServlet extends HttpServlet {
    List<String> packages = new ArrayList<>();
    List<Class<?>> classes = new ArrayList<>();
    Map<String, Object> instanceMap = new HashMap<>();
    Map<String,Invoke> invokeMap=new HashMap<>();
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


    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
       // super.doGet(req, resp);
        handleMapping(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //super.doPost(req, resp);
        handleMapping(req, resp);
    }


    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            initWinter();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    private void initWinter() throws URISyntaxException {
        //获得需要扫描的包 默认扫描当前包下
        getScanPackage();
        //将包内的类放到容器内
        getClazz();
        //将容器内的类进行实例化
        getInstance();
        //对使用了注解的对象进行注入
        doDI();
        //扫描controller,把路径和方法/类的映射关系放到容器
        setMapping();
    }

    private void setMapping() {
        //扫描所有controller
        for(Map.Entry<String,Object> entry:instanceMap.entrySet()){
            if(entry.getValue().getClass().isAnnotationPresent(Controller.class)){
                //将类上的requestMapping拿过来
                RequestMapping annotation = entry.getValue().getClass().getAnnotation(RequestMapping.class);
                String baseRequestMapping = annotation.value();
                for(Method method:entry.getValue().getClass().getMethods()){

                    System.out.println(method.getName()+"  controller 上面的方法名");

                    RequestMapping annotation1 = method.getAnnotation(RequestMapping.class);
                    if (annotation1==null)
                            return;
                    String value = annotation1.value();
                    invokeMap.put(baseRequestMapping+value,new Invoke(entry.getValue(),method));
                }
            }
        }
    }

    private void doDI() {

        System.out.println(instanceMap.size()+"---------instance map size");

        for (Map.Entry<String, Object> entry : instanceMap.entrySet()) {
            for (Field field : entry.getValue().getClass().getDeclaredFields()) {
                DI(entry.getValue(),field);
            }
        }
    }

    private void DI(Object parentObj,Field field) {

        field.setAccessible(true);
        //System.out.println(field.getName()+"-             -"+field.isAnnotationPresent(Autofired.class)+"        -------字 段名字");

        //注入
        try {
            if(field.isAnnotationPresent(Autofired.class)){
                for (Field f : field.getClass().getDeclaredFields()) {
                    System.out.println("先进来"+f.getName());
                    DI(f.getClass(),f);
                }
                System.out.println("注入了一个"+field.getName());
                try {
                    field.set(parentObj, field.getClass().newInstance());

                } catch (InstantiationException e) {
                    e.printStackTrace();
                }

            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }


    private void getInstance() {
        for (Class c : classes) {
            try {
                instanceMap.put(c.getName(), c.newInstance());

                System.out.println(c.getName()+" 名字放进去了");

                Class[] interfaces = c.getInterfaces();
                for(Class cc:interfaces){
                    instanceMap.put(cc.getName(),setInstance(cc.newInstance(),c));
                }

            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private Object setInstance(Object object,Class<?> c){
        for(Field f:c.getDeclaredFields()){
            if(f.isAnnotationPresent(Autofired.class)){
                try {
                    try {
                        f.set(object,f.getClass().newInstance());
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    }
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
        return object;
    }


    private void getClazz() {
        for (String pack : packages) {
            String replace = pack.replace(".", "/");
            File file = new File(getSrcPath() + "\\" + replace);

            System.out.println(file.getAbsolutePath() + "    这里也是路径哦");

            inflateClazz(classes, file);
        }
    }

    private void inflateClazz(List<Class<?>> classes, File file) {
        System.out.println(file.getAbsolutePath()+"路径");

        if(file.isDirectory()){
            for(File f:file.listFiles()){
                inflateClazz(classes,f);
            }
        }



        if(file.isFile()){

            String name = file.getAbsolutePath();
            name = name.replace("\\", "/");
            name = name.replace(getSrcPath(), "");
            System.out.println(getSrcPath() + "-    替换掉项目名后-" + name);
            name = name.replace("/", ".");
            name = name.substring(1);
            System.out.println(name + "     这是类名哦");
            try {
                Class clazz = Class.forName(name.substring(0,name.lastIndexOf(".java")));
                if(!clazz.isInterface()) {
                    classes.add(clazz);
                }System.out.println("这放进去了"+classes.size());
            } catch (ClassNotFoundException e) {
                return;
            }
        }
    }

    private void getScanPackage() throws URISyntaxException {
        //获得Config类,获得对应注解上面的值
        String path = getSrcPath();
        File file = new File(path);
        for (File f : file.listFiles()) {
            if (f.getName().endsWith(".java")) {
                try {
                    Class<?> aClass = Class.forName(f.getName().substring(0, f.getName().lastIndexOf(".")));
                    if (aClass.isAnnotationPresent(BasePackage.class)) {
                        BasePackage annotation = aClass.getAnnotation(BasePackage.class);
                        for (String str : annotation.value()) {

                            System.out.println("添加包名:      " + str);

                            packages.add(str);
                        }
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void handleMapping(HttpServletRequest req, HttpServletResponse resp) {
        //去存储了url和方法映射的容器里找方法并反射调用
        String requestURI = req.getRequestURI();


        System.out.println(invokeMap.get(requestURI));

        if(invokeMap.get(requestURI)!=null){
            try {
                Object invoke = invokeMap.get(requestURI).getMethod().invoke(invokeMap.get(requestURI).getObject());
                if(invoke instanceof String){

                    System.out.println("返回了String");
                    try {
                        req.getRequestDispatcher("/"+invoke+".jsp").forward(req,resp);
                    } catch (ServletException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }

        System.out.println(requestURI + "        --------请求路径");
    }

    private String getSrcPath() {
        String path = null;
        try {
            path = Thread.currentThread().getContextClassLoader().getResource("").toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        path = path.substring(1, path.indexOf("/out/artifact")) + "/src";
        return path;
    }
}
