package dev.canhdominich.jsp.mvc;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

@WebServlet("/login")
public class LoginServlet extends HttpServlet {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final UserDao users = new UserDao();

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        showForm(request, response, null, "", HttpServletResponse.SC_OK);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        HttpSession session = request.getSession();
        String expected = (String) session.getAttribute("csrf");
        String submitted = request.getParameter("csrf");
        if (expected == null || submitted == null || !MessageDigest.isEqual(expected.getBytes(), submitted.getBytes())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "invalid form token");
            return;
        }
        String username = request.getParameter("username") == null ? "" : request.getParameter("username").trim();
        Optional<UserDao.User> user = users.authenticate(username, request.getParameter("password") == null ? "" : request.getParameter("password"));
        if (user.isEmpty()) {
            showForm(request, response, "Sai tên đăng nhập hoặc mật khẩu", username, HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        request.changeSessionId();
        session.removeAttribute("csrf");
        session.setAttribute("user", user.get().username());
        session.setAttribute("displayName", user.get().displayName());
        String next = request.getParameter("next");
        response.sendRedirect(next != null && next.startsWith("/") && !next.startsWith("//") ? next : request.getContextPath() + "/dashboard");
    }

    private void showForm(HttpServletRequest request, HttpServletResponse response, String error, String username, int status) throws ServletException, IOException {
        byte[] token = new byte[24];
        RANDOM.nextBytes(token);
        String csrf = Base64.getUrlEncoder().withoutPadding().encodeToString(token);
        request.getSession().setAttribute("csrf", csrf);
        request.setAttribute("csrf", csrf);
        request.setAttribute("error", error);
        request.setAttribute("username", username);
        request.setAttribute("next", request.getParameter("next"));
        response.setStatus(status);
        request.getRequestDispatcher("/WEB-INF/views/login.jsp").forward(request, response);
    }
}
