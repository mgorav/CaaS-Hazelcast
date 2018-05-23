package com.ingenico.epayments.shared.jpa.eclipselink.customizer;

import org.eclipse.persistence.config.SessionCustomizer;
import org.eclipse.persistence.sessions.Session;
import org.eclipse.persistence.tools.profiler.PerformanceProfiler;

public class EclipseLinkCustomizer implements SessionCustomizer {
    @Override
    public void customize(Session session) throws Exception {
        PerformanceProfiler performanceProfiler = new PerformanceProfiler();
        performanceProfiler.logProfileSummaryByClass();
        performanceProfiler.logProfile();
        session.setProfiler(performanceProfiler);

    }
}