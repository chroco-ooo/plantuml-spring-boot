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

import javax.imageio.IIOException;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jp.livlog.plantuml.servlet.utility.UmlExtractor;
import jp.livlog.plantuml.servlet.utility.UrlDataExtractor;
import net.sourceforge.plantuml.FileFormat;

/**
 * Common service servlet to produce diagram from compressed UML source contained in the end part of the requested URI.
 */
public abstract class UmlDiagramService extends HttpServlet {

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {

        final var url = request.getRequestURI();
        final var encoded = UrlDataExtractor.getEncodedDiagram(url, "");
        final var idx = UrlDataExtractor.getIndex(url, 0);

        // build the UML source from the compressed request parameter
        final String uml;
        try {
            uml = UmlExtractor.getUmlSource(encoded);
        } catch (final Exception e) {
            e.printStackTrace();
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Bad Request");
            return;
        }

        this.doDiagramResponse(request, response, uml, idx);
    }


    @Override
    public void doPost(final HttpServletRequest request, final HttpServletResponse response) throws IOException {

        final var idx = UrlDataExtractor.getIndex(request.getRequestURI(), 0);

        // read textual diagram source from request body
        final var uml = new StringBuilder();
        try (var in = request.getReader()) {
            String line;
            while ((line = in.readLine()) != null) {
                uml.append(line).append('\n');
            }
        }

        this.doDiagramResponse(request, response, uml.toString(), idx);
    }


    /**
     * Send diagram response.
     *
     * @param request html request
     * @param response html response
     * @param uml textual UML diagram(s) source
     * @param idx diagram index of {@code uml} to send
     *
     * @throws IOException if an input or output exception occurred
     */
    private void doDiagramResponse(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final String uml,
            final int idx) throws IOException {

        // generate the response
        final var dr = new DiagramResponse(response, this.getOutputFormat(), request);
        try {
            dr.sendDiagram(uml, idx);
        } catch (final IIOException e) {
            // Browser has closed the connection, so the HTTP OutputStream is closed
            // Silently catch the exception to avoid annoying log
        }
    }


    /**
     * Gives the wished output format of the diagram. This value is used by the DiagramResponse class.
     *
     * @return the format
     */
    abstract public FileFormat getOutputFormat();

}
