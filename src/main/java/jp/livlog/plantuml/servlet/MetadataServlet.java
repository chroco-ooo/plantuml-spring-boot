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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.code.NoPlantumlCompressionException;
import net.sourceforge.plantuml.code.TranscoderUtil;
import net.sourceforge.plantuml.json.JsonObject;
import net.sourceforge.plantuml.klimt.drawing.svg.SvgGraphics;
import net.sourceforge.plantuml.png.MetadataTag;

/**
 * Meta data servlet for the webapp.
 * This servlet responses with the meta data of a specific file as text report or JSON object.
 */
@MultipartConfig
public class MetadataServlet extends HttpServlet {

    @Override
    public void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException, ServletException {

        request.setCharacterEncoding("UTF-8");
        final var urlString = request.getParameter("src");
        // validate URL
        final var url = ProxyServlet.validateURL(urlString, response);
        if (url == null) {
            return; // error is already set/handled inside `validateURL`
        }
        // fetch image via URL and extract meta data from it
        final var conn = ProxyServlet.getConnection(url);
        try (var is = conn.getInputStream()) {
            this.handleRequest(request, response, is, conn.getContentType(), null);
        }
    }


    @Override
    protected void doPost(
            final HttpServletRequest request,
            final HttpServletResponse response) throws IOException, ServletException {

        request.setCharacterEncoding("UTF-8");
        // get image via file upload
        final var filePart = request.getPart("diagram");
        final var filename = Paths.get(filePart.getSubmittedFileName()).getFileName().toString(); // MS IE fix
        try (var is = filePart.getInputStream()) {
            this.handleRequest(request, response, is, null, filename);
        }
    }


    /**
     * Handle request no matter whether GET or POST and
     * response with the PlantUML diagram image in the in the desired format if possible.
     *
     * @param request       an HttpServletRequest object that contains the request the client has made of the servlet
     * @param response      an HttpServletResponse object that contains the response the servlet sends to the client
     * @param is            PlantUML diagram image as input stream
     * @param contentType   the PlantUML diagram image content type [optional]
     * @param filename      the PlantUML diagram image filename [optional
     *
     * @throws IOException  if an input or output error is detected when the servlet handles the request
     */
    private void handleRequest(
            final HttpServletRequest request,
            final HttpServletResponse response,
            final InputStream is,
            final String contentType,
            final String filename) throws IOException {

        final var formString = request.getParameter("format");
        final var accept = request.getHeader("Accept");
        final var isJsonResponse = accept != null && accept.toLowerCase().contains("json");
        // extract meta data
        // @see <a
        // href="https://github.com/plantuml/plantuml/blob/26874fe610617738f958b7e8d012128fe621cff6/src/net/sourceforge/plantuml/Run.java#L570-L592">PlantUML
        // Code</a>
        final var format = this.getImageFileFormat(formString, contentType, filename, response);
        if (format == null) {
            return; // error is already set/handled inside `getImageFileFormat`
        }
        final var metadata = this.getMetadata(is, format, response);
        if (metadata == null) {
            return; // error is already set/handled inside `getMetadata`
        }
        response.addHeader("Access-Control-Allow-Origin", "*");
        if (isJsonResponse) {
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write(metadata.toJson().toString());
        } else {
            response.setContentType(FileFormat.UTXT.getMimeType());
            response.getWriter().write(metadata.toString());
        }
    }


    /**
     * Get the file format from the PlantUML diagram image.
     *
     * @param format       image format passed by the user via the request param `format`
     * @param contentType  response content type where the PlantUML diagram image is from
     * @param filename     diagram image file name
     * @param response     response object to `sendError` including error message
     *
     * @return  PlantUML diagram image format; if unknown format return `null`
     *
     * @throws IOException  `response.sendError` can result in a `IOException`
     */
    private FileFormat getImageFileFormat(
            final String format, final String contentType, final String filename, final HttpServletResponse response) throws IOException {

        if (format != null && !format.isEmpty()) {
            return this.getImageFileFormatFromFormatString(format, response);
        }
        if (filename != null && !filename.isEmpty()) {
            final var fileFormat = this.getImageFileFormatFromFilenameExtension(filename);
            if (fileFormat != null) {
                return fileFormat;
            }
        }
        if (contentType != null && !contentType.isEmpty()) {
            final var fileFormat = this.getImageFileFormatFromContentType(contentType);
            if (fileFormat != null) {
                return fileFormat;
            }
        }
        response.sendError(
                HttpServletResponse.SC_BAD_REQUEST,
                "PlantUML image format detection failed. Please set \"format\" (format) manually.");
        return null;
    }


