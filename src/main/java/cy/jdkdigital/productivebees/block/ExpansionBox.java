package cy.jdkdigital.productivebees.block;

import cy.jdkdigital.productivebees.state.properties.VerticalHive;
import cy.jdkdigital.productivebees.tileentity.AdvancedBeehiveTileEntity;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.common.util.Constants;
import org.apache.commons.lang3.tuple.Pair;

import javax.annotation.Nullable;

public class ExpansionBox extends Block
{
    public static final BooleanProperty HAS_HONEY = BooleanProperty.create("has_honey");

    public ExpansionBox(final Properties properties) {
        super(properties);
        this.setDefaultState(this.getDefaultState()
            .with(BeehiveBlock.FACING, Direction.NORTH)
            .with(AdvancedBeehive.EXPANDED, VerticalHive.NONE)
            .with(HAS_HONEY, false)
        );
    }

    @Override
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return this.getDefaultState().with(BeehiveBlock.FACING, context.getPlacementHorizontalFacing().getOpposite());
    }

    @Override
    protected void fillStateContainer(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(BeehiveBlock.FACING, AdvancedBeehive.EXPANDED, HAS_HONEY);
    }

    public void updateState(World world, BlockPos pos, BlockState state, boolean isRemoved) {
        Pair<Pair<BlockPos, Direction>, BlockState> pair = getAdjacentHive(world, pos);
        if (pair != null) {
            Pair<BlockPos, Direction> posAndDirection = pair.getLeft();
            BlockPos hivePos = posAndDirection.getLeft();
            VerticalHive directionProperty = AdvancedBeehive.calculateExpandedDirection(world, hivePos, isRemoved);

            if (!isRemoved) {
                updateStateWithDirection(world, pos, state, directionProperty);
            }
            ((AdvancedBeehive) pair.getRight().getBlock()).updateStateWithDirection(world, hivePos, pair.getRight(), directionProperty);
        }
    }

    public void updateStateWithDirection(World world, BlockPos pos, BlockState state, VerticalHive directionProperty) {
        world.setBlockState(pos, state.with(AdvancedBeehive.EXPANDED, directionProperty), Constants.BlockFlags.DEFAULT);
    }

    private static Pair<Pair<BlockPos, Direction>, BlockState> getAdjacentHive(World world, BlockPos pos) {
        for(Direction direction: BlockStateProperties.FACING.getAllowedValues()) {
            if (direction == Direction.UP) {
                continue;
            }
            BlockPos newPos = pos.offset(direction);
            BlockState blockStateAtPos = world.getBlockState(newPos);

            Block blockAtPos = blockStateAtPos.getBlock();
            if (blockAtPos instanceof AdvancedBeehive && !(blockAtPos instanceof DragonEggHive)) {
                return Pair.of(Pair.of(newPos, direction), blockStateAtPos);
            }
        }
        return null;
    }

    @Override
    public void onBlockPlacedBy(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.onBlockPlacedBy(world, pos, state, placer, stack);
        if (!world.isRemote()) {
            this.updateState(world, pos, state, false);
        }
    }

    @Override
    public boolean removedByPlayer(BlockState state, World world, BlockPos pos, PlayerEntity player, boolean willHarvest, FluidState fluid) {
        boolean removed = super.removedByPlayer(state, world, pos, player, willHarvest, fluid);

        if (!world.isRemote()) {
            this.updateState(world, pos, state, true);
        }

        return removed;
    }

    @Override
    public ActionResultType onBlockActivated(final BlockState state, final World worldIn, final BlockPos pos, final PlayerEntity player, final Hand handIn, final BlockRayTraceResult hit) {
        if (!worldIn.isRemote) {
            // Open the attached beehive, if there is one
            Pair<Pair<BlockPos, Direction>, BlockState> pair = getAdjacentHive(worldIn, pos);
            if (pair != null) {
                final TileEntity tileEntity = worldIn.getTileEntity(pair.getLeft().getLeft());
                if (tileEntity instanceof AdvancedBeehiveTileEntity) {
                    Block block = tileEntity.getBlockState().getBlock();
                    ((AdvancedBeehive) block).openGui((ServerPlayerEntity) player, (AdvancedBeehiveTileEntity) tileEntity);
                }
            }
        }
        return ActionResultType.SUCCESS;
    }
}
