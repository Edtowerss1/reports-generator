package com.example.JaspertReport.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class TenantContextTest {

    private final Tenant sampleTenant = new Tenant(
        "acme",
        "/reportes/acme/",
        Set.of("ventas", "stock"),
        Set.of("PDF"),
        new Tenant.Datasource("jdbc:mysql://host/acme", "user", "pass", "com.mysql.cj.jdbc.Driver")
    );

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldReturnNullWhenNoTenantSet() {
        assertNull(TenantContext.getCurrentTenant());
    }

    @Test
    void shouldReturnTenantAfterSet() {
        TenantContext.set(sampleTenant);
        assertSame(sampleTenant, TenantContext.getCurrentTenant());
    }

    @Test
    void shouldReturnNullAfterClear() {
        TenantContext.set(sampleTenant);
        TenantContext.clear();
        assertNull(TenantContext.getCurrentTenant());
    }

    @Test
    void shouldBeIsolatedBetweenThreads() throws InterruptedException {
        var latch = new CountDownLatch(1);
        var threadValue = new AtomicReference<Tenant>();

        var otherTenant = new Tenant(
            "corp",
            "/reportes/corp/",
            Set.of("reporte1"),
            Set.of("PDF"),
            new Tenant.Datasource("jdbc:mysql://host/corp", "user2", "pass2", "com.mysql.cj.jdbc.Driver")
        );

        TenantContext.set(sampleTenant);

        Thread other = new Thread(() -> {
            TenantContext.set(otherTenant);
            threadValue.set(TenantContext.getCurrentTenant());
            latch.countDown();
        });
        other.start();
        latch.await();

        assertSame(otherTenant, threadValue.get(), "Other thread should see its own tenant");
        assertSame(sampleTenant, TenantContext.getCurrentTenant(), "Main thread should still see its own tenant");
    }

    @Test
    void shouldNotInheritParentThreadValue() throws InterruptedException {
        var latch = new CountDownLatch(1);
        var threadValue = new AtomicReference<Tenant>();

        TenantContext.set(sampleTenant);

        Thread other = new Thread(() -> {
            threadValue.set(TenantContext.getCurrentTenant());
            latch.countDown();
        });
        other.start();
        latch.await();

        assertNull(threadValue.get(), "New thread should not inherit parent's TenantContext");
    }
}
