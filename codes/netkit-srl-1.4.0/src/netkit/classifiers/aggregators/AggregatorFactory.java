/**
 * AggregatorFactory.java
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

package netkit.classifiers.aggregators;

import netkit.util.Factory;
import netkit.util.Configuration;
import netkit.util.Configurable;
import netkit.util.NetKitEnv;
import netkit.graph.Attribute;
import netkit.graph.EdgeType;
import netkit.graph.Type;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * This Factory class is a singleton class which creates Aggregators.
 * It relies on the aggregator.properties file and on reflection to do this job.
 * It is final and provides the logic based on whether an aggregator is by value or general.
 * Any new aggregators should subclass AggregatorImp or AggregatorByValueImp and should then be added to the
 * aggretator.properties file.
 *
 * @author Sofus A. Macskassy (sofmac@gmail.com)
 * @see netkit.classifiers.aggregators.AggregatorByValueImp
 * @see netkit.classifiers.aggregators.AggregatorImp
 * @see netkit.util.Factory
 */
public final class AggregatorFactory extends Factory<Aggregator> {
    // Get a logger, whose settings are set in the logging.properties file
    private final Logger logger = NetKitEnv.getLogger(this);

    // The single instance of this Factor class
    private static AggregatorFactory instance=null;

    //
    private Map<String,List<Type>> accepts = new HashMap<String,List<Type>>();

    /**
     * Getter method to get the singleton AggregatorFactory class.
     * @return the singleton AggregatorFactory class.
     */
    public static AggregatorFactory getInstance() {
        if(instance == null)
            instance = new AggregatorFactory();
        return instance;
    }

    /**
     * This is private as it is a singleton.  It calls the super class constructor with 'aggregator'
     * as the parameter to specify which properties file is the one that sets the parameters for this class.
     */
    private AggregatorFactory() {
        super("aggregator");
    }

    /**
     * Checks to see if the aggregator by the given name is an instance of AggregatorByValue
     *
     * @param name Name of the aggregator whose value to check.  This is a fully specified class name.
     * @return true if the fully named class implements the AggregatorByValue interface
     *
     * @see netkit.classifiers.aggregators.AggregatorByValue
     */
    public boolean isByValue(String name) {
        String clsName = getClassName(name);
        boolean result = false;

        try
        {
            if(clsName != null)
                result = AggregatorByValueImp.class.isAssignableFrom(Class.forName(clsName));
        }
        catch(Exception ex)
        {
            logger.log(Level.WARNING,"Failed 'isByValue' check on aggregator("+name+")",ex);
            throw new RuntimeException(ex.getMessage(),ex);
        }
        return result;
    }

    /**
     * Checks if the fully specified classname is an aggregator that can aggregate a given
     * attribute.  For example, a numeric aggregator such as 'Max value' cannot aggregate on
     * categorical attributes that take on values such as 'blue' and 'red'.
     *
     * @param name The fully specified class name of an aggregator whose capabilities are being checked
     * @param attrib The attribute that we want to aggregate with the named aggregator
     * @return true if the named aggregator can aggregate the given attribute
     */
    public boolean canAggregate(String name, Attribute attrib) {
        logger.finer("AggregatorFactory.canAggregate('"+name+"','"+attrib+"')");

        List<Type> types = accepts.get(name);
        if(types == null)
        {
            types = new ArrayList<Type>();
            accepts.put(name,types);
            Configuration config = getConfig(name);
            if(config.containsKey("accept"))
            {
                String[] s = config.get("accept").split(",");
                for(String t : s)
                {
                    logger.finest("Aggregator '"+name+"' can aggregate attribute type "+Type.valueOf(t.trim()));
                    types.add(Type.valueOf(t.trim()));
                }
            }
        }
        return types.contains(attrib.getType());
    }

    /**
     * Get an instance of the fully named aggregator.  This method is needed to adhere to the
     * Factory API.  However, it makes no sense for this factory and always thorws an IllegalArgumentException.
     *
     * @param name The fully specificed name of an aggregator.
     * @return nothing.  This always throws an exception.
     * @exception java.lang.IllegalArgumentException Is always thrown
     */
    public Aggregator get(String name) {
        throw new IllegalArgumentException("You must specify an edgetype and an attribute value");
    }