    /**
     * Get the file format from the PlantUML diagram image based on a format string.
     *
     * @param format    image format passed by the user via the request param `format`
     * @param response  response object to `sendError` including error message; if `null` no error will be send
     *
     * @return  PlantUML diagram image format; if unknown format return `null`
     *
     * @throws IOException  `response.sendError` can result in a `IOException`
     */
    private FileFormat getImageFileFormatFromFormatString(
            final String format, final HttpServletResponse response) throws IOException {

        switch (format.toLowerCase()) {
            case "png":
                return FileFormat.PNG;
            case "svg":
                return FileFormat.SVG;
            default:
                if (response != null) {
                    response.sendError(
                            HttpServletResponse.SC_BAD_REQUEST,
                            "The format \"" + format + "\" is not supported for meta data extraction.");
                }
                return null;
        }
    }


    /**
     * Get the file format from the PlantUML diagram image based on the filenames extension.
     *
     * @param filename  PlantUML image file name
     *
     * @return  PlantUML diagram image format; if unknown format return `null`
     *
     * @throws IOException  Can not happend! Will not occur.
     */
    private FileFormat getImageFileFormatFromFilenameExtension(final String filename) throws IOException {

        final var extensionPosition = filename.lastIndexOf(".");
        if (extensionPosition != -1) {
            final var extension = filename.substring(extensionPosition + 1);
            return this.getImageFileFormatFromFormatString(extension, null);
        }
        final var logger = Logger.getLogger("com.plantuml");
        logger.log(Level.WARNING, "File name \"{0}\" is malformed. Should be: name.extension", filename);
        return null;
    }


    /**
     * Get the file format from the PlantUML diagram image based on the response content type.
     *
     * @param contentType  response content type where the PlantUML diagram image is from
     *
     * @return  PlantUML diagram image format; if unknown content type return `null`
     */
    private FileFormat getImageFileFormatFromContentType(final String contentType) {

        final var ct = contentType.toLowerCase();
        if (ct.contains("png")) {
            return FileFormat.PNG;
        }
        if (ct.contains("svg") || ct.contains("xml")) {
            return FileFormat.SVG;
        }
        final var logger = Logger.getLogger("com.plantuml");
        logger.log(Level.SEVERE, "Unknown content type \"{0}\" for meta data extraction", contentType);
        return null;
    }


    /**
     * Get meta data from PlantUML diagram image.
     *
     * @param is        PlantUML diagram image input stream
     * @param format    PlantUML diagram image file format
     * @param response  response object to `sendError` including error message
     *
     * @return  parsed meta data; on error return `null`
     *
     * @throws IOException  `response.sendError` can result in a `IOException`
     */
    private Metadata getMetadata(
            final InputStream is, final FileFormat format, final HttpServletResponse response) throws IOException {

        switch (format) {
            case PNG:
                return this.getMetadataFromPNG(is, response);
            case SVG:
                final String svg;
                try (var br = new BufferedReader(new InputStreamReader(is))) {
                    svg = br.lines().collect(Collectors.joining("\n"));
                }
                return this.getMetadataFromSVG(svg, response);
            default:
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Unsupported image format.");
                return null;
        }
    }


