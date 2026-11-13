package cn.howxu.mmcr.nekojs.bridge;

import cn.howxu.mmcr.nekojs.MMCRNekoJS;
import cn.howxu.mmcr.publicapi.event.RegisterMachineDefinitionsEvent;
import cn.howxu.mmcr.publicapi.registration.MachineDefinitionProvider;

/**
 * ServiceLoader entry point (see {@code META-INF/services/...MachineDefinitionProvider}).
 *
 * <p>MMCR calls every provider from {@code PublicMachineDefinitionProviders.registerAll(event)}
 * <em>before</em> it posts {@link RegisterMachineDefinitionsEvent} on the NeoForge bus, i.e. the
 * earliest possible hook. NekoJS STARTUP scripts have already run by then, so the store is full.
 */
public final class DefinitionsProvider implements MachineDefinitionProvider {
    @Override
    public void register(RegisterMachineDefinitionsEvent event) {
        var machines = DeclarationStore.machines();
        // MachineSpec overload — no need to replay a MachineDraft consumer.
        machines.values().forEach(event::registerMachine);
        DeclarationStore.markDefinitionsServed();
        if (!machines.isEmpty()) {
            MMCRNekoJS.LOG.info("MMCR/NekoJS submitted {} machine definition(s): {}",
                    machines.size(), machines.keySet());
        }
    }
}
