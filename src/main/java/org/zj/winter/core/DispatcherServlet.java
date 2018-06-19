package org.zj.winter.core;


import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.Enhancer;
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
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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
    Map<String, Invoke> invokeMap = new HashMap<>();
    Map<String, Map.Entry<List<Invoke>, List<Invoke>>> aspects = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // super.doGet(req, resp);
        handleMapping(req, resp);
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

        //初始化切面容器
        initAspect();

        //将容器内的类进行实例化
        getInstance();//这里没走完

        /*for(Map.Entry<String,Object> entry:instanceMap.entrySet()){
            System.out.println(entry.getValue()+"         <->   这是从instanceMap中获得的Object");
        }*/

        //对使用了注解的对象进行注入
        doDI();
        //扫描controller,把路径和方法/类的映射关系放到容器
        setMapping();


    }

    private void initAspect() {
        try {
            for (Class<?> c : classes) {
                String condition = null;

                System.out.println(c.getName()+"  ------>    name");

                final List<Invoke> after = new ArrayList<>();
                final List<Invoke> before = new ArrayList<>();

                if (c.isAnnotationPresent(Aspect.class)) {

                    System.out.println(c.getName()+"  ,.,.,.,.   isAnnotationPresent"+c.isAnnotationPresent(Condition.class));

                    if (c.isAnnotationPresent(Condition.class)) {
                        Condition condition1 =  c.getAnnotation(Condition.class);
                        condition = condition1.value();
                    }
                    for (Method method : c.getDeclaredMethods()) {
                        if (method.isAnnotationPresent(After.class)) {
                            after.add(new Invoke(c.newInstance(), method));
                        }

                        if (method.isAnnotationPresent(Before.class)) {
                            before.add(new Invoke(c.newInstance(), method));
                        }
                    }


                    System.out.println("           >>>放进去     aspects   "+c.getName()+"<       >"+condition+"        <-->        >条件");

                    aspects.put(condition, new Map.Entry<List<Invoke>, List<Invoke>>() {
                        @Override
                        public List<Invoke> getKey() {
                            return before;
                        }

                        @Override
                        public List<Invoke> getValue() {
                            return after;
                        }

                        @Override
                        public List<Invoke> setValue(List<Invoke> value) {
                            return after;
                        }
                    });


                }


            }
        } catch (Exception e) {
            e.printStackTrace();
        }



    }

    /**
     * 扫描controller，设置url和方法的映射关系用于调用
     */
    private void setMapping() {
        //扫描所有controller
        for (Map.Entry<String, Object> entry : instanceMap.entrySet()) {
            if (entry.getValue().getClass().isAnnotationPresent(Controller.class)) {
                //将类上的requestMapping拿过来
                RequestMapping annotation = entry.getValue().getClass().getAnnotation(RequestMapping.class);
                String baseRequestMapping = annotation.value();
                for (Method method : entry.getValue().getClass().getMethods()) {

                    System.out.println(method.getName() + "  controller 上面的方法名");

                    RequestMapping annotation1 = method.getAnnotation(RequestMapping.class);
                    if (annotation1 == null)
                        return;
                    String value = annotation1.value();
                    invokeMap.put(baseRequestMapping + value, new Invoke(entry.getValue(), method));
                }
            }
        }
    }

    /**
     * 对容器里的class进行注入
     */
    private void doDI() {

        System.out.println(instanceMap.size() + "---------instance map size");

        for (Map.Entry<String, Object> entry : instanceMap.entrySet()) {
            for (Field field : entry.getValue().getClass().getDeclaredFields()) {
                //DI(entry.getValue().getClass(),field);
                System.out.println(field.getName() + "     当前字段名" + field.isAnnotationPresent(Autofired.class) + "        -----");
                if (field.isAnnotationPresent(Autofired.class)) {
                    try {
                        System.out.println("为   " + entry.getKey() + "          -------------注入" + field.getName());
                        field.setAccessible(true);
                        //-------------------------------------------------------这里一定要注意哦这里是得到 字段的类型的Class类全名----
                        System.out.println(field.getType().getName() + "       这是从字段得到的类全名");
                        for (String key : instanceMap.keySet()) {
                            System.out.println("      <->      key" + key);
                        }
                        //System.out.println(instanceMap.get(field.getType().getName()) + "    从instancemap拿到的");
                        field.set(entry.getValue(), instanceMap.get(field.getType().getName()));
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }

            }
        }
    }

    /**
     * 对指定类进行递归注入
     *
     * @param parentObj
     * @param field
     */
    private void DI(Class parentObj, Field field) {

        field.setAccessible(true);
        //System.out.println(field.getName()+"-             -"+field.isAnnotationPresent(Autofired.class)+"        -------字 段名字");

        //注入
        try {
            if (field.isAnnotationPresent(Autofired.class)) {
                for (Field f : field.getClass().getDeclaredFields()) {
                    System.out.println("先进来" + f.getName());
                    DI(f.getClass(), f);
                }
                System.out.println("注入了一个" + field.getName());
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


    /**
     * 对class进行初始化
     */
    private void getInstance() {
        for (Class c : classes) {
            try {
                System.out.println(c.getName()+"           >class name");


                //如果当前类名被aspect的key包含,那就动态代理初始化对象
                if(containKey(c.getName(),aspects)){

                    System.out.println("         <在aspects中有这个s"+c.getName());

                    Map.Entry<List<Invoke>, List<Invoke>> listListEntry =getInvokeFromAspect(c.getName());


                    System.out.println(c.getInterfaces().length+"    <->      "+c.getName()+"     这是接口的长度");

                    //如果有接口,就用jdk动态代理
                    if(c.getInterfaces().length!=0&&listListEntry!=null){

                        JdkInvocationHandler jdkInvocationHandler=new JdkInvocationHandler(listListEntry.getKey(),listListEntry.getValue(),c.newInstance());

                        Object o = Proxy.newProxyInstance(jdkInvocationHandler.getClass().getClassLoader(), c.getInterfaces(), jdkInvocationHandler);
                        instanceMap.put(c.getName(),o);
                        System.out.println("            >使用jdk动态代理");
                        for(Class inte:c.getInterfaces()){
                            instanceMap.put(inte.getName(),o);
                        }
                        continue;
                    }
                    //不然就用cglib

                    System.out.println("               >使用cglib");
                    if(listListEntry!=null) {
                        Enhancer enhancer = new Enhancer();
                        enhancer.setCallbacks(new Callback[]{new CGlibMethodInterceptor(listListEntry.getKey(), listListEntry.getValue())});
                        enhancer.setSuperclass(c);
                        Object o = enhancer.create();
                        instanceMap.put(c.getName(), o);
                        continue;
                    }
                }

                //不然就直接newinstance
                instanceMap.put(c.getName(), c.newInstance());

                System.out.println(c.getName() + " 名字放进去了" + c.newInstance());

                Class[] interfaces = c.getInterfaces();
                for (Class cc : interfaces) {
                    instanceMap.put(cc.getName(), c.newInstance());
                }

            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private Map.Entry<List<Invoke>,List<Invoke>> getInvokeFromAspect(String name) {
        for(Map.Entry<String,Map.Entry<List<Invoke>,List<Invoke>>> entry:aspects.entrySet()){
            if(name.contains(entry.getKey())){
                return entry.getValue();
            }
        }
        return null;
    }

    private Object setInstance(Object object, Class<?> c) {
        for (Field f : c.getDeclaredFields()) {
            if (f.isAnnotationPresent(Autofired.class)) {
                try {
                    try {
                        f.set(object, f.getClass().newInstance());
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


    /**
     * 扫描所有包放到容器里
     */
    private void getClazz() {
        for (String pack : packages) {
            String replace = pack.replace(".", "/");
            File file = new File(getSrcPath() + "\\" + replace);

            System.out.println(file.getAbsolutePath() + "    这里也是路径哦");

            inflateClazz(classes, file);
        }
    }

    /**
     * 将类通过反射得到引用，然后放到容器
     *
     * @param classes
     * @param file
     */
    private void inflateClazz(List<Class<?>> classes, File file) {
        System.out.println(file.getAbsolutePath() + "路径");

        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                inflateClazz(classes, f);
            }
        }


        if (file.isFile()) {

            String name = file.getAbsolutePath();
            name = name.replace("\\", "/");
            name = name.replace(getSrcPath(), "");
            System.out.println(getSrcPath() + "-    替换掉项目名后-" + name);
            name = name.replace("/", ".");
            name = name.substring(1);
            System.out.println(name + "     这是类名哦");
            try {
                Class clazz = Class.forName(name.substring(0, name.lastIndexOf(".java")));
                if (!clazz.isInterface()) {
                    classes.add(clazz);
                }
                System.out.println("这放进去了" + classes.size());
            } catch (ClassNotFoundException e) {
                return;
            }
        }
    }

    /**
     * 扫描配置的包
     *
     * @throws URISyntaxException
     */
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
        System.out.println(packages.size()+"  包的大小         >"+packages.size());
    }

    /**
     * 根据URL调用对应方法
     *
     * @param req
     * @param resp
     */
    private void handleMapping(HttpServletRequest req, HttpServletResponse resp) {
        //去存储了url和方法映射的容器里找方法并反射调用
        String requestURI = req.getRequestURI();


        System.out.println(invokeMap.get(requestURI));

        if (invokeMap.get(requestURI) != null) {
            try {

                for(String string:invokeMap.keySet()){
                    System.out.println(string+"    key啦");
                }

                Object invoke = invokeMap.get(requestURI).getMethod().invoke(invokeMap.get(requestURI).getObject());
                if (invoke instanceof String) {

                    System.out.println("返回了String");
                    try {
                        req.getRequestDispatcher("/" + invoke + ".jsp").forward(req, resp);
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

    /**
     * 获得源码的绝对路径
     *
     * @return
     */
    private String getSrcPath() {
        String path = null;
        try {
            path = Thread.currentThread().getContextClassLoader().getResource("").toURI().getPath();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        path = path.substring(1, path.indexOf("/out/artifact")) + "/src/main/java";
        return path;
    }

    private boolean containKey(String key,Map<String,Map.Entry<List<Invoke>,List<Invoke>>> map){
        if(key==null){
            return false;
        }
        System.out.println("                >"+key+"     传递过来的类名");

        for(String k:map.keySet()){

            System.out.println("            >>aspectmap中的key"+k+"");

            if(key.contains(k)){
                return true;
            }
        }
        return false;/*
        return key!=null&&key.contains("org.zj.winter.service");*/
    }

}
