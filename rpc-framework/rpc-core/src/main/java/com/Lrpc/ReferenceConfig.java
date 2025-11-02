package com.Lrpc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Currency;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReferenceConfig<T> {
    private Class<T> interfaceClass;

    public T get() {
        //此处使用动态代理实现代理对象
        //ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        ClassLoader classLoader = sayHello.class.getClassLoader();//类加载器，可以用要代理的接口的类加载器/也可用当前线程的类加载器,如上行代码所示（还可以解决当要代理的接口不止一个时要怎么选择类加载器的问题）
        Class[] classes = new Class[]{interfaceClass};//一个装有要代理的类的class数组

        Object ret = Proxy.newProxyInstance(classLoader,
                classes,
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        System.out.println("hello proxy");
                        return null;
                    }
                });
        return (T) ret;
    }
}
