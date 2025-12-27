package dev.asdf00.mc.advcomp.items;

import dev.asdf00.mc.advcomp.api.ItemCanBeInitialized;
import dev.asdf00.mc.advcomp.lua.components.AcItemComponent;
import dev.asdf00.mc.advcomp.lua.components.LuaUserDataComponent;
import dev.asdf00.mc.advcomp.utils.ResourceUtil;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

public class FloppyDiskItem extends BaseAcDyableItem implements ItemCanBeInitialized, AcItemComponent {

    private final int totalCapacityBytes;

    public FloppyDiskItem(Properties pProperties, int totalCapacityBytes) {
        super(pProperties);
        this.totalCapacityBytes = totalCapacityBytes;
    }

    @Override
    public void onCraftedBy(@NotNull ItemStack pStack, @NotNull Level pLevel, @NotNull Player pPlayer) {
        super.onCraftedBy(pStack, pLevel, pPlayer);
        if (pLevel.isClientSide()) return;

        init(pStack, false);
    }

    public static boolean IsValidPremadeFloppyName(String name) {
        var valid = !name.isBlank();

        if (valid)
            for (int i = 0; i < name.length(); i++) {
                var chr = name.charAt(i);
                var thisCharIsOk = chr >= 'a' && chr <= 'z' ||
                                   chr >= 'A' && chr <= 'Z' ||
                                   chr == '_';
                if (!thisCharIsOk) {
                    valid = false;
                    break;
                }
            }

        return valid;
    }


    @Override
    public void Initialize(ItemStack is) {
        init(is, true);
    }

    private void init(ItemStack is, boolean forceInit) {
        var nbt = is.getTag();
        boolean willCopyData = nbt != null && nbt.contains("desiredDiskData");
        // initialize the fs on disk so we can copy data to it. Also sets the disk id nbt.
        if (forceInit || willCopyData)
            ManagedMassStorageUD.initFromItemStack("floppy", is, 0);

        nbt = is.getTag();
        if (willCopyData) {
            var desiredData = nbt.getString("desiredDiskData");
            if (IsValidPremadeFloppyName(desiredData)) {
                // write premade data to floppy folder
                ResourceUtil.copyPremadeFloppyIntoManagedDiskFolder(desiredData, nbt.getInt("mDiskId"));
            }

            var tag = is.getTag();
            tag.remove("desiredDiskData");
            // remove desiredDiskData tag, write data to disk, set id to reference it
        }
    }

    @Override
    public LuaUserDataComponent CreateUserdata(ItemStack stack) {
        return ManagedMassStorageUD.initFromItemStack("floppy", stack, totalCapacityBytes);
    }
}
