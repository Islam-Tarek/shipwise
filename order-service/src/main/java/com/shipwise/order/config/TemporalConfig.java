package com.shipwise.order.config;

import com.shipwise.order.saga.OrderSagaActivities;
import com.shipwise.order.saga.OrderSagaWorkflow;
import com.shipwise.order.saga.OrderSagaWorkflowImpl;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.opentracingshim.OpenTracingShim;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.opentracing.OpenTracingClientInterceptor;
import io.temporal.opentracing.OpenTracingWorkerInterceptor;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerFactoryOptions;
import io.opentracing.Tracer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TemporalConfig {

    private final OpenTelemetry openTelemetry;

    public TemporalConfig(OpenTelemetry openTelemetry) {
        this.openTelemetry = openTelemetry;
    }

    @Bean
    Tracer openTracingTracer() {
        return OpenTracingShim.createTracerShim(openTelemetry);
    }

    @Bean
    WorkflowServiceStubs workflowServiceStubs(@Value("${temporal.address:localhost:7233}") String address) {
        return WorkflowServiceStubs.newServiceStubs(
                WorkflowServiceStubsOptions.newBuilder().setTarget(address).build());
    }

    @Bean
    WorkflowClient workflowClient(WorkflowServiceStubs stubs, Tracer tracer) {
        WorkflowClientOptions clientOptions = WorkflowClientOptions.newBuilder()
                .setInterceptors(new OpenTracingClientInterceptor(io.temporal.opentracing.OpenTracingOptions.newBuilder().setTracer(tracer).build()))
                .build();
        return WorkflowClient.newInstance(stubs, clientOptions);
    }

    @Bean
    WorkerFactory workerFactory(WorkflowClient workflowClient, OrderSagaActivities activities, Tracer tracer) {
        WorkerFactoryOptions factoryOptions = WorkerFactoryOptions.newBuilder()
                .setWorkerInterceptors(new OpenTracingWorkerInterceptor(io.temporal.opentracing.OpenTracingOptions.newBuilder().setTracer(tracer).build()))
                .build();
        WorkerFactory factory = WorkerFactory.newInstance(workflowClient, factoryOptions);
        Worker worker = factory.newWorker(OrderSagaWorkflow.TASK_QUEUE);
        worker.registerWorkflowImplementationTypes(OrderSagaWorkflowImpl.class);
        worker.registerActivitiesImplementations(activities);
        return factory;
    }
}