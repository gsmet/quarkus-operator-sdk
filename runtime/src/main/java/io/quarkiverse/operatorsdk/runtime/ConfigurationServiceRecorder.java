package io.quarkiverse.operatorsdk.runtime;

import java.util.List;
import java.util.function.Supplier;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.kubernetes.client.KubernetesClient;
import io.javaoperatorsdk.operator.api.config.ConfigurationService;
import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class ConfigurationServiceRecorder {

    public Supplier<QuarkusConfigurationService> configurationServiceSupplier(Version version,
            List<QuarkusControllerConfiguration> configurations,
            boolean validateCustomResources, RunTimeOperatorConfiguration runTimeConfiguration) {
        final var maxThreads = runTimeConfiguration.concurrentReconciliationThreads
                .orElse(ConfigurationService.DEFAULT_RECONCILIATION_THREADS_NUMBER);
        final var timeout = runTimeConfiguration.terminationTimeoutSeconds
                .orElse(ConfigurationService.DEFAULT_TERMINATION_TIMEOUT_SECONDS);

        configurations.forEach(c -> {
            final var extConfig = runTimeConfiguration.controllers.get(c.getName());
            if (extConfig != null) {
                extConfig.finalizer.ifPresent(c::setFinalizer);
                extConfig.namespaces.ifPresent(c::setNamespaces);
                c.setRetryConfiguration(RetryConfigurationResolver.resolve(extConfig.retry));
            }
        });

        return () -> new QuarkusConfigurationService(
                version,
                configurations,
                Arc.container().instance(KubernetesClient.class).get(),
                validateCustomResources, maxThreads, timeout,
                Arc.container().instance(ObjectMapper.class).get());
    }
}
