package com.speedsneakers.productserviceelastic.config;

import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponseInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchClients;

@Configuration
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Value("${elasticsearch.host}")
    private String host;

    @Value("${elasticsearch.user}")
    private String username;

    @Value("${elasticsearch.password}")
    private String password;

    @Override
    public ClientConfiguration clientConfiguration() {

        return ClientConfiguration.builder()
                .connectedTo(host)
                .usingSsl()
                .withBasicAuth(username, password)
                .withClientConfigurer(
                    ElasticsearchClients.ElasticsearchHttpClientConfigurationCallback.from(clientBuilder ->
                        clientBuilder.addInterceptorLast(
                            (HttpRequestInterceptor) (request, context) -> {
                                request.removeHeaders("Content-Type");
                                request.addHeader("Content-Type", "application/json");
                            }
                        )
                        .addInterceptorLast((HttpResponseInterceptor) (response, context) -> {
                            response.addHeader("X-Elastic-Product", "Elasticsearch");
                        })
                    )
                )
                .build();
    }

}
