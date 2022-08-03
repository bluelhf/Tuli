package blue.lhf.tuli.impl;

import blue.lhf.tuli.api.Tuli;
import com.sun.tools.attach.*;

import java.io.IOException;
import java.lang.instrument.Instrumentation;

public class SneakyDevil {
    public static void agentmain(final String args, Instrumentation instrumentation) {
        Tuli.instrument(instrumentation);
    }

    public static void main(String[] args) throws IOException,
        AttachNotSupportedException, AgentLoadException, AgentInitializationException {
        VirtualMachine.attach(args[0]).loadAgent(args[1]);
    }
}
