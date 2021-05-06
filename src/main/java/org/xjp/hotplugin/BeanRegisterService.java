package org.xjp.hotplugin;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ResourceUtils;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.FileNotFoundException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Objects;

@Component
public class BeanRegisterService implements BeanDefinitionRegistryPostProcessor, ApplicationContextAware {

    private BeanDefinitionRegistry beanDefinitionRegistry;
    private ApplicationContext applicationContext;

    public BeanRegisterService() throws MalformedURLException, FileNotFoundException {
    }

    @Override
    public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry beanDefinitionRegistry) throws BeansException {
        this.beanDefinitionRegistry = beanDefinitionRegistry;
    }

    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory configurableListableBeanFactory) throws BeansException {

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }


    public void registerMapping() {
        try {
            RequestMappingHandlerMapping requestMappingHandlerMapping = (RequestMappingHandlerMapping) applicationContext.getBean("requestMappingHandlerMapping");
            if (requestMappingHandlerMapping != null) {
                String handler = "testController";
                Object controller = applicationContext.getBean(handler);
                if (controller == null) {
                    return;
                }
                Method method = requestMappingHandlerMapping.getClass().getSuperclass().getSuperclass().getDeclaredMethod("detectHandlerMethods", Object.class);
                method.setAccessible(true);
                method.invoke(requestMappingHandlerMapping, handler);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unregisterController(String controllerBeanName) {
        RequestMappingHandlerMapping requestMappingHandlerMapping = (RequestMappingHandlerMapping) applicationContext.getBean("requestMappingHandlerMapping");
        if (requestMappingHandlerMapping != null) {
            String handler = controllerBeanName;
            Object controller = applicationContext.getBean(handler);
            if (controller == null) {
                return;
            }
            final Class<?> targetClass = controller.getClass();
            ReflectionUtils.doWithMethods(targetClass, method -> {
                Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);
                try {
                    Method createMappingMethod = RequestMappingHandlerMapping.class.
                            getDeclaredMethod("getMappingForMethod", Method.class, Class.class);
                    createMappingMethod.setAccessible(true);
                    RequestMappingInfo requestMappingInfo = (RequestMappingInfo)
                            createMappingMethod.invoke(requestMappingHandlerMapping, specificMethod, targetClass);
                    if (requestMappingInfo != null) {
                        requestMappingHandlerMapping.unregisterMapping(requestMappingInfo);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }, ReflectionUtils.USER_DECLARED_METHODS);
        }

        beanDefinitionRegistry.removeBeanDefinition("testController");
        beanDefinitionRegistry.removeBeanDefinition("testService");
    }

    MyClassLoader classLoader = new MyClassLoader(ResourceUtils.getURL("classpath:static").getPath().replace("%20"," ").replace('/', '\\'),
            this.getClass().getClassLoader());

    public String registerBeanDefinition() {
        try {
            Class testServiceClass = classLoader.loadClass("org.xjp.mock.TestService");
            System.out.println(registerBeanDefinitionIfNotExists(beanDefinitionRegistry, "testService", testServiceClass));


            Class testControllerClass = classLoader.loadClass("org.xjp.mock.TestController");
            System.out.println(registerBeanDefinitionIfNotExists(beanDefinitionRegistry, "testController", testControllerClass));
            registerMapping();

        } catch (Exception e) {
            e.printStackTrace();
            return e.getMessage();
        }
        return "TestController register success!!";
    }

    public boolean registerBeanDefinitionIfNotExists(BeanDefinitionRegistry registry, String beanName, Class<?> beanClass) {
        return registerBeanDefinitionIfNotExists(registry, beanName, beanClass, null);
    }

    public boolean registerBeanDefinitionIfNotExists(BeanDefinitionRegistry registry, String beanName, Class<?> beanClass, Map<String, Object> extraPropertyValues) {
        if (registry.containsBeanDefinition(beanName)) {
            return false;
        }

        String[] candidates = registry.getBeanDefinitionNames();

        for (String candidate : candidates) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(candidate);
            if (Objects.equals(beanDefinition.getBeanClassName(), beanClass.getName())) {
                return false;
            }
        }
        BeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(beanClass).getBeanDefinition();

        if (extraPropertyValues != null) {
            for (Map.Entry<String, Object> entry : extraPropertyValues.entrySet()) {
                beanDefinition.getPropertyValues().add(entry.getKey(), entry.getValue());
            }
        }
        registry.registerBeanDefinition(beanName, beanDefinition);

        return true;
    }

}
