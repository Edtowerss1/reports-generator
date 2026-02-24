package com.example.JaspertReport.services;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import com.example.JaspertReport.dtos.DatosEmpresaDTO;
import com.example.JaspertReport.dtos.AtencionDTO;
import com.example.JaspertReport.dtos.ResultadoExamenDTO;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

@Service
public class ReportDataService {

    private final JdbcTemplate jdbcTemplate;

    public ReportDataService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    // ------------------------------------------------------------
    // 1) DATOS DE LA EMPRESA (QUERY 1)
    // ------------------------------------------------------------

    public DatosEmpresaDTO getDatosEmpresa() {
        String sql = """
                    SELECT *
                    FROM Datos_Empresa
                """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) -> mapDatosEmpresa(rs));
    }

    private DatosEmpresaDTO mapDatosEmpresa(ResultSet rs) throws SQLException {
        DatosEmpresaDTO dto = new DatosEmpresaDTO();

        dto.setRowId(rs.getLong("Row_Id"));
        dto.setEmpresa(rs.getString("Empresa"));
        dto.setNombre(rs.getString("Nombre"));
        dto.setDireccion1(rs.getString("Direccion1"));
        dto.setDireccion2(rs.getString("Direccion2"));
        dto.setDireccion3(rs.getString("Direccion3"));
        dto.setDireccion4(rs.getString("Direccion4"));
        dto.setNit(rs.getString("Nit"));
        dto.setTelefono1(rs.getString("Telefono1"));
        dto.setTelefono2(rs.getString("Telefono2"));
        dto.setTelefono3(rs.getString("Telefono3"));
        dto.setTelefono4(rs.getString("Telefono4"));
        dto.setCiudad(rs.getString("Ciudad"));
        dto.setWeb(rs.getString("Web"));
        dto.setEmail(rs.getString("Email"));
        dto.setCorte(rs.getDate("Corte").toLocalDate());
        dto.setCodSgSSS(rs.getString("CodSgSSS"));
        dto.setEspSgSSS(rs.getString("EspSgSSS"));
        dto.setNumeroPoliza(rs.getString("Numero_Poliza"));

        return dto;
    }

    // ------------------------------------------------------------
    // 2) DATOS DE LA ATENCIÓN / PACIENTE (QUERY 2)
    // ------------------------------------------------------------

    public AtencionDTO getAtencion(String codAten) {
        String sql = """
                    SELECT t1.*,
                           CONCAT(T2.primnomusu, ' ', T2.segunomusu, ' ', T2.primapelli, ' ', T2.seguapelli) AS nombre,
                           t3.Nommed AS Nommed,
                           CONCAT(TRIM(T2.valoredad), ' ', T2.unimededad) AS edadpac,
                           IF(t2.sexo=1, 'Masculino', 'Femenino') AS sexopac,
                           IF(t4.nomemp IS NULL, '', t4.nomemp) AS nomemp,
                           t2.fechanace
                    FROM Mae_Atencion t1
                    LEFT JOIN mae_pacientes t2 ON t2.NumIdentUs=t1.codpac
                    LEFT JOIN mae_medicos t3 ON t1.codmed=t3.codmed
                    LEFT JOIN mae_empresas t4 ON t1.empresa=t4.codemp
                    WHERE t1.codaten = ?
                """;

        return jdbcTemplate.queryForObject(sql, new Object[] { codAten }, (rs, rowNum) -> mapAtencion(rs));
    }

    private AtencionDTO mapAtencion(ResultSet rs) throws SQLException {
        AtencionDTO dto = new AtencionDTO();

        dto.setRowId(rs.getLong("row_id"));
        dto.setCodSuc(rs.getString("CODSUC"));
        dto.setCodAten(rs.getString("CODATEN"));

        if (rs.getDate("FECHA") != null)
            dto.setFecha(rs.getDate("FECHA").toLocalDate());

        dto.setCodPac(rs.getString("CODPAC"));
        dto.setNomPac(rs.getString("NOMPAC"));
        dto.setEdad(rs.getString("EDAD"));
        dto.setSexo(rs.getString("SEXO"));
        dto.setIdent(rs.getString("IDENT"));
        dto.setEmpresa(rs.getString("EMPRESA"));
        dto.setAseguradora(rs.getString("ASEGURA"));
        dto.setParemp(rs.getString("PAREMP"));
        dto.setCarnet(rs.getString("CARNET"));

        if (rs.getDate("FECORD") != null)
            dto.setFecOrd(rs.getDate("FECORD").toLocalDate());

        dto.setNumPoliza(rs.getString("NUMPOLIZA"));
        dto.setHora(rs.getString("HORA"));
        dto.setHoraRes(rs.getString("HORARES"));
        dto.setHoraTrans(rs.getString("HORATRANS"));
        dto.setHoraEntre(rs.getString("HORAENTRE"));
        dto.setNota(rs.getString("NOTA"));
        dto.setTel(rs.getString("TEL"));
        dto.setDescto(rs.getBigDecimal("DESCTO"));
        dto.setCopago(rs.getBigDecimal("COPAGO"));
        dto.setAbono(rs.getBigDecimal("ABONO"));
        dto.setTotal(rs.getBigDecimal("TOTAL"));
        dto.setDesctoProm(rs.getBigDecimal("DESCTOPROM"));
        dto.setObser(rs.getString("OBSER"));
        dto.setCodMed(rs.getString("CODMED"));
        dto.setBono(rs.getString("BONO"));

        dto.setFinCarnet(rs.getString("FINCARNET"));

        dto.setRecibo(rs.getString("RECIBO"));
        dto.setOrden(rs.getString("ORDEN"));
        dto.setCaa(rs.getString("CAA"));
        dto.setCons(rs.getString("CONS"));
        dto.setPreFactura(rs.getString("PREFACTURA"));
        dto.setCodFacCop(rs.getString("CODFACCOP"));
        dto.setCodFac(rs.getString("CODFAC"));
        dto.setCodRips(rs.getString("CODRIPS"));
        dto.setGlosa(rs.getString("GLOSA"));
        dto.setPreFactAnt(rs.getString("PREFACTANT"));
        dto.setCodFacCopAnt(rs.getString("CODFACCOPANT"));
        dto.setCodFacAnt(rs.getString("CODFACANT"));
        dto.setUsuario(rs.getString("USUARIO"));
        dto.setAnulado(rs.getInt("ANULADO"));
        dto.setUsuBorr(rs.getString("USUBORR"));

        if (rs.getDate("FECBORR") != null)
            dto.setFecBorr(rs.getDate("FECBORR").toLocalDate());

        dto.setContabil(rs.getString("CONTABIL"));
        dto.setUsuConta(rs.getString("USUCONTA"));
        dto.setEmbarazada(rs.getString("EMBARAZADA"));
        dto.setDomicilio(rs.getString("DOMICILIO"));
        dto.setDiagPrevio(rs.getString("DIAGPREVIO"));
        dto.setCodBac(rs.getString("CODBAC"));

        if (rs.getTimestamp("instante") != null)
            dto.setInstante(rs.getTimestamp("instante").toLocalDateTime());

        // CAMPOS CALCULADOS
        dto.setNombre(rs.getString("nombre"));
        dto.setNommed(rs.getString("Nommed"));
        dto.setEdadpac(rs.getString("edadpac"));
        dto.setSexopac(rs.getString("sexopac"));
        dto.setNomemp(rs.getString("nomemp"));

        if (rs.getDate("fechanace") != null)
            dto.setFechaNace(rs.getDate("fechanace").toLocalDate());

        return dto;
    }

    // ------------------------------------------------------------
    // 3) RESULTADOS DE EXÁMENES (QUERY 3)
    // ------------------------------------------------------------

    public List<ResultadoExamenDTO> getResultados(String codAten, String rowIds) {

        String sql = """
                    SELECT t1.Codaten,
                           t1.Codexa,
                           t1.Result,
                           t1.Obsexa,
                           t1.Rescom,
                           t2.nomexa,
                           t2.unidad,
                           IF(JSON_VALUE(t1.multiproposito, '$.h_otroparamref') IS NULL
                                OR JSON_VALUE(t1.multiproposito, '$.h_otroparamref')='',
                                t2.parnor,
                                JSON_UNQUOTE(JSON_EXTRACT(t2.MULTIPARAMNORMAL,
                                CONCAT('$.paramnormal.',
                                JSON_VALUE(t1.multiproposito, '$.h_otroparamref'),
                                '.valor')))) AS parnor,
                           t2.tipores,
                           t2.nota,
                           t2.ImpVarConResultado,
                           IF(t2.tipores>1,
                               CHAR(RIGHT(t1.Row_id, 2)-IF(RIGHT(t1.Row_id, 2)>36, 36, 0)),
                               t1.clasi) AS clasi,
                           t2.AumentaFuente,
                           IF(t9.descla IS NULL, '', t9.descla) AS descla,
                           t2.tecnica,
                           t3.codvar,
                           t3.resultado,
                           t3.grupo,
                           IF(t3.orden IS NULL, t2.nivel, t3.orden) AS orden,
                           t4.desvar,
                           t5.desgru,
                           IF(JSON_VALUE(t1.multiproposito, '$.h_otroparamref') IS NULL
                              OR JSON_VALUE(t1.multiproposito, '$.h_otroparamref')='',
                              t6.param,
                              JSON_UNQUOTE(JSON_EXTRACT(t6.MULTIPARAMNORMAL,
                              CONCAT('$.paramnormal.',
                              JSON_VALUE(t1.multiproposito, '$.h_otroparamref'),
                              '.valor')))) AS param,
                           t1.codbac,
                           t7.firma,
                           t8.firma AS firmav,
                           t1.firmavalidar AS validasino,
                           t8.nombac AS nombacvalida
                    FROM Mov_Atencion t1
                    LEFT JOIN mae_descripexa t2 ON t1.codexa=t2.codexa
                    LEFT JOIN mov_atencion_variable t3 ON t1.codaten=t3.codaten
                        AND t1.codexa=t3.codexa
                    LEFT JOIN mae_variableresulta t4 ON t3.codvar=t4.codvar
                    LEFT JOIN mae_descripexa_resultagru t5 ON t3.codexa=t5.codexa
                        AND t3.grupo=t5.codgru
                    LEFT JOIN mae_descripexa_resultavar t6 ON t3.codexa=t6.codexa
                        AND t3.codvar=t6.codvar
                    LEFT JOIN mae_bacteriologos t7 ON t1.codbac=t7.codbac
                    LEFT JOIN mae_bacteriologos t8 ON t1.codbacvalidar=t8.codbac
                    LEFT JOIN mae_clasifica t9 ON t1.clasi=t9.codcla
                    WHERE t1.codaten = ?
                      AND FIND_IN_SET(t1.row_id, ?) > 0
                      AND IF(t2.ImpVarConResultado=TRUE,
                             (IF(t3.resultado<>'', TRUE, FALSE)), TRUE)
                    ORDER BY clasi, t3.grupo, orden
                """;

        Object[] params = new Object[] { codAten, rowIds };
        return jdbcTemplate.query(sql, params, (rs, rowNum) -> mapResultado(rs));
    }

    private ResultadoExamenDTO mapResultado(ResultSet rs) throws SQLException {
        ResultadoExamenDTO dto = new ResultadoExamenDTO();

        dto.setCodaten(rs.getString("Codaten"));
        dto.setCodexa(rs.getString("Codexa"));
        dto.setResult(rs.getString("Result"));
        dto.setObsexa(rs.getString("Obsexa"));
        dto.setRescom(rs.getString("Rescom"));
        dto.setNomexa(rs.getString("nomexa"));
        dto.setUnidad(rs.getString("unidad"));
        dto.setParnor(rs.getString("parnor"));
        dto.setTipores(rs.getBigDecimal("tipores"));
        dto.setNota(rs.getString("nota"));
        dto.setImpVarConResultado(rs.getBoolean("ImpVarConResultado"));
        dto.setClasi(rs.getString("clasi"));
        dto.setAumentaFuente(rs.getInt("AumentaFuente"));
        dto.setDescla(rs.getString("descla"));
        dto.setTecnica(rs.getString("tecnica"));
        dto.setCodvar(rs.getString("codvar"));
        dto.setResultado(rs.getString("resultado"));
        dto.setGrupo(rs.getString("grupo"));
        dto.setOrden(rs.getString("orden"));
        dto.setDesvar(rs.getString("desvar"));
        dto.setDesgru(rs.getString("desgru"));
        dto.setParam(rs.getString("param"));
        dto.setCodbac(rs.getString("codbac"));

        // Firmas (BLOB → byte[])
        dto.setFirma(rs.getBytes("firma"));
        dto.setFirmav(rs.getBytes("firmav"));

        dto.setValidasino(rs.getBoolean("validasino"));
        dto.setNombacvalida(rs.getString("nombacvalida"));

        return dto;
    }
}
