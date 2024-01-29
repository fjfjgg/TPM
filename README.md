# TPM

Tool Provider Manager, a LTI v1.1 tool provider for managing non-LTI tools.

# Índice

 1. [Descripción](#descripcion)
 2. [Estado del proyecto](#estado-del-proyecto)
 3. [Tecnologías usadas](#tecnologías-usadas)
 4. [Compilación](#compilación)
 5. [Instalación](#instalación)
 6. [Ejemplos de uso](#ejemplos-de-uso)
 7. [Pendiente por hacer](#pendiente-por-hacer-to-do)
 8. [Contribuciones](#contribuciones)
 9. [Licencia](#licencia)
10. [Contacto](#contacto)
11. [Referencias](#referencias)

# Descripción

Tool Provider Manager, o TPM para abreviar, implementa un proveedor de herramientas (TP) [LTI v1.1](https://www.imsglobal.org/specs/ltiv1p1/implementation-guide). Un TP proporciona herramientas externas a una plataforma de enseñanza virtual LMS (Learning Management System) o consumidor de herramientas (TC) de tal manera que se comparten las credenciales de los usuarios (mediante [OAuth 1.0 Protocol](https://datatracker.ietf.org/doc/html/rfc5849)) y puede registrar calificaciones en el libro de calificaciones del LMS ([LTI Basic Outcomes](https://www.imsglobal.org/spec/lti-bo/v1p1)).

TPM es un gestor de herramientas externas que no soportan LTI v1.1 y que por sí solas no podrían interactuar con el LMS. TPM permite reutilizar herramientas de evaluación principalmente que no estaban pensadas para ejecutarse online o hacer nuevas herramientas fácilmente sin tener que preocuparse por implementar LTI v1.1.

Actualmente se soportan 2 tipos de "herramientas" que se pueden conectar al TPM:

* Herramientas de corrección que reciben un fichero suministrado por el estudiante y devuelven un resultado numérico (0-100) y contenido de texto o HTML a mostrar como resultado del procesamiento realizado. El resultado numérico puede enviarse al LMS para que quede visible en el libro de calificaciones.
* Aplicaciones web que quieren conectarse con el LMS para utilizar las credenciales y datos de este sin necesidad de usar el estándar LTI v1.1 y que no desean devolver nada al LMS.

En el siguiente diagrama se muestra la interacción de los distintos elementos:

```
                                   +-------+
                           .------>|TOOL_1 |
 +-------+      +-------+  |       +-------+
 |       |      |       |<-´
 |       |----->|       |          +-------+
 |  TC   |      |  TP   |<-------->|TOOL_2 |
 |       |      |       |<------.  +-------+
 |  LMS  |      |  TPM  |       |    ...
 |       |<-----|       |-----. |  +-------+
 |       |      |       |--.  | `->|TOOL_N |
 +-------+      +-------+  |  |    +-------+
                           |  |
                           |  |    +---------+
                           |  `--->|WEBAPP_1 |
                           |       +---------+
                           |         ...
                           |       +---------+
                           `------>|WEBAPP_N |
                                   +---------+
```

Se han desarrollado varios prototipos previos, pero esta es una implementación totalmente nueva e independiente.

TPM proporciona 2 interfaces diferentes:

* Interfaz con el LMS.
* Interfaz de gestión. Los usuarios acceden a esta interfaz con credenciales diferentes a las del LMS y no tienen que ser usuarios del LMS. Existen 4 tipos de usuarios:
  * Super usuarios: pueden crear otros usuarios, cambiar la configuración de la aplicación y realizar operaciones de mantenimiento.
  * Administradores de herramientas: crean herramientas *virtuales* y configuran la conexión con las herramientas y aplicaciones web externas y les asignan identificadores (claves) y secretos únicos. Pueden asignar otros usuarios a estas herramientas.
  * Editores: editan la configuración de herramientas que han creado otros administradores.
  * Probadores: solo pueden hacer pruebas de las herramientas que tienen asignadas

  Los administradores también pueden editar y probar, y los editores también pueden probar las herramientas.

TPM distingue la herramienta que debe ejecutar cuando llega una petición del LMS a partir del identificador (clave) y secreto que envía el LMS. Esto debe haberse configurado previamente.

TPM proporciona unas funcionalidades comunes a las herramientas:

* La gestión de ficheros enviados y los resultados obtenidos.
* Información de los intentos realizados por los estudiantes.

Una misma herramienta puede ser reutilizada en distintos cursos o plataformas de enseñanza virtual. TPM separa los estudiantes e intentos realizados según el identificador/clave utilizado. Una misma herramienta puede tener asignados distintos pares identificadores/clave para distinguir varios grupos de usuarios.

### Motivación

Esta aplicación se ha desarrollado principalmente para:

* Corregir automáticamente los ficheros entregados por los estudiantes proporcionando realimentación instantánea. Los LMS soportan corrección automática de algunos tipos de preguntas de exámenes, pero no todos.
* Hacer un procesamiento previo de los ficheros entregados para informar de errores comunes que no sean específicos de la materia a evaluar. Esto permite al estudiante corregir su entrega para que cumpla los requisitos mínimos y al profesor no tener que perder tiempo en aspectos formales.
* Reutilizar correctores que el profesor solía ejecutar manualmente después de descargar todos los ficheros entregados.
* Evitar crear aplicaciones web que tengan que duplicar la gestión de usuarios que ya hace las plataformas de enseñanza virtual. El acceso a las aplicaciones web puede restringirse a estudiantes y profesores seleccionados sin necesidad de dar de alta previamente a los usuarios y con garantías de autenticidad.

### Funcionalidades

La funcionalidad que proporciona TPM depende de si la herramienta es de corrección o es una aplicación web (modo *redirección*).

#### Herramientas de corrección

Existen los siguientes tipos de herramientas de corrección:

1. Tipo desconocido: se usa para hacer pruebas.
2. Local: herramienta que se ejecuta en la misma máquina que el servidor. Solo permitida para determinados administradores por las implicaciones de seguridad y porque puede afectar al rendimiento del servidor.
3. Por SSH: herramienta que se ejecutará a través de SSH. Permite configurar varios servidores y en envío alternativo (round-robin) a ellos. Incorpora mecanismos para la actualización remota de la herramienta a través de la interfaz del LMS.
4. Por HTTP: herramienta que se ejecutará a través de HTTP. Permite configurar qué tipo de petición HTTP se enviará, sus cabeceras y parámetros y cómo se procesará la respuesta identificando la calificación y el mensaje que se debe mostrar al usuario.
5. Solo almacenamiento: no se ejecuta ninguna herramienta de corrección externa y simplemente se utiliza las capacidades incorporadas de TPM para almacenar y gestionar entregas.

La interfaz LMS mínima común a todas las herramientas presenta una descripción personalizable, un formulario para enviar un fichero o texto y un espacio para mostrar el resultado. Si el usuario es un profesor también se muestra información sobre la herramienta. Opcionalmente se puede activar:

- Una clave de entrega. El usuario debe conocerla para poder hacer una entrega.
- Mostrar intentos anteriores. Un estudiante opcionalmente puede ver los ficheros/textos enviados por él y/o su resultado. Un profesor también puede ver los intentos de otros usuarios, borrar intentos, obtener información del usuario y volver a realizar la corrección como profesor (la herramienta de corrección puede realizar distintas acciones si el usuario es un estudiante o un profesor). Los intentos se pueden filtrar y hacer una descarga masiva de todos.
- Requisitos temporales de funcionamiento y visualización. Si queda poco tiempo para que la herramienta deje de admitir nuevos intentos al usuario se le mostrará una cuenta atrás.
- Requisitos al nombre y tamaño del fichero a entregar.
- Restricciones al número de usuarios que pueden enviar intentos simultáneamente.
- El almacenamiento de los ficheros y/o resultados.
- Restricciones al número de intentos máximos que un usuario puede realizar.
- Habilitar un cuadro de texto adicional para que el profesor pueda enviar comandos a la herramienta.
- En envío de calificaciones al libro de calificaciones del LMS.

Las herramientas locales, por SSH y por HTTP reciben como parámetros:

1. El fichero enviado. Si se elige por pedir texto, este se almacena en un fichero.
2. Un identificador del usuario que realiza el intento.
3. El nombre original del fichero.
4. Un contador único que se incremente en cada intento.
5. Si el usuario es un profesor.

Opcionalmente se pueden enviar más parámetros como el nombre del curso o enlace, el idioma, valores estáticos, etc.

#### Modo redirección

El modo redirección se basa en [OAuth 2.0 Client Credentials Grant Type](https://oauth.net/2/grant-types/client-credentials/), aunque no está limitado a ese modo de funcionamiento. La idea se basa en:

1. Enviar una petición a la aplicación web desde TPM. Esta petición llevará datos del usuario y desde dónde se está iniciando el intento de manera autenticada. La aplicación web genera un token y una URL para continuar. La petición también puede ser enviada por una herramienta local o por SSH.
2. Enviar una segunda petición desde el navegador del usuario a la nueva URL con el token generado anteriormente. La aplicación web recupera los datos asociados a ese token y permite el acceso.
3. Si fuera necesario, se realiza el navegador se redirige a una nueva URL para evitar problemas con [CORS](https://developer.mozilla.org/es/docs/Web/HTTP/CORS). El usuario a partir de este momento comenzaría a usar la aplicación, sin necesidad de haberse registrado ni pasar información de manera manual.

La aplicación web puede funcionar de manera diferente dependiendo si el usuario es profesor o estudiante y del lugar de procedencia del intento.

## Estado del proyecto

Funciona correctamente.

## Tecnologías usadas

* Java
* Servlets de Jakarta EE
* JSP

## Compilación

Se requiere tener instalado [maven](https://maven.apache.org/).

Simplemente hay que ejecutar:

```shell
mvn package
```

## Instalación

Cree una base de datos usando uno de los scripts SQL que se encuentran en `src/scripts/sql`. Se soporta SQLite, PostgreSQL y MariaDB.

Debe instalar el `war` generado en un servidor de aplicaciones Jakarta EE 10. El servidor solo necesita implementar el *Jakarta EE Web Profile*, como por ejemplo [Tomcat 10.1](https://tomcat.apache.org/download-10.cgi). Debe configurar el recurso a la base de datos en el contexto o globalmente con el nombre `jdbc/ltidb`. Por ejemplo, en Tomcat 10 puede crear el siguiente XML de configuración (`tpm.xml`) para una base de datos SQLite:

````xml
<?xml version="1.0" encoding="UTF-8"?>
<Context reloadable="true" >
        <Resource name="jdbc/ltidb"
                    auth="Container"
                    type="javax.sql.DataSource"
                    driverClassName="org.sqlite.JDBC"
                    url="jdbc:sqlite:/path/tpm.db/"
                username=""
                password=""
                    maxTotal="1"
                    maxIdle="1"
                    maxWaitMillis="-1" />
        <CookieProcessor sameSiteCookies="strict" />
</Context>
````

Arranque el servidor.

Posteriormente, debe acceder a la ruta de contexto de la aplicación una vez desplegada y entrar con el usuario y clave `super`. Cambie la contraseña a otra más segura y cree nuevos usuarios.

## Ejemplos de uso

* [TPM-sshtool-template](https://github.com/fjfjgg/TPM-sshtool-template) es un ejemplo de herramienta de corrección accesible a través de SSH.
* [demo-oauth-lti](https://github.com/fjfjgg/demo-oauth-lti) es una aplicación de intercambio de mensajes integrada con un LMS. Es un ejemplo de una integración de una aplicación web con TPM usando HTTP para el intercambio de datos inicial.
* [TPM-httptool-template](https://github.com/fjfjgg/TPM-httptool-template) es un ejemplo de herramienta de corrección accesible a través de HTTP.

## Pendiente por hacer (TO DO)

* [ ] Traducir la aplicación de gestión a múltiples idiomas. La interfaz con el LMS está traducida a español e inglés.
* [ ] Crear distintos métodos de almacenamiento.
* [ ] Crear un manual de usuario.

## Contribuciones

Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are **appreciated**.

If you have a suggestion that would make this better, please fork the repo and create a pull request. You can also simply open an issue with the tag "enhancement".
Do not forget to give the project a star! Thanks again!

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## Licencia

Distributed under the GNU GENERAL PUBLIC LICENSE Version 3. See `LICENSE.txt` for more information.

## Contacto

Francisco José Fernández Jiménez - [@fjfjes](ht) - fjfj @ us.es

Project Link: <https://github.com/fjfjgg/tpm>

## Referencias

* [LTI v1.1](https://www.imsglobal.org/specs/ltiv1p1/implementation-guide)
* [LTI Basic Outcomes](https://www.imsglobal.org/spec/lti-bo/v1p1)
* [OAuth 1.0 Protocol](https://datatracker.ietf.org/doc/html/rfc5849)
* [OAuth 2.0 Client Credentials Grant Type](https://oauth.net/2/grant-types/client-credentials/)
