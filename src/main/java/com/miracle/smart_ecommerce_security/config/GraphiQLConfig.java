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
    <label for="auth-token">X-Auth-Token:</label>
    <input id="auth-token" placeholder="paste token here" style="width:430px" />
    <label for="user-id">X-User-Id:</label>
    <input id="user-id" placeholder="optional user id (UUID)" style="width:260px" />
    <button id="apply-headers">Apply Headers</button>
</div>
<div id="graphiql">Loading...</div>
<script src="https://unpkg.com/react@18.2.0/umd/react.production.min.js" crossorigin></script>
<script src="https://unpkg.com/react-dom@18.2.0/umd/react-dom.production.min.js" crossorigin></script>
<script src="https://unpkg.com/graphiql@3.0.6/graphiql.min.js" crossorigin></script>
<script>
    // Load persisted headers (if any)
    try {
        const savedToken = localStorage.getItem('graphiql_x_auth_token');
        const savedUserId = localStorage.getItem('graphiql_x_user_id');
        if (savedToken) document.getElementById('auth-token').value = savedToken;
        if (savedUserId) document.getElementById('user-id').value = savedUserId;
    } catch (e) { /* ignore localStorage errors */ }

    // Custom fetcher that sends X-Auth-Token and X-User-Id from the toolbar inputs
    async function customFetcher(graphQLParams) {
        const token = document.getElementById('auth-token').value;
        const userId = document.getElementById('user-id').value;
        const headers = {
            'Content-Type': 'application/json'
        };
        if (token && token.trim().length > 0) headers['X-Auth-Token'] = token.trim();
        if (userId && userId.trim().length > 0) headers['X-User-Id'] = userId.trim();

        const resp = await fetch('/graphql', {
            method: 'post',
            headers: headers,
            body: JSON.stringify(graphQLParams),
            credentials: 'same-origin'
        });
        return resp.json();
    }

    const fetcher = GraphiQL.createFetcher({url: '/graphql', fetch: customFetcher});
    const root = ReactDOM.createRoot(document.getElementById('graphiql'));
    const graphiqlElement = React.createElement(GraphiQL, {fetcher: fetcher});
    root.render(graphiqlElement);

    // Apply button persists headers and focuses editor
    document.getElementById('apply-headers').addEventListener('click', function() {
        try {
            localStorage.setItem('graphiql_x_auth_token', document.getElementById('auth-token').value || '');
            localStorage.setItem('graphiql_x_user_id', document.getElementById('user-id').value || '');
        } catch (e) { /* ignore */ }
        // focus the GraphiQL editor
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
