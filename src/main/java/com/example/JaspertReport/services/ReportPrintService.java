package com.example.JaspertReport.services;

import com.example.JaspertReport.exceptions.PrinterNotFoundException;
import com.example.JaspertReport.exceptions.ReportPrintException;
import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.export.JRPrintServiceExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimplePrintServiceExporterConfiguration;
import org.springframework.stereotype.Service;

import javax.print.PrintService;
import javax.print.PrintServiceLookup;
import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;

@Slf4j
@Service
public class ReportPrintService {

	public void print(JasperPrint jasperPrint, String printerName, int copies) {
		PrintService printService = resolvePrinter(printerName);

		PrintRequestAttributeSet printRequestAttributes = new HashPrintRequestAttributeSet();
		printRequestAttributes.add(new Copies(copies));

		JRPrintServiceExporter exporter = new JRPrintServiceExporter();
		exporter.setExporterInput(new SimpleExporterInput(jasperPrint));

		SimplePrintServiceExporterConfiguration configuration = new SimplePrintServiceExporterConfiguration();
		configuration.setPrintService(printService);
		configuration.setDisplayPageDialog(false);
		configuration.setDisplayPrintDialog(false);
		configuration.setPrintRequestAttributeSet(printRequestAttributes);

		exporter.setConfiguration(configuration);

		try {
			exporter.exportReport();
			log.info("Reporte enviado a impresora '{}' con {} copia(s)", printService.getName(), copies);
		} catch (JRException e) {
			throw new ReportPrintException("No se pudo enviar el reporte a la impresora", e);
		}
	}

	private PrintService resolvePrinter(String printerName) {
		PrintService[] services = PrintServiceLookup.lookupPrintServices(null, null);
		if (services == null || services.length == 0) {
			throw new PrinterNotFoundException(printerName);
		}

		for (PrintService service : services) {
			if (service.getName().equalsIgnoreCase(printerName)) {
				return service;
			}
		}

		for (PrintService service : services) {
			if (service.getName().toLowerCase().contains(printerName.toLowerCase())) {
				return service;
			}
		}

		throw new PrinterNotFoundException(printerName);
	}
}
