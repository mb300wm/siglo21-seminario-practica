# SIGTOA

Sistema de Gestión y Trazabilidad de Operaciones Agrícolas. Prototipo en Java del TP3 de Seminario de Práctica de Informática (Universidad Siglo 21).

Incluye: iniciar sesión (CU-01), registrar siembras (CU-02) y cosechas (CU-03), registrar órdenes de trabajo (CU-04) con sus aplicaciones de insumos (CU-05) y consultar la trazabilidad por campaña (CU-07).

## Requisitos

- **JDK 21** o superior.
- **MySQL 8.0** y **MySQL Workbench**.
- **Conexión a internet** la primera vez que se ejecuta.

No hace falta instalar Maven. El proyecto incluye el wrapper `mvnw`, que en la primera ejecución descarga Maven y las dependencias: JavaFX 21, MySQL Connector/J, Password4j y JUnit 5.

## Ejecución

1. En MySQL Workbench, ejecutar `db/01_schema.sql` y luego, una sola vez, `db/02_datos_de_prueba.sql`. El primer script también crea el usuario de MySQL `sigtoa_app` con el que se conecta la aplicación.
2. Desde la carpeta del proyecto, ejecutar:

   ```
   .\mvnw.cmd javafx:run
   ```

   En Linux o macOS: `./mvnw javafx:run`.
3. Ingresar con el usuario `usuario_demo` y la contraseña `sigtoa-demo-2026`.

## Pruebas

```
.\mvnw.cmd verify
```
