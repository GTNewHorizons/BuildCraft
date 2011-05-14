package net.minecraft.src;

import net.minecraft.src.buildcraft.core.Utils;
import net.minecraft.src.buildcraft.devel.BlockCheat;

public class mod_BuildCraftDevel extends BaseModMp {	

	public static BlockCheat cheatBlock;
	
    public void ModsLoaded() {	
    	super.ModsLoaded();
    	
		mod_BuildCraftCore.initialize();
		
		CraftingManager craftingmanager = CraftingManager.getInstance();
		
		cheatBlock = new BlockCheat(Integer.parseInt(Utils.getProperty(
				"cheatBlock.blockId", "150")));
		ModLoader.RegisterBlock(cheatBlock);
		craftingmanager.addRecipe(new ItemStack(cheatBlock, 1), new Object[] {
			"# ", "  ", Character.valueOf('#'), Block.dirt });
		
		Utils.saveProperties();
	}
	
	@Override
	public String Version() {
		return "1.5_01.4";
	}
}
