package com.newrelic.labs;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;

import org.springframework.web.server.ServerWebExchange;

import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Trace;
import com.newrelic.api.agent.weaver.MatchType;
import com.newrelic.api.agent.weaver.Weave;
import com.newrelic.api.agent.weaver.Weaver;

import reactor.core.publisher.Mono;

@Weave(type = MatchType.ExactClass, originalName = "org.springframework.cloud.gateway.handler.FilteringWebHandler")
public abstract class FilteringWebHandler_Instrumentation {

    @Trace(dispatcher = true, async = true)
    public Mono<Void> handle(ServerWebExchange exchange) {
        Token token = NewRelic.getAgent().getTransaction().getToken();
        try {
            final Pattern versionPattern = Pattern.compile("[vV][0-9]{1,}");
            final Pattern idPattern = Pattern.compile("^(?=[^\\s]*?[0-9])[-{}().:_|0-9]+$");
            final Pattern codPattern = Pattern.compile("^(?=[^\\s]*?[0-9])(?=[^\\s]*?[a-zA-Z])(?!\\{id\\}).*$");

            String path = exchange.getRequest().getPath().value();

            String simplifiedPath = path;

            final String[] splitPath = path.split("/");

            if (splitPath.length > 0) {
                simplifiedPath = "";
                for (String p : splitPath) {
                    if (idPattern.matcher(p).matches()) {
                        simplifiedPath = simplifiedPath.concat("/").concat(p.replaceAll(idPattern.toString(), "{id}"));
                    } else if (codPattern.matcher(p).matches() && !versionPattern.matcher(p).matches()) {
                        simplifiedPath = simplifiedPath.concat("/").concat(p.replaceAll(codPattern.toString(), "{cod}"));
                    } else {
                        simplifiedPath = simplifiedPath.concat("/").concat(p);
                    }
                }
            }

            if(simplifiedPath.startsWith("//"))
                simplifiedPath = simplifiedPath.substring(2);

            NewRelic.getAgent().getLogger().log(Level.FINER,
                    "spring-cloud-gateway Instrumentation: Setting web transaction name to " + simplifiedPath);
        } catch (Exception e) {
            System.out.println("ERROR spring-cloud-gateway Instrumentation: " + e.getMessage());
        }

        token.expire();

        return Weaver.callOriginal();
    }


}
