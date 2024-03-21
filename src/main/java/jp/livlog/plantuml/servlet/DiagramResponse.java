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
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.sourceforge.plantuml.BlockUml;
import net.sourceforge.plantuml.ErrorUml;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.NullOutputStream;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.StringUtils;
import net.sourceforge.plantuml.error.PSystemError;
import net.sourceforge.plantuml.preproc.Defines;
import net.sourceforge.plantuml.security.SecurityProfile;
import net.sourceforge.plantuml.security.SecurityUtils;
import net.sourceforge.plantuml.utils.Base64Coder;
import net.sourceforge.plantuml.version.Version;

/**
 * Delegates the diagram generation from the UML source and the filling of the HTTP response with the diagram in the
 * right format. Its own responsibility is to produce the right HTTP headers.
 */
public class DiagramResponse {

    private static class BlockSelection {

        private final BlockUml block;

        private final int      systemIdx;

        BlockSelection(final BlockUml blk, final int idx) {

            this.block = blk;
            this.systemIdx = idx;
        }
    }

    /**
     * X-Powered-By http header value included in every response by default.
     */
    private static final String        POWERED_BY  = "PlantUML Version " + Version.versionString();

    /**
     * PLANTUML_CONFIG_FILE content.
     */
    private static final List <String> CONFIG      = new ArrayList <>();

    /**
     * Cache/flag to ensure that the `init()` method is called only once.
     */
    private static boolean             initialized = false;

    static {
        DiagramResponse.init();
    }

    /**
     * Response format.
     */
    private final FileFormat          format;

    /**
     * Http request.
     */
    private final HttpServletRequest  request;

    /**
     * Http response.
     */
    private final HttpServletResponse response;

    /**
     * Create new diagram response instance.
     *
     * @param res http response
     * @param fmt target file format
     * @param req http request
     */
    public DiagramResponse(final HttpServletResponse res, final FileFormat fmt, final HttpServletRequest req) {

        this.response = res;
        this.format = fmt;
        this.request = req;
    }


