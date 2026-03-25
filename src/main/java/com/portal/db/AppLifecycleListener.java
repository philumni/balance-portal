package com.portal.db;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;

/**
 * Hooks into Tomcat's deploy/undeploy lifecycle.
 *
 * @WebListener registers this with Tomcat automatically — no web.xml needed.
 *
 * contextInitialized → called when the WAR is deployed
 *   - initializes HikariCP pool
 *   - wires repository implementations (DB or Mock)
 *
 * contextDestroyed → called when Tomcat shuts down or the WAR is undeployed
 *   - closes the HikariCP pool gracefully
 *   - prevents "connection leak" warnings in Tomcat logs
 */
@WebListener
public class AppLifecycleListener implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("[AppLifecycleListener] Application starting — initializing repositories.");
        RepositoryFactory.initialize();

        String mode = RepositoryFactory.isUsingDb() ? "MariaDB" : "MockDataStore (fallback)";
        sce.getServletContext().setAttribute("dataMode", mode);
        System.out.println("[AppLifecycleListener] Data layer ready: " + mode);
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("[AppLifecycleListener] Application stopping — shutting down pool.");
        RepositoryFactory.shutdown();
    }
}
