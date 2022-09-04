package cz.oneblock.core.reward;

import cz.oneblock.core.BootLoader;
import cz.oneblock.core.ProjectDaemon;
import cz.oneblock.core.SystemDaemon;
import cz.oneblock.core.configuration.ConfigurateSection;
import cz.oneblock.core.util.Properties;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

//UNFINISHED
public abstract class RewardDaemon<P, B extends BootLoader> extends ProjectDaemon<B> {

    protected final Map<RewardType, RewardTypeOperator<P>> operators;

    protected RewardDaemon(SystemDaemon systemDaemon) {
        super(systemDaemon);
        operators = new HashMap<>();
    }

    @Override
    public void start() {
        loadOperators();
    }

    @Override
    public void stop() {

    }

    @Override
    public boolean useLanguage() {
        return true;
    }

    @Override
    public String getShortName() {
        return "Reward";
    }

    public Reward loadReward(ConfigurateSection configuration) {
        var type = RewardType.valueOf(configuration.getString("type").toUpperCase(Locale.ROOT));

        var operator = operators.get(type);

        if (operator == null) {
            throw new IllegalArgumentException("Unknown reward type: " + type);
        }

        var properties = new Properties();

        var count = configuration.getInt("count", 1);

        operator.loadReward(count, properties, configuration);

        return new Reward(type, count, properties);
    }

    public void claimReward(Reward reward, P to) {

    }

    public RewardTypeOperator<P> getOperator(RewardType type) {
        return operators.get(type);
    }

    protected abstract void loadOperators();

}
