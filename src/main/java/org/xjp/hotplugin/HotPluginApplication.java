package org.xjp.hotplugin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;

@RestController
@SpringBootApplication
public class HotPluginApplication {

    @Autowired
    BeanRegisterService beanRegisterService;

    @GetMapping("load")
    public String load() {
        testService();
        try {
            return beanRegisterService.registerBeanDefinition();
        } finally {
            testService();
        }
    }

    @GetMapping("unLoad")
    public String unLoad() {
        testService();
        try {
            beanRegisterService.unregisterController("testController");
            return "testController un register!";
        } finally {
            testService();
        }
    }

    private void testService() {
        try {
            Object testService = run.getBean("testService");
            Method method= testService.getClass().getDeclaredMethod("test", null);
            System.out.println(method.invoke(testService, null));
        } catch (Exception e) {
            System.out.println("testService not found!!");
        }
    }

    static ConfigurableApplicationContext run;

    public static void main(String[] args) {
        run = SpringApplication.run(HotPluginApplication.class, args);
    }

}
