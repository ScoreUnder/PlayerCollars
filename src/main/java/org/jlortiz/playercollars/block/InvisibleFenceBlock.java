package org.jlortiz.playercollars.block;

import com.mojang.serialization.MapCodec;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.block.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ai.pathing.NavigationType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.particle.ParticleUtil;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;
import org.jlortiz.playercollars.PlayerCollarsMod;

public class InvisibleFenceBlock extends HorizontalConnectingBlock {
    private static final MapCodec<InvisibleFenceBlock> CODEC = createCodec(InvisibleFenceBlock::new);
    private static final float FENCE_THICKNESS = 3.25F;  // Thick enough that players can't slip through holes vertically
    public static final RegistryKey<Block> REGISTRY_KEY = RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(PlayerCollarsMod.MOD_ID, "invisible_fence"));
    public static final RegistryKey<Item> ITEM_REGISTRY_KEY = RegistryKey.of(RegistryKeys.ITEM, Identifier.of(PlayerCollarsMod.MOD_ID, "invisible_fence"));
    public static final BooleanProperty POWERED = Properties.POWERED;

    public InvisibleFenceBlock(AbstractBlock.Settings settings) {
        super(FENCE_THICKNESS, FENCE_THICKNESS, 16.0F, 16.0F, 24.0F, settings.registryKey(REGISTRY_KEY));
        setDefaultState(stateManager.getDefaultState().with(NORTH, false).with(EAST, false).with(SOUTH, false).with(WEST, false).with(WATERLOGGED, false).with(POWERED, false));
    }

    @Override
    public MapCodec<FenceBlock> getCodec() {
        // The superclass should return a MapCodec<? extends FenceBlock> but it doesn't...
        //noinspection unchecked
        return (MapCodec<FenceBlock>) (MapCodec<?>) CODEC;
    }

    @Override
    protected BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.INVISIBLE;
    }

    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(NORTH, EAST, WEST, SOUTH, WATERLOGGED, POWERED);
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView tickView, BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        if (state.get(WATERLOGGED)) {
            tickView.scheduleFluidTick(pos, Fluids.WATER, Fluids.WATER.getTickRate(world));
        }

        state = direction.getAxis().isHorizontal()
                ? state.with(FACING_PROPERTIES.get(direction), this.canConnect(neighborState, neighborState.isSideSolidFullSquare(world, neighborPos, direction.getOpposite())))
                : super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
        if (neighborState.isOf(this) && neighborState.get(POWERED) != state.get(POWERED)) {
            state = state.with(POWERED, neighborState.get(POWERED));
        }
        return state;
    }

    @Override
    protected VoxelShape getCameraCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        return VoxelShapes.empty();
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        World world = ctx.getWorld();
        BlockPos blockPos = ctx.getBlockPos();
        BlockState state = super.getPlacementState(ctx)
                .with(NORTH, canConnect(world, blockPos.north(), Direction.SOUTH))
                .with(EAST, canConnect(world, blockPos.east(), Direction.WEST))
                .with(SOUTH, canConnect(world, blockPos.south(), Direction.NORTH))
                .with(WEST, canConnect(world, blockPos.west(), Direction.EAST))
                .with(WATERLOGGED, world.getFluidState(blockPos).getFluid() == Fluids.WATER);

        if (hasNeighbouringPoweredInvisibleFence(world, blockPos)) state = state.with(POWERED, true);
        return state;
    }

    private boolean hasNeighbouringPoweredInvisibleFence(World world, BlockPos blockPos) {
        for (Direction dir : Direction.Type.HORIZONTAL) {
            BlockState neighbor = world.getBlockState(blockPos.offset(dir));
            if (neighbor.isOf(this) && neighbor.get(POWERED)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (context instanceof EntityShapeContext e) {
            Entity entity = e.getEntity();
            // Vertical collision is cached using EntityShapeContext.ABSENT.
            // This will be re-checked if something actually lands on the fence, so this is safe for players.
            // It can cause unusual behaviour if something tries to pathfind through it, so that is left disabled.
            if (entity == null) return super.getCollisionShape(state, world, pos, context);

            if (state.get(POWERED) && isPetOrPetsVehicle(entity)) {
                return super.getCollisionShape(state, world, pos, context);
            }
        }
        return VoxelShapes.empty();
    }

    private static boolean isPetOrPetsVehicle(Entity entity) {
        if (PlayerCollarsMod.entityIsPet(entity)) return true;
        for (Entity passenger : entity.getPassengersDeep()) {
            if (PlayerCollarsMod.entityIsPet(passenger)) return true;
        }
        return false;
    }

    @Override
    protected float calcBlockBreakingDelta(BlockState state, PlayerEntity player, BlockView world, BlockPos pos) {
        if (PlayerCollarsMod.isPet(player)) return 0;
        return super.calcBlockBreakingDelta(state, player, world, pos);
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (context instanceof EntityShapeContext e && PlayerCollarsMod.entityIsPet(e.getEntity())) {
            return VoxelShapes.empty();
        }
        return super.getOutlineShape(state, world, pos, context);
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void randomDisplayTick(BlockState state, World world, BlockPos pos, Random random) {
        super.randomDisplayTick(state, world, pos, random);
        if (state.get(POWERED) && random.nextFloat() < 0.25 && !isLocalPlayerAPet())
            ParticleUtil.spawnParticlesAround(world, pos, 1, 0.5, 0.5, true, DustParticleEffect.DEFAULT);
    }

    @Environment(EnvType.CLIENT)
    private static boolean isLocalPlayerAPet() {
        var localPlayer = MinecraftClient.getInstance().player;
        return localPlayer != null && PlayerCollarsMod.isPet(localPlayer);
    }

    @Override
    protected ActionResult onUseWithItem(ItemStack stack, BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (stack.getItem() == PlayerCollarsMod.INVISIBLE_FENCE_BLOCK_ITEM) {
            return ActionResult.PASS;
        } else {
            return ActionResult.PASS_TO_DEFAULT_BLOCK_ACTION;
        }
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world.isClient()) return ActionResult.CONSUME;
        if (PlayerCollarsMod.isPet(player)) {
            player.sendMessage(Text.translatable("block.playercollars.invisible_fence.toggle_fail").formatted(Formatting.RED), true);
            return ActionResult.FAIL;
        }
        state = state.with(POWERED, !state.get(POWERED));
        world.setBlockState(pos, state, 7);
        player.sendMessage(Text.translatable(
                state.get(POWERED) ? "block.playercollars.invisible_fence.toggle_on"
                        : "block.playercollars.invisible_fence.toggle_off")
                .formatted(Formatting.GREEN), true);
        return ActionResult.SUCCESS;
    }

    protected boolean canPathfindThrough(BlockState state, NavigationType type) {
        return false;
    }

    private boolean canConnect(BlockView world, BlockPos pos, Direction reverseDir) {
        BlockState blockState = world.getBlockState(pos);
        return this.canConnect(blockState, blockState.isSideSolidFullSquare(world, pos, reverseDir));
    }

    private boolean canConnect(BlockState state, boolean neighborIsFullSquare) {
        return !cannotConnect(state) && neighborIsFullSquare || state.isOf(PlayerCollarsMod.INVISIBLE_FENCE_BLOCK);
    }
}