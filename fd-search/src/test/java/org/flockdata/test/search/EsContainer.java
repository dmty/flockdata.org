package org.flockdata.test.search;

import com.github.dockerjava.core.command.PullImageResultCallback;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.LazyFuture;

import java.time.Duration;

/**
 * @author mikeh
 * @since 6/05/18
 */
@Configuration
public class EsContainer {

    GenericContainer esContainer;
    private static EsContainer es = null;

    public static EsContainer getInstance() {
        if (es == null) {
            es = new EsContainer();
            es.start();
        }
        return es;
    }

    private Network network() {
        return Network.newNetwork();
    }

    private void start() {
        esContainer = esContainer();
    }

    public GenericContainer esContainer() {
        if (esContainer == null) {
            esContainer = serviceContainer("docker.elastic.co/elasticsearch/elasticsearch:5.6.9", network())
                .withExposedPorts(9200, 9300)
                .withCommand("elasticsearch -E cluster.name=fd-test -E node.master=true -E discovery.type=single-node -E network.host=0.0.0.0 -E xpack.security.enabled=false")
            ;
            esContainer.start();
        }
        return esContainer;
    }

    private GenericContainer serviceContainer(String serviceIdentifier, Network network) {
        return new GenericContainer(forcePullImageFuture(serviceIdentifier))
            .withStartupTimeout(Duration.ofSeconds(30))
            .withLogConsumer(new Slf4jLogConsumer(LoggerFactory.getLogger("🐳 " + serviceIdentifier)))
            .withNetwork(network)
            .withNetworkAliases(serviceIdentifier);
    }

    private LazyFuture<String> forcePullImageFuture(String resolvedDockerImage) {
        return new LazyFuture<String>() {
            @Override
            public String resolve() {
                if (StringUtils.countMatches(resolvedDockerImage, "/") > 1) {
                    DockerClientFactory.instance()
                        .client()
                        .pullImageCmd(resolvedDockerImage)
                        .exec(new PullImageResultCallback())
                        .awaitSuccess();
                }
                return resolvedDockerImage;
            }
        };
    }


}
