package cz.oneblock.core.reward;

import cz.oneblock.core.util.Properties;

public record Reward(RewardType type, int count, Properties properties) {
}
