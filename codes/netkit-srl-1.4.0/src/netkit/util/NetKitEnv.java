/**
 * NetKitEnv.java
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
 * $Id: NetKitEnv.java,v 1.3 2007/03/26 23:45:07 sofmac Exp $
 * Part of the open-source Network Learning Toolkit
 *
 * User: smacskassy
 * Date: Jan 17, 2005
 * Time: 6:22:56 PM
 */
package netkit.util;

import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.Enumeration;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

public final class NetKitEnv {
    public final static String newline;
    private final static Logger logger;
    private final static String name = "NetKit";
    private final static String cp;
    private static long starttime = -1;
    private static Properties loggingProperties = null;
    public static final PrintWriter systemOut = new PrintWriter(System.out, true);
    private static PrintWriter stdOut = new PrintWriter(System.out, true);

    static {
        newline = System.getProperty("line.separator");
        String delim = System.getProperty("path.separator");
        cp = System.getProperty("java.class.path");
        String logprop = "logging.properties";
        for(String pe : cp.split(delim)) {
            // System.out.println("Class-path token: "+pe);
            File f = new File(pe);
            File l = new File(f.getParentFile(),logprop);
            if(l.exists())
            {
                logprop = l.toString();
            }
        }
        loggingProperties = new Properties();
        System.setProperty("java.util.logging.config.file",logprop);
        try {
          loggingProperties.load(new FileReader(logprop));
        } catch (FileNotFoundException e) {
          System.err.println("Could not read logging.properties file ("+logprop+")!");
          e.printStackTrace();
        } catch (IOException e) {
          System.err.println("Error when reading logging.properties file ("+logprop+")!");
          e.printStackTrace();
        }        
        logger = getLogger(NetKitEnv.class.getName());
    }
    
    public static void setLogfile(String filename) {
      LogManager mgr = LogManager.getLogManager();
      loggingProperties.setProperty("java.util.logging.FileHandler.pattern", filename);
      StringWriter sw = new StringWriter();
      try {
        loggingProperties.store(sw, "Logging Properties");
        sw.close();
      } catch (IOException e) {
        System.err.println("Error writing logging properties to StringWriter");
        e.printStackTrace();
      }

      try {
        InputStream is = new ByteArrayInputStream(sw.toString().getBytes());
        mgr.readConfiguration(is);
      } catch (Exception e) {
        System.err.println("Could not read new logging properties from string: "+sw.toString());
        e.printStackTrace();
      }
      String p = mgr.getProperty("java.util.logging.FileHandler.pattern");
      logger.info("Set new log file to '"+p+"'");
    }
    
    public static void printLoggers() {
      LogManager mgr = LogManager.getLogManager();
      Enumeration<String> e = mgr.getLoggerNames();
      while(e.hasMoreElements())
      {
        String s = e.nextElement();
        Logger l = Logger.getLogger(s);
        Logger p = l;

        java.util.logging.Level lvl = l.getLevel();
        java.util.logging.Level wlvl = lvl;
        while(p != null && wlvl == null) {
          p = p.getParent();
          if(p!=null)
            wlvl = p.getLevel();
        }
        System.out.println("valid logger: "+s+" level="+l.getLevel()+" workingLevel="+wlvl);
      }
      
    }

    private static java.util.logging.Level getWorkingLevel(Logger l) {
      Logger p = l;
      while(p.getLevel() == null && p.getParent() != null)
        p = p.getParent();
      return ( p == null ) ? null : p.getLevel();
    }
    
    private static ResourceBundle getBundle() {
        return getBundle(name);
    }

    public static ResourceBundle getBundle(String name) {
        try
        {
            return ResourceBundle.getBundle(name);
        }
        catch(MissingResourceException mre)
        {
            logger.warning("Failed to get resources from '"+name+".properties'.  Please verify this file is in a directory in your classpath: "+cp);
        }
        return null;
    }

    public static Logger getLogger(String name) {
        Logger l = Logger.getLogger(name);
        if(logger!=null) logger.finer("getLogger("+name+")="+l+" level="+l.getLevel()+" workingLevel="+getWorkingLevel(l));
        return l;
    }

    public static Logger getLogger(Object obj) {
        return getLogger(obj.getClass().getName());
    }

    public static String get(String key) {
        return get(key, null);
    }
    public static String get(String key, String defValue) {
        try
        {
            return getBundle().getString(key);
        }
        catch(MissingResourceException mre)
        {
            logger.warning("get("+key+","+defValue+") - MissingResourceException: "+mre.getMessage());
        }
        return defValue;
    }

    public static int getInt(String key) {
        return getInt(key, 0);
    }
    public static int getInt(String key, int defValue) {
        try
        {
            return Integer.parseInt(getBundle().getString(key));
        }
        catch(MissingResourceException mre)
        {
            logger.warning("getInt("+key+","+defValue+") - MissingResourceException: "+mre.getMessage());
        }
        return defValue;
    }

    public static double getLong(String key) {
        return getLong(key, 0L);
    }
    public static double getLong(String key, long defValue) {
        try
        {
            return Long.parseLong(getBundle().getString(key));
        }
        catch(MissingResourceException mre)
        {
            logger.warning("getLong("+key+","+defValue+") - MissingResourceException: "+mre.getMessage());
        }
        return defValue;
    }

    public static double getDouble(String key) {
        return getDouble(key, 0D);
    }
    public static double getDouble(String key, double defValue) {
        try
        {
            return Double.parseDouble(getBundle().getString(key));
        }
        catch(MissingResourceException mre)
        {
            logger.warning("getDouble("+key+","+defValue+") - MissingResourceException: "+mre.getMessage());
        }
        return defValue;
    }

    public static float getFloat(String key) {
        return getFloat(key, 0F);
    }
    public static float getFloat(String key, float defValue) {
        try
        {
            return Float.parseFloat(getBundle().getString(key));
        }
        catch(MissingResourceException mre)
        {
            logger.warning("getFloat("+key+","+defValue+") - MissingResourceException: "+mre.getMessage());
        }
        return defValue;
    }

    public static boolean getBoolean(String key) {
        return getBoolean(key, false);
    }
    public static boolean getBoolean(String key, boolean defValue) {
        try
        {
            return Boolean.parseBoolean(getBundle().getString(key));
        }
        catch(MissingResourceException mre)
        {
            logger.warning("getBoolean("+key+","+defValue+") - MissingResourceException: "+mre.getMessage());
        }
        return defValue;
    }
    
    public static void resetTime() {
      starttime = System.currentTimeMillis();
    }
    public static void logTime(String message) {
      if(starttime == -1)
        resetTime();
      long diff = (System.currentTimeMillis() - starttime)/1000;
      
      logger.info("Time-from-start: "+diff+" ["+message+"]");
    }
    
    public static PrintWriter getStdOut() {
      return stdOut;
    }
    
    public static void setStdOut(final PrintWriter pw) {
      stdOut = (pw == null) ? stdOut : pw;
    }
    
    public static PrintWriter getPrintWriter(final String s) {   
      if(s == null)
        return stdOut;
      else if(s.equals("-"))
        return systemOut;
      
      try {
        return new PrintWriter(new FileWriter(s), true);
      } catch(IOException ioe) {
        throw new IllegalArgumentException("Could not open file "+s+": "+ioe.getMessage());
      }
    }
    


}
