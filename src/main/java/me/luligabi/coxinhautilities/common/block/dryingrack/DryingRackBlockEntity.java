package me.luligabi.coxinhautilities.common.block.dryingrack;

import me.luligabi.coxinhautilities.common.block.BlockEntityRegistry;
import me.luligabi.coxinhautilities.common.block.ClientSyncedBlockEntity;
import me.luligabi.coxinhautilities.common.recipe.drying.DryingRecipe;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.Optional;

public class DryingRackBlockEntity extends ClientSyncedBlockEntity implements Inventory /*, SidedInventory*/ {

    public DryingRackBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistry.DRYING_RACK_BLOCK_ENTITY, pos, state);
        this.inventory = DefaultedList.ofSize(1, ItemStack.EMPTY);
    }

    private DefaultedList<ItemStack> inventory;
    int dryingTime;
    boolean checkedRecipe;
    boolean canDry;


    public static void tick(World world, BlockPos pos, BlockState state, DryingRackBlockEntity blockEntity) {
        if(world.isClient || blockEntity.inventory.isEmpty()) return;
        if(!blockEntity.checkedRecipe) checkRecipe(blockEntity, world);

        if(blockEntity.canDry) {
            blockEntity.dryingTime++;
            craft(world, blockEntity);
        }
    }

    private static void checkRecipe(DryingRackBlockEntity blockEntity, World world) {
        Optional<DryingRecipe> recipeOptional = createRecipeOptional(blockEntity, world);
        if(recipeOptional.isEmpty()) {
            blockEntity.checkedRecipe = true;
            blockEntity.canDry = false;
            markDirty(world, blockEntity.pos, blockEntity.getCachedState());
            return;
        }

        blockEntity.checkedRecipe = true;
        blockEntity.canDry = recipeOptional.get().getIngredient().test(blockEntity.inventory.get(0));
        markDirty(world, blockEntity.pos, blockEntity.getCachedState());
    }

    private static void craft(World world, DryingRackBlockEntity blockEntity) {
        Optional<DryingRecipe> recipeOptional = createRecipeOptional(blockEntity, world);
        if(recipeOptional.isEmpty() || blockEntity.dryingTime < recipeOptional.get().getDryingTime()) return;

        blockEntity.inventory.set(0, recipeOptional.get().getOutput());

        blockEntity.canDry = false;
        blockEntity.checkedRecipe = false;
        blockEntity.dryingTime = 0;
        markDirty(world, blockEntity.pos, blockEntity.getCachedState());
    }


    @Override
    public void toTag(NbtCompound nbt) {
        Inventories.writeNbt(nbt, inventory);
        nbt.putShort("DryingTime", (short) dryingTime);
        nbt.putBoolean("CheckedRecipe", checkedRecipe);
        nbt.putBoolean("CanDry", canDry);
    }

    @Override
    public void fromTag(NbtCompound nbt) {
        this.inventory = DefaultedList.ofSize(this.size(), ItemStack.EMPTY);
        Inventories.readNbt(nbt, inventory);
        this.dryingTime = nbt.getShort("DryingTime");
        this.checkedRecipe = nbt.getBoolean("CheckedRecipe");
        this.canDry = nbt.getBoolean("CanDry");
    }

    @Override
    public void toClientTag(NbtCompound nbt) {
        toTag(nbt);
    }

    @Override
    public void fromClientTag(NbtCompound nbt) {
        fromTag(nbt);
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        return true;
    }

    public DefaultedList<ItemStack> getInventory() {
        return inventory;
    }

    public int size() {
        return this.inventory.size();
    }

    public boolean isEmpty() {
        return this.inventory.get(0).isEmpty();
    }

    public ItemStack getStack(int slot) {
        return slot >= 0 && slot < this.inventory.size() ? this.inventory.get(slot) : ItemStack.EMPTY;
    }

    public ItemStack removeStack(int slot, int amount) {
        return Inventories.splitStack(this.inventory, slot, amount);
    }

    public ItemStack removeStack(int slot) {
        return Inventories.removeStack(this.inventory, slot);
    }

    public void setStack(int slot, ItemStack stack) {
        if (slot >= 0 && slot < this.inventory.size()) {
            this.inventory.set(slot, stack);
        }
    }

    public void clear() {
        this.inventory.clear();
    }


    /*public boolean isValid(int slot, ItemStack stack) { // TODO: Implement hopper interactions
        return switch(slot) {
            case 3 -> true; // input slots
            case 4 -> EssenceExtractorScreenHandler.isCatalyst(stack); // catalyst slot
            default -> inventory.get(slot).isEmpty() && stack.isOf(Items.GLASS_BOTTLE); // bottle slots
        };
    }

    public int[] getAvailableSlots(Direction side) {
        return switch(side.getId()) {
            case 0 -> new int[]{0, 1, 2}; // DOWN - bottle slots
            case 1 -> new int[]{3}; // UP - input slot
            default -> new int[]{4, 0, 1, 2}; // SIDES - catalyst slot, bottle slots
        };
    }

    public boolean canInsert(int slot, ItemStack stack, @Nullable Direction dir) {
        return isValid(slot, stack);
    }

    public boolean canExtract(int slot, ItemStack stack, Direction side) {
        return switch(side.getId()) {
            case 0 -> brewTime <= 0 && inventory.get(3).isEmpty() && !stack.isOf(Items.GLASS_BOTTLE); // Extract downward if nothing is being brewed, the input slot is empty and the extracted is not a glass bottle.
            case 1 -> brewTime <= 0; // Extract upward if nothing is being brewed.
            default -> false; // Never extract from sides
        };
    }*/

    @Override
    public void markDirty() {
        super.markDirty();
        if(!isClientSide()) {
            sync();
        }
    }

    @SuppressWarnings("ConstantConditions")
    private static Optional<DryingRecipe> createRecipeOptional(DryingRackBlockEntity blockEntity, World world) {
        return world.getServer().getRecipeManager().getFirstMatch(DryingRecipe.Type.INSTANCE, blockEntity, world);
    }
    
}