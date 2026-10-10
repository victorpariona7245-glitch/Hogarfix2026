// Prepara las tablas para verse como tarjetas en celular (pone la etiqueta de cada columna)
(function () {
  function init() {
    document.querySelectorAll("table.table-clean").forEach(function (t) {
      var heads = Array.prototype.map.call(t.querySelectorAll("thead th"), function (th) {
        return th.textContent.trim();
      });
      t.querySelectorAll("tbody tr").forEach(function (tr) {
        Array.prototype.forEach.call(tr.children, function (td, i) {
          if (td.tagName !== "TD" || td.dataset.listo) return;
          td.dataset.listo = "1";
          td.setAttribute("data-label", heads[i] || "");
          var w = document.createElement("div");
          w.className = "td-v";
          while (td.firstChild) w.appendChild(td.firstChild);
          td.appendChild(w);
        });
      });
    });
  }
  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", init);
  else init();
})();