    /**
     * Initialize PlantUML configurations and properties as well as loading the PlantUML config file.
     */
    public static void init() {

        if (DiagramResponse.initialized) {
            return;
        }
        DiagramResponse.initialized = true;
        // set security profile to INTERNET by default
        // NOTE: this property is cached inside PlantUML and cannot be changed after the first call of PlantUML
        System.setProperty("PLANTUML_SECURITY_PROFILE", SecurityProfile.INTERNET.toString());
        if (System.getenv("PLANTUML_SECURITY_PROFILE") != null) {
            System.setProperty("PLANTUML_SECURITY_PROFILE", System.getenv("PLANTUML_SECURITY_PROFILE"));
        }
        // load properties from file
        if (System.getenv("PLANTUML_PROPERTY_FILE") != null) {
            try (var propertyFileReader = new FileReader(System.getenv("PLANTUML_PROPERTY_FILE"))) {
                System.getProperties().load(propertyFileReader);
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
        // load PlantUML config file
        if (System.getenv("PLANTUML_CONFIG_FILE") != null) {
            try (var br = new BufferedReader(new FileReader(System.getenv("PLANTUML_CONFIG_FILE")))) {
                br.lines().forEach(DiagramResponse.CONFIG::add);
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }


    /**
     * Render and send a specific uml diagram.
     *
     * @param uml textual UML diagram(s) source
     * @param idx diagram index of {@code uml} to send
     *
     * @throws IOException if an input or output exception occurred
     */
    public void sendDiagram(final String uml, final int idx) throws IOException {

        this.response.addHeader("Access-Control-Allow-Origin", "*");
        this.response.setContentType(this.getContentType());

        if (idx < 0) {
            this.response.sendError(HttpServletResponse.SC_BAD_REQUEST, String.format("Invalid diagram index: {0}", idx));
            return;
        }
        final var reader = this.getSourceStringReader(uml);
        if (reader == null) {
            this.response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No UML diagram found");
            return;
        }

        if (this.format == FileFormat.BASE64) {
            byte[] imageBytes;
            try (var outstream = new ByteArrayOutputStream()) {
                reader.outputImage(outstream, idx, new FileFormatOption(FileFormat.PNG));
                imageBytes = outstream.toByteArray();
            }
            final var base64 = Base64Coder.encodeLines(imageBytes).replaceAll("\\s", "");
            final var encodedBytes = "data:image/png;base64," + base64;
            this.response.getOutputStream().write(encodedBytes.getBytes());
            return;
        }

        final var blockSelection = this.getOutputBlockSelection(reader, idx);
        if (blockSelection == null) {
            this.response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (this.notModified(blockSelection.block)) {
            this.addHeaderForCache(blockSelection.block);
            this.response.sendError(HttpServletResponse.SC_NOT_MODIFIED);
            return;
        }
        if (StringUtils.isDiagramCacheable(uml)) {
            this.addHeaderForCache(blockSelection.block);
        }
        final var diagram = blockSelection.block.getDiagram();
        if (diagram instanceof PSystemError) {
            this.response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
        diagram.exportDiagram(this.response.getOutputStream(), blockSelection.systemIdx, new FileFormatOption(this.format));
    }


    private BlockSelection getOutputBlockSelection(final SourceStringReader reader, int numImage) {

        if (numImage < 0) {
            return null;
        }

        final Collection <BlockUml> blocks = reader.getBlocks();
        if (blocks.isEmpty()) {
            return null;
        }

        for (final BlockUml b : blocks) {
            final var system = b.getDiagram();
            final var nbInSystem = system.getNbImages();
            if (numImage < nbInSystem) {
                return new BlockSelection(b, numImage);
            }
            numImage -= nbInSystem;
        }

        return null;
    }


    private SourceStringReader getSourceStringReader(String uml) {

        var reader = this.getSourceStringReaderWithConfig(uml);
        if (reader.getBlocks().isEmpty()) {
            uml = "@startuml\n" + uml + "\n@enduml";
            reader = this.getSourceStringReaderWithConfig(uml);
            if (reader.getBlocks().isEmpty()) {
                return null;
            }
        }
        return reader;
    }


    private SourceStringReader getSourceStringReaderWithConfig(final String uml) {

        final var defines = this.getPreProcDefines();
        var reader = new SourceStringReader(defines, uml, DiagramResponse.CONFIG);
        if (!DiagramResponse.CONFIG.isEmpty() && reader.getBlocks().get(0).getDiagram().getWarningOrError() != null) {
            reader = new SourceStringReader(defines, uml);
        }
        return reader;
    }


    /**
     * Get PlantUML preprocessor defines.
     *
     * @return preprocessor defines
     */
    private Defines getPreProcDefines() {

        final Defines defines;
        if (SecurityUtils.getSecurityProfile() == SecurityProfile.UNSECURE) {
            // set dirpath to current dir but keep filename and filenameNoExtension undefined
            defines = Defines.createWithFileName(new java.io.File("dummy.puml"));
            defines.overrideFilename("");
        } else {
            defines = Defines.createEmpty();
        }
        return defines;
    }


    /**
     * Is block uml unmodified?
     *
     * @param blockUml block uml
     *
     * @return true if unmodified; otherwise false
     */
    private boolean notModified(final BlockUml blockUml) {

        final var ifNoneMatch = this.request.getHeader("If-None-Match");
        final var ifModifiedSince = this.request.getDateHeader("If-Modified-Since");
        if (ifModifiedSince != -1 && ifModifiedSince != blockUml.lastModified()) {
            return false;
        }
        final var etag = blockUml.etag();
        if (ifNoneMatch == null) {
            return false;
        }
        return ifNoneMatch.contains(etag);
    }


    /**
     * Produce and send the image map of the uml diagram in HTML format.
     *
     * @param uml textual UML diagram source
     * @param idx diagram index of {@code uml} to send
     *
     * @throws IOException if an input or output exception occurred
     */
    public void sendMap(final String uml, final int idx) throws IOException {

        this.response.addHeader("Access-Control-Allow-Origin", "*");
        this.response.setContentType(this.getContentType());

        if (idx < 0) {
            this.response.sendError(HttpServletResponse.SC_BAD_REQUEST, String.format("Invalid diagram index: {0}", idx));
            return;
        }
        final var reader = this.getSourceStringReader(uml);
        if (reader == null) {
            this.response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No UML diagram found");
            return;
        }
        final var blockSelection = this.getOutputBlockSelection(reader, idx);
        if (blockSelection == null) {
            this.response.sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        if (StringUtils.isDiagramCacheable(uml)) {
            this.addHeaderForCache(blockSelection.block);
        }
        final var diagram = blockSelection.block.getDiagram();
        if (diagram instanceof PSystemError) {
            this.response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
        final var map = diagram.exportDiagram(
                new NullOutputStream(),
                blockSelection.systemIdx,
                new FileFormatOption(FileFormat.PNG, false));
        if (map.containsCMapData()) {
            final var httpOut = this.response.getWriter();
            final var cmap = map.getCMapData("plantuml");
            httpOut.print(cmap);
        }
    }


    /**
     * Check the syntax of the diagram and send a report in TEXT format.
     *
     * @param uml textual UML diagram source
     *
     * @throws IOException if an input or output exception occurred
     */
    public void sendCheck(final String uml) throws IOException {

        this.response.setContentType(this.getContentType());
        final var reader = new SourceStringReader(uml);
        final var desc = reader.outputImage(
                new NullOutputStream(),
                new FileFormatOption(FileFormat.PNG, false));
        final var httpOut = this.response.getWriter();
        httpOut.print(desc.getDescription());
    }


    /**
     * Add default header including cache headers to response.
     *
     * @param blockUml response block uml
     */
    private void addHeaderForCache(final BlockUml blockUml) {

        final var today = System.currentTimeMillis();
        // Add http headers to force the browser to cache the image
        final var maxAge = 3600 * 24 * 5;
        this.response.addDateHeader("Expires", today + 1000L * maxAge);
        this.response.addDateHeader("Date", today);

        this.response.addDateHeader("Last-Modified", blockUml.lastModified());
        this.response.addHeader("Cache-Control", "public, max-age=" + maxAge);
        // response.addHeader("Cache-Control", "max-age=864000");
        this.response.addHeader("Etag", "\"" + blockUml.etag() + "\"");
        final var diagram = blockUml.getDiagram();
        this.response.addHeader("X-PlantUML-Diagram-Description", diagram.getDescription().getDescription());
        if (diagram instanceof PSystemError) {
            final var error = (PSystemError) diagram;
            for (final ErrorUml err : error.getErrorsUml()) {
                this.response.addHeader("X-PlantUML-Diagram-Error", err.getError());
                this.response.addHeader("X-PlantUML-Diagram-Error-Line", "" + err.getLineLocation().getPosition());
            }
        }
        DiagramResponse.addHeaders(this.response);
    }


    /**
     * Add default headers to response.
     *
     * @param response http response
     */
    private static void addHeaders(final HttpServletResponse response) {

        response.addHeader("X-Powered-By", DiagramResponse.POWERED_BY);
        response.addHeader("X-Patreon", "Support us on https://plantuml.com/patreon");
        response.addHeader("X-Donate", "https://plantuml.com/paypal");
    }


    /**
     * Get response content type.
     *
     * @return response content type
     */
    private String getContentType() {

        return this.format.getMimeType();
    }

}
