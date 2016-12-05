/**
 * Factory.java
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
 * $Id: Factory.java,v 1.8 2007/04/11 21:58:03 sofmac Exp $
 * Part of the open-source Network Learning Toolkit
 *
 * User: smacskassy
 * Date: Dec 1, 2004
 * Time: 7:42:35 AM
 */
package netkit.util;

import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.regex.Pattern;

public class Factory<T> {
    private final Logger logger = NetKitEnv.getLogger(Factory.class.getName());

    private final String name;
    private final static Pattern pattern = Pattern.compile("\\.");

    public Factory(String name) {
        this.name = name;
    }

    public String getClassName(String stem) {
        ResourceBundle bundle = NetKitEnv.getBundle(name);
        if(bundle == null)
        {
          throw new RuntimeException("'"+name+".properties' not found in classpath.");
        }
        
        try { return bundle.getString(stem+".class"); }
        catch(MissingResourceException mre) {}

        String clsName = null;
        try
        {
            clsName = bundle.getString(stem);
        }
        catch(MissingResourceException mre)
        {
            throw new RuntimeException("No definition found for "+stem+" in '"+name+".properties'.  Please check that either '"+stem+"."+name+"' or '"+stem+".class' are present (this is case sensitive).");
        }
        return clsName;
    }

    public Configuration getConfig(String stem) {
        return Configuration.getConfiguration(NetKitEnv.getBundle(name), stem);
    }

    public String[] getValidNames() {
        ResourceBundle bundle = NetKitEnv.getBundle(name);
        if(bundle == null)
            return new String[0];
        Enumeration<String> keys = bundle.getKeys();
        ArrayList<String> names = new ArrayList<String>();
        while(keys.hasMoreElements())
        {
            String key = keys.nextElement();

            // key = name.{class|attrib}
            String[] dd = pattern.split(key);
            if(dd.length == 2 && dd[1].equalsIgnoreCase("class"))
                names.add(dd[0]);
        }
        return names.toArray(new String[0]);
    }

    public T get(String name) {
        return get(name,null);
    }

    public T get(String name, Configuration defaultConf) {
        String clsName = getClassName(name);
        T instance = null;
        if(clsName == null)
            throw new RuntimeException("Could not find class name for '"+name+"' - Resource Bundle (e.g., '"+name+".properties') is probably not in the classpath");
        
        try
        {
            @SuppressWarnings("unchecked")
            Class<T> cls = (Class<T>)Class.forName(clsName);
            instance = cls.newInstance();
            if(instance instanceof Configurable)
            {
                Configuration config = getConfig(name);
                config.setParent(((Configurable)instance).getDefaultConfiguration());
                if(defaultConf == null)
                    ((Configurable)instance).configure(config);
                else
                {
                    Configuration root = defaultConf.getRoot();
                    root.setParent(config);
                    ((Configurable)instance).configure(defaultConf);
                    root.setParent(null);
                }
            }
        }
        catch(Exception ex)
        {
            logger.log(Level.WARNING,"Failed to instantiate("+name+")["+ex+"]",ex);
            throw new RuntimeException(ex.getMessage(),ex);
        }

        return instance;
    }
}
