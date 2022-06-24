package cz.projectzet.core.reward;

import cz.projectzet.core.util.Properties;

public record Reward(RewardType type, int count, Properties properties) {
}
