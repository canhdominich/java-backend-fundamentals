package dev.canhdominich.servlet.filters;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@WebServlet("/app/*")
public class FrontControllerServlet extends HttpServlet {

    interface Handler {
        String handle(HttpServletRequest request);
    }

    private final Map<String, Handler> routes = new LinkedHashMap<>();

    @Override
    public void init() {
        routes.put("/orders", request -> "order list");
        routes.put("/orders/{id}", request -> "order " + request.getAttribute("id"));
        routes.put("/health", request -> "up");
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String path = request.getPathInfo() == null ? "/" : request.getPathInfo();
        response.setContentType("text/plain;charset=UTF-8");
        Handler handler = resolve(path, request);
        if (handler == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            response.getWriter().println("no handler for " + path);
            return;
        }
        response.getWriter().println("front controller routed " + path + " -> " + handler.handle(request));
    }

    private Handler resolve(String path, HttpServletRequest request) {
        for (Map.Entry<String, Handler> route : routes.entrySet()) {
            String pattern = route.getKey();
            if (pattern.equals(path)) {
                return route.getValue();
            }
            int variable = pattern.indexOf("{id}");
            if (variable > 0 && path.startsWith(pattern.substring(0, variable)) && path.length() > variable) {
                request.setAttribute("id", path.substring(variable));
                return route.getValue();
            }
        }
        return null;
    }
}
