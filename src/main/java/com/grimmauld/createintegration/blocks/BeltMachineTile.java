package com.grimmauld.createintegration.blocks;

import com.grimmauld.createintegration.recipes.BeltMachineRecipe;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.contraptions.base.KineticTileEntity;
import com.simibubi.create.content.contraptions.components.saw.SawTileEntity;
import com.simibubi.create.content.contraptions.processing.ProcessingInventory;
import com.simibubi.create.content.contraptions.relays.belt.BeltHelper;
import com.simibubi.create.content.contraptions.relays.belt.BeltTileEntity;
import com.simibubi.create.content.contraptions.relays.belt.transport.TransportedItemStack;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.utility.VecHelper;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.BlockParticleData;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ItemParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static com.grimmauld.createintegration.blocks.BeltMachine.RUNNING;

public abstract class BeltMachineTile extends KineticTileEntity {
    public ProcessingInventory inventory;
    protected int recipeIndex;
    protected int recipeNumber;
    protected LazyOptional<IItemHandler> invProvider;
    protected boolean destroyed;

    public BeltMachineTile(TileEntityType<?> TET) {
        super(TET);
        inventory = new ProcessingInventory(this::start);
        inventory.remainingTime = -1;
        recipeIndex = 0;
        recipeNumber = 0;
        invProvider = LazyOptional.of(() -> inventory);
    }

    @Override
    public void addBehaviours(List<TileEntityBehaviour> behaviours) {
        super.addBehaviours(behaviours);
    }

    protected BeltTileEntity getTargetingBelt() {
        BlockPos targetPos = getTargetingBeltBlock();
        assert world != null;
        if (!AllBlocks.BELT.has(world.getBlockState(targetPos)))
            return null;
        return BeltHelper.getSegmentTE(world, targetPos);
    }

    private BlockPos getTargetingBeltBlock() {
        Vector3d itemMovement = getItemMovementVec();
        return pos.add(-itemMovement.x + .5f, -itemMovement.y + .5f, -itemMovement.z + .5f);
    }


    /*@Override  // FIXME
    public boolean hasFastRenderer() {
        return false;
    }*/

    @Override
    public void onSpeedChanged(float prevSpeed) {
        super.onSpeedChanged(prevSpeed);
        boolean shouldRun = Math.abs(getSpeed()) > 1 / 64f;
        boolean running = getBlockState().get(RUNNING);
        if (shouldRun != running && !destroyed) {
            assert world != null;
            world.setBlockState(pos, getBlockState().with(RUNNING, shouldRun), 2 | 16);
        }
    }

    @Override
    public CompoundNBT write(CompoundNBT compound) {
        compound.put("Inventory", inventory.serializeNBT());
        compound.putInt("RecipeIndex", recipeIndex);
        compound.putInt("RecipeNumber", recipeNumber);
        return super.write(compound);
    }

    /*@Override  // FIXME
    public void read(CompoundNBT compound) {
        super.read(compound);
        inventory.deserializeNBT(compound.getCompound("Inventory"));
        recipeIndex = compound.getInt("RecipeIndex");
        recipeNumber = compound.getInt("RecipeNumber");
    }*/

