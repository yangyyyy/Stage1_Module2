package com.lagou.edu.factory;

import com.alibaba.druid.util.StringUtils;
import com.lagou.edu.annotation.MyAutowired;
import com.lagou.edu.annotation.MyComponent;
import com.lagou.edu.annotation.MyService;
import com.lagou.edu.annotation.MyTransactional;
import com.lagou.edu.utils.ClassUtil;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MyBeanFactory {

    private static ConcurrentHashMap<String,Object> beans = new ConcurrentHashMap<>();

    static{
        //使用java反射机制扫包，获取当前包下所有类
        List<Class<?>> classes = ClassUtil.getClasses("com.lagou.edu");

        try {
            //将包含自定义注解的类存入map
            findAnnotations(classes);
            //自动把值注入filed
            initEntryField();
            //事务处理
            initTransaction();

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取Bean
     */
    public static Object getBean (String beanId) throws Exception {
        if (StringUtils.isEmpty(beanId)){
            throw new Exception("bean Id 不能为空");
        }

        Object o = beans.get(beanId);
        return o;
    }

    /**
     * 通过反射解析对象
     */
    private static Object newInstance(Class<?> classInfo) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        return classInfo.newInstance();
    }

    /**
     * 参数：通过工具类扫描的改包下所有的类信息
     * 返回值：返回一个map集合
     * */
    public static ConcurrentHashMap<String,Object> findAnnotations (  List<Class<?>> classes) throws ClassNotFoundException, InstantiationException, IllegalAccessException {
        for (Class<?> classInfo:classes) {
            //判断类上是否有自定义ExtService注解
            MyService myService = classInfo.getAnnotation(MyService.class);
            MyComponent myComponent = classInfo.getAnnotation(MyComponent.class);

            if(myService != null){
                Class<?>[] interfaces = classInfo.getInterfaces();
                for (Class<?> anInterface : interfaces) {
                    String simpleName = anInterface.getSimpleName();
                    //获取当前类名
                    String beanID =toLowerCaseFirstOne(simpleName);
                    //如果当前类上有注解，将该类的信息，添加到map集合
                    if(!StringUtils.isEmpty(myService.value())){
                        beanID = myService.value();
                    }
                    beans.put(beanID,newInstance(classInfo));
                }
            }else if (myComponent != null){
                //beans(类名小写,classInfo)
                String className = classInfo.getSimpleName();
                //将类名首字母变为小写
                String beanID = toLowerCaseFirstOne(className);
                //如果当前类上有Service注解，将该类的信息，添加到map集合
                beans.put(beanID,newInstance(classInfo));
            }
        }
        return beans;
    }

    /**
     * 首字母转小写
     */
    public static String toLowerCaseFirstOne(String s) {
        if (Character.isLowerCase(s.charAt(0)))
            return s;
        else
            return (new StringBuilder()).append(Character.toLowerCase(s.charAt(0))).append(s.substring(1)).toString();
    }

    /**
     * 初始化属性
      */
    private static void initEntryField() throws Exception {
        // 1.遍历所有的bean容器对象
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            // 2.判断属性上面是否有加注解 自动注入
            Object bean = entry.getValue();
            attriAssign(bean);
        }
    }

    /**
     *依赖注入注解
     */
    public static void attriAssign(Object object) throws Exception {

        // 1.使用反射机制,获取当前类的所有属性
        Class<? extends Object> classInfo = object.getClass();
        Field[] declaredFields = classInfo.getDeclaredFields();

        // 2.判断当前类属性是否存在注解
        for (Field field : declaredFields) {
            MyAutowired autowired = field.getAnnotation(MyAutowired.class);
            if (autowired != null) {
                // 获取属性名称
                String beanId = field.getName();
                for (Map.Entry<String, Object> entry : beans.entrySet()) {
                    Class<?>[] interfaces = entry.getValue().getClass().getInterfaces();
                    for (Class<?> anInterface : interfaces) {
                        if(anInterface == field.getType()){
                            beanId = entry.getKey();
                        }
                    }
                }

                Object bean = getBean(beanId);
                if (bean != null) {
                    // 3.默认使用属性名称，查找bean容器对象 1参数 当前对象 2参数给属性赋值
                    field.setAccessible(true); // 允许访问私有属性
                    field.set(object, bean);
                }
                String className = classInfo.getSimpleName();
                //将类名首字母变为小写
                String beanID = toLowerCaseFirstOne(className);
                beans.put(beanID, object);
            }
        }

    }

    /**
     *
     */
    private static void initTransaction() {
        // 1.遍历所有的bean容器对象
        for (Map.Entry<String, Object> entry : beans.entrySet()) {
            // 2.判断属性上面是否有加注解 自动注入
            Object bean = entry.getValue();
            transation(bean);
        }
    }

    private static void transation(Object bean) {
        // 1.使用反射机制,获取当前类的所有属性
        Class<? extends Object> classInfo = bean.getClass();
        MyTransactional transactional = classInfo.getAnnotation(MyTransactional.class);
        Class<?>[] interfaces = classInfo.getInterfaces();
        ProxyFactory proxyFactory = (ProxyFactory)beans.get("proxyFactory");
        Object proxy = null;

        if(interfaces != null && interfaces.length > 0){

            proxy = proxyFactory.getJdkProxy(interfaces[0],bean);
        }
        if(interfaces != null || interfaces.length == 0){
            proxy = proxyFactory.getCglibProxy(interfaces[0]); //TODO
        }

        if (transactional != null) {
            String name = classInfo.getSimpleName();
            String beanID = toLowerCaseFirstOne(name);
            beans.put(beanID,proxy);
        }

    }

}
