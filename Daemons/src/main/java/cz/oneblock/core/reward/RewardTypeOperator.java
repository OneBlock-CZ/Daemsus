package cz.oneblock.core.reward;

import cz.oneblock.core.configuration.ConfigurateSection;
import cz.oneblock.core.util.Properties;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Nullable;

public interface RewardTypeOperator<P> {

    void claimReward(int count, Properties data, P to);

    void loadReward(int count, Properties data, ConfigurateSection configuration);

    @Nullable
    default Component getName(Properties data) {
        return null;
    }

}