    /*@Override // FIXME
    public void tick() {
        super.tick();
        if (getSpeed() == 0)
            return;


        // insert from belt
        if (inventory.getStackInSlot(0).getCount() < 64) {
            assert world != null;
            if (AllBlocks.BELT.has(world.getBlockState(getTargetingBeltBlock()))) {
                BeltTileEntity beltTE = getTargetingBelt();
                if (beltTE == null)
                    return;
                BeltTileEntity controllerTE = beltTE.getControllerTE();
                if (controllerTE == null)
                    return;
                controllerTE.getInventory().forEachWithin(beltTE.index + (beltTE.index > 0 ? 1 : 0), .1f, stack -> {
                    ItemStack insertStack = stack.stack.copy();
                    List<TransportedItemStack> returnList = new ArrayList<>();
                    returnList.add(stack);
                    if (inventory.isEmpty()) {
                        inventory.insertItem(0, insertStack, false);
                        this.markDirty();
                        controllerTE.markDirty();
                        return Collections.emptyList();
                    } else if (inventory.getStackInSlot(0).getItem().equals(insertStack.getItem())) {
                        if (inventory.getStackInSlot(0).getCount() + insertStack.getCount() > 64) {
                            stack.stack.setCount(stack.stack.getCount() + inventory.getStackInSlot(0).getCount() - 64);
                            inventory.getStackInSlot(0).setCount(64);
                            return returnList;
                        } else {
                            inventory.getStackInSlot(0).setCount(inventory.getStackInSlot(0).getCount() + insertStack.getCount());
                            return Collections.emptyList();
                        }
                    }
                    return returnList;


                });
            }
        }

        float processingSpeed = MathHelper.clamp(Math.abs(getSpeed()) / 32, 1, 128) * (canProcess() ? 1 : 0);
        inventory.remainingTime -= processingSpeed;

        if (inventory.remainingTime > 0)
            spawnParticles(inventory.getStackInSlot(0));

        assert world != null;
        if (world.isRemote)
            return;

        if (inventory.remainingTime <= 0 && !inventory.isEmpty()) {
            for (ItemStack output : applyRecipe()) {
                ejectItems(output);
            }
            sendData();
            markDirty();
            if (!inventory.isEmpty())
                start();
            markDirty();
            return;
        }

        if (inventory.remainingTime == -1) {
            if (!inventory.isEmpty())
                start();
            markDirty();
        }
    }*/

    protected void ejectItems(ItemStack outputStack) {
        if (outputStack.isEmpty()) {
            return;
        }

        markDirty();
        inventory.remainingTime = -1;
        Vector3d itemMovement = getItemMovementVec();
        Direction itemMovementFacing = Direction.getFacingFromVector(itemMovement.x, itemMovement.y, itemMovement.z);
        Vector3d outPos = VecHelper.getCenterOf(pos).add(itemMovement.scale(.5f).add(0.0, .5, 0.0));
        Vector3d outMotion = itemMovement.scale(.0625).add(0.0, .125, 0.0);

        /*// Try moving items onto the belt  // FIXME
        BlockPos nextPos = pos.add(itemMovement.x, itemMovement.y, itemMovement.z);
        assert world != null;
        if (AllBlocks.BELT.has(world.getBlockState(nextPos))) {
            TileEntity te = world.getTileEntity(nextPos);
            if (te instanceof BeltTileEntity) {
                if (((BeltTileEntity) te).tryInsertingFromSide(itemMovementFacing, outputStack, false)) {
                    return;
                }
            }
        }*/

        /*// Try moving items onto next saw/belt machine // FIXME
        if (AllBlocks.MECHANICAL_SAW.has(world.getBlockState(nextPos)) || world.getBlockState(nextPos).getBlock() instanceof BeltMachine) {
            TileEntity te = world.getTileEntity(nextPos);
            if (te != null) {
                if (te instanceof SawTileEntity) {
                    SawTileEntity sawTileEntity = (SawTileEntity) te;
                    Vector3d otherMovement = sawTileEntity.getItemMovementVec();
                    if (Direction.getFacingFromVector(otherMovement.x, otherMovement.y,
                            otherMovement.z) != itemMovementFacing.getOpposite()) {
                        ProcessingInventory sawInv = sawTileEntity.inventory;
                        if (sawInv.isEmpty()) {
                            sawInv.insertItem(0, outputStack, false);
                            return;
                        }
                    }
                }
                if (te instanceof BeltMachineTile) {
                    BeltMachineTile beltMachineTile = (BeltMachineTile) te;
                    Vector3d otherMovement = beltMachineTile.getItemMovementVec();
                    if (Direction.getFacingFromVector(otherMovement.x, otherMovement.y, otherMovement.z) != itemMovementFacing.getOpposite()) {
                        outputStack = beltMachineTile.mergeInsert(outputStack);
                        if (outputStack.isEmpty()) {
                            return;
                        }
                    }
                }
            }
        }*/

        // Eject Items
        ItemEntity entityIn = new ItemEntity(world, outPos.x, outPos.y, outPos.z, outputStack);
        entityIn.setMotion(outMotion);
        world.addEntity(entityIn);
        world.updateComparatorOutputLevel(pos, getBlockState().getBlock());
    }

