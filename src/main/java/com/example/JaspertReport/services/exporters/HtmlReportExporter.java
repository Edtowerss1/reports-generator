package com.example.JaspertReport.services.exporters;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.HtmlResourceHandler;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleHtmlExporterConfiguration;
import net.sf.jasperreports.export.SimpleHtmlExporterOutput;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class HtmlReportExporter implements ReportExporter {

    @Override
    public String getSupportedFormat() {
        return "HTML";
    }

    @Override
    public String getContentType() {
        return "text/html";
    }

    @Override
    public String getFileExtension() {
        return "html";
    }

    @Override
    public byte[] export(JasperPrint jasperPrint) throws JRException {
        HtmlExporter exporter = new HtmlExporter();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // Handler que intercepta cada imagen y la almacena como base64
        Map<String, String> imageBase64Map = new HashMap<>();
        HtmlResourceHandler imageHandler = new HtmlResourceHandler() {
            @Override
            public void handleResource(String id, byte[] data) {
                imageBase64Map.put(id, Base64.getEncoder().encodeToString(data));
            }

            @Override
            public String getResourcePath(String id) {
                String base64 = imageBase64Map.get(id);
                if (base64 != null) {
                    String mime = id.toLowerCase().endsWith(".png") ? "image/png" : "image/jpeg";
                    return "data:" + mime + ";base64," + base64;
                }
                return id;
            }
        };

        SimpleHtmlExporterOutput htmlOutput = new SimpleHtmlExporterOutput(outputStream);
        htmlOutput.setImageHandler(imageHandler);

        exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
        exporter.setExporterOutput(htmlOutput);
        exporter.setConfiguration(new SimpleHtmlExporterConfiguration());

        exporter.exportReport();

        String html = outputStream.toString(StandardCharsets.UTF_8);
        html = wrapWithResponsiveStyles(html);

        return html.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Envuelve el HTML generado con estilos responsivos que escalan las imágenes
     * y ajustan el contenido al ancho de la pantalla manteniendo proporciones.
     */
    private String wrapWithResponsiveStyles(String html) {
        String responsiveHtml = "<!DOCTYPE html>\n" +
                "<html lang=\"es\">\n" +
                "<head>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                "    <title>Reporte</title>\n" +
                "    <style>\n" +
                "        body {\n" +
                "            background-color: #f5f5f5;\n" +
                "            padding: 20px;\n" +
                "        }\n" +
                "        .report-container {\n" +
                "            max-width: 800px;\n" +
                "            margin: 0 auto;\n" +
                "            background-color: white;\n" +
                "            overflow-x: auto;\n" +
                "            box-shadow: 0 2px 8px rgba(0,0,0,0.1);\n" +
                "        }\n" +
                "        .report-container img {\n" +
                "            max-width: 100%;\n" +
                "            height: auto;\n" +
                "        }\n" +
                "        @media print {\n" +
                "            body {\n" +
                "                background-color: white;\n" +
                "                padding: 0;\n" +
                "            }\n" +
                "            .report-container {\n" +
                "                max-width: 100%;\n" +
                "                box-shadow: none;\n" +
                "                overflow-x: visible;\n" +
                "            }\n" +
                "        }\n" +
                "    </style>\n" +
                "</head>\n" +
                "<body>\n" +
                "    <div class=\"report-container\">\n" +
                html +
                "    </div>\n" +
                "</body>\n" +
                "</html>";
        return responsiveHtml;
    }
}
