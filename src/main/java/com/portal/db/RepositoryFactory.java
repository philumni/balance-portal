package com.portal.db;

import com.portal.repository.*;

/**
 * Singleton factory that holds the active repository implementations.
 *
 * On first access it tries to connect to MariaDB via HikariCP.
 * If the DB is available  → wires MariaDb* implementations
 * If the DB is unavailable → wires Mock* implementations (in-memory fallback)
 *
 * All servlets call RepositoryFactory.customers() / .invoices() / .pending()
 * and never import a concrete implementation. This is the Dependency
 * Inversion principle in practice — high-level code depends on abstractions.
 *
 * LIFECYCLE
 * ---------
 * AppLifecycleListener (a ServletContextListener) calls initialize()
 * when Tomcat deploys the WAR, and shutdown() when it undeploys it.
 */
public class RepositoryFactory {

    private static volatile RepositoryFactory INSTANCE;

    private final CustomerRepository             customers;
    private final InvoiceRepository              invoices;
    private final PendingRegistrationRepository  pending;
    private final boolean                        usingDatabase;

    // -------------------------------------------------------------------------
    // Initialization
    // -------------------------------------------------------------------------

    private RepositoryFactory() {
        boolean dbAvailable = ConnectionPool.initialize();
        this.usingDatabase  = dbAvailable;

        if (dbAvailable) {
            this.customers = new MariaDbCustomerRepository();
            this.invoices  = new MariaDbInvoiceRepository();
            this.pending   = new MariaDbPendingRepository();
            System.out.println("[RepositoryFactory] Using MariaDB repositories.");
        } else {
            this.customers = new MockCustomerRepository();
            this.invoices  = new MockInvoiceRepository();
            this.pending   = new MockPendingRepository();
            System.out.println("[RepositoryFactory] Using in-memory Mock repositories (fallback).");
        }
    }

    public static synchronized void initialize() {
        if (INSTANCE == null) {
            INSTANCE = new RepositoryFactory();
        }
    }

    private static RepositoryFactory get() {
        if (INSTANCE == null) {
            initialize(); // safe lazy init if called before listener
        }
        return INSTANCE;
    }

    // -------------------------------------------------------------------------
    // Public accessors — used by all servlets
    // -------------------------------------------------------------------------

    public static CustomerRepository            customers() { return get().customers; }
    public static InvoiceRepository             invoices()  { return get().invoices;  }
    public static PendingRegistrationRepository pending()   { return get().pending;   }
    public static boolean                       isUsingDb() { return get().usingDatabase; }

    // -------------------------------------------------------------------------
    // Shutdown
    // -------------------------------------------------------------------------

    public static synchronized void shutdown() {
        ConnectionPool.shutdown();
        INSTANCE = null;
    }
}
