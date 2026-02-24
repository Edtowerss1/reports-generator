package com.example.JaspertReport.dtos;

import java.time.LocalDate;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor // Genera un constructor sin argumentos
@AllArgsConstructor // Genera un constructor con todos los argumentos
public class DatosEmpresaDTO {

	private Long rowId;
	private String empresa;
	private String nombre;
	private String direccion1;
	private String direccion2;
	private String direccion3;
	private String direccion4;
	private String nit;
	private String telefono1;
	private String telefono2;
	private String telefono3;
	private String telefono4;
	private String ciudad;
	private String web;
	private String email;
	private LocalDate corte;
	private String codSgSSS;
	private String espSgSSS;
	private String numeroPoliza;

}
