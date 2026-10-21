package com.hogarfix.service;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.sql.DataSource;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

/**
 * Respaldo de los datos: genera un archivo SQL (INSERTs de todas las tablas) comprimido en ZIP.
 * La estructura de las tablas la vuelve a crear la aplicación sola al arrancar; este archivo guarda los datos.
 */
@Service
@RequiredArgsConstructor
public class RespaldoService {

    private static final int FILAS_POR_INSERT = 100;

    private final DataSource dataSource;

    public void escribirZip(OutputStream salida) throws IOException, SQLException {
        try (ZipOutputStream zip = new ZipOutputStream(salida, StandardCharsets.UTF_8);
                Connection con = dataSource.getConnection()) {
            zip.putNextEntry(new ZipEntry("respaldo-hogarfix.sql"));
            escribirSql(con, zip);
            zip.closeEntry();
            zip.finish();
        }
    }

    private void escribirSql(Connection con, OutputStream out) throws IOException, SQLException {
        StringBuilder sb = new StringBuilder();
        sb.append("-- Respaldo de HogarFix generado el ").append(LocalDateTime.now()).append('\n');
        sb.append("-- Contiene datos personales y contraseñas cifradas: guárdalo en un lugar seguro.\n");
        sb.append("SET NAMES utf8mb4;\nSET FOREIGN_KEY_CHECKS=0;\n\n");
        volcar(out, sb);

        for (String tabla : tablas(con)) {
            try (Statement st = con.createStatement(); ResultSet rs = st.executeQuery("SELECT * FROM `" + tabla + "`")) {
                ResultSetMetaData md = rs.getMetaData();
                int cols = md.getColumnCount();
                StringBuilder cabecera = new StringBuilder("INSERT INTO `").append(tabla).append("` (");
                for (int i = 1; i <= cols; i++) {
                    if (i > 1) cabecera.append(", ");
                    cabecera.append('`').append(md.getColumnName(i)).append('`');
                }
                cabecera.append(") VALUES\n");

                sb.append("-- Tabla ").append(tabla).append('\n');
                int enLote = 0;
                while (rs.next()) {
                    sb.append(enLote == 0 ? cabecera : ",\n").append('(');
                    for (int i = 1; i <= cols; i++) {
                        if (i > 1) sb.append(", ");
                        sb.append(literal(rs, i, md.getColumnType(i)));
                    }
                    sb.append(')');
                    enLote++;
                    if (enLote == FILAS_POR_INSERT) {
                        sb.append(";\n");
                        enLote = 0;
                        volcar(out, sb);
                    }
                }
                if (enLote > 0) sb.append(";\n");
                sb.append('\n');
                volcar(out, sb);
            }
        }
        sb.append("SET FOREIGN_KEY_CHECKS=1;\n");
        volcar(out, sb);
    }

    private static void volcar(OutputStream out, StringBuilder sb) throws IOException {
        out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        sb.setLength(0);
    }

    private List<String> tablas(Connection con) throws SQLException {
        List<String> lista = new ArrayList<>();
        try (ResultSet rs = con.getMetaData().getTables(con.getCatalog(), null, "%", new String[] { "TABLE" })) {
            while (rs.next()) lista.add(rs.getString("TABLE_NAME"));
        }
        java.util.Collections.sort(lista);
        return lista;
    }

    private String literal(ResultSet rs, int i, int tipo) throws SQLException {
        switch (tipo) {
            case Types.BINARY, Types.VARBINARY, Types.LONGVARBINARY, Types.BLOB -> {
                byte[] b = rs.getBytes(i);
                if (b == null) return "NULL";
                StringBuilder h = new StringBuilder("0x");
                for (byte x : b) h.append(String.format("%02x", x));
                return b.length == 0 ? "''" : h.toString();
            }
            case Types.BIT, Types.BOOLEAN -> {
                Object o = rs.getObject(i);
                if (o == null) return "NULL";
                return rs.getBoolean(i) ? "1" : "0";
            }
            case Types.TINYINT, Types.SMALLINT, Types.INTEGER, Types.BIGINT, Types.DECIMAL, Types.NUMERIC,
                    Types.FLOAT, Types.DOUBLE, Types.REAL -> {
                String s = rs.getString(i);
                return s == null ? "NULL" : s;
            }
            default -> {
                String s = rs.getString(i);
                return s == null ? "NULL" : "'" + escapar(s) + "'";
            }
        }
    }

    private static String escapar(String s) {
        StringBuilder r = new StringBuilder(s.length() + 8);
        for (char c : s.toCharArray()) {
            switch (c) {
                case '\\' -> r.append("\\\\");
                case '\'' -> r.append("\\'");
                case '\n' -> r.append("\\n");
                case '\r' -> r.append("\\r");
                case '\0' -> r.append("\\0");
                case '\u001a' -> r.append("\\Z");
                default -> r.append(c);
            }
        }
        return r.toString();
    }
}
