package com.vicious.experiencedworlds.common.data;

import com.vicious.experiencedworlds.common.EWCFG;
import com.vicious.experiencedworlds.common.EWMath;
import com.vicious.serverstatistics.ServerStatistics;
import com.vicious.viciouscore.aunotamation.isyncablecompoundholder.annotation.Obscured;
import com.vicious.viciouscore.common.data.structures.SyncableCompound;
import com.vicious.viciouscore.common.data.structures.SyncablePrimitive;

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

    public double getSizeMultiplier() {
        int numAdvancements = ServerStatistics.getData().advancers.size();
        return numAdvancements <= 0 ? 1 : 1 + EWMath.summate(numAdvancements,d1(),getCurrentMultiplierGain());
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
        return EWCFG.getInstance().multipliersExponentialGain.getBoolean() ? EWMath.baseToTheX(mb,numAdvancements,-1) : mb*numAdvancements;
    }

    public void reset() {
        expansions.setValue(0);
    }
}
