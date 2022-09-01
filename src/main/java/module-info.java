module blue.lhf.tuli {
    exports blue.lhf.tuli.api;
    exports blue.lhf.tuli.impl to java.instrument, blue.lhf.tuli.test;

    requires java.instrument;
    requires jdk.attach;
}