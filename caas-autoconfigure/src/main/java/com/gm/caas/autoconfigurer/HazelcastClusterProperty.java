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
package com.gm.caas.autoconfigurer;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Cluster properties
 */
@Component
@EnableConfigurationProperties
@ConfigurationProperties(prefix = "com.gm.caas")
/**
 * NOTE: cache name, properties will added as:
 * com.gm.cass.cache.config.cache.names[0]=myCache
 * com.gm.cass.cache.config.cache.myCache.type=Near
 * com.gm.cass.cache.config.cache.myCache.cacheStore=FullQualifiedCacheName
 * com.gm.cass.cache.config.cache.myCache.eviction=LRU
 *
 */
@Data
public class HazelcastClusterProperty {
    private String members;

    private String managementCenterUrl;

    private int maxThreads;

    private List<String> names;

}
