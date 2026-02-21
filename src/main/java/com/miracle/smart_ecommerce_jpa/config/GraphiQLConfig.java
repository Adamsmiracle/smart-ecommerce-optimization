package com.miracle.smart_ecommerce_jpa.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.RouterFunctions;
import org.springframework.web.servlet.function.ServerResponse;

/**
 * Serves a custom GraphiQL page that works without the broken explorer plugin.
 */
@Configuration
public class GraphiQLConfig {

    @Bean
    public RouterFunction<ServerResponse> graphiqlRouter() {
        String html = """
<!DOCTYPE html>
<html lang="en">
<head>
    <title>GraphiQL</title>
    <style>
        body { height: 100%; margin: 0; width: 100%; overflow: hidden; }
        #graphiql { height: 100vh; }
    </style>
    <link rel="stylesheet" href="https://unpkg.com/graphiql@3.0.6/graphiql.min.css" />
</head>
<body>
<div id="graphiql">Loading...</div>
<script src="https://unpkg.com/react@18.2.0/umd/react.production.min.js" crossorigin></script>
<script src="https://unpkg.com/react-dom@18.2.0/umd/react-dom.production.min.js" crossorigin></script>
<script src="https://unpkg.com/graphiql@3.0.6/graphiql.min.js" crossorigin></script>
<script>
    const fetcher = GraphiQL.createFetcher({url: '/graphql'});
    const root = ReactDOM.createRoot(document.getElementById('graphiql'));
    root.render(React.createElement(GraphiQL, {fetcher: fetcher}));
</script>
</body>
</html>
""";
        return RouterFunctions.route()
                .GET("/graphiql", request -> ServerResponse.ok()
                        .contentType(MediaType.TEXT_HTML)
                        .body(html))
                .build();
    }
}

