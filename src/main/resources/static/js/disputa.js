// Botón "Reportar problema" (cliente y técnico) + aviso del resultado
function reportarProblema(btn) {
  var servicioId = btn.dataset.servicioId;
  var volver = btn.dataset.volver || "cliente";
  var token = btn.dataset.csrf || "";
  var param = btn.dataset.csrfParam || "_csrf";

  var opciones = {
    NO_LLEGO: "El técnico no llegó",
    TRABAJO_INCOMPLETO: "Trabajo incompleto o mal hecho",
    COBRO_INCORRECTO: "Cobro incorrecto",
    PAGO_NO_RECIBIDO: "No recibí el pago",
    MAL_TRATO: "Mal trato",
    OTRO: "Otro",
  };

  function enviar(motivo, descripcion) {
    var f = document.createElement("form");
    f.method = "post";
    f.action = "/disputas/abrir";
    [["servicioId", servicioId], ["motivo", motivo], ["descripcion", descripcion], ["volver", volver], [param, token]]
      .forEach(function (par) {
        var i = document.createElement("input");
        i.type = "hidden"; i.name = par[0]; i.value = par[1];
        f.appendChild(i);
      });
    document.body.appendChild(f);
    f.submit();
  }

  if (!window.Swal) {
    var d = prompt("Cuéntanos qué pasó (mínimo 10 letras):");
    if (d) enviar("OTRO", d);
    return;
  }
  var html = '<select id="disp-motivo" class="swal2-select" style="width:90%;margin:8px auto;">' +
    Object.keys(opciones).map(function (k) { return '<option value="' + k + '">' + opciones[k] + '</option>'; }).join("") +
    '</select><textarea id="disp-desc" class="swal2-textarea" maxlength="600" placeholder="Cuéntanos qué pasó…" style="width:90%;margin:8px auto;"></textarea>';
  Swal.fire({
    title: "Reportar un problema",
    html: html,
    showCancelButton: true,
    confirmButtonText: "Enviar reporte",
    cancelButtonText: "Cancelar",
    confirmButtonColor: "#030213",
    preConfirm: function () {
      var m = document.getElementById("disp-motivo").value;
      var t = document.getElementById("disp-desc").value.trim();
      if (t.length < 10) { Swal.showValidationMessage("Cuéntanos qué pasó (mínimo 10 letras)"); return false; }
      return { m: m, t: t };
    },
  }).then(function (r) { if (r.isConfirmed) enviar(r.value.m, r.value.t); });
}

(function () {
  var q = new URLSearchParams(location.search);
  var r = q.get("disputa");
  if (!r) return;
  function mostrar() {
    if (!window.Swal) return;
    if (r === "ok") {
      Swal.fire({ icon: "success", title: "Reporte enviado", text: "HogarFix lo revisará y te avisará. Mientras tanto el pago queda retenido.", confirmButtonColor: "#030213" });
    } else {
      Swal.fire({ icon: "error", title: "No se pudo enviar", text: q.get("msg") || "Inténtalo de nuevo", confirmButtonColor: "#030213" });
    }
    history.replaceState(null, "", location.pathname);
  }
  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", mostrar); else mostrar();
})();
