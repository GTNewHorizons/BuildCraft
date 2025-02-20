/**
 * Copyright (c) 2011-2017, SpaceToad and the BuildCraft Team http://www.mod-buildcraft.com
 *
 * The BuildCraft API is distributed under the terms of the MIT License. Please check the contents of the license, which
 * should be located as "LICENSE.API" in the BuildCraft source code distribution.
 */
package buildcraft.api.blueprints;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;

public interface ISchematicRegistry {

    void registerSchematicBlock(Block block, Class<? extends Schematic> clazz, Object... params);

    void registerSchematicBlock(Block block, int meta, Class<? extends Schematic> clazz, Object... params);

    void registerSchematicEntity(Class<? extends Entity> entityClass, Class<? extends SchematicEntity> schematicClass,
            Object... params);

    boolean isSupported(Block block, int metadata);
}
