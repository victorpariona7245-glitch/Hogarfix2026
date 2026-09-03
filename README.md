# HOGARFIX

**HOGARFIX** es una plataforma web desarrollada con el objetivo de **conectar usuarios que requieren servicios técnicos para el hogar** con **profesionales calificados** (técnicos en electricidad, plomería, gasfitería, mantenimiento, entre otros).  
El sistema busca optimizar la gestión de solicitudes, asignación de técnicos y seguimiento del servicio brindado, garantizando **rapidez, transparencia y confianza** en cada atención.

---

## Objetivo del Proyecto

Brindar una **solución digital integral** que permita a los usuarios publicar solicitudes de servicios domésticos y recibir asistencia técnica de manera eficiente, confiable y segura.  
Además, ofrece una interfaz moderna y una arquitectura escalable que puede crecer con las necesidades del negocio.

---

## Características Principales

- 🧾 **Registro y autenticación** de usuarios y técnicos.  
- 📋 **Gestión de solicitudes** (crear, editar, aceptar, rechazar).  
- 🧰 **Asignación automática o manual** de técnicos según disponibilidad o calificación.  
- 💬 **Sistema de comunicación interna** entre cliente y técnico.  
- ⭐ **Valoración del servicio** al finalizar la atención.  
- 🔐 **Control de roles y permisos** para usuarios y técnicos.  
- 📊 **Panel de administración** con métricas del sistema.

---

## Tecnologías Utilizadas

| Tipo | Tecnologías |
|------|--------------|
| **Backend** | Java 21, Spring Boot 3.5.6 (Spring Web, Spring Data JPA, Spring Security, Spring Mail, Spring Actuator) |
| **Frontend** | HTML5, CSS3, JavaScript, Thymeleaf, Tailwind CSS (CDN) |
| **Base de Datos** | MySQL (producción) / H2 (pruebas en memoria) |
| **Mapeo de Objetos** | ModelMapper |
| **Documentación API** | Swagger / OpenAPI (springdoc-openapi) |
| **Otras librerías** | Lombok, Apache Commons Math3, Apache POI (Excel) |
| **Control de Versiones** | Git + GitHub |
| **Contenerización** | Docker |
| **Herramientas de Desarrollo** | Visual Studio Code |

---

## Arquitectura del Proyecto
El proyecto se basa en una arquitectura **modular** con separación clara entre capas:

- `config` → configuraciones (seguridad, CORS, inicialización de datos)
- `controller` → endpoints REST
- `dto` → objetos de transferencia de datos
- `mapper` → conversión entre entidades y DTOs
- `model` → entidades JPA
- `repository` → acceso a datos
- `scheduler` → tareas programadas
- `service` → lógica de negocio