    /**
     * Get an instance of the fully named aggregator using a given Configuration map.
     * This method is needed to adhere to the Factory API.  However, it makes no sense
     * for this factory and always thorws an IllegalArgumentException.
     *
     * @param name The fully specificed name of an aggregator.
     * @param defaultConf A default configuration to use
     *
     * @return nothing.  This always throws an exception.
     * @exception java.lang.IllegalArgumentException Is always thrown
     */
    public Aggregator get(String name, Configuration defaultConf) {
        throw new IllegalArgumentException("You must specify an edgetype and an attribute value");
    }

    /**
     * Get an instance of the named general attribute aggregator for the given relation and attribute.
     * Note that this does not verify if the aggregator can in fact aggregate this attribute and will throw
     * an exception if this there are any such problems.  Please use the 'canAggregate' method to check
     * before callings this method.
     *
     * @param name The fully specified name of an aggregator
     * @param edgeType The name of a relationship used to find neighbors
     * @param attribute The attribute to aggregate on
     * @return A new instance of the named aggregator
     *
     * @exception java.lang.RuntimeException If the named aggregator is not found in the aggregator.properties file or if there are any trouble instantiating the aggregator (such as the fact that it cannot aggregate the given attribute)
     *
     * @see netkit.classifiers.aggregators.Aggregator
     * @see netkit.classifiers.aggregators.AggregatorFactory#canAggregate(String, netkit.graph.Attribute)
     */
    public Aggregator get(String name, EdgeType edgeType, Attribute attribute) {
        return get(name,edgeType,attribute,null);
    }

    /**
     * Get an instance of the named general attribute aggregator for the given relation and attribute.
     * Note that this does not verify if the aggregator can in fact aggregate this attribute and will throw
     * an exception if this there are any such problems.  Please use the 'canAggregate' method to check
     * before callings this method.
     *
     * @param name The fully specified name of an aggregator
     * @param edgeType The name of a relationship used to find neighbors
     * @param attribute The attribute to aggregate on
     * @param defaultConf The default configuration to fall back on in case nothing appropriate is found in the 'aggregator.properties' file.
     *
     * @return A new instance of the named aggregator
     *
     * @exception java.lang.RuntimeException If the named aggregator is not found in the aggregator.properties file or if there are any trouble instantiating the aggregator (such as the fact that it cannot aggregate the given attribute)
     *
     * @see netkit.classifiers.aggregators.Aggregator
     * @see netkit.classifiers.aggregators.AggregatorFactory#canAggregate(String, netkit.graph.Attribute)
     */
    public Aggregator get(String name, EdgeType edgeType, Attribute attribute, Configuration defaultConf) {
        String clsName = getClassName(name);
        if(clsName == null)
            throw new RuntimeException("Aggregator["+name+"] not found.");

        Aggregator aggregator = null;
        try
        {
            @SuppressWarnings("unchecked")
            Class<Aggregator> cls = (Class<Aggregator>)Class.forName(clsName);
            Constructor<Aggregator> c = cls.getConstructor(new Class[]{EdgeType.class, Attribute.class});
            aggregator = c.newInstance(new Object[]{edgeType, attribute});
            if(aggregator instanceof Configurable)
            {
                Configuration config = getConfig(name);
                config.setParent(((Configurable)aggregator).getDefaultConfiguration());
                if(defaultConf == null)
                    ((Configurable)aggregator).configure(config);
                else
                {
                    defaultConf.setParent(config);
                    ((Configurable)aggregator).configure(defaultConf);
                }
            }
        }
        catch(Exception ex)
        {
            logger.log(Level.SEVERE,"Failed to instantiate aggregator("+name+"): ",ex);
            throw new RuntimeException(ex.getMessage(),ex);
        }

        return aggregator;
    }

    /**
     * Get an instance of the named attribute aggregator-by-value for the given relation, attribute and value.
     * Note that this does not verify if the aggregator can in fact aggregate this attribute and will throw
     * an exception if this there are any such problems.  Please use the 'canAggregate' method to check
     * before callings this method.
     *
     * @param name The fully specified name of an aggregator (which should be of type AggregatorByValue)
     * @param edgeType The name of a relationship used to find neighbors
     * @param attribute The attribute to aggregate on
     * @param value The double value representing the value of the attribute to aggregate on (e.g., 'aquavit')
     *
     * @return A new instance of the named aggregator
     *
     * @exception java.lang.RuntimeException If the named aggregator is not found in the aggregator.properties file or if there are any trouble instantiating the aggregator (such as the fact that it cannot aggregate the given attribute)
     *
     * @see netkit.classifiers.aggregators.AggregatorByValue
     * @see netkit.classifiers.aggregators.AggregatorFactory#canAggregate(String, netkit.graph.Attribute)
     */
    public Aggregator get(String name, EdgeType edgeType, Attribute attribute, double value) {
        return get(name,edgeType,attribute,value,null);
    }

