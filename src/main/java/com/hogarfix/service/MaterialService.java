package com.hogarfix.service;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.hogarfix.model.Servicio;
import com.hogarfix.model.ServicioMaterial;
import com.hogarfix.repository.ServicioMaterialRepository;
import com.hogarfix.repository.ServicioMaterialRepository.MaterialResumen;

import lombok.RequiredArgsConstructor;

/** Materiales que el técnico compró para un servicio (se suman al total, con comprobante). */
@Service
@RequiredArgsConstructor
public class MaterialService {

    public static final int MAX_MATERIALES = 8;
    private static final BigDecimal PRECIO_MIN = new BigDecimal("0.50");
    private static final BigDecimal PRECIO_MAX = new BigDecimal("3000");
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    private final ServicioMaterialRepository repository;

    /** Material ya validado, con su imagen reducida, listo para guardar. */
    public record Preparado(String descripcion, BigDecimal precio, byte[] imagen) {}

    /** Valida las filas del formulario y reduce las fotos. Lanza IllegalStateException con un mensaje para el técnico. */
    public List<Preparado> preparar(List<String> descs, List<BigDecimal> precios, List<MultipartFile> fotos) {
        List<Preparado> lista = new ArrayList<>();
        int n = Math.max(descs == null ? 0 : descs.size(), Math.max(precios == null ? 0 : precios.size(), fotos == null ? 0 : fotos.size()));
        for (int i = 0; i < n; i++) {
            String d = descs != null && i < descs.size() && descs.get(i) != null ? descs.get(i).trim() : "";
            BigDecimal p = precios != null && i < precios.size() ? precios.get(i) : null;
            MultipartFile f = fotos != null && i < fotos.size() ? fotos.get(i) : null;
            boolean sinFoto = f == null || f.isEmpty();
            if (d.isEmpty() && p == null && sinFoto) continue; // fila vacía
            if (d.isEmpty()) throw new IllegalStateException("Escribe el nombre de cada material");
            if (d.length() > 120) d = d.substring(0, 120);
            if (p == null || p.compareTo(PRECIO_MIN) < 0 || p.compareTo(PRECIO_MAX) > 0) {
                throw new IllegalStateException("El precio de \"" + d + "\" debe estar entre S/ 0.50 y S/ 3000");
            }
            if (sinFoto) throw new IllegalStateException("Falta la captura o foto del comprobante de \"" + d + "\"");
            lista.add(new Preparado(d, p.setScale(2, java.math.RoundingMode.HALF_UP), reducir(f, d)));
        }
        if (lista.size() > MAX_MATERIALES) {
            throw new IllegalStateException("Máximo " + MAX_MATERIALES + " materiales por servicio");
        }
        return lista;
    }

    /** Convierte la imagen a JPEG de máximo 1400 px (ocupa ~200 KB). */
    private byte[] reducir(MultipartFile f, String nombre) {
        try {
            if (f.getSize() > MAX_BYTES) throw new IllegalStateException("La imagen de \"" + nombre + "\" pesa más de 10 MB");
            BufferedImage origen = ImageIO.read(new ByteArrayInputStream(f.getBytes()));
            if (origen == null) {
                throw new IllegalStateException("El comprobante de \"" + nombre + "\" debe ser una imagen (JPG o PNG)");
            }
            int w = origen.getWidth(), h = origen.getHeight();
            double escala = Math.min(1.0, 1400.0 / Math.max(w, h));
            int nw = Math.max(1, (int) Math.round(w * escala)), nh = Math.max(1, (int) Math.round(h * escala));
            BufferedImage rgb = new BufferedImage(nw, nh, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = rgb.createGraphics();
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, nw, nh);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(origen, 0, 0, nw, nh, null);
            g.dispose();

            ImageWriter writer = ImageIO.getImageWritersByFormatName("jpg").next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.8f);
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            try (MemoryCacheImageOutputStream os = new MemoryCacheImageOutputStream(salida)) {
                writer.setOutput(os);
                writer.write(null, new IIOImage(rgb, null, null), param);
            } finally {
                writer.dispose();
            }
            return salida.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("No se pudo leer la imagen de \"" + nombre + "\"");
        }
    }

    /** Guarda los materiales de un servicio (reemplaza los anteriores, por si se reintenta). */
    @Transactional
    public void guardar(Servicio servicio, List<Preparado> lista) {
        repository.deleteByServicio_IdServicio(servicio.getIdServicio());
        for (Preparado p : lista) {
            ServicioMaterial m = new ServicioMaterial();
            m.setServicio(servicio);
            m.setDescripcion(p.descripcion());
            m.setPrecio(p.precio());
            m.setComprobante(p.imagen());
            repository.save(m);
        }
    }

    @Transactional
    public void borrarDeServicio(Long idServicio) {
        repository.deleteByServicio_IdServicio(idServicio);
    }

    @Transactional(readOnly = true)
    public List<MaterialResumen> listar(Long idServicio) {
        return repository.findByServicio_IdServicioOrderByIdMaterialAsc(idServicio);
    }

    public static BigDecimal total(List<MaterialResumen> lista) {
        return lista.stream().map(MaterialResumen::getPrecio).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static BigDecimal totalPreparados(List<Preparado> lista) {
        return lista.stream().map(Preparado::precio).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional(readOnly = true)
    public Map<Long, List<MaterialResumen>> listarPorServicios(List<Long> ids) {
        return ids.stream().distinct().collect(Collectors.toMap(id -> id, this::listar));
    }

    @Transactional(readOnly = true)
    public Optional<ServicioMaterial> buscar(Long idMaterial) {
        return repository.findById(idMaterial);
    }
}
