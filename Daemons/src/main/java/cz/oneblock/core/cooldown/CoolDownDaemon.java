package cz.oneblock.core.cooldown;

import cz.oneblock.core.BootLoader;
import cz.oneblock.core.ProjectDaemon;
import cz.oneblock.core.SystemDaemon;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class CoolDownDaemon extends ProjectDaemon<BootLoader> {

    private final Map<String, RateLimiter<Object>> rateLimiters;

    protected CoolDownDaemon(SystemDaemon systemD) {
        super(systemD);

        rateLimiters = new HashMap<>();
    }

    private RateLimiter<Object> getRateLimiter(String key, long time) {
        return rateLimiters.computeIfAbsent(key, x -> new RateLimiter<>(time, TimeUnit.MILLISECONDS));
    }

    public Duration throttle(String limiter, Object key, long time) {
        var limit = getRateLimiter(limiter, time).tryAndLimit(key);
        return limit == null ? null : Duration.between(LocalDateTime.now(), limit);
    }

    @Override
    public void start() {

    }

    @Override
    public void stop() {

    }
}
