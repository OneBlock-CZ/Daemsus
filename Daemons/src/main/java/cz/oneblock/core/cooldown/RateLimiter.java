package cz.oneblock.core.cooldown;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class RateLimiter<T> {

    private final Cache<T, LocalDateTime> expiring;
    private final long amount;
    private final TimeUnit unit;

    public RateLimiter(long amount, TimeUnit unit) {
        this.amount = amount;
        this.unit = unit;
        expiring = Caffeine.newBuilder()
                .ticker(Ticker.systemTicker())
                .expireAfterWrite(amount, unit)
                .build();
    }

    public LocalDateTime tryAndLimit(T t) {
        var wasLimited = new AtomicReference<>(true);
        var time = expiring.get(t, x -> {
            wasLimited.set(false);
            return LocalDateTime.now().plus(Duration.ofMillis(unit.toMillis(amount)));
        });
        var limited = wasLimited.get();
        return limited ? time : null;
    }

}
