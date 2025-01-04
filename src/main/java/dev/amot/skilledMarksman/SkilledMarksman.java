package dev.amot.skilledMarksman;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SkilledMarksman implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("Skilled Marksman");

    @Override
    public void onInitialize() {
        LOGGER.info("Mod loaded!");
    }
}
