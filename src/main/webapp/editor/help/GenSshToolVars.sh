#Obligatorios

TPMfilepath=/ruta/absoluta/fichero.ext
TPMconsumerid=uvus
TPMfilename=fichero.ext
TPMcounter=1
TPMinstructor=true
#La ruta debe ser absoluta si el puerto de un servidor no es el 22
# ahí se copiarán los ficheros en el servidor.
TPMremotefolder="/tmp"
#Envio normal, ejecutable del servidor, debe admitir 2 tipos de llamada
# - $TPMcorrector test   #y devolver 0
# - $TPMcorrector $fileremote $TPMconsumerid $TPMfilename $TPMcounter $TPMinstructor
TPMcorrector="~/usuario/corrector"
#Lista de nombre de servidores separados por espacios
TPMservers="usuario1@servidor1 usuario2@servidor2 //usuario3@servidor3:puerto"
# si el puerto es el 22, los servidores se pueden escribir como usuario@servidor, 
# si no como //usuario@servidor:puerto y se le añade delante ssh o scp según el comando

#Opcionales

#Fichero de clave privada para hacer la conexión, no se permite acceso con clave
# si está vacía las claves por defecto del sistema (compartida por todos los proyectos)
TPMsshkey=id.key
#Esto provocará que se envíen a todos los servidores. Pueden estar vacíos.
# solo se permite a un usuario y un nombre de fichero
TPMspecialuser="test"
TPMspecialfile="update.zip"
#El specialcorrector se llamará sin argumentos, ese programa debe saber que el
# archivo entregado está en la ruta $TPMremotefolder/$TPMspecialfile
TPMspecialcorrector="~/usuario/updater"
