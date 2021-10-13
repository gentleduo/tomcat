package com.duo;

import org.apache.catalina.connector.Request;
import org.apache.catalina.connector.Response;
import org.apache.catalina.valves.RequestFilterValve;
import org.apache.juli.logging.Log;

import javax.servlet.ServletException;
import java.io.IOException;

public class TestValve extends RequestFilterValve {


    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        System.out.println("test value");
        getNext().invoke(request, response);
    }

    @Override
    protected Log getLog() {
        return null;
    }


}
