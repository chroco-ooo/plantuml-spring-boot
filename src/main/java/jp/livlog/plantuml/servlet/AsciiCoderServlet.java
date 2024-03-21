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

import java.io.IOException;
import java.util.regex.Pattern;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.livlog.plantuml.servlet.utility.UrlDataExtractor;
import net.sourceforge.plantuml.code.Transcoder;
import net.sourceforge.plantuml.code.TranscoderUtil;

/**
 * ASCII encoder and decoder servlet for the webapp.
 * This servlet encodes the diagram in text format or decodes the compressed diagram string.
 */
public class AsciiCoderServlet extends HttpServlet {

    /**
     * Regex pattern to fetch last part of the URL.
     */
    private static final Pattern URL_PATTERN = Pattern.compile("^.*[^a-zA-Z0-9\\-\\_]([a-zA-Z0-9\\-\\_]+)");

    /**
     * Context path from the servlet mapping URL pattern.
     *
     * @return servlet context path without leading or tailing slash
     */
    protected String getServletContextPath() {

        return "coder";
    }


    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {

        request.setCharacterEncoding("UTF-8");

        final var encodedText = this.getEncodedTextFromUrl(request);

        var text = "";
        try {
            text = this.getTranscoder().decode(encodedText);
        } catch (final Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        }

        response.addHeader("Access-Control-Allow-Origin", "*");
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(text);
    }


    @Override
    protected void doPost(
            final HttpServletRequest request,
            final HttpServletResponse response) throws ServletException, IOException {

        request.setCharacterEncoding("UTF-8");

        // read textual diagram source from request body
        final var uml = new StringBuilder();
        try (var in = request.getReader()) {
            String line;
            while ((line = in.readLine()) != null) {
                uml.append(line).append('\n');
            }
        }

        // encode textual diagram source
        var encoded = "";
        try {
            encoded = this.getTranscoder().encode(uml.toString());
        } catch (final Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            e.printStackTrace();
        }

        response.addHeader("Access-Control-Allow-Origin", "*");
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(encoded);
    }


    /**
     * Get PlantUML transcoder.
     *
     * @return transcoder instance
     */
    protected Transcoder getTranscoder() {

        return TranscoderUtil.getDefaultTranscoder();
    }


    /**
     * Get encoded textual diagram source from URL.
     *
     * @param request http request which contains the source URL
     *
     * @return if successful encoded textual diagram source from URL; otherwise empty string
     *
     * @throws IOException if an input or output exception occurred
     */
    protected String getEncodedTextFromUrl(final HttpServletRequest request) throws IOException {

        // textual diagram source from request URI
        var url = request.getRequestURI();
        final var contextpath = "/" + this.getServletContextPath() + "/";
        if (url.contains(contextpath) && !url.endsWith(contextpath)) {
            final String encoded = UrlDataExtractor.getEncodedDiagram(request.getRequestURI(), "");
            if (!encoded.isEmpty()) {
                return encoded;
            }
        }
        // textual diagram source from "url" parameter
        url = request.getParameter("url");
        if (url != null && !url.trim().isEmpty()) {
            // Catch the last part of the URL if necessary
            final var matcher = AsciiCoderServlet.URL_PATTERN.matcher(url);
            if (matcher.find()) {
                url = matcher.group(1);
            }
            return url;
        }
        // nothing found
        return "";
    }

}
