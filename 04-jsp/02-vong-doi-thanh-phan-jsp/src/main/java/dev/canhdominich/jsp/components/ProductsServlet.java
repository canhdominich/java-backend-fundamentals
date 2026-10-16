package dev.canhdominich.jsp.components;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@WebServlet("/products")
public class ProductsServlet extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        request.setAttribute("products", List.of(
                new Product("KB-01", "Keyboard", new BigDecimal("49.90"), 12),
                new Product("MS-02", "Mouse <b>Pro</b>", new BigDecimal("19.50"), null),
                new Product("HD-03", "Monitor", new BigDecimal("1299.00"), 0)));
        request.getRequestDispatcher("/products.jsp").forward(request, response);
    }
}
