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
import java.net.URL;
import java.util.regex.Pattern;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;

/**
 * Proxy servlet of the webapp.
 * This servlet retrieves the diagram source of a web resource (web html page)
 * and renders it.
 */
public class OldProxyServlet extends HttpServlet {

    /**
     * Proxy request URI regex pattern.
     */
    private static final Pattern PROXY_PATTERN = Pattern.compile("/\\w+/proxy/((\\d+)/)?((\\w+)/)?(https?://.*)");

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {

        final var uri = request.getRequestURI();

        // Check if the src URL is valid
        final var proxyMatcher = OldProxyServlet.PROXY_PATTERN.matcher(uri);
        if (!proxyMatcher.matches()) {
            // Bad URI format.
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "URL malformed.");
            return;
        }

        final var num = proxyMatcher.group(2); // Optional number of the diagram source
        final var format = proxyMatcher.group(4); // Expected format of the generated diagram
        final var sourceURL = proxyMatcher.group(5);
        if (ProxyServlet.forbiddenURL(sourceURL)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Forbidden URL format.");
            return;
        }

        this.handleImageProxy(response, num, format, sourceURL);
    }


    /**
     * Handle image proxy request.
     *
     * @param response http response
     * @param num image number/index of uml {@code source}
     * @param format file format name
     * @param source diagram source URL
     *
     * @throws IOException if an input or output exception occurred
     */
    private void handleImageProxy(
            final HttpServletResponse response,
            final String num,
            final String format,
            final String source) throws IOException {

        final var reader = new SourceStringReader(this.getSource(source));
        final var n = num == null ? 0 : Integer.parseInt(num);

        final var fileFormat = this.getOutputFormat(format);
        response.addHeader("Content-Type", fileFormat.getMimeType());
        reader.outputImage(response.getOutputStream(), n, new FileFormatOption(fileFormat, false));
    }


    /**
     * Get textual diagram source from URL.
     *
     * @param uri diagram source URL
     *
     * @return textual diagram source
     *
     * @throws IOException if an input or output exception occurred
     */
    private String getSource(final String uri) throws IOException {

        final var url = new URL(uri);
        try (
                var responseStream = url.openStream();
                var isr = new InputStreamReader(responseStream);
                var br = new BufferedReader(isr);) {
            String line;
            final var sb = new StringBuilder();
            while ((line = br.readLine()) != null) {
                sb.append(line);
                sb.append("\n");
            }
            return sb.toString().trim();
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
        if ("svg".equals(format)) {
            return FileFormat.SVG;
        }
        if ("eps".equals(format)) {
            return FileFormat.EPS;
        }
        if ("epstext".equals(format)) {
            return FileFormat.EPS_TEXT;
        }
        if ("txt".equals(format)) {
            return FileFormat.ATXT;
        }
        return FileFormat.PNG;
    }

}
