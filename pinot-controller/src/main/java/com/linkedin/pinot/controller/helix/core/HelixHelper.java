package com.linkedin.pinot.controller.helix.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.helix.HelixAdmin;
import org.apache.helix.HelixDataAccessor;
import org.apache.helix.HelixManager;
import org.apache.helix.HelixProperty;
import org.apache.helix.PropertyKey;
import org.apache.helix.PropertyKey.Builder;
import org.apache.helix.model.ExternalView;
import org.apache.helix.model.HelixConfigScope;
import org.apache.helix.model.HelixConfigScope.ConfigScopeProperty;
import org.apache.helix.model.IdealState;
import org.apache.helix.model.InstanceConfig;
import org.apache.helix.model.builder.HelixConfigScopeBuilder;
import org.apache.log4j.Logger;


public class HelixHelper {
  private static final Logger logger = Logger.getLogger(HelixHelper.class);

  public static void removeInstance(HelixAdmin admin, String clusterName, String instanceName) {
    admin.dropInstance(clusterName, getInstanceConfigFor(clusterName, instanceName, admin));
  }

  public static List<String> getAllInstances(HelixAdmin admin, String clusterName) {
    return admin.getInstancesInCluster(clusterName);
  }

  public static Set<String> getAllInstancesForResource(IdealState state) {
    final Set<String> instances = new HashSet<String>();

    for (final String partition : state.getPartitionSet()) {
      for (final String instance : state.getInstanceSet(partition)) {
        instances.add(instance);
      }
    }
    return instances;
  }

  public static List<ExternalView> getAllExternalViews(HelixAdmin admin, String cluster) {
    final List<String> resources = getAllResources(admin, cluster);
    final List<ExternalView> ret = new ArrayList<ExternalView>();
    for (final String resource : resources) {
      ret.add(getExternalViewForResouce(admin, cluster, resource));
    }
    return ret;
  }

  public static InstanceConfig getInstanceConfigFor(String clusterName, String instanceName, HelixAdmin admin) {
    return admin.getInstanceConfig(clusterName, instanceName);
  }

  public static List<InstanceConfig> getAllInstanceConfigsFromInstanceNameList(String clusterName,
      List<String> instanceNames, HelixAdmin admin) {
    final List<InstanceConfig> configs = new ArrayList<InstanceConfig>();

    for (final String instance : instanceNames) {
      getInstanceConfigFor(clusterName, instance, admin);
    }

    return configs;
  }

  public static List<InstanceConfig> getAllInstanceConfigsFromInstanceNameList(String clusterName,
      Set<String> instanceNames, HelixAdmin admin) {
    final List<InstanceConfig> configs = new ArrayList<InstanceConfig>();

    for (final String instance : instanceNames) {
      configs.add(admin.getInstanceConfig(clusterName, instance));
    }

    return configs;
  }

  public static void toggleInstance(String instanceName, String clusterName, HelixAdmin admin, boolean toggle) {
    admin.enableInstance(clusterName, instanceName, toggle);
  }

  public static void toggleInstancesWithInstanceNameList(List<String> instances, String clusterName, HelixAdmin admin,
      boolean toggle) {
    for (final String instance : instances) {
      toggleInstance(instance, clusterName, admin, toggle);
    }
  }

  public static void toggleInstancesWithPinotInstanceList(List<String> instances, String clusterName, HelixAdmin admin,
      boolean toggle) {
    for (final String instance : instances) {
      toggleInstance(instance, clusterName, admin, toggle);
    }
  }

  public static void toggleInstancesWithInstanceConfigList(List<InstanceConfig> instances, String clusterName,
      HelixAdmin admin, boolean toggle) {
    for (final InstanceConfig instance : instances) {
      toggleInstance(instance.getInstanceName(), clusterName, admin, toggle);
    }
  }

  public static void toggleInstancesWithInstanceNameSet(Set<String> instances, String clusterName, HelixAdmin admin,
      boolean toggle) {
    for (final String instanceName : instances) {
      toggleInstance(instanceName, clusterName, admin, toggle);
    }
  }

  public static Map<String, String> getInstanceConfigsMapFor(String instanceName, String clusterName, HelixAdmin admin) {
    final HelixConfigScope scope = getInstanceScopefor(clusterName, instanceName);
    final List<String> keys = admin.getConfigKeys(scope);
    return admin.getConfig(scope, keys);
  }

  public static HelixConfigScope getInstanceScopefor(String clusterName, String instanceName) {
    return new HelixConfigScopeBuilder(ConfigScopeProperty.PARTICIPANT, clusterName).forParticipant(instanceName)
        .build();
  }

