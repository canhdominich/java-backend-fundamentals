package dev.canhdominich.servlet.reqresp;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public final class EncodingServlet {

    private EncodingServlet() {
    }

    @WebServlet("/encoding/response-default")
    public static class ResponseDefault extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.getWriter().println("Xin chào Việt Nam");
        }
    }

    @WebServlet("/encoding/response-utf8")
    public static class ResponseUtf8 extends HttpServlet {

        @Override
        protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().println("Xin chào Việt Nam");
        }
    }

    @WebServlet("/encoding/request")
    public static class RequestEncoding extends HttpServlet {

        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
            String forced = request.getHeader("X-Force-Charset");
            boolean late = "late".equals(request.getHeader("X-Order"));
            if (forced != null && !late) {
                request.setCharacterEncoding(forced);
            }
            String ten = request.getParameter("ten");
            if (forced != null && late) {
                request.setCharacterEncoding(forced);
            }
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().println("declared encoding = " + request.getCharacterEncoding());
            response.getWriter().println("ten = " + ten);
        }
    }
}
