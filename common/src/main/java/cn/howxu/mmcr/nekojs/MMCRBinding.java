package cn.howxu.mmcr.nekojs;

/** Global `MMCR` binding, same shape as the KubeJS integration. */
public final class MMCRBinding {
    private final MMCRApiJS api = new MMCRApiJS();
    private final MMCRValuesJS values = new MMCRValuesJS();

    public MMCRApiJS getAPI() { return api; }
    public MMCRValuesJS getValues() { return values; }
}
