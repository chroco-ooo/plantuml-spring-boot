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
 * Check servlet of the webapp.
 * This servlet checks the syntax of the diagram and send a report in TEXT format.
 */
public class CheckSyntaxServlet extends HttpServlet {

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {

        // build the UML source from the compressed request parameter
        final var url = request.getRequestURI();
        final String uml = UmlExtractor.getUmlSource(UrlDataExtractor.getEncodedDiagram(url, ""));

        // generate the response
        final var dr = new DiagramResponse(response, this.getOutputFormat(), request);
        try {
            dr.sendCheck(uml);
        } catch (final IIOException e) {
            // Browser has closed the connection, do nothing
        }
    }


    /**
     * Gives the wished output format of the diagram.
     * This value is used by the DiagramResponse class.
     *
     * @return the format for check responses
     */
    public FileFormat getOutputFormat() {

        return FileFormat.UTXT;
    }

}
