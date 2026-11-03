# SIGTOA

Sistema de Gestión y Trazabilidad de Operaciones Agrícolas. Prototipo en Java del TP4 de Seminario de Práctica de Informática (Universidad Siglo 21).

Incluye: iniciar sesión (CU-01), registrar siembras (CU-02) y cosechas (CU-03), registrar órdenes de trabajo (CU-04) con sus aplicaciones de insumos (CU-05) y consultar la trazabilidad por campaña (CU-07). El reporte de trazabilidad se puede exportar a un archivo CSV y volver a abrir sin conexión a MySQL.

## Diseño

La aplicación sigue la arquitectura MVC y usa el patrón Repository para la persistencia: vista FXML → `controller` → `service` → `repository` → MySQL. Todo el SQL está en el paquete `repository`.

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

## Otra conexión a MySQL

Por defecto, la aplicación se conecta a `localhost:3306/agro` con el usuario `sigtoa_app`. Para usar otro servidor, usuario o contraseña, copiar `config/db.example.properties` como `config/db.local.properties` y modificarlo. Ese archivo no se sube al repositorio.