  public static void updateInstannceConfigKeyValue(String key, String value, String instanceName, String clusterName,
      HelixAdmin admin) {
    final Map<String, String> props = new HashMap<String, String>();
    props.put(key, value);

    final HelixConfigScope scope = getInstanceScopefor(clusterName, instanceName);
    admin.setConfig(scope, props);
  }

  public static void updateInstanceConfig(Map<String, String> newConfigs, String clusterName, String instanceName,
      HelixAdmin admin) {
    final HelixConfigScope scope = getInstanceScopefor(clusterName, instanceName);
    admin.setConfig(scope, newConfigs);
  }

  public static boolean instanceExist(String instanceName, String clusterName, HelixAdmin admin) {
    return getAllInstances(admin, clusterName).contains(instanceName);
  }

  public static void deleteInstancePropertyFromHelix(HelixAdmin admin, String clusterName, String instanceName,
      String configKey) {
    final List<String> keys = new ArrayList<String>();
    keys.add(configKey);

    final HelixConfigScope scope = getInstanceScopefor(clusterName, instanceName);
    admin.removeConfig(scope, keys);
  }

  public static void deleteInstancePropertyFromHelix(HelixManager manager, String instanceName, String configKey) {
    final HelixDataAccessor accessor = manager.getHelixDataAccessor();
    final Builder builder = accessor.keyBuilder();
    final PropertyKey key = builder.instanceConfig(instanceName);
    final HelixProperty property = accessor.getProperty(key);
    if (property.getRecord().getSimpleFields().containsKey(configKey)) {
      property.getRecord().getSimpleFields().remove(configKey);
      accessor.setProperty(key, property);
    }
  }

  public static List<String> getAllResources(HelixAdmin admin, String clusterName) {
    return admin.getResourcesInCluster(clusterName);
  }

  public static HelixConfigScope getResourceScopeFor(String clusterName, String resourceName) {
    return new HelixConfigScopeBuilder(ConfigScopeProperty.RESOURCE, clusterName).forResource(resourceName).build();
  }

  public static boolean resourceExist(String resourceName, String clusterName, HelixAdmin admin) {
    return getAllResources(admin, resourceName).contains(resourceName);
  }

  public static Map<String, String> getResourceConfigsFor(String clusterName, String resourceName, HelixAdmin admin) {
    final HelixConfigScope scope = getResourceScopeFor(clusterName, resourceName);
    final List<String> keys = admin.getConfigKeys(scope);
    return admin.getConfig(scope, keys);
  }

  public static void updateResourceConfigsFor(Map<String, String> newConfigs, String resourceName, String clusterName,
      HelixAdmin admin) {
    final HelixConfigScope scope = getResourceScopeFor(clusterName, resourceName);
    admin.setConfig(scope, newConfigs);
  }

  public static void updateResourceConfigKeyValue(String key, String value, String resourceName, String clusterName,
      HelixAdmin admin) {
    final Map<String, String> props = new HashMap<String, String>();
    props.put(key, value);

    final HelixConfigScope scope = getResourceScopeFor(clusterName, resourceName);
    admin.setConfig(scope, props);
  }

  public static boolean updateResourceIdealState(HelixManager manager, String resourceName, IdealState state) {

    final HelixDataAccessor accessor = manager.getHelixDataAccessor();
    final Builder builder = accessor.keyBuilder();
    final PropertyKey key = builder.idealStates(resourceName);
    accessor.setProperty(key, state);

    return true;
  }

  public static void deleteResourcePropertyFromHelix(HelixAdmin admin, String clusterName, String resourceName,
      String configKey) {
    final List<String> keys = new ArrayList<String>();
    keys.add(configKey);

    final HelixConfigScope scope = getResourceScopeFor(clusterName, resourceName);
    admin.removeConfig(scope, keys);
  }

  public static void deleteResourcePropertyFromHelix(HelixManager manager, String resourceName, String configKey) {
    final HelixDataAccessor accessor = manager.getHelixDataAccessor();
    final Builder builder = accessor.keyBuilder();
    final PropertyKey key = builder.resourceConfig(resourceName);
    final HelixProperty property = accessor.getProperty(key);
    if (property.getRecord().getSimpleFields().containsKey(configKey)) {
      property.getRecord().getSimpleFields().remove(configKey);
      accessor.updateProperty(key, property);
    }
  }

  public static String getPartitionNameFromIdealStateForInstance(IdealState state, String instanceName) {
    final Set<String> partitions = state.getPartitionSet();
    for (final String partition : partitions) {
      final Map<String, String> instanceToStateMapping = state.getInstanceStateMap(partition);
      for (final String ins : instanceToStateMapping.keySet()) {
        if (ins.equals(instanceName)) {
          return partition;
        }
      }
    }
    return null;
  }

