/**
 * Copyright (C) 2017, Ingenico ePayments - https://www.ingenico.com/epayments
 * <p>
 * All rights reserved
 * <p>
 * This software is owned by Ingenico ePayments (hereinafter the Owner).
 * No material from this software owned, operated and controlled by the Owner
 * may be copied, reproduced, republished, uploaded, posted, transmitted, or
 * distributed in any way by any third party without the Owner's explicit
 * written consent. All intellectual and other property rights of this software
 * are held by the Owner. No rights of any kind are licensed or assigned or
 * shall otherwise pass to third parties making use of this software. Said use
 * by third parties shall only be in accordance with applicable license
 * agreements between such party and the Owner. Making, acquiring, or using
 * unauthorized copies of this software or other copyrighted materials may
 * result in disciplinary or legal action as the circumstances may warrant.
 */
package com.ingenico.epayments.acquiring.terminalservice.configuration;

import com.hazelcast.config.Config;
import com.hazelcast.config.ExecutorConfig;
import com.hazelcast.config.ManagementCenterConfig;
import com.hazelcast.config.MapConfig;
import com.ingenico.epayments.acquiring.terminalservice.model.PortableFactoryAware;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.ListableBeanFactory;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.hazelcast.config.EvictionPolicy.NONE;
import static com.hazelcast.spi.ExecutionService.OFFLOADABLE_EXECUTOR;
import static com.ingenico.epayments.acquiring.terminalservice.configuration.TerminalServiceSpringConfiguration.*;
import static com.ingenico.epayments.acquiring.terminalservice.constant.TerminalServiceAppConstants.TERMINAL_SERVICE_APP_CACHE;
import static com.ingenico.epayments.acquiring.terminalservice.constant.TerminalServiceAppConstants.TERMINAL_SERVICE_APP_PROPERTIES_CACHE;
import static java.lang.Runtime.getRuntime;
import static java.net.NetworkInterface.getByInetAddress;
import static org.apache.commons.lang3.StringUtils.join;

/**
 * Hazelcast builder: A simple DSL style Hazelcast configuration builder.
 */
@Slf4j
public class TerminalServiceHazelcastConfigBuilder {

    private static TerminalServiceHazelcastConfigBuilder builder;
    private String commaSepratedClusterMembers = "";
    private String managementCenterUrl;
    private List<String> members;
    private ListableBeanFactory listableBeanFactory;
    private int maxThreads;


    private TerminalServiceHazelcastConfigBuilder() {

    }

    public static TerminalServiceHazelcastConfigBuilder newHazelcastConfigBuilderWith(TerminalServiceAppHazelcastClusterProperty prop) {
        builder = new TerminalServiceHazelcastConfigBuilder();
        builder.commaSepratedClusterMembers = prop.getMembers();
        builder.managementCenterUrl = prop.getManagementCenterUrl();
        builder.maxThreads = prop.getMaxThreads();
        return builder;
    }

    private static boolean checkAddressLocal(String address) {
        if (address.indexOf(':') >= 0) {
            address = StringUtils.split(address, ':')[0];
        }
        try {
            InetAddress inetAddress = InetAddress.getByName(address);
            if (inetAddress.isAnyLocalAddress() || inetAddress.isLoopbackAddress()) {
                return true;
            }

            return getByInetAddress(inetAddress) != null;
        } catch (UnknownHostException ex) {
            log.warn("Address " + address + " is unknown", ex);
            return false;
        } catch (SocketException ex) {
            log.error("I/O error while determining if address " + address + " belong to a network interface", ex);
            return false;
        }
    }

    public TerminalServiceHazelcastConfigBuilder withBeanFactory(ListableBeanFactory listableBeanFactory) {
        this.listableBeanFactory = listableBeanFactory;

        return this;
    }

    private TerminalServiceHazelcastConfigBuilder buildeClusterMemberName() {


        if (commaSepratedClusterMembers == null) {
            members = Collections.singletonList(DEFAULT_MEMBER);
            log.info("Property " + CLUSTER_MEMBERS + " not specified, using default: " + join(members, ", "));
        } else {
            String[] tokens = StringUtils.split(commaSepratedClusterMembers, ',');
            List<String> members = new ArrayList<>(tokens.length);
            for (String token : tokens) {
                members.add(token.trim());
            }
            this.members = members;
            log.info(CLUSTER_MEMBERS + " is set to: " + join(this.members, ", "));
        }

        return builder;
    }

    public Config buildHazelcastConfig() {
        buildeClusterMemberName();
        Config hazelcastConfig = new Config();
        hazelcastConfig.setProperty("hazelcast.rest.enabled", "true");
        hazelcastConfig.setProperty("hazelcast.backpressure.enabled", "true");
        hazelcastConfig.setProperty("hazelcast.jmx", "true");
        hazelcastConfig.setProperty("hazelcast.client.statistics.period.seconds", "5");
        hazelcastConfig.setInstanceName("terminal-service-app-cache-instance");
        hazelcastConfig.addMapConfig(new MapConfig()
                .setName(TERMINAL_SERVICE_APP_CACHE)
                .setEvictionPolicy(NONE)
                .setStatisticsEnabled(true));

        // Offloading will unblock the partition thread allowing the user to profit from much higher throughput.
        // The key will be locked for the time span of the processing in order to not generate a write conflict.
        hazelcastConfig.addExecutorConfig(new ExecutorConfig(OFFLOADABLE_EXECUTOR)
                .setPoolSize(getRuntime().availableProcessors() * 2)
                .setQueueCapacity(maxThreads));

        hazelcastConfig.addMapConfig(new MapConfig()
                .setName(TERMINAL_SERVICE_APP_PROPERTIES_CACHE)
                .setStatisticsEnabled(true)
                .setEvictionPolicy(NONE)
                .setStatisticsEnabled(true));

        if (!org.springframework.util.StringUtils.isEmpty(managementCenterUrl)) {
            ManagementCenterConfig manCenterCfg = new ManagementCenterConfig();
            manCenterCfg.setUpdateInterval(1);
            manCenterCfg.setEnabled(true).setUrl(managementCenterUrl);

            hazelcastConfig.setManagementCenterConfig(manCenterCfg);
        }

        handlePortableObjectConfiguration(hazelcastConfig);


        hazelcastConfig.getGroupConfig().setName(GROUP_NAME);
        hazelcastConfig.getGroupConfig().setPassword("password");
        hazelcastConfig.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(false);
        hazelcastConfig.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(true);
        for (String member : members) {
            if (checkAddressLocal(member)) {
                log.info("Using " + member + " as local address");
                String[] address = StringUtils.split(member, ':');
                if (address.length > 0) {
                    hazelcastConfig.getNetworkConfig().getInterfaces().addInterface(address[0]);
                }
                if (address.length > 1) {
                    try {
                        hazelcastConfig.getNetworkConfig().setPort(Integer.parseInt(address[1]));
                    } catch (NumberFormatException ex) {
                        throw new IllegalStateException("Cannot parse local address port", ex);
                    }
                }
            }
            log.info("Adding " + member + " to the cluster members list");
            hazelcastConfig.getNetworkConfig().getJoin().getTcpIpConfig().addMember(member).setEnabled(true);
        }
        return hazelcastConfig;
    }

    private void handlePortableObjectConfiguration(Config hazelcastConfig) {
        for (PortableFactoryAware portableFactoryAware : listableBeanFactory.getBeansOfType(PortableFactoryAware.class).values()) {

            hazelcastConfig.getSerializationConfig().addPortableFactory(portableFactoryAware.getPortableFactoryId(), portableFactoryAware);
        }
    }
}