    protected boolean canProcess() {
        return (!inventory.isEmpty()) && getSpeed() != 0;
    }

    public ItemStack mergeInsert(ItemStack toInsert) {
        ItemStack outputStack = toInsert.copy();
        if (inventory.isEmpty()) {
            inventory.insertItem(0, toInsert.copy(), false);
            this.markDirty();
            return ItemStack.EMPTY;
        } else if (inventory.getStackInSlot(0).getItem().equals(toInsert.getItem())) {
            if (inventory.getStackInSlot(0).getCount() + toInsert.getCount() > 64) {
                outputStack.setCount(toInsert.getCount() + inventory.getStackInSlot(0).getCount() - 64);
                inventory.getStackInSlot(0).setCount(64);
                return outputStack;
            } else {
                inventory.getStackInSlot(0).setCount(inventory.getStackInSlot(0).getCount() + toInsert.getCount());
                return ItemStack.EMPTY;
            }
        }
        return toInsert;
    }


    @Override
    public void remove() {
        invProvider.invalidate();
        destroyed = true;
        super.remove();
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> cap, Direction side) {
        if (cap == CapabilityItemHandler.ITEM_HANDLER_CAPABILITY)
            return invProvider.cast();
        return super.getCapability(cap, side);
    }

    protected void spawnParticles(ItemStack stack) {
        if (stack == null || stack.isEmpty())
            return;

        IParticleData particleData;
        float speed = 1;
        if (stack.getItem() instanceof BlockItem)
            particleData =
                    new BlockParticleData(ParticleTypes.BLOCK, ((BlockItem) stack.getItem()).getBlock().getDefaultState());
        else {
            particleData = new ItemParticleData(ParticleTypes.ITEM, stack);
            speed = .125f;
        }

        assert world != null;
        Random r = world.rand;
        Vector3d vec = getItemMovementVec();
        Vector3d pos = VecHelper.getCenterOf(this.pos);
        float offset = inventory.recipeDuration != 0 ? inventory.remainingTime / inventory.recipeDuration : 0;
        offset -= .5f;
        world.addParticle(particleData, pos.getX() + -vec.x * offset, pos.getY() + .45f, pos.getZ() + -vec.z * offset,
                -vec.x * speed, r.nextFloat() * speed, -vec.z * speed);
    }

    public Vector3d getItemMovementVec() {
        boolean alongX = getBlockState().get(BlockStateProperties.FACING).getXOffset() != 0;
        int offset = getSpeed() > 0 ? -1 : 1;
        return new Vector3d(offset * (alongX ? 0 : -1), 0, offset * (alongX ? 1 : 0));
    }

    protected abstract List<ItemStack> applyRecipe();

    abstract List<? extends IRecipe<?>> getRecipes();

    public void start(ItemStack inserted) {
        start();
    }

    public void start() {
        ItemStack inserted = inventory.getStackInSlot(0);
        if (inventory.isEmpty())
            return;
        assert world != null;
        if (world.isRemote)
            return;

        List<? extends IRecipe<?>> recipes = getRecipes();
        int time = 100;
        recipeNumber = inserted.getCount();

        if (recipes.isEmpty()) {
            inventory.remainingTime = inventory.recipeDuration = 10;
            sendData();
            return;
        }

        recipeIndex++;
        recipeIndex %= recipes.size();

        IRecipe<?> recipe = recipes.get(recipeIndex);
        if (recipe instanceof BeltMachineRecipe) {
            time = ((BeltMachineRecipe) recipe).getProcessingDuration();
        }

        inventory.remainingTime = time * Math.max(1, (recipeNumber / 5));
        inventory.recipeDuration = inventory.remainingTime;

        sendData();
    }

    @Override
    public abstract float calculateStressApplied();
}
