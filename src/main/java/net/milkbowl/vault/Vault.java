package net.milkbowl.vault;

import com.google.common.collect.ImmutableMap;

import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServiceRegisterEvent;
import org.bukkit.event.server.ServiceUnregisterEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicesManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Vault extends JavaPlugin implements Listener {

    private static final Map<Class<?>, VaultService<?>> VAULT_SERVICES = ImmutableMap.<Class<?>, VaultService<?>>builder()
            .put(Economy.class, new VaultService<>(Economy.class, Economy::getName))
            .put(Permission.class, new VaultService<>(Permission.class, Permission::getName))
            .put(Chat.class, new VaultService<>(Chat.class, Chat::getName))
            .build();

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(this, this);
    }

    @EventHandler
    public void onServiceRegister(ServiceRegisterEvent e) {
        RegisteredServiceProvider<?> provider = e.getProvider();
        VaultService<?> vaultService = VAULT_SERVICES.get(provider.getService());
        if (vaultService != null) {
            getLogger().info(vaultService.registeredMsg(provider.getProvider()));
        }
    }

    @EventHandler
    public void onServiceUnregister(ServiceUnregisterEvent e) {
        RegisteredServiceProvider<?> provider = e.getProvider();
        VaultService<?> vaultService = VAULT_SERVICES.get(provider.getService());
        if (vaultService != null) {
            getLogger().info(vaultService.unregisteredMsg(provider.getProvider()));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        ServicesManager servicesManager = getServer().getServicesManager();

        sender.sendMessage("[Vault] Vault v" + getDescription().getVersion() + " Information");
        for (VaultService<?> service : VAULT_SERVICES.values()) {
            sender.sendMessage("[Vault] " + service.infoMsg(servicesManager));
        }

        return true;
    }

    private static final class VaultService<T> {
        private final String name;
        private final Class<T> clazz;
        private final Function<T, String> providerNameFunc;

        private VaultService(Class<T> clazz, Function<T, String> providerNameFunc) {
            this.name = clazz.getSimpleName();
            this.clazz = clazz;
            this.providerNameFunc = providerNameFunc;
        }

        private String registeredMsg(Object provider) {
            T castedProvider = this.clazz.cast(provider);
            return "New " + this.name + " service registered: " + this.providerNameFunc.apply(castedProvider);
        }

        private String unregisteredMsg(Object provider) {
            T castedProvider = this.clazz.cast(provider);
            return this.name + " service unregistered: " + this.providerNameFunc.apply(castedProvider);
        }

        private String infoMsg(ServicesManager servicesManager) {
            RegisteredServiceProvider<T> registration = servicesManager.getRegistration(this.clazz);
            String provider = registration == null ? "None" : this.providerNameFunc.apply(registration.getProvider());

            String providers = servicesManager.getRegistrations(this.clazz).stream()
                    .map(this.providerNameFunc.compose(RegisteredServiceProvider::getProvider))
                    .collect(Collectors.joining(", ", "[", "]"));

            return this.name + ": " + provider + " " + providers;
        }
    }

}
