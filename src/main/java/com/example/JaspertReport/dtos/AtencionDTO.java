package com.example.JaspertReport.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AtencionDTO {

	private Long rowId;
	private String codSuc;
	private String codAten;
	private LocalDate fecha;
	private String codPac;
	private String nomPac;
	private String edad;
	private String sexo;
	private String ident;
	private String empresa;
	private String aseguradora;
	private String paremp;
	private String carnet;
	private LocalDate fecOrd;
	private String numPoliza;
	private String hora;
	private String horaRes;
	private String horaTrans;
	private String horaEntre;
	private String nota;
	private String tel;
	private BigDecimal descto;
	private BigDecimal copago;
	private BigDecimal abono;
	private BigDecimal total;
	private BigDecimal desctoProm;
	private String obser;
	private String codMed;
	private String bono;
	private String finCarnet;
	private String recibo;
	private String orden;
	private String caa;
	private String cons;
	private String preFactura;
	private String codFacCop;
	private String codFac;
	private String codRips;
	private String glosa;
	private String preFactAnt;
	private String codFacCopAnt;
	private String codFacAnt;
	private String usuario;
	private Integer anulado;
	private String usuBorr;
	private LocalDate fecBorr;
	private String contabil;
	private String usuConta;
	private String embarazada;
	private String domicilio;
	private String diagPrevio;
	private String codBac;
	private LocalDateTime instante;

	// CAMPOS CALCULADOS POR LA QUERY
	private String nombre; // CONCAT(T2.primnomusu...)
	private String nommed; // Nombre del médico
	private String edadpac; // Edad formateada
	private String sexopac; // Masculino/Femenino
	private String nomemp; // Empresa formateada (t4)
	private LocalDate fechaNace; // fechanace // t1.USUARIO

}
