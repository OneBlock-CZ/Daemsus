package cz.projectzet.core.reward;

import cz.projectzet.core.BootLoader;
import cz.projectzet.core.ProjectDaemon;
import cz.projectzet.core.SystemDaemon;
import cz.projectzet.core.configuration.ConfigurateSection;
import cz.projectzet.core.util.Properties;

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
