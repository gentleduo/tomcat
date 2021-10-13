package com.duo;

import org.apache.catalina.*;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.core.*;
import org.apache.catalina.startup.Catalina;

import java.io.IOException;

public class Test {

    public static void main(String[] args) throws IOException {

        Catalina catalina = new Catalina();

        catalina.setAwait(true);

        StandardServer server = new StandardServer();
        catalina.setServer(server);

        StandardService service = new StandardService();
        server.addService(service);

        Executor executor = new StandardThreadExecutor();
        service.addExecutor(executor);

        Connector connector = new Connector();
        service.addConnector(connector);

        Engine engine = new StandardEngine();
        service.setContainer(engine);

        Host host = new StandardHost();
        engine.addChild(host);

        Context context = new StandardContext();
        host.addChild(context);

        engine.setParentClassLoader(Catalina.class.getClassLoader()); // shareClassLoader

        server.setCatalina(catalina);

        try {
            server.init();  //
        } catch (LifecycleException e) {
            e.printStackTrace();
        }

        catalina.start();

    }
}
