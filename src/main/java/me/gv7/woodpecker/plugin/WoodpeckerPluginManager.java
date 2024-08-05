package me.gv7.woodpecker.plugin;

public class WoodpeckerPluginManager implements IPluginManager {
    @Override
    public void registerPluginManagerCallbacks(IPluginManagerCallbacks pluginManagerCallbacks) {
        pluginManagerCallbacks.registerHelperPlugin(new JExprEncoderPlugin());
    }
}
