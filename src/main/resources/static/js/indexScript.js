/*
 * Confirmaciones bonitas (SweetAlert2) para formularios.
 * Uso: <form data-confirm="¿Seguro?" data-confirm-titulo="Título" data-confirm-ok="Sí, hacerlo" ...>
 */
(function () {
  function cargarSwal(cb) {
    if (window.Swal) return cb();
    var s = document.createElement("script");
    s.src = "https://cdn.jsdelivr.net/npm/sweetalert2@11";
    s.onload = cb;
    s.onerror = function () { cb(); };
    document.head.appendChild(s);
  }

  function colorPrimario() {
    var c = getComputedStyle(document.documentElement).getPropertyValue("--primary").trim();
    return c || "#030213";
  }

  document.addEventListener("submit", function (e) {
    var f = e.target;
    if (!f || !f.dataset || !f.dataset.confirm || f.dataset.confirmado === "1") return;
    e.preventDefault();
    var enviador = e.submitter && f.contains(e.submitter) ? e.submitter : null;

    function enviar() {
      f.dataset.confirmado = "1";
      if (f.requestSubmit) f.requestSubmit(enviador || undefined);
      else f.submit();
    }

    cargarSwal(function () {
      if (!window.Swal) {
        if (window.confirm(f.dataset.confirm)) enviar();
        return;
      }
      var peligro = f.dataset.confirmTipo === "peligro";
      Swal.fire({
        title: f.dataset.confirmTitulo || "¿Estás seguro?",
        text: f.dataset.confirm,
        icon: peligro ? "warning" : "question",
        showCancelButton: true,
        confirmButtonText: f.dataset.confirmOk || "Sí, continuar",
        cancelButtonText: "Volver",
        confirmButtonColor: peligro ? "#dc2626" : colorPrimario(),
        cancelButtonColor: "#6b7280",
        reverseButtons: true,
        focusCancel: peligro
      }).then(function (r) {
        if (r.isConfirmed) enviar();
      });
    });
  }, true);
})();
