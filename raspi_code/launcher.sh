#!/bin/sh

#launcher.sh
#this will launch the init_vehicle.py script and then the bt_comm.py script 

cd /
cd home/pi/Documents/GPS/module_2/raspi_code/
python init_vehicle.py &
wait $!  			#wait for the init file to run to completion


while true;
do
	echo "waiting for google"
	ping -c1 google.com
	if [ $? -eq 0 ] 
	then
		echo "launching bt_comm.py"
		python bt_comm.py & 		#launch this is a background process as not to block
		cd /
		break
	fi
done
