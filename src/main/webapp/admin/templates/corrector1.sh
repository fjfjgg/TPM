#!/bin/sh

# Ejemplo de corrector local con shell script.

# También es posible subir binarios u otros tipos
# de ejecutables.

# Advertencia, NO edite esto en la web si el fin
# de línea es importante (súbalo como fichero).
# Es posible que si edita esto en la web, 
# el fin de línea de unix \n se convierta a \r\n,
# dando fallos al ejecutar posteriormente el script.
# Con python el siguiente shebang admite \n o \r\n:
#!/usr/bin/env -S python3 -W " "

echo "Corrector local: " $@
exit 100
