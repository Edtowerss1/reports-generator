package com.example.JaspertReport.dtos;

import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoExamenDTO {
	private String codaten;
	private String codexa;
	private String result; // t1.Result (0.52, 0.23, 96.9, etc.)
	private String obsexa;
	private String rescom;
	private String nomexa;
	private String unidad;
	private String parnor;
	private BigDecimal tipores;
	private String nota;
	private Boolean impVarConResultado; // ImpVarConResultado
	private String clasi; // viene como CHAR en la query
	private Integer aumentaFuente;
	private String descla;
	private String tecnica;
	private String codvar;
	private String resultado; // t3.resultado (texto/valor variable)
	private String grupo;
	private String orden;
	private String desvar;
	private String desgru;
	private String param;
	private String codbac;

	// Firmas: normalmente son BLOB en BD. Para Jasper es cómodo como byte[]
	private byte[] firma;
	private byte[] firmav;

	private Boolean validasino; // 0 / 1
	private String nombacvalida;
}