    /**
     * Get meta data from PNG PlantUML diagram image.
     *
     * Challenge: PNG meta data is only a single String and contains more than the PlantUML diagram.
     * PNG meta data contains:
     *   1. decoded PlantUML code
     *   2. empty line
     *   3. version information
     * Notes:
     *   - in theory the meta data could contain the PlantUML `RawString` as well as the `PlainString`
     *     but since both are ALWAYS identical (methods to get them are identical), one will ALWAYS dropped.
     *     @see <a href="https://github.com/plantuml/plantuml/blob/26874fe610617738f958b7e8d012128fe621cff6/src/net/sourceforge/plantuml/core/UmlSource.java#L173-L189">PlantUML Code</a>
     *   - version information do not contain any empty lines
     * Solution: split meta data at the last occurring empty line the result in
     *   a. decoded PlantUML diagram
     *   b. version information
     *
     * @param is        PNG image input stream
     * @param response  response object to `sendError` including error message
     *
     * @return  parsed meta data; on error return `null`
     *
     * @throws IOException  `response.sendError` can result in a `IOException`
     */
    private Metadata getMetadataFromPNG(final InputStream is, final HttpServletResponse response) throws IOException {

        final var rawMetadata = new MetadataTag(is, "plantuml").getData();
        if (rawMetadata == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No meta data found.");
            return null;
        }
        // parse meta data
        final var metadata = new Metadata(rawMetadata.trim());
        metadata.decoded = metadata.rawContent.substring(0, metadata.rawContent.lastIndexOf("\n\n"));
        metadata.encoded = TranscoderUtil.getDefaultTranscoder().encode(metadata.decoded);
        metadata.version = metadata.rawContent.substring(rawMetadata.lastIndexOf("\n\n")).trim();
        // add additionally the encoded plantuml string to raw meta data since it's missing by default
        metadata.rawContent = metadata.encoded + "\n\n" + metadata.rawContent;
        return metadata;
    }


    /**
     * Get meta data from SVG PlantUML diagram image.
     * @see <a href="https://github.com/plantuml/plantuml/blob/26874fe610617738f958b7e8d012128fe621cff6/src/net/sourceforge/plantuml/Run.java#L574-L587">PlantUML Code</a>
     *
     * @param svg       PlantUML digram in SVG format
     * @param response  response object to `sendError` including error message
     *
     * @return  parsed meta data; on error return `null`
     *
     * @throws IOException  `response.sendError` can result in a `IOException`
     */
    private Metadata getMetadataFromSVG(final String svg, final HttpServletResponse response) throws IOException {

        final var metadata = new Metadata();
        // search for meta data start token
        final var idx = svg.lastIndexOf(SvgGraphics.META_HEADER);
        if (idx == -1) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "No meta data found.");
            return null;
        }
        // search for meta data end token
        final var part = svg.substring(idx + SvgGraphics.META_HEADER.length());
        final var idxEnd = part.indexOf("]");
        if (idxEnd == -1) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid meta data: No end token found.");
            return null;
        }
        // parse meta data
        metadata.encoded = part.substring(0, idxEnd);
        try {
            metadata.decoded = TranscoderUtil.getDefaultTranscoderProtected().decode(metadata.encoded);
        } catch (final NoPlantumlCompressionException ex) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid meta data: PlantUML diagram is corrupted.");
            return null;
        }
        return metadata;
    }

    /**
     * Helper class to store meta data.
     */
    private static class Metadata {

        public String rawContent;

        public String decoded;

        public String encoded;

        public String version;

        Metadata() {

        }


        Metadata(final String rawMetadataContent) {

            this.rawContent = rawMetadataContent;
        }


        public JsonObject toJson() {

            final var metadata = new JsonObject();
            metadata.add("encoded", this.encoded);
            metadata.add("decoded", this.decoded);
            if (this.version != null && !this.version.isEmpty()) {
                metadata.add("version", this.version);
            }
            return metadata;
        }


        @Override
        public String toString() {

            if (this.rawContent != null && !this.rawContent.isEmpty()) {
                return this.rawContent;
            }
            if (this.version == null || this.version.isEmpty()) {
                return this.encoded + "\n\n" + this.decoded;
            }
            return this.encoded + "\n\n" + this.decoded + "\n\n" + this.version;
        }
    }
}
