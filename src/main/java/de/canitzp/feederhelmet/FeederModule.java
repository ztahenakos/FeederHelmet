package de.canitzp.feederhelmet;

import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.energy.CapabilityEnergy;
import net.minecraftforge.event.ForgeEventFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class FeederModule implements IHelmetModule{
    
    @Override
    public Item getCorrespondingModuleItem(){
        return FeederHelmet.FEEDER_HELMET_MODULE_ITEM.get();
    }
    
    @Override
    public String getTagName(){
        return "feeder_module"; // todo change with breaking release
    }
    
    @Override
    public boolean isModuleApplicableTo(Item item){
        return FeederHelmet.isItemHelmet(item);
    }
    
    @Override
    public void renderTooltip(@Nonnull ItemStack stack, @Nullable PlayerEntity entityPlayer, List<ITextComponent> list, ITooltipFlag flags){
        list.add(new StringTextComponent(TextFormatting.YELLOW.toString() + TextFormatting.ITALIC.toString() + I18n.get("item.feederhelmet:feeder_helmet_module_installed.text") + TextFormatting.RESET.toString()));
    }
    
    @Override
    public void updatePlayer(PlayerEntity player, ItemStack helmetStack){
        if(player.canEat(false) && FeederHelmet.canDamageBeReducedOrEnergyConsumed(helmetStack)){
            for(ItemStack inventoryStack : player.inventory.items){
                if(FeederModule.isStackEatable(inventoryStack)){
                    if(FeederModule.canPlayerEat(player, inventoryStack)){
                        if(player.canEat(false)){
                            AtomicBoolean hasEnergy = new AtomicBoolean(false);
                            AtomicBoolean canEat = new AtomicBoolean(false);
                            helmetStack.getCapability(CapabilityEnergy.ENERGY).ifPresent(energy -> {
                                hasEnergy.set(true);
                                int energyConsumption = FeederConfig.GENERAL.ENERGY_CONSUMPTION.get();
                                if(energy.extractEnergy(energyConsumption, true) == energyConsumption){
                                    energy.extractEnergy(energyConsumption, false);
                                    canEat.set(true);
                                }
                            });
                            if(!hasEnergy.get()){
                                if(helmetStack.isDamageableItem()){
                                    helmetStack.setDamageValue(helmetStack.getDamageValue() + FeederConfig.GENERAL.DURABILITY.get());
                                    if(helmetStack.getMaxDamage() - helmetStack.getDamageValue() <= 0){
                                        helmetStack.setCount(0);
                                    }
                                }
                                // removed out of damage check if, because there are non-damageable helmets
                                canEat.set(true);
                            }
                            if(canEat.get()){
                                ForgeEventFactory.onItemUseStart(player, inventoryStack, 0);
                                ItemStack result = inventoryStack.getItem().finishUsingItem(inventoryStack, player.getCommandSenderWorld(), player);
                                ForgeEventFactory.onItemUseFinish(player, inventoryStack, 0, result);
                            }
                        }
                    }
                }
            }
        }
    }
    
    private static boolean isStackEatable(@Nonnull ItemStack stack){
        return !stack.isEmpty()
            && !FeederConfig.GENERAL.FOOD_BLACKLIST.get().contains(stack.getItem().getRegistryName().toString())
            && (stack.getItem().isEdible() || FeederConfig.GENERAL.FOOD_WHITELIST.get().contains(stack.getItem().getRegistryName().toString()));
    }
    
    private static boolean canPlayerEat(PlayerEntity player, ItemStack stack){
        if(!stack.isEmpty() && stack.getItem().isEdible()){
            if(FeederConfig.GENERAL.WAIT_UNITL_FILL_ALL_HUNGER.get()){
                return player.getFoodData().getFoodLevel() + stack.getItem().getFoodProperties().getNutrition() <= 20 || (FeederConfig.GENERAL.IGNORE_WAITING_WHEN_LOW_HEART.get() && player.getHealth() <= 10.0F);
            }
            return true;
        }
        return true;
    }
}
