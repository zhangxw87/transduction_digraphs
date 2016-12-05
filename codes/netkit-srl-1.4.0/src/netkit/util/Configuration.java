/**
 * Configuration.java
 * Copyright (C) 2008 Sofus A. Macskassy
 *
 * Part of the open-source Network Learning Toolkit
 * http://netkit-srl.sourceforge.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 **/

/**
 * $Id$
 **/

/**
 * $Id: Configuration.java,v 1.6 2007/04/11 21:58:03 sofmac Exp $
 * Part of the open-source Network Learning Toolkit
 *
 * User: smacskassy
 * Date: Dec 1, 2004
 * Time: 8:04:21 AM
 */
package netkit.util;

import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.io.IOException;

public final class Configuration extends Properties
{
    private final Logger logger = NetKitEnv.getLogger(this);

    private static final long serialVersionUID = 1L;
    
    private final static Pattern pattern = Pattern.compile("\\.");

    public static Configuration getConfiguration(ResourceBundle bundle, String name) {
        Configuration config = new Configuration();
        if(bundle == null)
            return config;
        
        String stem = name+".";
        Enumeration<String> keys = bundle.getKeys();
        while(keys.hasMoreElements())
        {
            String key = keys.nextElement();
            if(!key.startsWith(stem))
                continue;

            String value = bundle.getString(key);

            // key = name.{class|attrib}
            String[] dd = pattern.split(key);
            if(dd.length != 2)
                throw new RuntimeException("invalid key("+key+") found in '"+bundle+"' - it has "+dd.length+" elements where 2 were required.");
            config.put(dd[1],value);
        }
        return config;
    }

    Map<String,Integer> ints    = new HashMap<String,Integer>();
    Map<String,Long>    longs   = new HashMap<String,Long>();
    Map<String,Double>  doubles = new HashMap<String,Double>();

    public Configuration() {
	    super();
    }
    public Configuration(Configuration defaults) {
	    super(defaults);
    }
    public Configuration(java.io.InputStream in) throws IOException {
        this();
        load(in);
    }
    public Configuration(Configuration defaults, java.io.InputStream in) throws IOException {
        this(defaults);
        load(in);
    }

    public void setParent(Configuration defaultConfiguration) {
        this.defaults = defaultConfiguration;
    }
    public Configuration getParent() {
        return (Configuration)this.defaults;
    }
    public Configuration getRoot() {
        return ((this.defaults == null) ? this : getParent());
    }

    public boolean containsKey(String name) {
    	return (getProperty(name)!=null);
    }

    public void set(String name, String value) {
    	setProperty(name,value);
        ints.remove(name);
        doubles.remove(name);
        longs.remove(name);
    }
    public void set(String name, double value) {
        setProperty(name,String.valueOf(value));
        doubles.put(name,value);
    }
    public void set(String name, int value) {
        setProperty(name,String.valueOf(value));
        ints.put(name,value);
    }
    public void set(String name, long value) {
        setProperty(name,String.valueOf(value));
        longs.put(name,value);
    }
    public void set(String name, boolean value) {
        setProperty(name,String.valueOf(value));
        ints.put(name,(value?1:0));
    }

    public String get(String name) {
    	return get(name,null);
    }
    public String get(String name, String defaultValue) {
        String sVal = getProperty(name);
        if(sVal == null)
        {
            logger.fine("Configuration:get("+name+","+defaultValue+")=default "+defaultValue);
            return defaultValue;
        }
        logger.fine("Configuration:get("+name+","+defaultValue+")="+sVal);
        return sVal;
    }

    /**
     * return (int)-1 if no such value exists.
     **/
    public int getInt(String name) {
        return getInt(name,-1);
    }
    public int getInt(String name, int defaultValue) {
        if(!ints.containsKey(name))
        {
            if(!keySet().contains(name) && defaults != null && defaults.containsKey(name))
                return ((Configuration)defaults).getInt(name,defaultValue);

            String sVal = getProperty(name);
            if(sVal == null)
            {
                logger.fine("Configuration:getInt("+name+","+defaultValue+")=default ("+defaultValue+")");
                return defaultValue;
            }
            try
            {
                ints.put(name,Integer.parseInt(sVal));
            }
            catch(NumberFormatException nfe)
            {
                NumberFormatException nfe2 = new NumberFormatException("Failed to parse Configuration key["+name+"] of value["+sVal+"] into an int: "+nfe.getMessage());
                nfe2.setStackTrace(nfe.getStackTrace());
                throw nfe2;
            }
        }
        logger.fine("Configuration:getInt("+name+","+defaultValue+")="+ints.get(name));
        return ints.get(name);
    }

