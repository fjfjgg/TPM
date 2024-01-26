#Mandatory

TPMfilepath=/absolute/path/file.ext
TPMconsumerid=consumer_user_id
TPMfilename=file.ext
TPMcounter=1
TPMinstructor=true
#The path must be absolute if the port of a server is not 22
# there the files will be copied to the server.
TPMremotefolder="/tmp"
#Normal send, server executable, should support 2 call types
# $TPMcorrector test   #and return 0
# $TPMcorrector $fileremote $TPMconsumerid $TPMfilename $TPMcounter $TPMinstructor
TPMcorrector="~/user/corrector"
#Space-separated list of server names
TPMservers="user1@server1 user2@server2 user3@server3:port"
# if port is 22, servers can be written to as user@server, otherwise as
# user@server:port, and add ssh:// or scp:// according to the command
# and the name of the file when copying it must be encoded (URLencoded)
TPMfileurlencoded=file.ext

#Optional

#Private key file to make the connection, password access is not allowed.
# If empty, the system default keys (shared by all projects)
TPMsshkey=id.key
#These will cause the file to be sent to all servers. They may be empty.
# solo se permite a un usuario y un nombre de fichero
TPMspecialuser="test"
TPMspecialfile="update.zip"
#The specialcorrector will be called with no arguments, that program must know
# that the delivered file is in the path $TPMremotefolder/$TPMspecialfile
TPMspecialcorrector="~/user/updater"
TMPextraargs=
