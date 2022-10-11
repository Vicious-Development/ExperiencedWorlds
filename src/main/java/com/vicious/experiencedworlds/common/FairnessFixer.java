package com.vicious.experiencedworlds.common;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

public class FairnessFixer {
    private static final int range = 5;
    private static BlockPos vec = null;
    public static boolean checkFair(int x, int z, Level l){
        AtomicInteger airCount = new AtomicInteger();
        BlockPos topBlock = scanDown(x, z, l, (state) -> {
            if (state.getBlock() == Blocks.AIR) {
                airCount.getAndAdd(1);
            } else {
                airCount.set(0);
            }
            return airCount.get() == 2;
        });
        Set<Block> blocksFound = new HashSet<>();
        Set<BlockPos> leafPos = new HashSet<>();
        if(isSafeSpawnBlock(l.getBlockState(topBlock))){
            for (int x2 = -range; x2 < range; x2++) {
                for (int z2 = -range; z2 < range; z2++) {
                    BlockPos topOption = scanDown(x+x2, z+z2, l, (state) -> state.getMaterial().isSolid());
                    BlockState top = l.getBlockState(topOption);
                    while(!top.requiresCorrectToolForDrops()){
                        if(top.getBlock() instanceof LeavesBlock){
                            leafPos.add(topOption);
                        }
                        blocksFound.add(top.getBlock());
                        topOption = topOption.below();
                        top = l.getBlockState(topOption);
                    }
                }
            }
            if(leafPos.size() < 5){
                for (BlockPos p : leafPos) {
                    vec=p;
                    break;
                }
                return false;
            }
            return blocksFound.size() >= 5;
        }
        return false;
    }
    public static BlockPos getFairPos(int x, int z, Level l) throws UnfairnessException {
        int vecX = l.getRandom().nextIntBetweenInclusive(-1,1);
        int vecZ = l.getRandom().nextIntBetweenInclusive(-1,1);
        //Avoid going nowhere.
        if(vecX == vecZ && vecX == 0){
            vecX = -1;
            vecZ = 1;
        }
        vecX*=range;
        vecZ*=range;
        long time = System.currentTimeMillis()+EWCFG.getInstance().fairnessCheckMaximumTime.value()*1000;
        while(System.currentTimeMillis() < time) {
            if(checkFair(x,z,l)) return scanDown(x,z,l,(b)->b.getMaterial().isSolid());
            if(vec != null){
                vecX = vec.getX()/Math.abs(vec.getX());
                vecZ = vec.getZ()/Math.abs(vec.getZ());
            }
            x+=vecX;
            z+=vecZ;
        }
        throw new UnfairnessException();
    }
    private static final Set<Block> unsafeBlocks = Set.of(Blocks.ICE,Blocks.BLUE_ICE,Blocks.STONE,Blocks.CALCITE,Blocks.PACKED_ICE);
    public static boolean isSafeSpawnBlock(BlockState state){
        if(state.getMaterial().isLiquid()) return false;
        return !unsafeBlocks.contains(state.getBlock());
    }
    public static BlockPos scanDown(int x, int z, Level l, Predicate<BlockState> validator){
        return scanDown(x,l.getHeight(),z,l,validator);
    }
    public static BlockPos scanDown(int x, int y, int z, Level l, Predicate<BlockState> validator){
        BlockPos pos = new BlockPos(x,y,z);
        BlockState state = l.getBlockState(pos);
        while(!validator.test(state)){
            pos = pos.below();
            state = l.getBlockState(pos);
        }
        return pos;
    }

    public static class UnfairnessException extends Exception{}
}
