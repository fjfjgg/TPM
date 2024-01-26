#!/bin/sh

set_server()
{
    #Add ssh: or scp: if needed
    if [ "${1%%:*}" != "$1" ]; then 
        SCP_SERVER="scp://$1/"
        SSH_SERVER="ssh://$1"
        SSH_URL=true
    else
        SCP_SERVER="$1:"
        SSH_SERVER="$1"
    fi
}

#Initial values
LOGFILE=data/corrector.log
nota=0
SCP_SERVER=
SSH_SERVER=
SSH_URL=
ERROR_CORRECTOR=111

#LOAD VARIABLES
#check for the existence of required variables.
# if not try to load file stored in GEN_SSH_TOOL_VARS
# if it doesn't exist try to load from GenSshToolVars.sh
# if mandatory variables do not exist, give an error and exit
if [ -z "$TPMfilepath" ]; then
    if [ "$GEN_SSH_TOOL_VARS" ]; then
        . "$GEN_SSH_TOOL_VARS"
    elif [ -f ./GenSshToolVars.sh ]; then
        . ./GenSshToolVars.sh
    fi
fi

if [ -z "$TPMfilepath" ]; then
	echo ERROR DATA
	exit $ERROR_CORRECTOR;
fi
#if key file exists
if [ "$TPMsshkey" ]; then
    SSH_OPTIONS="-i $TPMsshkey"
    SCP_OPTIONS="-i $TPMsshkey" 
else
    SSH_OPTIONS=   
    SCP_OPTIONS= 
fi
printf "%s\t%s\t%s\t%s\tSTART\n" "$(date -Ins)" "$TPMfilepath" "$TPMconsumerid" "$TPMfilename" >> "$LOGFILE"

if [ "$TPMconsumerid" = "$TPMspecialuser" ]; then
    if echo "$TPMspecialfile" | grep "$TPMfilename" -q ; then
        #special file
        #copy files
        for s in $TPMservers; do
            set_server "$s"
            echo "<div style='text-align: left'><h3>Sending special file to $s</h3>"
            if scp $SCP_OPTIONS -oConnectTimeout=5 "$TPMfilepath" "${SCP_SERVER}${TPMremotefolder}/${TPMfilename}" 2>&1; then
                if ssh $SSH_OPTIONS "$SSH_SERVER" "$TPMspecialcorrector" 2>&1; then
                    echo "<p>OK</p>"
                else
                    echo "<p>Error</p>"
                fi
            else
                echo "<p>KO</p>"
            fi
	    echo "</div>"
        done
        exit 0
    fi
fi

#check active server
server=""
for s in $TPMservers; do
    echo "<p>Testing server $s..." 1>&2
    set_server "$s"
    #call corrector with "test" to see what works
    if ssh $SSH_OPTIONS -oBatchMode=yes -oConnectTimeout=1 "$SSH_SERVER" "$TPMcorrector test"; then
        echo "OK" 1>&2
        server=$s
        break
    else
        echo "KO" 1>&2
    fi
done
if [ "$server" ]; then
    set_server "$server"
    echo "<p>Using server $server.</p>" 1>&2
    printf "%s\t%s\t%s\t%s\tUSING\t%s\n" "$(date -Ins)" "$TPMfilepath" "$TPMconsumerid" "$TPMfilename" "$server" >> "$LOGFILE"
    #the file to be delivered is copied, we use the base name
    fileremote="$TPMremotefolder"/$(basename "$TPMfilepath")
    if [ "$SSH_URL" ]; then
    	scppath="$TPMremotefolder"/"$TPMfileurlencoded"
    else
    	scppath="$fileremote"
    fi
    if scp $SCP_OPTIONS -q "$TPMfilepath" "${SCP_SERVER}${scppath}" 2>&1; then
        #execute corrector
        ssh $SSH_OPTIONS "$SSH_SERVER" "$TPMcorrector \"$fileremote\" \"$TPMconsumerid\" \"$TPMfilename\" $TPMcounter \"$TPMinstructor\" $TPMextraargs"
    fi
    nota=$?
else
    echo "<p>El servidor de corrección no está disponible. Inténtelo más tarde.</p>"
    echo "<p>The correction server is not available. Please try again later.</p>"
    #should not count as attempt
    nota=$ERROR_CORRECTOR
fi

printf "%s\t%s\t%s\t%s\tEND\t%s\n" "$(date -Ins)" "$TPMfilepath" "$TPMconsumerid" "$TPMfilename" "$nota" >> "$LOGFILE"

exit $nota
