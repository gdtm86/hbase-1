/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.hadoop.hbase;

import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.classification.InterfaceAudience;
import org.apache.hadoop.classification.InterfaceStability;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.io.util.HeapMemorySizeUtil;
import org.apache.hadoop.hbase.util.VersionInfo;

/**
 * Adds HBase configuration files to a Configuration
 */
@InterfaceAudience.Public
@InterfaceStability.Stable
public class HBaseConfiguration extends Configuration {

  private static final Log LOG = LogFactory.getLog(HBaseConfiguration.class);

  /**
   * Instantinating HBaseConfiguration() is deprecated. Please use
   * HBaseConfiguration#create() to construct a plain Configuration
   */
  @Deprecated
  public HBaseConfiguration() {
    //TODO:replace with private constructor, HBaseConfiguration should not extend Configuration
    super();
    addHbaseResources(this);
    LOG.warn("instantiating HBaseConfiguration() is deprecated. Please use"
        + " HBaseConfiguration#create() to construct a plain Configuration");
  }

  /**
   * Instantiating HBaseConfiguration() is deprecated. Please use
   * HBaseConfiguration#create(conf) to construct a plain Configuration
   */
  @Deprecated
  public HBaseConfiguration(final Configuration c) {
    //TODO:replace with private constructor
    this();
    merge(this, c);
  }

  private static void checkDefaultsVersion(Configuration conf) {
    if (conf.getBoolean("hbase.defaults.for.version.skip", Boolean.FALSE)) return;
    String defaultsVersion = conf.get("hbase.defaults.for.version");
    String thisVersion = VersionInfo.getVersion();
    if (!thisVersion.equals(defaultsVersion)) {
      throw new RuntimeException(
        "hbase-default.xml file seems to be for and old version of HBase (" +
        defaultsVersion + "), this version is " + thisVersion);
    }
  }

  public static Configuration addHbaseResources(Configuration conf) {
    conf.addResource("hbase-default.xml");
    conf.addResource("hbase-site.xml");

    checkDefaultsVersion(conf);
    HeapMemorySizeUtil.checkForClusterFreeMemoryLimit(conf);
    return conf;
  }

  /**
   * Creates a Configuration with HBase resources
   * @return a Configuration with HBase resources
   */
  public static Configuration create() {
    Configuration conf = new Configuration();
    return addHbaseResources(conf);
  }

  /**
   * @param that Configuration to clone.
   * @return a Configuration created with the hbase-*.xml files plus
   * the given configuration.
   */
  public static Configuration create(final Configuration that) {
    Configuration conf = create();
    merge(conf, that);
    return conf;
  }

  /**
   * Merge two configurations.
   * @param destConf the configuration that will be overwritten with items
   *                 from the srcConf
   * @param srcConf the source configuration
   **/
  public static void merge(Configuration destConf, Configuration srcConf) {
    for (Entry<String, String> e : srcConf) {
      destConf.set(e.getKey(), e.getValue());
    }
  }

  /**
   * @return whether to show HBase Configuration in servlet
   */
  public static boolean isShowConfInServlet() {
    boolean isShowConf = false;
    try {
      if (Class.forName("org.apache.hadoop.conf.ConfServlet") != null) {
        isShowConf = true;
      }
    } catch (LinkageError e) {
       // should we handle it more aggressively in addition to log the error?
       LOG.warn("Error thrown: ", e);
    } catch (ClassNotFoundException ce) {
      LOG.debug("ClassNotFound: ConfServlet");
      // ignore
    }
    return isShowConf;
  }

  /**
   * Get the value of the <code>name</code> property as an <code>int</code>, possibly
   * referring to the deprecated name of the configuration property.
   * If no such property exists, the provided default value is returned,
   * or if the specified value is not a valid <code>int</code>,
   * then an error is thrown.
   *
   * @param name property name.
   * @param deprecatedName a deprecatedName for the property to use
   * if non-deprecated name is not used
   * @param defaultValue default value.
   * @throws NumberFormatException when the value is invalid
   * @return property value as an <code>int</code>,
   *         or <code>defaultValue</code>.
   */
  // TODO: developer note: This duplicates the functionality of deprecated
  // property support in Configuration in Hadoop 2. But since Hadoop-1 does not
  // contain these changes, we will do our own as usual. Replace these when H2 is default.
  public static int getInt(Configuration conf, String name,
      String deprecatedName, int defaultValue) {
    if (conf.get(deprecatedName) != null) {
      LOG.warn(String.format("Config option \"%s\" is deprecated. Instead, use \"%s\""
        , deprecatedName, name));
      return conf.getInt(deprecatedName, defaultValue);
    } else {
      return conf.getInt(name, defaultValue);
    }
  }

  /** For debugging.  Dump configurations to system output as xml format.
   * Master and RS configurations can also be dumped using
   * http services. e.g. "curl http://master:16010/dump"
   */
  public static void main(String[] args) throws Exception {
    HBaseConfiguration.create().writeXml(System.out);
  }
}
