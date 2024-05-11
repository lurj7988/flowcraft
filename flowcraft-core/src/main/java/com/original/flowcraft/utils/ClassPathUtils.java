package com.original.flowcraft.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class ClassPathUtils {
    private static final transient Logger log = LoggerFactory.getLogger(ClassPathUtils.class);
    private static final String PATH = Objects.requireNonNull(ClassPathUtils.class.getResource("/")).toString();

    public ClassPathUtils() {
    }

    public static String getWebContext() {
        if (isTomcat()) {
            String DeployPath = getDeployWarPath();
            return DeployPath.substring(DeployPath.lastIndexOf("/") + 1);
        } else {
            return "";
        }
    }

    public static String getDeployWarPath() {
        String DeployPath;
        if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
            DeployPath = PATH.replace("jar:file:/", "").replace("file:/", "");
        } else {
            DeployPath = PATH.replace("jar:file:", "").replace("file:", "");
        }
        log.debug("DeployPath:{}", DeployPath);
        if (DeployPath.contains("/target/classes/")) {
            DeployPath = DeployPath.replace("/target/classes/", "/src/main/webapp/");
            log.debug("调试:{}", DeployPath);
        } else if (isTomcat()) {
            DeployPath = DeployPath.replace("/WEB-INF/classes/", "");
            log.debug("tomcat部署:{}", DeployPath);
        } else {
            //springboot2.0
            DeployPath = DeployPath.replace("!/BOOT-INF/classes!/", "");
            // spinrgboot3.0
            DeployPath = DeployPath.replace("/!BOOT-INF/classes/!/", "");
            DeployPath = DeployPath.replace("jar:nested:/", "");
            DeployPath = DeployPath.substring(0, DeployPath.lastIndexOf("/"));
            log.debug("springboot部署:{}", DeployPath);
        }

        return DeployPath;
    }

    public static boolean isTomcat() {
        return PATH.endsWith("/WEB-INF/classes/");
    }

    static {
        log.info("ClassPathUtils.class.getResource(\"/\").toString():{}", PATH);
        log.info("ClassPathUtils.class.getResource(ClassPathUtil.class.getSimpleName() + \".class\").toString():{}", Objects.requireNonNull(ClassPathUtils.class.getResource(ClassPathUtils.class.getSimpleName() + ".class")));
    }
}
