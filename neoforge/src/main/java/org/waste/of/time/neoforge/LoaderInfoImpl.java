package org.waste.of.time.neoforge;

import net.neoforged.fml.ModList;

public class LoaderInfoImpl {
    public static String getVersion() {
        return ModList.get().getModContainerById("worldtools")
                .map(container -> container.getModInfo().getVersion().toString())
                .orElse("unknown");
    }

}
