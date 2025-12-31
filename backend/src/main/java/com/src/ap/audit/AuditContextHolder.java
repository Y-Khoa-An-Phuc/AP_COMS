package com.src.ap.audit;

public final class AuditContextHolder {

    private static final ThreadLocal<AuditRequestContext> CONTEXT = new ThreadLocal<>();

    private AuditContextHolder() {
    }

    public static void set(AuditRequestContext context) {
        CONTEXT.set(context);
    }

    public static AuditRequestContext get() {
        return CONTEXT.get();
    }

    public static AuditRequestContext getOrCreate(String fallbackActor) {
        AuditRequestContext context = CONTEXT.get();
        if (context == null) {
            context = new AuditRequestContext(fallbackActor);
            CONTEXT.set(context);
        }
        return context;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
