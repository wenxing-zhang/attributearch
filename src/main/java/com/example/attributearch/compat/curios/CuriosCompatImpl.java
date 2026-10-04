package com.example.attributearch.compat.curios;

import com.example.attributearch.AttributeArch;
import com.example.attributearch.item.WenxingTotemItem;
import com.example.attributearch.registry.ModItems;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

final class CuriosCompatImpl {
    private static final String CURIOS_API = "top.theillusivec4.curios.api.CuriosApi";
    private static final String ICURIO_ITEM = "top.theillusivec4.curios.api.type.capability.ICurioItem";

    private static final AtomicBoolean LOGGED_LOOKUP_FAIL = new AtomicBoolean();
    private static volatile boolean apiMissing;
    private static volatile Method getCuriosInventoryMethod;

    private CuriosCompatImpl() {
    }

    static void register() {
        try {
            Class<?> curioItemClass = Class.forName(ICURIO_ITEM);
            Object handler = Proxy.newProxyInstance(
                    CuriosCompatImpl.class.getClassLoader(),
                    new Class<?>[]{curioItemClass},
                    (proxy, method, args) -> switch (method.getName()) {
                        case "canEquip", "canEquipFromUse" -> {
                            Object slotContext = args != null && args.length > 0 ? args[0] : null;
                            yield slotContext != null && isDedicatedSlot(slotContext);
                        }
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> "WenxingTotemCurio";
                        default -> defaultValue(method.getReturnType());
                    });
            Class<?> api = Class.forName(CURIOS_API);
            Item item = ModItems.WENXING_TOTEM.get();
            boolean registered = false;
            try {
                Method registerCurio = api.getMethod("registerCurio", ItemStack.class, curioItemClass);
                registerCurio.invoke(null, item.getDefaultInstance(), handler);
                registered = true;
            } catch (ReflectiveOperationException | IllegalArgumentException ignored) {

            }
            if (!registered) {
                Method alt = api.getMethod("registerCurio", Item.class, curioItemClass);
                alt.invoke(null, item, handler);
            }
            AttributeArch.LOGGER.info("Registered WenxingTotemCurio with Curios");
        } catch (ClassNotFoundException e) {
            apiMissing = true;
        } catch (Throwable t) {

            AttributeArch.LOGGER.warn("Curios item behavior not registered (tag validator still applies): {}", t.toString());
        }
    }

    static boolean hasTotemInDedicatedSlot(Player player) {
        if (apiMissing) {
            return false;
        }
        try {
            Method getInv = getCuriosInventoryMethod;
            if (getInv == null) {
                Class<?> api = Class.forName(CURIOS_API);
                getInv = api.getMethod("getCuriosInventory", LivingEntity.class);
                getCuriosInventoryMethod = getInv;
            }
            Object optionalOrLazy = getInv.invoke(null, player);
            Object handler = unwrapOptional(optionalOrLazy);
            if (handler == null) {
                return false;
            }
            Object stacksHandler = unwrapOptional(invokeGetStacksHandler(handler, CuriosCompat.SLOT_ID));
            if (stacksHandler == null) {
                stacksHandler = findStacksHandlerLoose(handler);
            }
            if (stacksHandler == null) {
                return false;
            }
            Object stacks = stacksHandler.getClass().getMethod("getStacks").invoke(stacksHandler);
            int slots = (int) stacks.getClass().getMethod("getSlots").invoke(stacks);
            for (int i = 0; i < slots; i++) {
                Object stack = stacks.getClass().getMethod("getStackInSlot", int.class).invoke(stacks, i);
                if (stack instanceof ItemStack itemStack && WenxingTotemItem.isTotem(itemStack)) {
                    return true;
                }
            }
            return false;
        } catch (ClassNotFoundException e) {
            apiMissing = true;
            return false;
        } catch (Throwable t) {

            if (LOGGED_LOOKUP_FAIL.compareAndSet(false, true)) {
                AttributeArch.LOGGER.warn("Curios dedicated-slot lookup failed (will retry): {}", t.toString());
            }
            return false;
        }
    }

    private static boolean isDedicatedSlot(Object slotContext) {
        try {
            Object id = slotContext.getClass().getMethod("identifier").invoke(slotContext);
            return CuriosCompat.SLOT_ID.equals(String.valueOf(id));
        } catch (Throwable t) {
            return false;
        }
    }

    private static Object findStacksHandlerLoose(Object handler) {
        try {
            Object curios = invokeNoArg(handler, "getCurios");
            if (curios instanceof Map<?, ?> map) {
                for (Map.Entry<?, ?> e : map.entrySet()) {
                    if (String.valueOf(e.getKey()).toLowerCase().contains("wenxing")) {
                        return e.getValue();
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object invokeGetStacksHandler(Object handler, String slotId) {
        try {
            return handler.getClass().getMethod("getStacksHandler", String.class).invoke(handler, slotId);
        } catch (Throwable t) {
            try {
                return handler.getClass()
                        .getMethod("getStacksHandler", net.minecraft.resources.ResourceLocation.class)
                        .invoke(handler, net.minecraft.resources.ResourceLocation.parse(slotId));
            } catch (Throwable ignored) {
                return null;
            }
        }
    }

    private static Object invokeNoArg(Object target, String name) {
        try {
            return target.getClass().getMethod(name).invoke(target);
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object unwrapOptional(Object raw) {
        if (raw == null) {
            return null;
        }
        if (raw instanceof java.util.Optional<?> opt) {
            return opt.orElse(null);
        }
        try {
            Object resolved = raw.getClass().getMethod("resolve").invoke(raw);
            if (resolved instanceof java.util.Optional<?> opt) {
                return opt.orElse(null);
            }
            return resolved;
        } catch (Throwable t) {
            return null;
        }
    }

    private static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive() || type == void.class) {
            return null;
        }
        if (type == boolean.class) {
            return false;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == double.class) {
            return 0.0D;
        }
        if (type == float.class) {
            return 0.0F;
        }
        return 0;
    }
}
