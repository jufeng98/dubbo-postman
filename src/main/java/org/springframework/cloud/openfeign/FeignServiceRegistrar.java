package org.springframework.cloud.openfeign;

import com.rpcpostman.service.GAV;
import com.rpcpostman.service.load.classloader.ApiJarClassLoader;
import com.rpcpostman.util.SpringUtil;
import lombok.SneakyThrows;
import org.springframework.context.support.GenericApplicationContext;

/**
 * @author yudong
 * @date 2022/11/12
 */
public class FeignServiceRegistrar {

    @SneakyThrows
    public static GenericApplicationContext register(GAV gav, ApiJarClassLoader apiJarClassLoader) {
        FeignClientsRegistrar registrar = new FeignClientsRegistrar();
        registrar.setResourceLoader(new ApiJarResourceLoader(apiJarClassLoader));
        registrar.setEnvironment(SpringUtil.getContext().getEnvironment());
        GenericApplicationContext context = new GenericApplicationContext();
        context.setParent(SpringUtil.getContext());
        context.setClassLoader(apiJarClassLoader);
        FeignContext feignContext = new FeignContext();
        feignContext.setApplicationContext(context);
        context.getBeanFactory().registerSingleton("feignContext", feignContext);
        context.getBeanFactory().registerSingleton("feignClientProperties", new FeignClientProperties());
        context.getBeanFactory().registerSingleton("feignClient", new FeignClientDefault());
        context.getBeanFactory().registerSingleton("feignTargeter", new HystrixTargeter());
        registrar.registerBeanDefinitions(
                new MockAnnotationMetadata(FeignServiceRegistrar.class, gav.getGroupID()),
                context
        );
        context.refresh();
        return context;
    }

}
