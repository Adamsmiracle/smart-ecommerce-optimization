package com.miracle.smart_ecommerce_security.config;

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
        html, body { height: 100%; margin: 0; padding: 0; }
        /* Fixed toolbar at the top so it remains visible above the GraphiQL UI */
        #graphiql-toolbar { position: fixed; top: 0; left: 0; right: 0; height: 56px; display: flex; align-items: center; gap: 8px; padding: 6px 12px; background: #f6f8fa; border-bottom: 1px solid #e1e4e8; z-index: 9999; }
        #graphiql-toolbar input { padding: 6px 8px; font-size: 14px; }
        #graphiql-toolbar label { font-size: 13px; color: #333; }
        /* Push the GraphiQL container down by toolbar height */
        #graphiql { margin-top: 56px; height: calc(100vh - 56px); }
    </style>
    <link rel="stylesheet" href="https://unpkg.com/graphiql@3.0.6/graphiql.min.css" />
</head>
<body>
<div id="graphiql-toolbar">
    <label for="auth-token">Bearer Token:</label>
    <input id="auth-token" placeholder="paste JWT token here" style="width:500px" />
    <button id="apply-headers">Apply Token</button>
</div>
<div id="graphiql">Loading...</div>
<script src="https://unpkg.com/react@18.2.0/umd/react.production.min.js" crossorigin></script>
<script src="https://unpkg.com/react-dom@18.2.0/umd/react-dom.production.min.js" crossorigin></script>
<script src="https://unpkg.com/graphiql@3.0.6/graphiql.min.js" crossorigin></script>
<script>
    // Load persisted token
    try {
        const savedToken = localStorage.getItem('graphiql_bearer_token');
        if (savedToken) document.getElementById('auth-token').value = savedToken;
    } catch (e) { /* ignore */ }

    // Custom fetcher that sends Authorization: Bearer <token>
    function customFetcher(graphQLParams, opts) {
        const token = document.getElementById('auth-token').value;
        const headers = {
            'Content-Type': 'application/json',
            'Accept': 'application/json'
        };
        if (token && token.trim().length > 0) {
            headers['Authorization'] = 'Bearer ' + token.trim();
        }

        return fetch('/graphql', {
            method: 'POST',
            headers: headers,
            body: JSON.stringify(graphQLParams),
            credentials: 'same-origin'
        }).then(resp => resp.json());
    }

    const root = ReactDOM.createRoot(document.getElementById('graphiql'));
    const graphiqlElement = React.createElement(GraphiQL, {fetcher: customFetcher});
    root.render(graphiqlElement);

    // Apply button persists token and focuses editor
    document.getElementById('apply-headers').addEventListener('click', function() {
        try {
            localStorage.setItem('graphiql_bearer_token', document.getElementById('auth-token').value || '');
        } catch (e) { /* ignore */ }
        const textarea = document.querySelector('.graphiql-container textarea');
        if (textarea) textarea.focus();
    });
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
