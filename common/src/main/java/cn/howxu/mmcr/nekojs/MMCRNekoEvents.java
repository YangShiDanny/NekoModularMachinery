package cn.howxu.mmcr.nekojs;

import cn.howxu.mmcr.nekojs.event.*;
import com.tkisor.nekojs.api.event.EventBusJS;
import com.tkisor.nekojs.api.event.EventGroup;

/** Script event groups. `MMCREvents` mirrors the official KubeJS group name and semantics. */
public interface MMCRNekoEvents {
    String STARTUP_ID = "mmcr.startup";
    String SERVER_ID = "mmcr.server";
    String CLIENT_ID = "mmcr.client";

    EventGroup GROUP = EventGroup.of("MMCREvents");
    EventBusJS<StartupEventJS, Void> STARTUP = GROUP.startup("startup", StartupEventJS.class);
    EventBusJS<ServerEventJS, Void> SERVER = GROUP.server("server", ServerEventJS.class);

    EventBusJS<ClientInfoEventJS, Void> CLIENT = GROUP.client("client", ClientInfoEventJS.class);

    static void postStartup() {
        if (STARTUP.hasListeners()) STARTUP.post(new StartupEventJS());
    }
    static void postServer() {
        if (SERVER.hasListeners()) SERVER.post(new ServerEventJS());
    }
    static void postClient() {
        if (CLIENT.hasListeners()) CLIENT.post(new ClientInfoEventJS());
    }
}