    /**
     * return (int)-1 if no such value exists.
     **/
    public long getLong(String name) {
        return getLong(name,-1);
    }
    public long getLong(String name, long defaultValue) {
        if(!longs.containsKey(name))
        {
            if(!keySet().contains(name) && defaults != null && defaults.containsKey(name))
                return ((Configuration)defaults).getLong(name,defaultValue);

            String sVal = getProperty(name);
            if(sVal == null)
            {
                logger.fine("Configuration:getLong("+name+","+defaultValue+")=default ("+defaultValue+")");
                return defaultValue;
            }
            try
            {
                longs.put(name,Long.parseLong(sVal));
            }
            catch(NumberFormatException nfe)
            {
                NumberFormatException nfe2 = new NumberFormatException("Failed to parse Configuration key["+name+"] of value["+sVal+"] into a long: "+nfe.getMessage());
                nfe2.setStackTrace(nfe.getStackTrace());
                throw nfe2;
            }
        }
        logger.fine("Configuration:getLong("+name+","+defaultValue+")="+longs.get(name));
        return longs.get(name);
    }

    public boolean getBoolean(String name) {
	    return getBoolean(name,false);
    }
    public boolean getBoolean(String name, boolean defaultValue) {
        if(!ints.containsKey(name))
        {
            if(!keySet().contains(name) && defaults != null && defaults.containsKey(name))
                return ((Configuration)defaults).getBoolean(name,defaultValue);

            String sVal = getProperty(name);
            if(sVal == null)
            {
                logger.fine("Configuration:getBoolean("+name+","+defaultValue+")=default ("+defaultValue+")");
                return defaultValue;
            }
            try
            {
                ints.put(name,(Boolean.parseBoolean(sVal)?1:0));
            }
            catch(NumberFormatException nfe)
            {
                NumberFormatException nfe2 = new NumberFormatException("Failed to parse Configuration key["+name+"] of value["+sVal+"] into a boolean: "+nfe.getMessage());
                nfe2.setStackTrace(nfe.getStackTrace());
                throw nfe2;
            }
        }
        logger.fine("Configuration:getBoolean("+name+","+defaultValue+")="+(ints.get(name)==1));
        return (ints.get(name)==1);
    }

    public double getDouble(String name)
	    throws NumberFormatException
    {
	    return getDouble(name,-1);
    }
    public double getDouble(String name, double defaultValue)
	    throws NumberFormatException
    {
        if(!doubles.containsKey(name))
        {
            if(!keySet().contains(name) && defaults != null && defaults.containsKey(name))
                return ((Configuration)defaults).getDouble(name,defaultValue);

            String sVal = getProperty(name);
            if(sVal == null)
            {
                logger.fine("Configuration:getDouble("+name+","+defaultValue+")=default ("+defaultValue+")");
                return defaultValue;
            }
            try
            {
                doubles.put(name,Double.parseDouble(sVal));
            }
            catch(NumberFormatException nfe)
            {
                NumberFormatException nfe2 = new NumberFormatException("Failed to parse Configuration key["+name+"] of value["+sVal+"] into a double: "+nfe.getMessage());
                nfe2.setStackTrace(nfe.getStackTrace());
                throw nfe2;
            }
        }
        logger.fine("Configuration:getDouble("+name+","+defaultValue+")="+doubles.get(name));
        return doubles.get(name);
	}

    public String toString(String prefix) {
        final StringBuilder sb = new StringBuilder(prefix);
        sb.append("Configuration::\n");
        if(defaults != null)
        {
            sb.append(prefix).append("  PARENT::\n").append(prefix).append("  [[\n");
            sb.append(((Configuration)defaults).toString(prefix+"    "));
            sb.append(prefix).append("  ]]\n");
        }
        for(Object obj : keySet())
        {
            String key = (String)obj;
            String val = get(key);
            sb.append(prefix).append("  ").append(key);
            if(ints.containsKey(key)) sb.append("(int)");
            if(longs.containsKey(key)) sb.append("(long)");
            if(doubles.containsKey(key)) sb.append("(double)");
            sb.append('=').append(val).append('\n');
        }
        return sb.toString();
    }
    public String toString() {
        return toString("");
    }
}
