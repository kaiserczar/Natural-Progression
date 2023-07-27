package com.oitsjustjose.natprog.common.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Random;

@SuppressWarnings("deprecation")
public class PebbleBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;

    @Nullable
    private final ResourceLocation parentBlockRl;

    /**
     * @param parent The block which this pebble is made of.
     *               Can be null in the case that the block isn't loaded,
     *               or doesn't exist due to the mod owning the block not being present
     */
    public PebbleBlock(@Nullable ResourceLocation parent) {
        super(Properties.of().strength(0.125F, 2F).sound(SoundType.STONE).dynamicShape().noCollission().offsetType(OffsetType.XZ).replaceable().pushReaction(PushReaction.DESTROY).mapColor(MapColor.STONE));
        this.registerDefaultState(this.getStateDefinition().any().setValue(WATERLOGGED, Boolean.FALSE));
        this.parentBlockRl = parent;
    }

    public @Nullable Block getParentBlock() {
        var block = ForgeRegistries.BLOCKS.getValue(this.parentBlockRl);
        return block == Blocks.AIR ? null : block;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        if (context.getLevel().getBlockState(context.getClickedPos()).getBlock() == Blocks.WATER) {
            return this.defaultBlockState().setValue(WATERLOGGED, Boolean.TRUE);
        }
        return this.defaultBlockState();
    }

    @Nonnull
    @Override
    public VoxelShape getShape(BlockState state, @NotNull BlockGetter worldIn, @NotNull BlockPos pos, @NotNull CollisionContext context) {
        var offset = state.getOffset(worldIn, pos);
        return Shapes.create(0.37D, 0.0D, 0.37D, 0.69D, 0.065D, 0.69D).move(offset.x, offset.y, offset.z);
    }

    @Override
    public void fallOn(@NotNull Level level, @NotNull BlockState state, @NotNull BlockPos pos, @NotNull Entity entity, float fallDistance) {
        super.fallOn(level, state, pos, entity, fallDistance);
        // One in ten chance for the sample to break when fallen on
        var random = new Random();
        if (((int) fallDistance) > 0) {
            if (random.nextInt((int) fallDistance) > 5) {
                level.destroyBlock(pos, true);
            }
        }
    }

    @Nonnull
    public InteractionResult use(@NotNull BlockState state, @NotNull Level worldIn, @NotNull BlockPos pos, Player player, @NotNull InteractionHand handIn, @NotNull BlockHitResult hit) {
        if (!player.isCrouching()) {
            worldIn.destroyBlock(pos, true);
            player.swing(handIn);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    public boolean canSurvive(@NotNull BlockState state, LevelReader worldIn, BlockPos pos) {
        var below = worldIn.getBlockState(pos.below());
        return below.isSolidRender(worldIn, pos.below());
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    public @NotNull FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public void neighborChanged(@NotNull BlockState state, @NotNull Level worldIn, @NotNull BlockPos pos, @NotNull Block blockIn, @NotNull BlockPos fromPos, boolean isMoving) {
        super.neighborChanged(state, worldIn, pos, blockIn, fromPos, isMoving);
        if (!this.canSurvive(state, worldIn, pos)) {
            worldIn.destroyBlock(pos, true);
        }
        // Update the water from flowing to still or vice-versa
        if (state.getValue(WATERLOGGED)) {
            worldIn.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(worldIn));
        }
    }
}
