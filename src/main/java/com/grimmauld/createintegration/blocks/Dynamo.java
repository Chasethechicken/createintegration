package com.grimmauld.createintegration.blocks;

import com.grimmauld.createintegration.Config;
import com.simibubi.create.content.contraptions.base.DirectionalKineticBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.Direction.Axis;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorldReader;
import net.minecraftforge.common.ToolType;

import java.util.Objects;

import static com.grimmauld.createintegration.tools.ModUtil.getFacingFromEntity;

public class Dynamo extends DirectionalKineticBlock {
    public Dynamo() {
        super(Properties.create(Material.IRON)
                .sound(SoundType.METAL)
                .hardnessAndResistance(2.0f)
                .harvestTool(ToolType.PICKAXE)
                .harvestLevel(2)
        );
        setRegistryName("dynamo");
    }

    @Override
    public boolean hasTileEntity(BlockState state) {
        return true;
    }

    @Override
    public TileEntity createTileEntity(BlockState state, IBlockReader world) {
        return new DynamoTile();
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        Direction preferred = getPreferredFacing(context);
        if ((Objects.requireNonNull(context.getPlayer()).isSneaking() || preferred == null) && Config.PART_SNAPPING.get())
            return this.getDefaultState().with(BlockStateProperties.FACING, getFacingFromEntity(context.getPos(), context.getPlayer()));
        assert preferred != null;
        return getDefaultState().with(BlockStateProperties.FACING, preferred);
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(BlockStateProperties.FACING, BlockStateProperties.POWERED);
    }

    /*@Override // FIXME
    protected boolean hasStaticPart() {
        return true;
    }*/

    @Override
    public boolean hasShaftTowards(IWorldReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.get(BlockStateProperties.FACING);
    }

    @Override
    public boolean hideStressImpact() {
        return true;
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        System.out.println("get axis");
        return state.get(BlockStateProperties.FACING).getAxis();
    }
}
