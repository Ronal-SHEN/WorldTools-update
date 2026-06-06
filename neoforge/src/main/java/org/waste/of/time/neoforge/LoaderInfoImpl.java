package org.waste.of.time.neoforge;

import net.neoforged.fml.loading.FMLLoader;

public class LoaderInfoImpl {
    public static String getVersion() {
        return FMLLoader.getLoadingModList().getModFileById("worldtools").versionString();
    }

}