    /**
     * Get an instance of the named attribute aggregator-by-value for the given relation, attribute and value.
     * Note that this does not verify if the aggregator can in fact aggregate this attribute and will throw
     * an exception if this there are any such problems.  Please use the 'canAggregate' method to check
     * before callings this method.
     *
     * @param name The fully specified name of an aggregator (which should be of type AggregatorByValue)
     * @param edgeType The name of a relationship used to find neighbors
     * @param attribute The attribute to aggregate on
     * @param value The double value representing the value of the attribute to aggregate on (e.g., 'aquavit')
     * @param defaultConf The default configuration to fall back on in case nothing appropriate is found in the 'aggregator.properties' file.
     *
     * @return A new instance of the named aggregator
     *
     * @exception java.lang.RuntimeException If the named aggregator is not found in the aggregator.properties file or if there are any trouble instantiating the aggregator (such as the fact that it cannot aggregate the given attribute)
     *
     * @see netkit.classifiers.aggregators.AggregatorByValue
     * @see netkit.classifiers.aggregators.AggregatorFactory#canAggregate(String, netkit.graph.Attribute)
     */
    public Aggregator get(String name, EdgeType edgeType, Attribute attribute, double value, Configuration defaultConf) {
        String clsName = getClassName(name);
        if(clsName == null)
            throw new RuntimeException("Aggregator["+name+"] not found.");

        Aggregator aggregator = null;
        try
        {
            @SuppressWarnings("unchecked")
            Class<Aggregator> cls = (Class<Aggregator>)Class.forName(clsName);
            Constructor<Aggregator> c = cls.getConstructor(new Class[]{EdgeType.class, Attribute.class, double.class});
            aggregator = c.newInstance(new Object[]{edgeType, attribute, value});
            if(aggregator instanceof Configurable)
            {
                Configuration config = getConfig(name);
                config.setParent(((Configurable)aggregator).getDefaultConfiguration());
                if(defaultConf == null)
                    ((Configurable)aggregator).configure(config);
                else
                {
                    defaultConf.setParent(config);
                    ((Configurable)aggregator).configure(defaultConf);
                }
            }
        }
        catch(Exception ex)
        {
            logger.log(Level.SEVERE,"Failed to instantiate aggregator("+name+"): ",ex);
            throw new RuntimeException(ex.getMessage(),ex);
        }

        return aggregator;
    }

    /**
     * This is not yet supported.
     * @param name
     * @param edgeTypes
     * @param attribute
     * @return
     * @exception java.lang.UnsupportedOperationException is always thrown
     */
    public Aggregator get(String name, EdgeType[] edgeTypes, Attribute attribute) {
        return get(name,edgeTypes,attribute,null);
    }

    /**
     * This is not yet supported.
     * @param name
     * @param edgeTypes
     * @param attribute
     * @return
     * @exception java.lang.UnsupportedOperationException is always thrown
     */
    public Aggregator get(String name, EdgeType[] edgeTypes, Attribute attribute, Configuration defaultConf) {
        throw new UnsupportedOperationException("This method is not yet supported");
    }

    /**
     * This is not yet supported.
     * @param name
     * @param edgeTypes
     * @param attribute
     * @return
     * @exception java.lang.UnsupportedOperationException is always thrown
     */
    public Aggregator get(String name, EdgeType[] edgeTypes, Attribute attribute, double value) {
        return get(name,edgeTypes,attribute,null);
    }

    /**
     * This is not yet supported.
     * @param name
     * @param edgeTypes
     * @param attribute
     * @return
     * @exception java.lang.UnsupportedOperationException is always thrown
     */
    public Aggregator get(String name, EdgeType[] edgeTypes, Attribute attribute, double value, Configuration defaultConf) {
        throw new UnsupportedOperationException("This method is not yet supported");
    }
}
