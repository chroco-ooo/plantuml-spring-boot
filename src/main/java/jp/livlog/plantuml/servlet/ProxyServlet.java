/* ========================================================================
 * PlantUML : a free UML diagram generator
 * ========================================================================
 *
 * Project Info:  https://plantuml.com
 *
 * This file is part of PlantUML.
 *
 * PlantUML is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PlantUML distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 */
package jp.livlog.plantuml.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.stream.Collectors;

import javax.imageio.IIOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.sourceforge.plantuml.FileFormat;

/**
 * Proxy servlet of the webapp.
 * This servlet retrieves the diagram source of a web resource (web html page)
 * and renders it.
 */
public class ProxyServlet extends HttpServlet {

    public static boolean forbiddenURL(final String full) {

        if ((full == null) || (full.startsWith("https://") == false && full.startsWith("http://") == false) || full.matches("^https?://[-#.0-9:\\[\\]+]+/.*")) {
            return true;
        }
        if (full.matches("^https?://[^.]+/.*")) {
            return true;
        }
        if (full.matches("^https?://[^.]+$")) {
            return true;
        }
        return false;
    }


    /**
     * Validate external URL.
     *
     * @param url URL to validate
     * @param response  response object to `sendError` including error message; if `null` no error will be send
     *
     * @return valid URL; otherwise `null`
     *
     * @throws IOException  `response.sendError` can result in a `IOException`
     */
    public static URL validateURL(final String url, final HttpServletResponse response) throws IOException {

        final URL parsedUrl;
        try {
            parsedUrl = new URL(url);
        } catch (final MalformedURLException mue) {
            if (response != null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "URL malformed.");
            }
            return null;
        }
        // Check if URL is in a forbidden format (e.g. IP-Address)
        if (ProxyServlet.forbiddenURL(url)) {
            if (response != null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Forbidden URL format.");
            }
            return null;
        }
        return parsedUrl;
    }


    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {

        final var fmt = request.getParameter("fmt");
        final var source = request.getParameter("src");
        final var index = request.getParameter("idx");

        final var idx = index == null ? 0 : Integer.parseInt(index);
        final var srcUrl = ProxyServlet.validateURL(source, response);
        if (srcUrl == null) {
            return; // error is already set/handled inside `validateURL`
        }

        // fetch diagram from URL
        final var uml = this.getSource(srcUrl);

        // generate the response
        final var dr = new DiagramResponse(response, this.getOutputFormat(fmt), request);
        try {
            // special handling for the MAP since it's not using "#sendDiagram()" like the other types
            if ("map".equals(fmt)) {
                dr.sendMap(uml, idx);
            } else {
                dr.sendDiagram(uml, idx);
            }
        } catch (final IIOException e) {
            // Browser has closed the connection, so the HTTP OutputStream is closed
            // Silently catch the exception to avoid annoying log
        }
    }


    /**
     * Get textual uml diagram source from URL.
     *
     * @param url source URL
     *
     * @return textual uml diagram source
     *
     * @throws IOException if an input or output exception occurred
     */
    private String getSource(final URL url) throws IOException {

        final var conn = ProxyServlet.getConnection(url);
        try (var br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            return br.lines().collect(Collectors.joining("\n"));
        }
    }


    /**
     * Get {@link FileFormat} instance from string.
     *
     * @param format file format name
     *
     * @return corresponding file format instance,
     *         if {@code format} is null or unknown the default {@link FileFormat#PNG} will be returned
     */
    private FileFormat getOutputFormat(final String format) {

        if (format == null) {
            return FileFormat.PNG;
        }
        switch (format.toLowerCase()) {
            case "png":
                return FileFormat.PNG;
            case "svg":
                return FileFormat.SVG;
            case "eps":
                return FileFormat.EPS;
            case "epstext":
                return FileFormat.EPS_TEXT;
            case "txt":
                return FileFormat.UTXT;
            case "map":
                return FileFormat.UTXT;
            case "pdf":
                return FileFormat.PDF;
            default:
                return FileFormat.PNG;
        }
    }


    /**
     * Get open http connection from URL.
     *
     * @param url URL to open connection
     *
     * @return open http connection
     *
     * @throws IOException if an input or output exception occurred
     */
    public static HttpURLConnection getConnection(final URL url) throws IOException {

        final var conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        final var token = System.getenv("HTTP_AUTHORIZATION");
        if (token != null) {
            conn.setRequestProperty("Authorization", token);
        }
        final var timeoutString = System.getenv("HTTP_PROXY_READ_TIMEOUT");
        var timeout = 10000; // 10 seconds as default
        if (timeoutString != null && timeoutString.matches("^\\d+$")) {
            timeout = Integer.parseInt(timeoutString);
        }
        conn.setReadTimeout(timeout);
        conn.connect();
        return conn;
    }
}
