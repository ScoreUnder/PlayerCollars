package org.jlortiz.playercollars.leash;

import net.minecraft.entity.Entity;
import org.jetbrains.annotations.Nullable;

public interface LeashHeldByProxyImpl {
    LeashProxyEntity playerCollars$getLeashProxy();

    void playerCollars$setLeashProxy(LeashProxyEntity value);

    default @Nullable Entity playerCollars$getRealLeashHolder() {
        LeashProxyEntity proxy = playerCollars$getLeashProxy();
        return proxy == null ? null : proxy.getLeashHolder();
    }
}
