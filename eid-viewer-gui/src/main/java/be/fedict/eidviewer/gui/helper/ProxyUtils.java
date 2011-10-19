/*
 * eID Middleware Project.
 * Copyright (C) 2010-2011 FedICT.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version
 * 3.0 as published by the Free Software Foundation.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, see
 * http://www.gnu.org/licenses/.
 */

package be.fedict.eidviewer.gui.helper;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Frank Marien
 */

public class ProxyUtils
{
    private static final Logger logger              = Logger.getLogger(ProxyUtils.class.getName());
    private static final String USE_SYSTEM_PROXIES  = "java.net.useSystemProxies";
    private static final String PROXY_TEST_URL      = "https://trust-ws.services.belgium.be/eid-trust-service-ws/xkms2";
    private static final Proxy  systemProxy         =determineSystemProxy(PROXY_TEST_URL);
    
    public static Proxy getSystemProxy()
    {
        if(systemProxy==null)
            return Proxy.NO_PROXY;
        return systemProxy;
    }

    public static String getHostName(Proxy proxy)
    {
        InetSocketAddress address=(InetSocketAddress) proxy.address();
        return address.getHostName();
    }
    
    public static int getPort(Proxy proxy)
    {
        InetSocketAddress address=(InetSocketAddress) proxy.address();
        return address.getPort();
    }
    
    private static Proxy determineSystemProxy(String forURL)
    {
        Proxy newProxy=null;
        
        final String savedProxySetting = System.getProperty(USE_SYSTEM_PROXIES);

        try
        {
            System.setProperty(USE_SYSTEM_PROXIES,"true");
            List<Proxy> availableProxies=ProxySelector.getDefault().select(new java.net.URI(PROXY_TEST_URL));
            for (Proxy proxy : availableProxies)            // prefer direct connection
                if(proxy.equals(Proxy.NO_PROXY))
                    newProxy=proxy;
            for(Proxy proxy : availableProxies)             // then HTTP proxies
                if(proxy.type().equals(Proxy.Type.HTTP))
                    newProxy=proxy;
        }
        catch(Exception e)
        {
            logger.log(Level.WARNING,"Cannot Determine System HTTP Proxy", e);
        }
        finally
        {
            if(savedProxySetting!=null)
                System.setProperty(USE_SYSTEM_PROXIES, savedProxySetting);
        }

        return newProxy;
    }
}
