package com.original.flowcraft.core;

import com.original.flowcraft.annotation.EnableSeleniumTest;
import com.original.flowcraft.annotation.SeleniumTest;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.lang.NonNull;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SeleniumTestRegistrar implements ImportBeanDefinitionRegistrar {


    @Override
    public void registerBeanDefinitions(@NonNull AnnotationMetadata metadata, @NonNull BeanDefinitionRegistry registry) {
//        AnnotationAttributes mapperScanAttrs = AnnotationAttributes
//                .fromMap(metadata.getAnnotationAttributes(SeleniumTest.class.getName()));
//        GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
//        beanDefinition.setBeanClass(importingClassMetadata.getClass());
//        registry.registerBeanDefinition(mapperScanAttrs.getString("value"),);

        ClassPathScanningCandidateComponentProvider scan = new ClassPathScanningCandidateComponentProvider(false);
        scan.addIncludeFilter(new AnnotationTypeFilter(SeleniumTest.class));
        Map<String, Object> attrs = metadata
                .getAnnotationAttributes(EnableSeleniumTest.class.getName());
        Set<String> basePackages = getBasePackages(metadata);

        BeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();

        for (String basePackage : basePackages) {
            Set<BeanDefinition> candidateComponents = scan.findCandidateComponents(basePackage);

            for (BeanDefinition candidateComponent : candidateComponents) {
                if (candidateComponent instanceof AnnotatedBeanDefinition beanDefinition) {
                    // verify annotated class is an interface
//                    AnnotationMetadata annotationMetadata = beanDefinition.getMetadata();
                    beanDefinition.setScope(BeanDefinition.SCOPE_PROTOTYPE);
//                    Map<String, Object> attributes = annotationMetadata
//                            .getAnnotationAttributes(
//                                    SeleniumTest.class.getCanonicalName());
//                    assert attributes != null;
                    String beanName = beanNameGenerator.generateBeanName(beanDefinition, registry);
                    registry.registerBeanDefinition(beanName, beanDefinition);
                }
            }
        }
    }

    protected Set<String> getBasePackages(AnnotationMetadata importingClassMetadata) {
        Map<String, Object> attributes = importingClassMetadata
                .getAnnotationAttributes(EnableSeleniumTest.class.getCanonicalName());

        Set<String> basePackages = new HashSet<>();
        assert attributes != null;
        for (String pkg : (String[]) attributes.get("value")) {
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg);
            }
        }
        for (String pkg : (String[]) attributes.get("basePackages")) {
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg);
            }
        }
        for (Class<?> clazz : (Class<?>[]) attributes.get("basePackageClasses")) {
            basePackages.add(ClassUtils.getPackageName(clazz));
        }

        if (basePackages.isEmpty()) {
            basePackages.add(
                    ClassUtils.getPackageName(importingClassMetadata.getClassName()));
        }
        return basePackages;
    }
}
