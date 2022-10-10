package com.vicious.experiencedworlds.common.data;

public interface IWorldBorderData {
    double getExpansions();
    void expand(int expansions);
    double getSizeMultiplier();
    void setSizeMultiplier(double multiplier);
    double getTransformedBorderSize();
}