  public static String getCurrentStateFromIdealStateForInstance(IdealState state, String instanceName) {
    final Set<String> partitions = state.getPartitionSet();
    for (final String partition : partitions) {
      final Map<String, String> instanceToStateMapping = state.getInstanceStateMap(partition);
      for (final String ins : instanceToStateMapping.keySet()) {
        if (ins.equals(instanceName)) {
          return instanceToStateMapping.get(ins);
        }
      }
    }
    return null;
  }

  public static int getNumReplicaSetFromIdealState(IdealState state) {
    int numReplicaSet = 0;

    for (final String partition : state.getPartitionSet()) {
      if (numReplicaSet < state.getInstanceSet(partition).size()) {
        numReplicaSet = state.getInstanceSet(partition).size();
      }
    }
    return numReplicaSet;
  }

  public static Map<String, Map<String, String>> getReplicaSetsFromIdealState(IdealState state) {
    final Map<String, Map<String, String>> ret = new HashMap<String, Map<String, String>>();

    final Map<String, String> instanceToPartitionMap = new HashMap<String, String>();
    final Set<String> partitions = state.getPartitionSet();
    final int maxInstancesPerPartition = getNumReplicaSetFromIdealState(state);

    for (int i = 0; i < maxInstancesPerPartition; i++) {
      ret.put(String.valueOf(i), new HashMap<String, String>());
    }

    for (final String partition : partitions) {
      final Set<String> instancesWithinAPartition = state.getInstanceSet(partition);
      for (final String instance : instancesWithinAPartition) {
        instanceToPartitionMap.put(instance, partition);
      }
    }

    for (final String replicaSetKey : ret.keySet()) {
      final Map<String, String> replicaSet = ret.get(replicaSetKey);
      for (final String instance : instanceToPartitionMap.keySet()) {
        if (!replicaSet.containsKey(instanceToPartitionMap.get(instance))) {
          replicaSet.put(instanceToPartitionMap.get(instance), instance);
        }
      }

      for (final String p : replicaSet.keySet()) {
        instanceToPartitionMap.remove(replicaSet.get(p));
      }

      ret.put(replicaSetKey, replicaSet);
    }

    return ret;
  }

  public static String getPartitionNameFromExternalViewForResource(ExternalView view, String instanceName) {
    final Set<String> partitions = view.getPartitionSet();
    for (final String partition : partitions) {
      final Map<String, String> instanceToStateMapping = view.getStateMap(partition);
      for (final String ins : instanceToStateMapping.keySet()) {
        if (ins.equals(instanceName)) {
          return partition;
        }
      }
    }
    return null;
  }

  public static String getCurrentStateFromExternalViewForInstance(ExternalView view, String instanceName) {
    final Set<String> partitions = view.getPartitionSet();
    for (final String partition : partitions) {
      final Map<String, String> instanceToStateMapping = view.getStateMap(partition);
      for (final String ins : instanceToStateMapping.keySet()) {
        if (ins.equals(instanceName)) {
          return instanceToStateMapping.get(ins);
        }
      }
    }
    return null;
  }

  public static IdealState getResourceIdealState(HelixManager manager, String resourceName) {
    final HelixDataAccessor accessor = manager.getHelixDataAccessor();
    final Builder builder = accessor.keyBuilder();
    return accessor.getProperty(builder.idealStates(resourceName));
  }

  public static List<ExternalView> getExternalViewForAllResources(HelixAdmin admin, String clusterName) {
    final List<ExternalView> ret = new ArrayList<ExternalView>();
    for (final String resourseName : getAllResources(admin, clusterName)) {
      ret.add(getExternalViewForResouce(admin, clusterName, resourseName));
    }
    return ret;
  }

  public static ExternalView getExternalViewForResouce(HelixAdmin admin, String clusterName, String resourceName) {
    return admin.getResourceExternalView(clusterName, resourceName);
  }
}

// DataUpdater<ZNRecord> updater = new DataUpdater<ZNRecord>()
// {
// private ZNRecord newIdeal;
//
// @Override
// public ZNRecord update(ZNRecord currentData)
// {
// return newIdeal;
// }
// };
// //List<DataUpdater<ZNRecord>> updaters = new ArrayList<DataUpdater<ZNRecord>>(updater);
// //List<String> paths = new ArrayList<String>(key.getPath());
// //accessor.updateChildren(paths, updaters, AccessOption.PERSISTENT);
// //return accessor.updateProperty(key, state);