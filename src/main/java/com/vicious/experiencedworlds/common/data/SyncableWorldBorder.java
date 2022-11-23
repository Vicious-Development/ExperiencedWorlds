package com.vicious.experiencedworlds.common.data;

import com.vicious.experiencedworlds.common.config.EWCFG;
import com.vicious.experiencedworlds.common.EWMath;
import com.vicious.serverstatistics.ServerStatistics;
import com.vicious.viciouscore.aunotamation.isyncablecompoundholder.annotation.Obscured;
import com.vicious.viciouscore.common.data.DataAccessor;
import com.vicious.viciouscore.common.data.structures.SyncableCompound;
import com.vicious.viciouscore.common.data.structures.SyncablePrimitive;
import net.minecraft.nbt.CompoundTag;

public class SyncableWorldBorder extends SyncableCompound implements IWorldBorderData {
    @Obscured
    public SyncablePrimitive<Integer> expansions = new SyncablePrimitive<>("expansions",0);
    @Obscured
    public SyncablePrimitive<Integer> fairnesslevel = new SyncablePrimitive<>("fairnesslevel",-1);


    public SyncableWorldBorder(String key) {
        super(key);
    }

    @Override
    public int getExpansions() {
        return expansions.getValue();
    }

    @Override
    public void expand(int expansions) {
        this.expansions.setValue(expansions+this.expansions.getValue());
    }

    @Override
    public double getTransformedBorderSize() {
        return EWCFG.getInstance().startingSize.value()+expansions.getValue()*EWCFG.getInstance().sizeGained.value()*getSizeMultiplier();
    }

    public boolean maximumMultiplier(){
        return getUnmaxedSizeMultiplier() >= EWCFG.getInstance().advancementMultiplierMax.get();
    }

    public double getUnmaxedSizeMultiplier(){
        int numAdvancements = ServerStatistics.getData().advancers.size();
        double mb = getMultiplierBase();
        return numAdvancements <= 0 ? 1 : 1 + EWMath.summate(numAdvancements,d1(),getCurrentMultiplierGain());
    }

    public double getSizeMultiplier() {
        return Math.min(getUnmaxedSizeMultiplier(),EWCFG.getInstance().advancementMultiplierMax.get());
    }
    private double d1(){
        if(EWCFG.getInstance().multipliersExponentialGain.value()){
            return Math.abs(getMultiplierBase())-1;
        }
        else{
            return Math.abs(getMultiplierBase());
        }
    }
    public double getMultiplierBase(){
        return EWCFG.getInstance().advancementMultiplierBase.value();
    }

    public double getCurrentMultiplierGain() {
        int numAdvancements = ServerStatistics.getData().advancers.size();
        double mb = getMultiplierBase();
        return EWCFG.getInstance().multipliersExponentialGain.value() ? EWMath.baseToTheX(mb,numAdvancements,-1) : mb*numAdvancements;
    }

    @Override
    public void deserializeNBT(CompoundTag tag, DataAccessor sender) {
        super.deserializeNBT(tag, sender);
    }

    public void reset() {
        expansions.setValue(0);
    }
}
