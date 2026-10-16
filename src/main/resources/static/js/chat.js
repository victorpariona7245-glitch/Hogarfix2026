/*
 * Chat flotante cliente ↔ técnico (consulta al servidor cada pocos segundos).
 * Se usa con el bloque #chat-widget de las páginas del cliente y del técnico.
 * Los botones "Chat" llaman a abrirChatFlotante(idServicio, nombreDelContacto).
 */
(function () {
  var raiz = document.getElementById("chat-widget");
  if (!raiz) return;

  var MI_ROL = raiz.dataset.rol || "CLIENTE"; // CLIENTE o TECNICO
  var panel = document.getElementById("chat-panel");
  var burbuja = document.getElementById("chat-burbuja");
  var badge = document.getElementById("chat-badge");
  var lista = document.getElementById("chat-mensajes");
  var titulo = document.getElementById("chat-titulo");
  var entrada = document.getElementById("chat-entrada");
  var enviarBtn = document.getElementById("chat-enviar");
  var aviso = document.getElementById("chat-aviso");

  var activoId = null;
  var activoNombre = "";
  var abierto = false;
  var ultimaClave = "";
  var ultimoVisto = 0; // id del último mensaje ajeno que ya vio la persona
  var sinLeer = 0;
  var timer = null;
  var enviando = false;

  // ---------- utilidades ----------
  function csrfHeaders(extra) {
    var h = Object.assign({}, extra || {});
    var t = document.querySelector('meta[name="_csrf"]');
    var n = document.querySelector('meta[name="_csrf_header"]');
    if (t && n) h[n.content] = t.content;
    return h;
  }
  function esc(t) {
    var d = document.createElement("div");
    d.textContent = t;
    return d.innerHTML;
  }
  function guardar() {
    try {
      if (activoId) sessionStorage.setItem("chatActivo", JSON.stringify({ id: activoId, nombre: activoNombre, visto: ultimoVisto }));
      else sessionStorage.removeItem("chatActivo");
    } catch (e) {}
  }
  function mostrarAviso(texto) {
    if (!aviso) return;
    aviso.textContent = texto || "";
    aviso.style.display = texto ? "block" : "none";
  }
  function pintarBadge() {
    if (!badge) return;
    badge.textContent = sinLeer > 9 ? "9+" : String(sinLeer);
    badge.style.display = sinLeer > 0 && !abierto ? "block" : "none";
  }
  function pintarVisibilidad() {
    panel.style.display = activoId && abierto ? "block" : "none";
    burbuja.style.display = activoId && !abierto ? "flex" : "none";
    pintarBadge();
  }

  // ---------- consulta periódica ----------
  function programar() {
    if (timer) clearTimeout(timer);
    if (!activoId) return;
    var espera = document.hidden ? 20000 : abierto ? 4000 : 10000;
    timer = setTimeout(function () { cargar(false); }, espera);
  }

  function cargar(forzarBajar) {
    if (!activoId) return Promise.resolve();
    var id = activoId;
    return fetch("/api/mensajes/" + id, { credentials: "same-origin" })
      .then(function (r) {
        if (id !== activoId) return null;
        if (r.status === 401) { mostrarAviso("Tu sesión expiró. Vuelve a iniciar sesión."); return null; }
        if (r.status === 403 || r.status === 404) { mostrarAviso("No tienes acceso a esta conversación."); return null; }
        if (!r.ok) { mostrarAviso("No se pudo cargar el chat. Reintentando…"); return null; }
        mostrarAviso("");
        return r.json();
      })
      .then(function (mensajes) {
        if (mensajes) procesar(mensajes, forzarBajar);
      })
      .catch(function () { mostrarAviso("Sin conexión. Reintentando…"); })
      .then(programar);
  }

  function procesar(mensajes, forzarBajar) {
    var ultimoId = mensajes.length ? mensajes[mensajes.length - 1].id : 0;
    var ajenos = mensajes.filter(function (m) { return m.autor !== MI_ROL; });
    var ultimoAjeno = ajenos.length ? ajenos[ajenos.length - 1].id : 0;

    if (abierto) {
      ultimoVisto = Math.max(ultimoVisto, ultimoAjeno);
      sinLeer = 0;
    } else {
      sinLeer = ajenos.filter(function (m) { return m.id > ultimoVisto; }).length;
    }
    guardar();
    pintarBadge();

    var clave = mensajes.length + ":" + ultimoId;
    if (clave === ultimaClave) return; // nada nuevo: no se vuelve a dibujar
    ultimaClave = clave;
    dibujar(mensajes, forzarBajar);
  }

  function dibujar(mensajes, forzarBajar) {
    var cercaDelFinal = lista.scrollHeight - lista.scrollTop - lista.clientHeight < 80;
    if (!mensajes.length) {
      lista.innerHTML = '<p style="font-size:12px;color:#94a3b8;text-align:center;margin-top:24px;">Aún no hay mensajes. ¡Escribe el primero!</p>';
      return;
    }
    var hoy = new Date();
    var hoyTxt = ("0" + hoy.getDate()).slice(-2) + "/" + ("0" + (hoy.getMonth() + 1)).slice(-2);
    var diaAnterior = "";
    var html = "";
    mensajes.forEach(function (m) {
      var partes = (m.fecha || "").split(" ");
      var dia = partes[0] || "";
      var hora = partes[1] || "";
      if (dia !== diaAnterior) {
        diaAnterior = dia;
        html += '<div style="text-align:center;margin:10px 0 6px;"><span style="font-size:11px;color:#64748b;background:#e2e8f0;padding:2px 10px;border-radius:999px;">' +
          esc(dia === hoyTxt ? "Hoy" : dia) + "</span></div>";
      }
      var mio = m.autor === MI_ROL;
      html += '<div style="display:flex;justify-content:' + (mio ? "flex-end" : "flex-start") + ';margin-bottom:6px;">' +
        '<div style="max-width:78%;padding:7px 11px;border-radius:' + (mio ? "14px 14px 3px 14px" : "14px 14px 14px 3px") + ';font-size:14px;line-height:1.35;white-space:pre-wrap;word-break:break-word;' +
        (mio ? "background:#030213;color:#fff;" : "background:#fff;color:#111;border:1px solid #e2e8f0;") + '">' +
        esc(m.contenido) +
        '<div style="font-size:10px;opacity:.65;margin-top:3px;text-align:right;">' + esc(hora) + "</div></div></div>";
    });
    lista.innerHTML = html;
    if (forzarBajar || cercaDelFinal) lista.scrollTop = lista.scrollHeight;
  }

  // ---------- acciones ----------
  function abrir(id, nombre) {
    var cambio = String(id) !== String(activoId);
    activoId = id;
    activoNombre = nombre || "Chat";
    titulo.textContent = activoNombre;
    abierto = true;
    if (cambio) {
      ultimaClave = "";
      ultimoVisto = 0;
      sinLeer = 0;
      lista.innerHTML = '<p style="font-size:12px;color:#94a3b8;text-align:center;margin-top:24px;">Cargando…</p>';
    }
    mostrarAviso("");
    pintarVisibilidad();
    guardar();
    cargar(true);
    if (window.innerWidth > 640) setTimeout(function () { entrada.focus(); }, 50);
  }

  function minimizar() {
    abierto = false;
    pintarVisibilidad();
    programar();
  }

  function cerrar() {
    activoId = null;
    abierto = false;
    if (timer) clearTimeout(timer);
    guardar();
    pintarVisibilidad();
  }

  function alternar() {
    if (!activoId) return;
    abierto = !abierto;
    if (abierto) {
      sinLeer = 0;
      pintarVisibilidad();
      cargar(true);
    } else {
      pintarVisibilidad();
    }
  }

  function enviar() {
    if (!activoId || enviando) return;
    var texto = entrada.value.trim();
    if (!texto) return;
    enviando = true;
    enviarBtn.disabled = true;
    fetch("/api/mensajes/" + activoId, {
      method: "POST",
      headers: csrfHeaders({ "Content-Type": "application/json" }),
      credentials: "same-origin",
      body: JSON.stringify({ contenido: texto }),
    })
      .then(function (r) {
        if (r.ok) {
          entrada.value = "";
          autoAltura();
          mostrarAviso("");
          return cargar(true);
        }
        return r.json().catch(function () { return {}; }).then(function (j) {
          mostrarAviso(j.error || (r.status === 401 ? "Tu sesión expiró. Vuelve a iniciar sesión." : "No se pudo enviar el mensaje."));
        });
      })
      .catch(function () { mostrarAviso("Sin conexión: tu mensaje no se envió."); })
      .then(function () { enviando = false; enviarBtn.disabled = false; entrada.focus(); });
  }

  function autoAltura() {
    entrada.style.height = "auto";
    entrada.style.height = Math.min(entrada.scrollHeight, 96) + "px";
  }

  // ---------- eventos ----------
  enviarBtn.addEventListener("click", enviar);
  entrada.addEventListener("input", autoAltura);
  entrada.addEventListener("keydown", function (e) {
    if (e.key === "Enter" && !e.shiftKey) { e.preventDefault(); enviar(); }
  });
  document.getElementById("chat-minimizar").addEventListener("click", minimizar);
  document.getElementById("chat-cerrar").addEventListener("click", cerrar);
  burbuja.addEventListener("click", alternar);
  document.addEventListener("visibilitychange", function () { if (!document.hidden && activoId) cargar(false); });

  // Funciones que usan los botones "Chat" de las páginas
  window.abrirChatFlotante = abrir;
  window.minimizarChatFlotante = minimizar;
  window.toggleChatFlotante = alternar;

  // ---------- al cargar la página ----------
  function inicio() {
    var q = new URLSearchParams(location.search);
    var pedido = q.get("chat"); // viene del aviso de la campanita
    if (pedido) {
      var btn = document.querySelector('[data-servicio-id="' + pedido + '"]');
      var nombre = btn && btn.dataset.nombre ? btn.dataset.nombre : "Chat";
      history.replaceState(null, "", location.pathname);
      abrir(pedido, nombre);
      return;
    }
    try {
      var g = JSON.parse(sessionStorage.getItem("chatActivo") || "null");
      if (g && g.id) {
        activoId = g.id;
        activoNombre = g.nombre || "Chat";
        ultimoVisto = g.visto || 0;
        titulo.textContent = activoNombre;
        abierto = false; // vuelve minimizado, con su contador de mensajes nuevos
        pintarVisibilidad();
        cargar(false);
      }
    } catch (e) {}
  }
  inicio();
})();
