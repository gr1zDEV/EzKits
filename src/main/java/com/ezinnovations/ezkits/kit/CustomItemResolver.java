package com.ezinnovations.ezkits.kit;

import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;
import java.util.Locale;
import java.util.Optional;

public class CustomItemResolver {

    private final JavaPlugin plugin;

    public CustomItemResolver(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public Optional<ItemStack> resolve(String provider, String itemId, int amount) {
        if (provider == null || provider.isBlank() || itemId == null || itemId.isBlank()) {
            return Optional.empty();
        }

        return switch (provider.toLowerCase(Locale.ROOT)) {
            case "nexo" -> resolveNexo(itemId, amount);
            case "executableitems", "executable_items", "ei" -> resolveExecutableItems(itemId, amount);
            default -> Optional.empty();
        };
    }

    private Optional<ItemStack> resolveNexo(String itemId, int amount) {
        if (!isPluginEnabled("Nexo")) {
            return Optional.empty();
        }

        try {
            Class<?> nexoItemsClass = Class.forName("com.nexomc.nexo.api.NexoItems");
            Method itemFromId = nexoItemsClass.getMethod("itemFromId", String.class);
            Object builderOrItem = itemFromId.invoke(null, itemId);
            Optional<ItemStack> resolved = extractItemStack(builderOrItem, amount);
            if (resolved.isEmpty()) {
                plugin.getLogger().warning("Nexo item could not be resolved for id: " + itemId);
            }
            return resolved;
        } catch (Throwable throwable) {
            plugin.getLogger().warning("Failed to resolve Nexo item '" + itemId + "': " + throwable.getMessage());
            return Optional.empty();
        }
    }

    private Optional<ItemStack> resolveExecutableItems(String itemId, int amount) {
        if (!isPluginEnabled("ExecutableItems")) {
            return Optional.empty();
        }

        String[] apiClassNames = new String[]{
                "com.ssomar.executableitems.api.ExecutableItemsAPI",
                "com.ssomar.score.api.executableitems.ExecutableItemsAPI"
        };

        for (String apiClassName : apiClassNames) {
            try {
                Class<?> apiClass = Class.forName(apiClassName);
                Optional<ItemStack> direct = tryStaticFactoryMethods(apiClass, itemId, amount);
                if (direct.isPresent()) {
                    return direct;
                }
            } catch (ClassNotFoundException ignored) {
            } catch (Throwable throwable) {
                plugin.getLogger().warning("Failed to resolve ExecutableItems item '" + itemId + "' with " + apiClassName + ": " + throwable.getMessage());
            }
        }

        plugin.getLogger().warning("ExecutableItems API class not found or item unavailable for id: " + itemId);
        return Optional.empty();
    }

    private Optional<ItemStack> tryStaticFactoryMethods(Class<?> apiClass, String itemId, int amount) throws ReflectiveOperationException {
        Object result;

        result = invokeIfPresent(apiClass, "getExecutableItemStack", new Class<?>[]{String.class}, itemId);
        if (result != null) {
            return extractItemStack(result, amount);
        }

        result = invokeIfPresent(apiClass, "getExecutableItemStack", new Class<?>[]{String.class, int.class}, itemId, amount);
        if (result != null) {
            return extractItemStack(result, amount);
        }

        result = invokeIfPresent(apiClass, "getItemStack", new Class<?>[]{String.class}, itemId);
        if (result != null) {
            return extractItemStack(result, amount);
        }

        result = invokeIfPresent(apiClass, "getExecutableItem", new Class<?>[]{String.class}, itemId);
        if (result != null) {
            return extractItemStack(result, amount);
        }

        return Optional.empty();
    }

    private Object invokeIfPresent(Class<?> owner, String name, Class<?>[] params, Object... args) throws ReflectiveOperationException {
        try {
            Method method = owner.getMethod(name, params);
            return method.invoke(null, args);
        } catch (NoSuchMethodException ignored) {
            return null;
        }
    }

    private Optional<ItemStack> extractItemStack(Object source, int amount) {
        if (source == null) {
            return Optional.empty();
        }

        if (source instanceof Optional<?> optional) {
            if (optional.isEmpty()) {
                return Optional.empty();
            }
            return extractItemStack(optional.get(), amount);
        }

        if (source instanceof ItemStack stack) {
            ItemStack clone = stack.clone();
            clone.setAmount(Math.max(1, amount));
            return Optional.of(clone);
        }

        for (String methodName : new String[]{"build", "buildItem", "getItemStack", "toItemStack"}) {
            Optional<ItemStack> built = invokeBuilderMethod(source, methodName, amount);
            if (built.isPresent()) {
                return built;
            }
        }

        return Optional.empty();
    }

    private Optional<ItemStack> invokeBuilderMethod(Object source, String methodName, int amount) {
        Class<?> sourceClass = source.getClass();

        try {
            Method m = sourceClass.getMethod(methodName);
            Object built = m.invoke(source);
            return extractItemStack(built, amount);
        } catch (NoSuchMethodException ignored) {
        } catch (Throwable ignored) {
        }

        try {
            Method m = sourceClass.getMethod(methodName, int.class);
            Object built = m.invoke(source, Math.max(1, amount));
            return extractItemStack(built, amount);
        } catch (NoSuchMethodException ignored) {
        } catch (Throwable ignored) {
        }

        try {
            Method m = sourceClass.getMethod(methodName, org.bukkit.entity.Player.class);
            Object built = m.invoke(source, (Object) null);
            return extractItemStack(built, amount);
        } catch (NoSuchMethodException ignored) {
        } catch (Throwable ignored) {
        }

        return Optional.empty();
    }

    private boolean isPluginEnabled(String name) {
        Plugin installed = Bukkit.getPluginManager().getPlugin(name);
        return installed != null && installed.isEnabled();
    }
}
