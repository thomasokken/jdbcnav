import java.io.*;

public class txt2html {
    public static void main(String[] args) {
        System.out.print(
                "<html>\n"
                + "<head>\n"
                + "  <title>" + args[0] + "</title>\n"
                + "<head>\n"
                + "<body>\n"
                + "<h3>" + args[0] + "</h3>\n"
                + "<pre>");
        int c;
        try {
            while ((c = System.in.read()) != -1) {
                switch (c) {
                    case '<': System.out.print("&lt;"); break;
                    case '>': System.out.print("&gt;"); break;
                    case '&': System.out.print("&amp;"); break;
                    default: System.out.write(c);
                }
            }
        } catch (IOException e) {}
        System.out.print(
                "</pre>\n"
                + "<p>\n"
                + "Go <a href=\"index.html\">back</a>.\n"
                + "</body>\n"
                + "</html>\n");
    }
}
